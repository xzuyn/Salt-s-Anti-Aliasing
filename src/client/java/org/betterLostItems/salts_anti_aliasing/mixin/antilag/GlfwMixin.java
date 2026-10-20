package org.betterLostItems.salts_anti_aliasing.mixin.antilag;

import org.betterLostItems.salts_anti_aliasing.client.antilag.AntiLagRuntime;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The INPUT stage of AMD Anti-Lag. Minecraft samples input by polling window events, so the first poll after
 * a present is where a new frame's input is taken. This is hooked on GLFW itself rather than on a
 * Minecraft method so it keeps working however Mojang rearranges its frame loop.
 */
@Mixin(value = GLFW.class, remap = false)
public abstract class GlfwMixin {
    @Inject(method = "glfwPollEvents()V", at = @At("HEAD"), require = 0)
    private static void saltsAntiAliasing$beforePollEvents(CallbackInfo callbackInfo) {
        AntiLagRuntime.beforePollEvents();
    }
}
