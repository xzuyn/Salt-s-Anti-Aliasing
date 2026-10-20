package org.betterLostItems.salts_anti_aliasing.mixin.antilag;

import org.betterLostItems.salts_anti_aliasing.SaltsAntiAliasing;
import org.betterLostItems.salts_anti_aliasing.client.antilag.VulkanAntiLag;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks LWJGL's Vulkan device creation and destruction so AMD Anti-Lag can be enabled on the device
 * Minecraft creates. {@code vkCreateDevice(...)} delegates to {@code nvkCreateDevice}, so this one hook
 * sees every way of creating a device through LWJGL.
 *
 * <p>Every handler swallows its own errors: a problem here must never reach the game.</p>
 */
@Mixin(value = VK10.class, remap = false)
public abstract class Vk10Mixin {
    private static final String CREATE_DEVICE = "nvkCreateDevice(Lorg/lwjgl/vulkan/VkPhysicalDevice;JJJ)I";
    private static final String DESTROY_DEVICE = "nvkDestroyDevice(Lorg/lwjgl/vulkan/VkDevice;J)V";

    @Inject(method = CREATE_DEVICE, at = @At("HEAD"), require = 0)
    private static void saltsAntiAliasing$beforeCreateDevice(
            VkPhysicalDevice physicalDevice,
            long pCreateInfo,
            long pAllocator,
            long pDevice,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        try {
            VulkanAntiLag.beforeCreateDevice(physicalDevice, pCreateInfo);
        } catch (Throwable throwable) {
            SaltsAntiAliasing.LOGGER.warn("AMD Anti-Lag could not inspect Vulkan device creation", throwable);
        }
    }

    @Inject(method = CREATE_DEVICE, at = @At("RETURN"), cancellable = true, require = 0)
    private static void saltsAntiAliasing$afterCreateDevice(
            VkPhysicalDevice physicalDevice,
            long pCreateInfo,
            long pAllocator,
            long pDevice,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        try {
            Integer replacement = VulkanAntiLag.afterCreateDevice(
                    physicalDevice, pCreateInfo, pAllocator, pDevice, callbackInfo.getReturnValue());
            if (replacement != null) {
                callbackInfo.setReturnValue(replacement);
            }
        } catch (Throwable throwable) {
            SaltsAntiAliasing.LOGGER.warn("AMD Anti-Lag could not check Vulkan device creation", throwable);
        }
    }

    @Inject(method = DESTROY_DEVICE, at = @At("HEAD"), require = 0)
    private static void saltsAntiAliasing$beforeDestroyDevice(VkDevice device, long pAllocator, CallbackInfo callbackInfo) {
        try {
            VulkanAntiLag.beforeDestroyDevice(device);
        } catch (Throwable throwable) {
            SaltsAntiAliasing.LOGGER.warn("AMD Anti-Lag could not release the Vulkan device", throwable);
        }
    }
}
