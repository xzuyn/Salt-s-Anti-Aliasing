package org.betterLostItems.salts_anti_aliasing.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.betterLostItems.salts_anti_aliasing.client.render.SsaaController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While supersampling, anything that asks Minecraft's main render target for its textures gets the
 * supersampled scene target's textures instead.
 */
@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {
    @Inject(method = "getColorTexture", at = @At("HEAD"), cancellable = true)
    private void saltsAntiAliasing$redirectColorTexture(CallbackInfoReturnable<GpuTexture> callbackInfo) {
        GpuTexture redirectedTexture = SsaaController.instance().overrideColorTexture((RenderTarget) (Object) this);
        if (redirectedTexture != null) {
            callbackInfo.setReturnValue(redirectedTexture);
        }
    }

    @Inject(method = "getColorTextureView", at = @At("HEAD"), cancellable = true)
    private void saltsAntiAliasing$redirectColorTextureView(CallbackInfoReturnable<GpuTextureView> callbackInfo) {
        GpuTextureView redirectedTextureView =
                SsaaController.instance().overrideColorTextureView((RenderTarget) (Object) this);
        if (redirectedTextureView != null) {
            callbackInfo.setReturnValue(redirectedTextureView);
        }
    }

    @Inject(method = "getDepthTexture", at = @At("HEAD"), cancellable = true)
    private void saltsAntiAliasing$redirectDepthTexture(CallbackInfoReturnable<GpuTexture> callbackInfo) {
        GpuTexture redirectedTexture = SsaaController.instance().overrideDepthTexture((RenderTarget) (Object) this);
        if (redirectedTexture != null) {
            callbackInfo.setReturnValue(redirectedTexture);
        }
    }

    @Inject(method = "getDepthTextureView", at = @At("HEAD"), cancellable = true)
    private void saltsAntiAliasing$redirectDepthTextureView(CallbackInfoReturnable<GpuTextureView> callbackInfo) {
        GpuTextureView redirectedTextureView =
                SsaaController.instance().overrideDepthTextureView((RenderTarget) (Object) this);
        if (redirectedTextureView != null) {
            callbackInfo.setReturnValue(redirectedTextureView);
        }
    }

    @Inject(method = "copyDepthFrom", at = @At("HEAD"), cancellable = true)
    private void saltsAntiAliasing$redirectDepthCopy(RenderTarget sourceTarget, CallbackInfo callbackInfo) {
        if (SsaaController.instance().redirectCopyDepth((RenderTarget) (Object) this, sourceTarget)) {
            callbackInfo.cancel();
        }
    }
}
