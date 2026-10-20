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
 * Both AMD Anti-Lag stages are driven from the present call, on whichever thread presents (the render thread):
 * the PRESENT stage right before {@code vkQueuePresentKHR}, and the INPUT stage of the next frame right after
 * it returns. {@code vkQueuePresentKHR(...)} delegates to {@code nvkQueuePresentKHR}, so this sees every present.
 *
 * <p>The INPUT stage is deliberately not hooked on {@code glfwPollEvents}: mods such as Ixeris move event
 * polling to another thread, where it says nothing about when the render thread samples input.</p>
 */
@Mixin(value = KHRSwapchain.class, remap = false)
public abstract class SwapchainMixin {
    private static final String PRESENT = "nvkQueuePresentKHR(Lorg/lwjgl/vulkan/VkQueue;J)I";

    @Inject(method = PRESENT, at = @At("HEAD"), require = 0)
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

    @Inject(method = PRESENT, at = @At("RETURN"), require = 0)
    private static void saltsAntiAliasing$afterPresent(
            VkQueue queue,
            long pPresentInfo,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        try {
            VulkanAntiLag.afterPresent(queue);
        } catch (Throwable throwable) {
            AntiLagRuntime.fail(throwable);
        }
    }
}
