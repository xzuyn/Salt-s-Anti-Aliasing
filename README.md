# Salt's Anti Aliasing

A client-side Fabric mod for **Minecraft 26.2** that does one thing: super sampling anti-aliasing (SSAA).

The 3D world is rendered at a higher resolution and then averaged down to your screen. The HUD and menus
are drawn afterwards at normal resolution, so text stays sharp.

## Using it

Open **Options > Video Settings** and use the sliders at the bottom: **Supersampling (SSAA)** and
**SSAA Sharpness**. With Mod Menu installed there is also a settings screen, and with Sodium installed there
is a "Salt's Anti Aliasing" page.

Levels are named by how many rendered pixels are averaged into each screen pixel, like other games and
GPU drivers do:

| Level | Pixels rendered | Per axis |
|-------|-----------------|----------|
| Off   | 1x              | 1.00     |
| 2x    | 2x              | 1.41     |
| 4x    | 4x              | 2.00     |
| 9x    | 9x              | 3.00     |
| 16x   | 16x             | 4.00     |
| 25x   | 25x             | 5.00     |
| 36x   | 36x             | 6.00     |
| 64x   | 64x             | 8.00     |

Levels above 16x are marked with `(!)`: they cost a lot of performance and GPU memory, and are skipped
(rendering normally, with a log warning) if the scaled image would exceed your GPU's maximum texture size.

SSAA cannot be turned on or changed while Minecraft's **Improved Transparency** option is on. Turning it
off is always allowed.

### If the image looks soft

Some softness is inherent: averaging many pixels into one is an anti-aliasing filter. In a test on a
Minecraft-style pixel-art texture, a correct resolve ended up about 8% lower in edge contrast than rendering
without SSAA. Three things help:

- **SSAA Sharpness** (default Low) applies a gentle, edge-aware sharpen to the result. Low is calibrated to
  roughly win back that 8%; Medium and High go beyond native crispness and can add faint halos on
  high-contrast edges.
- **Prefer 4x, 9x, 16x...** over 2x. Those scale each axis by a whole number, so output pixels line up with
  rendered pixels. 2x is a factor of 1.41 per axis, so every output pixel blends partial source pixels,
  which is measurably softer.
- **Check Minecraft's own Texture Filtering option** (Video Settings). Since 1.21.11 the game offers
  anisotropic filtering and RGSS texture filtering, and players report that these blur block textures. They
  stack with SSAA's smoothing, so try turning them off.

## How it works

1. `GameRendererMixin` swaps Minecraft's main render target for a larger scene target at the start of the
   world render (`SsaaController.beginSceneRendering`).
2. The world renders into it at `sqrt(samples)` times the width and height.
3. When the world is done, the `ssaa_resolve` post effect averages it into the real main target
   (`SsaaController.endSceneRendering`).

The resolve shader (`shaders/post/ssaa_resolve.fsh`) is an exact **area-weighted box filter**: each output
pixel averages every source pixel under it, weighted by how much of that pixel it covers. This works for any
level, including 2x, keeps brightness exact, and uses every rendered sample. Averaging happens on
Minecraft's stored (gamma-encoded) colors, matching how the game blends everything else.

When sharpness is not Off, a second pass (`ssaa_sharpen.fsh`) applies contrast adaptive sharpening in the
style of AMD's FidelityFX CAS, with the strength range recalibrated for Minecraft's high-contrast textures.
Flat areas are unchanged and the weight fades to zero near pure black and white, so it never clips.

Earlier versions resolved with a single hardware linear blit. That only reads 2x2 pixels per output pixel,
so above 2x it threw most samples away, and at odd integer scales (300%, 500%, 700%) it reduced to a single
point sample, which is no anti-aliasing at all.

## Config

`config/salts_anti_aliasing.json` stores `level` and `sharpness`. Config files from earlier versions are migrated
automatically: if SSAA was selected, the closest level is chosen (for example the old 200% becomes 4x);
anything else becomes Off. All other old settings are discarded.

## Building

```
./gradlew build
./gradlew runClient
```

Requires Java 25. Unit tests cover the level math, config migration and persistence, and keep the lang
file, post effect and shader in sync.

## What was removed

Everything except SSAA: FXAA, SMAA, MSAA, TAA, NIS, FSR1/2/3, DLSS, frame generation, sharpening, the mode
dropdown and keybind, the edge-debug view, performance metrics, the native bridges and bundled AMD
runtime, and all Vulkan-specific mixins. Because the resolve is an ordinary post-chain pass instead of a
Vulkan blit, the mod no longer contains any Vulkan-specific code.
