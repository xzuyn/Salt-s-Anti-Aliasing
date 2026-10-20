package org.betterLostItems.salts_anti_aliasing.client.antilag;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.AMDAntiLag;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceAntiLagFeaturesAMD;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * Adds {@code VK_AMD_anti_lag} and its {@code antiLag} feature to a {@link VkDeviceCreateInfo} that
 * Minecraft has already filled in, and can take the change back out again.
 *
 * <p>The edit is made on the native struct, so it is visible to the {@code vkCreateDevice} call that
 * follows <em>and</em> to LWJGL's {@code new VkDevice(handle, physicalDevice, createInfo)}, which reads
 * the enabled extension names from the same struct to decide which functions to load.</p>
 *
 * <p>The replacement extension list, the extension name and the feature struct are deliberately not freed
 * after a successful edit: {@code VkDevice}'s constructor reads them after {@code vkCreateDevice} returns,
 * and the cost is a few dozen bytes once per device. They are freed by {@link #undo()}.</p>
 */
final class DeviceCreateInfoEdit {
    private final VkDeviceCreateInfo info;
    private final long originalPNext;
    private final PointerBuffer originalNames;
    private final PointerBuffer editedNames;
    private final ByteBuffer extensionName;
    private final VkPhysicalDeviceAntiLagFeaturesAMD feature;

    private DeviceCreateInfoEdit(
            VkDeviceCreateInfo info,
            long originalPNext,
            PointerBuffer originalNames,
            PointerBuffer editedNames,
            ByteBuffer extensionName,
            VkPhysicalDeviceAntiLagFeaturesAMD feature
    ) {
        this.info = info;
        this.originalPNext = originalPNext;
        this.originalNames = originalNames;
        this.editedNames = editedNames;
        this.extensionName = extensionName;
        this.feature = feature;
    }

    /**
     * Enables the extension and the feature on {@code info}.
     * @return the edit, or null if the caller already enabled the extension and nothing was changed
     */
    static DeviceCreateInfoEdit apply(VkDeviceCreateInfo info) {
        PointerBuffer original = info.ppEnabledExtensionNames();
        int originalCount = original == null ? 0 : original.remaining();
        for (int i = 0; i < originalCount; i++) {
            if (AMDAntiLag.VK_AMD_ANTI_LAG_EXTENSION_NAME.equals(memUTF8(original.get(i)))) {
                return null;
            }
        }

        long originalPNext = info.pNext();
        PointerBuffer names = null;
        ByteBuffer name = null;
        VkPhysicalDeviceAntiLagFeaturesAMD feature = null;
        try {
            names = memAllocPointer(originalCount + 1);
            for (int i = 0; i < originalCount; i++) {
                names.put(i, original.get(i));
            }
            name = memUTF8(AMDAntiLag.VK_AMD_ANTI_LAG_EXTENSION_NAME);
            names.put(originalCount, memAddress(name));

            // The feature struct extends VkDeviceCreateInfo, so it can simply be put at the front of
            // whatever chain Minecraft built (VkPhysicalDeviceFeatures2, Vulkan12Features, ...).
            feature = VkPhysicalDeviceAntiLagFeaturesAMD.calloc()
                    .sType$Default()
                    .pNext(originalPNext)
                    .antiLag(true);

            info.ppEnabledExtensionNames(names);
            info.pNext(feature.address());
            return new DeviceCreateInfoEdit(info, originalPNext, original, names, name, feature);
        } catch (Throwable throwable) {
            // Put the struct back exactly as it was before anything is freed.
            info.ppEnabledExtensionNames(original);
            info.pNext(originalPNext);
            free(names, name, feature);
            throw throwable;
        }
    }

    /** Restores the struct to what Minecraft built and frees what {@link #apply} allocated. */
    void undo() {
        info.ppEnabledExtensionNames(originalNames);
        info.pNext(originalPNext);
        free(editedNames, extensionName, feature);
    }

    /** The extension names as they are now, for logging and tests. */
    PointerBuffer enabledNames() {
        return info.ppEnabledExtensionNames();
    }

    private static void free(PointerBuffer names, ByteBuffer name, VkPhysicalDeviceAntiLagFeaturesAMD feature) {
        if (names != null) {
            memFree(names);
        }
        if (name != null) {
            memFree(name);
        }
        if (feature != null) {
            feature.free();
        }
    }
}
