package org.betterLostItems.salts_anti_aliasing.client.antilag;

import org.betterLostItems.salts_anti_aliasing.SaltsAntiAliasing;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.AMDAntiLag;
import org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VKCapabilitiesInstance;
import org.lwjgl.vulkan.VkAntiLagDataAMD;
import org.lwjgl.vulkan.VkAntiLagPresentationInfoAMD;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceAntiLagFeaturesAMD;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkQueue;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * AMD Anti-Lag through {@code VK_AMD_anti_lag}.
 *
 * <p>Minecraft's own code is never touched. Three LWJGL entry points are hooked instead, which is all the
 * extension needs:</p>
 * <ol>
 *   <li>{@code vkCreateDevice}: the extension and its {@code antiLag} feature have to be enabled when the
 *       device is created, long before the player can open a settings screen. So they are enabled whenever
 *       the GPU supports them, regardless of the setting. Enabling an extension nobody calls does nothing.
 *       If the driver refuses the edited request, the original request is sent again unchanged.</li>
 *   <li>{@code glfwPollEvents}: the first poll after a present is the start of a frame, just before the
 *       game samples input. That is the INPUT stage.</li>
 *   <li>{@code vkQueuePresentKHR}: the PRESENT stage, with the same frame index as the INPUT stage.</li>
 * </ol>
 *
 * <p>All of this runs on Minecraft's render thread. Nothing here is reached on OpenGL.</p>
 */
public final class VulkanAntiLag {
    /** {@code VK_MAKE_API_VERSION(0, 1, 1, 0)}. */
    private static final int VULKAN_1_1 = (1 << 22) | (1 << 12);
    private static final int NO_STAGE = -1;
    /** 0 means the driver imposes no frame rate limit of its own; Minecraft's own limiter stays in charge. */
    private static final int NO_DRIVER_FPS_LIMIT = 0;

    private static final AntiLagFrameSequencer SEQUENCER = new AntiLagFrameSequencer();
    private static final ThreadLocal<DeviceCreateInfoEdit> PENDING_EDIT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> RETRYING = new ThreadLocal<>();

    /** The device Anti-Lag is currently being used on; render thread only. */
    private static VkDevice device;
    /** Whether the driver was last told Anti-Lag is on, so turning it off sends exactly one OFF. */
    private static boolean driverTold;

    private VulkanAntiLag() {
    }

    // ---- vkCreateDevice --------------------------------------------------------------------------------

    /** Runs just before {@code vkCreateDevice}: asks for the extension if this GPU has it. */
    public static void beforeCreateDevice(VkPhysicalDevice physicalDevice, long pCreateInfo) {
        PENDING_EDIT.remove();
        if (RETRYING.get() != null || !AntiLagRuntime.hooksEnabled()) {
            return;
        }

        try {
            String problem = whyUnsupported(physicalDevice);
            if (problem != null) {
                AntiLagRuntime.setStatus(AntiLagStatus.UNSUPPORTED);
                SaltsAntiAliasing.LOGGER.info("AMD Anti-Lag is not available on this Vulkan device: {}", problem);
                return;
            }

            DeviceCreateInfoEdit edit = DeviceCreateInfoEdit.apply(VkDeviceCreateInfo.create(pCreateInfo));
            if (edit != null) {
                PENDING_EDIT.set(edit);
                AntiLagRuntime.setStatus(AntiLagStatus.REQUESTED);
                SaltsAntiAliasing.LOGGER.info("Requesting VK_AMD_anti_lag on the Vulkan device");
            }
        } catch (Throwable throwable) {
            // Device creation carries on exactly as Minecraft asked for it.
            SaltsAntiAliasing.LOGGER.warn("Could not add VK_AMD_anti_lag to Vulkan device creation", throwable);
        }
    }

    /**
     * Runs just after {@code vkCreateDevice}. If the driver rejected the request because of our edit,
     * undoes the edit and asks again, so a misbehaving driver can never stop the game from starting.
     *
     * @return the result to return instead of {@code result}, or null to keep {@code result}
     */
    public static Integer afterCreateDevice(
            VkPhysicalDevice physicalDevice,
            long pCreateInfo,
            long pAllocator,
            long pDevice,
            int result
    ) {
        DeviceCreateInfoEdit edit = PENDING_EDIT.get();
        PENDING_EDIT.remove();
        if (edit == null || result == VK10.VK_SUCCESS) {
            return null;
        }

        SaltsAntiAliasing.LOGGER.warn(
                "Vulkan device creation failed with {} after enabling VK_AMD_anti_lag; retrying without it", result);
        try {
            edit.undo();
            AntiLagRuntime.setStatus(AntiLagStatus.UNSUPPORTED);
            RETRYING.set(Boolean.TRUE);
            try {
                return VK10.nvkCreateDevice(physicalDevice, pCreateInfo, pAllocator, pDevice);
            } finally {
                RETRYING.remove();
            }
        } catch (Throwable throwable) {
            SaltsAntiAliasing.LOGGER.error("Could not retry Vulkan device creation without VK_AMD_anti_lag", throwable);
            return null;
        }
    }

