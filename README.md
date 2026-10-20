# Salt's Anti Aliasing

A client-side Fabric mod for **Minecraft 26.2** that does super sampling anti-aliasing (SSAA), plus an optional,
separate **AMD Anti-Lag** switch for lower input latency on Vulkan.

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

## AMD Anti-Lag

**AMD Anti-Lag** (under the **Latency** heading in the same settings screens) is independent of SSAA: it works
with SSAA on or off, and changes apply immediately. It uses the Vulkan extension
[`VK_AMD_anti_lag`](https://docs.vulkan.org/refpages/latest/refpages/source/VK_AMD_anti_lag.html) so the driver can
keep the CPU from getting ahead of the GPU, which shortens the delay between your input and the picture.

It only does something when **Minecraft renders with Vulkan** on a GPU whose **driver exposes the extension**
(AMD's, in practice). On OpenGL or other GPUs the setting is harmless and does nothing; its tooltip says why.

How it is wired (all through LWJGL, none of it in Minecraft's own classes):

| Stage | Hook | What is sent |
|-------|------|--------------|
| Device creation | `vkCreateDevice` | Enables `VK_AMD_anti_lag` and its `antiLag` feature when the GPU supports them, whatever the setting is, so toggling needs no restart. If the driver rejects it, device creation is retried unchanged. |
| Start of frame | `glfwPollEvents` (first poll after a present) | INPUT stage with a new frame index |
| Before present | `vkQueuePresentKHR` | PRESENT stage with the same frame index |

Turning the setting Off sends one OFF to the driver. The driver frame limit is left at 0 (none), so Minecraft's own
frame limiter stays in charge. The hooks live in a separate, non-required mixin config, so SSAA is unaffected if
they cannot apply, and any runtime error turns Anti-Lag off for the session with one log line. Start the game with
`-Dsalts_anti_aliasing.antilag=false` to keep the mod out of Vulkan device creation entirely.

Requires LWJGL 3.3.5 or newer at runtime (the first release with the `VK_AMD_anti_lag` bindings that this uses).

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

`config/salts_anti_aliasing.json` stores `level`, `resolveFilter`, `sharpness` and `antiLag` (`OFF` or `ON`). Config files from earlier versions are migrated
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

Everything except SSAA and Anti-Lag: FXAA, SMAA, MSAA, TAA, NIS, FSR1/2/3, DLSS, frame generation, sharpening, the mode
dropdown and keybind, the edge-debug view, performance metrics, the native bridges and bundled AMD
runtime, and all of the old Vulkan-specific mixins. The SSAA resolve is an ordinary post-chain pass instead of a
Vulkan blit, so SSAA itself contains no Vulkan code. The only Vulkan code is the optional Anti-Lag hooks, which
touch LWJGL rather than Minecraft's classes.
