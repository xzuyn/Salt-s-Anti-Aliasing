package org.betterLostItems.salts_anti_aliasing.client.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.betterLostItems.salts_anti_aliasing.SaltsAntiAliasing;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaLevel;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaSharpness;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Supersampling for the 3D world.
 *
 * <p>Each frame, while a level above Off is selected, Minecraft's main render target is
 * temporarily replaced by a larger scene target so the world renders at {@code level.scaleFactor()}
 * times the output size on each axis. Once the world is finished, a {@code ssaa_resolve*} post
 * effect averages that image down into the real main target with an exact area-weighted box
 * filter (optionally followed by a gentle adaptive sharpen), and the HUD and menus then draw on top
 * at native resolution.</p>
 *
 * <p>The resolve is an ordinary post-chain pass rather than a hardware blit. A linear blit only
 * reads a 2x2 neighbourhood per output pixel, which wastes most samples above 2x and collapses to
 * a single point sample at odd integer scales.</p>
 */
public final class SsaaController {
    private static final SsaaController INSTANCE = new SsaaController();
    private static final String TARGET_LABEL = "Salt's SSAA Scene";
    private static final Identifier SCENE_TARGET_ID = Identifier.parse(SaltsAntiAliasing.MOD_ID + ":scene_color");
    private static final Set<Identifier> EXTERNAL_TARGETS = Set.of(PostChain.MAIN_TARGET_ID, SCENE_TARGET_ID);

    private final CrossFrameResourcePool resourcePool = new CrossFrameResourcePool(3);
    private boolean disabledAfterFailure;
    private boolean active;
    private TextureTarget sceneTarget;
    private RenderTarget mainTarget;
    private PostChain resolveChain;
    private String lastRejectedSceneSize;
    private String failedSceneSetupSignature;
    private boolean warnedResolveUnavailable;

    private static Identifier resolveEffect(SsaaSharpness sharpness) {
        return Identifier.parse(SaltsAntiAliasing.MOD_ID + ":" + sharpness.effectName());
    }

    private SsaaController() {
    }

    public static SsaaController instance() {
        return INSTANCE;
    }

