package org.betterLostItems.salts_anti_aliasing.mixin.antilag;

import org.betterLostItems.salts_anti_aliasing.client.antilag.AntiLagRuntime;
import org.betterLostItems.salts_anti_aliasing.client.antilag.VulkanAntiLag;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The PRESENT stage of AMD Anti-Lag: runs right before every {@code vkQueuePresentKHR}.
 * {@code vkQueuePresentKHR(...)} delegates to {@code nvkQueuePresentKHR}, so this sees every present.
 */
@Mixin(value = KHRSwapchain.class, remap = false)
public abstract class SwapchainMixin {
    @Inject(method = "nvkQueuePresentKHR(Lorg/lwjgl/vulkan/VkQueue;J)I", at = @At("HEAD"), require = 0)
    private static void saltsAntiAliasing$beforePresent(
            VkQueue queue,
            long pPresentInfo,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        try {
            VulkanAntiLag.beforePresent(queue);
        } catch (Throwable throwable) {
            AntiLagRuntime.fail(throwable);
        }
    }
}