    /** Why Anti-Lag cannot be requested on this device, or null if it can. */
    private static String whyUnsupported(VkPhysicalDevice physicalDevice) {
        VKCapabilitiesInstance caps = physicalDevice.getCapabilities();
        boolean core11 = caps.apiVersion >= VULKAN_1_1;
        if (!core11 && !caps.VK_KHR_get_physical_device_properties2) {
            return "the Vulkan instance is older than 1.1 and lacks VK_KHR_get_physical_device_properties2";
        }
        if (!advertisesExtension(physicalDevice)) {
            return "the driver does not list VK_AMD_anti_lag";
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceAntiLagFeaturesAMD feature = VkPhysicalDeviceAntiLagFeaturesAMD.calloc(stack)
                    .sType$Default();
            VkPhysicalDeviceFeatures2 features = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType$Default()
                    .pNext(feature.address());
            if (core11) {
                VK11.vkGetPhysicalDeviceFeatures2(physicalDevice, features);
            } else {
                KHRGetPhysicalDeviceProperties2.vkGetPhysicalDeviceFeatures2KHR(physicalDevice, features);
            }
            if (!feature.antiLag()) {
                return "the driver lists the extension but reports the antiLag feature as unsupported";
            }
        }
        return null;
    }

    private static boolean advertisesExtension(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            // A full extension list is larger than the default stack, so it lives on the heap.
            if (VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, count, null) != VK10.VK_SUCCESS
                    || count.get(0) == 0) {
                return false;
            }

            VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count.get(0));
            try {
                if (VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, count, properties) < 0) {
                    return false;
                }
                for (int i = 0; i < count.get(0); i++) {
                    if (AMDAntiLag.VK_AMD_ANTI_LAG_EXTENSION_NAME.equals(properties.get(i).extensionNameString())) {
                        return true;
                    }
                }
                return false;
            } finally {
                properties.free();
            }
        }
    }

    // ---- vkDestroyDevice -------------------------------------------------------------------------------

    /** Stops using a device before it goes away, so no call can ever reach a destroyed device. */
    public static void beforeDestroyDevice(VkDevice destroyed) {
        if (destroyed != null && destroyed == device) {
            device = null;
            driverTold = false;
            SEQUENCER.reset();
            AntiLagRuntime.setReady(false);
        }
    }

    // ---- vkQueuePresentKHR ------------------------------------------------------------------------------

    /** Runs just before {@code vkQueuePresentKHR}: the PRESENT stage of the frame that was opened earlier. */
    public static void beforePresent(VkQueue queue) {
        VkDevice queueDevice = queue.getDevice();
        if (queueDevice != device) {
            adopt(queueDevice);
        }
        if (!AntiLagRuntime.ready()) {
            return;
        }

        if (!AntiLagRuntime.requestedMode().enabled()) {
            tellDriverOff();
            return;
        }

        long frame = SEQUENCER.endFrame();
        if (frame != AntiLagFrameSequencer.NO_FRAME) {
            send(AMDAntiLag.VK_ANTI_LAG_MODE_ON_AMD, AMDAntiLag.VK_ANTI_LAG_STAGE_PRESENT_AMD, frame);
        }
    }

    /** First sight of a device: checks that the extension really was enabled on it. */
    private static void adopt(VkDevice newDevice) {
        device = newDevice;
        driverTold = false;
        SEQUENCER.reset();

        boolean enabled = newDevice.getCapabilities().VK_AMD_anti_lag;
        if (enabled) {
            AntiLagRuntime.setStatus(AntiLagStatus.SUPPORTED);
            SaltsAntiAliasing.LOGGER.info("AMD Anti-Lag is available (VK_AMD_anti_lag enabled on the Vulkan device)");
        } else {
            AntiLagRuntime.setStatus(AntiLagStatus.UNSUPPORTED);
        }
        AntiLagRuntime.setReady(enabled);
    }

    // ---- glfwPollEvents ---------------------------------------------------------------------------------

    /** The INPUT stage: called right before the game samples input for a new frame. */
    static void inputStage() {
        if (!AntiLagRuntime.requestedMode().enabled()) {
            tellDriverOff();
            return;
        }

        long frame = SEQUENCER.beginFrame();
        if (frame != AntiLagFrameSequencer.NO_FRAME) {
            send(AMDAntiLag.VK_ANTI_LAG_MODE_ON_AMD, AMDAntiLag.VK_ANTI_LAG_STAGE_INPUT_AMD, frame);
        }
    }

    // ---- vkAntiLagUpdateAMD -------------------------------------------------------------------------------

    /** Sends one OFF when the player turns Anti-Lag off, so the driver stops pacing the CPU. */
    private static void tellDriverOff() {
        if (driverTold) {
            driverTold = false;
            SEQUENCER.reset();
            send(AMDAntiLag.VK_ANTI_LAG_MODE_OFF_AMD, NO_STAGE, 0L);
        }
    }

    private static void send(int mode, int stage, long frameIndex) {
        VkDevice target = device;
        if (target == null) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAntiLagPresentationInfoAMD presentation = stage == NO_STAGE
                    ? null
                    : VkAntiLagPresentationInfoAMD.calloc(stack)
                            .sType$Default()
                            .stage(stage)
                            .frameIndex(frameIndex);
            VkAntiLagDataAMD data = VkAntiLagDataAMD.calloc(stack)
                    .sType$Default()
                    .mode(mode)
                    .maxFPS(NO_DRIVER_FPS_LIMIT)
                    .pPresentationInfo(presentation);
            AMDAntiLag.vkAntiLagUpdateAMD(target, data);
        }
        driverTold = mode == AMDAntiLag.VK_ANTI_LAG_MODE_ON_AMD;
    }
}