    /**
     * Redirects Minecraft's world rendering into a supersampled scene target for this frame, if the
     * selected level and the GPU allow it. Does nothing (native rendering) otherwise.
     */
    public void beginSceneRendering(GameRenderer gameRenderer, SsaaLevel level, SsaaSharpness sharpness) {
        RenderSystem.assertOnRenderThread();
        clearFrameState();

        if (disabledAfterFailure) {
            return;
        }
        if (!level.enabled()) {
            failedSceneSetupSignature = null;
            lastRejectedSceneSize = null;
            // Do not hold on to a potentially huge scene target while SSAA is off.
            destroyResources();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        RenderTarget mainTarget = gameRenderer.mainRenderTarget();
        if (!mainTarget.useDepth || mainTarget.width <= 0 || mainTarget.height <= 0) {
            return;
        }

        try {
            int sceneWidth = level.sceneSize(mainTarget.width);
            int sceneHeight = level.sceneSize(mainTarget.height);
            int maxTextureSize = RenderSystem.getDevice().getDeviceInfo().limits().maxTextureSize();
            if (sceneWidth > maxTextureSize || sceneHeight > maxTextureSize) {
                warnUnsupportedSceneSize(sceneWidth, sceneHeight, maxTextureSize);
                return;
            }

            String setupSignature = level + ":" + sceneWidth + "x" + sceneHeight + ":" + mainTarget.useDepth;
            if (setupSignature.equals(failedSceneSetupSignature)) {
                return;
            }

            // Look the resolve up before allocating anything, so a broken shader never leaves the
            // frame redirected into a target that cannot be resolved.
            Identifier resolveEffect = resolveEffect(sharpness);
            PostChain chain = minecraft.getShaderManager().getPostChain(resolveEffect, EXTERNAL_TARGETS);
            if (chain == null) {
                warnResolveUnavailable(resolveEffect);
                return;
            }
            warnedResolveUnavailable = false;

            lastRejectedSceneSize = null;
            try {
                ensureSceneTarget(sceneWidth, sceneHeight, mainTarget.useDepth);
            } catch (RuntimeException exception) {
                rejectSceneSetupUntilConfigurationChanges(setupSignature, sceneWidth, sceneHeight, exception);
                return;
            }

            failedSceneSetupSignature = null;
            this.mainTarget = mainTarget;
            this.resolveChain = chain;
            active = true;
        } catch (RuntimeException exception) {
            disableAfterFailure("Disabling SSAA after a setup failure", exception);
        }
    }

    /** Averages the supersampled scene down into Minecraft's real main target. */
    public void endSceneRendering() {
        RenderSystem.assertOnRenderThread();
        if (!active) {
            clearFrameState();
            return;
        }

        RenderTarget mainTarget = this.mainTarget;
        TextureTarget sceneTarget = this.sceneTarget;
        PostChain chain = this.resolveChain;

        try {
            // Stop redirecting first: the resolve must read the scene target and write the real main target.
            active = false;
            if (mainTarget != null
                    && mainTarget.getColorTextureView() != null
                    && sceneTarget != null
                    && sceneTarget.getColorTextureView() != null
                    && chain != null) {
                resolve(mainTarget, sceneTarget, chain);
            }
        } catch (RuntimeException exception) {
            disableAfterFailure("Disabling SSAA after a resolve failure", exception);
        } finally {
            resourcePool.endFrame();
            clearFrameState();
        }
    }

    private void resolve(RenderTarget mainTarget, TextureTarget sceneTarget, PostChain chain) {
        FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> mainHandle = frameGraphBuilder.importExternal("salts_ssaa_main", mainTarget);
        ResourceHandle<RenderTarget> sceneHandle = frameGraphBuilder.importExternal("salts_ssaa_scene", sceneTarget);
        SceneTargetBundle targetBundle = new SceneTargetBundle(mainHandle, sceneHandle);
        chain.addToFrame(frameGraphBuilder, mainTarget.width, mainTarget.height, targetBundle);
        frameGraphBuilder.execute(resourcePool);
    }

    // ---- Render target redirection, called from RenderTargetMixin / GameRendererMixin -----------------

    public RenderTarget overrideMainTarget() {
        return active ? sceneTarget : null;
    }

    public GpuTexture overrideColorTexture(RenderTarget target) {
        RenderTarget redirectedTarget = mappedTarget(target);
        return redirectedTarget == null ? null : redirectedTarget.getColorTexture();
    }

    public GpuTextureView overrideColorTextureView(RenderTarget target) {
        RenderTarget redirectedTarget = mappedTarget(target);
        return redirectedTarget == null ? null : redirectedTarget.getColorTextureView();
    }

    public GpuTexture overrideDepthTexture(RenderTarget target) {
        RenderTarget redirectedTarget = mappedTarget(target);
        return redirectedTarget == null ? null : redirectedTarget.getDepthTexture();
    }

    public GpuTextureView overrideDepthTextureView(RenderTarget target) {
        RenderTarget redirectedTarget = mappedTarget(target);
        return redirectedTarget == null ? null : redirectedTarget.getDepthTextureView();
    }

    /**
     * Keeps depth copies coherent when Minecraft copies to or from the redirected main target.
     * @return true if the copy was handled here and vanilla's copy must be skipped
     */
    public boolean redirectCopyDepth(RenderTarget target, RenderTarget sourceTarget) {
        RenderTarget redirectedTarget = mappedTarget(target);
        RenderTarget redirectedSource = mappedTarget(sourceTarget);
        if (redirectedTarget == null && redirectedSource == null) {
            return false;
        }

        RenderTarget resolvedTarget = redirectedTarget != null ? redirectedTarget : target;
        RenderTarget resolvedSource = redirectedSource != null ? redirectedSource : sourceTarget;
        if (resolvedTarget == resolvedSource) {
            return true;
        }

        if (!canCopyDepth(resolvedTarget, resolvedSource)) {
            return true;
        }

        resolvedTarget.copyDepthFrom(resolvedSource);
        return true;
    }

    private RenderTarget mappedTarget(RenderTarget target) {
        if (!active || target != mainTarget) {
            return null;
        }

        return sceneTarget;
    }

    private static boolean canCopyDepth(RenderTarget target, RenderTarget source) {
        GpuTexture targetDepth = target.getDepthTexture();
        GpuTexture sourceDepth = source.getDepthTexture();
        if (targetDepth == null || sourceDepth == null) {
            return false;
        }

        return targetDepth.getFormat() == sourceDepth.getFormat()
                && targetDepth.getWidth(0) == sourceDepth.getWidth(0)
                && targetDepth.getHeight(0) == sourceDepth.getHeight(0);
    }

    // ---- Resources and failure handling ----------------------------------------------------------------

    private void ensureSceneTarget(int width, int height, boolean useDepth) {
        if (sceneTarget == null || sceneTarget.useDepth != useDepth) {
            destroyResources();
            sceneTarget = new TextureTarget(TARGET_LABEL, width, height, useDepth, GpuFormat.RGBA8_UNORM);
            return;
        }

        if (sceneTarget.width != width || sceneTarget.height != height) {
            sceneTarget.resize(width, height);
        }
    }

    private void destroyResources() {
        if (sceneTarget != null) {
            sceneTarget.destroyBuffers();
            sceneTarget = null;
        }
    }

    private void clearFrameState() {
        active = false;
        mainTarget = null;
        resolveChain = null;
    }

    /** Warns once per rejected size; lowering the level or resizing the window can recover. */
    private void warnUnsupportedSceneSize(int width, int height, int maxTextureSize) {
        String rejectedSceneSize = width + "x" + height + ":" + maxTextureSize;
        if (rejectedSceneSize.equals(lastRejectedSceneSize)) {
            return;
        }

        lastRejectedSceneSize = rejectedSceneSize;
        SaltsAntiAliasing.LOGGER.warn(
                "Skipping SSAA scene target {}x{} because the active GPU supports at most {}x{}; "
                        + "lower the SSAA level or the window size",
                width,
                height,
                maxTextureSize,
                maxTextureSize
        );
    }

    private void warnResolveUnavailable(Identifier resolveEffect) {
        if (warnedResolveUnavailable) {
            return;
        }

        warnedResolveUnavailable = true;
        SaltsAntiAliasing.LOGGER.error(
                "SSAA resolve effect {} could not be loaded; rendering at native resolution", resolveEffect
        );
    }

    /**
     * Treats an impossible target allocation as specific to this configuration: it is not retried
     * every frame, but changing the level or the window size produces a new signature and recovers.
     */
    private void rejectSceneSetupUntilConfigurationChanges(
            String setupSignature,
            int width,
            int height,
            RuntimeException exception
    ) {
        failedSceneSetupSignature = setupSignature;
        destroyResources();
        resourcePool.clear();
        clearFrameState();
        SaltsAntiAliasing.LOGGER.error(
                "Could not allocate SSAA scene target {}x{}; rendering at native resolution until the "
                        + "SSAA level or window size changes",
                width,
                height,
                exception
        );
    }

    private void disableAfterFailure(String message, RuntimeException exception) {
        disabledAfterFailure = true;
        destroyResources();
        resourcePool.clear();
        clearFrameState();
        SaltsAntiAliasing.LOGGER.error(message, exception);
    }

    /** Exposes the main target and the scene target to the resolve post chain by id. */
    private static final class SceneTargetBundle implements PostChain.TargetBundle {
        private final Map<Identifier, ResourceHandle<RenderTarget>> targets = new HashMap<>();

        private SceneTargetBundle(ResourceHandle<RenderTarget> mainHandle, ResourceHandle<RenderTarget> sceneHandle) {
            targets.put(PostChain.MAIN_TARGET_ID, mainHandle);
            targets.put(SCENE_TARGET_ID, sceneHandle);
        }

        @Override
        public void replace(Identifier id, ResourceHandle<RenderTarget> handle) {
            targets.put(id, handle);
        }

        @Override
        public ResourceHandle<RenderTarget> get(Identifier id) {
            return targets.getOrDefault(id, ResourceHandle.invalid());
        }
    }
}
