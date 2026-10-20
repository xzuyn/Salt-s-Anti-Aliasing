# Changelog

## 0.3.0 - AMD Anti-Lag

### Added

- **AMD Anti-Lag** setting (Off / On, default Off), separate from SSAA, in Video Settings, Mod Menu and Sodium.
  It uses `VK_AMD_anti_lag` to keep the CPU from running ahead of the GPU, which shortens the delay between
  your input and the picture on screen. It works with SSAA on or off and takes effect immediately.
- It only does something when Minecraft renders with Vulkan on a GPU whose driver exposes the extension. The
  setting's tooltip says so when it cannot do anything (OpenGL, unsupported GPU/driver, or after an error).

### How it works

- LWJGL hooks, none on Minecraft's own classes: `vkCreateDevice` (enables the extension and its feature
  whenever the GPU supports them), `vkDestroyDevice` (stop using a device before it goes away) and
  `vkQueuePresentKHR` (PRESENT stage just before it, INPUT stage of the next frame just after it returns, same
  frame index per pair). Nothing is hooked on `glfwPollEvents`, so it is safe with Ixeris, which polls events on
  another thread.
- The hooks live in their own mixin config (`salts_anti_aliasing.antilag.mixins.json`) that is not required and
  fails soft, so SSAA is unaffected if they cannot apply to a Minecraft build.
- If a driver rejects device creation with the extension, creation is retried unchanged. Any runtime error turns
  Anti-Lag off for the session and logs once. `-Dsalts_anti_aliasing.antilag=false` keeps the mod out of device
  creation entirely.

### Notes

- Config files need no migration: a missing `antiLag` key reads as Off.
- Verified by compiling against LWJGL 3.3.5, 3.3.6 and 3.4.0 and by exercising the device-creation edit on real
  native memory. It has **not** been run on an AMD GPU against Minecraft's Vulkan renderer.

## 0.2.1 - Sharpness

### Added

- **SSAA Sharpness** setting (Off / Low / Medium / High, default Low) in Video Settings, Mod Menu and Sodium.
  It runs a gentle contrast adaptive sharpen after the resolve to win back the detail that averaging removes.
  The strength range is recalibrated from AMD's CAS defaults, which measured far too strong for Minecraft's
  pixel-art textures (texture contrast 30-80% above native even at CAS's gentlest setting, with up to 12% edge
  overshoot).
- README guidance on why SSAA can look soft: the inherent filtering loss, the softer non-integer 2x level,
  and Minecraft's own anisotropic/RGSS texture filtering.

### Notes

- Measured with the real shaders on a software OpenGL context: the resolve matches the ideal anti-aliased
  image's texture contrast (within 3%), and Low sharpness brings it to within 1-2% of unfiltered rendering.

## 0.2.0 - SSAA only

### Changed

- The mod now provides super sampling anti-aliasing (SSAA) and nothing else. FXAA, SMAA, MSAA, TAA, NIS,
  FSR, DLSS, frame generation, sharpening, debug views, metrics, the native bridges and all Vulkan-specific
  code were removed.
- SSAA levels are now named by sample count like other games and drivers do: Off, 2x, 4x, 9x, 16x, 25x,
  36x, 64x. The old per-axis percentage slider (125% to 800%) is gone; existing configs are converted to the
  nearest level.
- The settings UI is a single **Supersampling (SSAA)** slider in Video Settings, Mod Menu and Sodium.
  Levels above 16x show a performance and memory warning.

### Fixed

- **Downscale quality.** The resolve used a single linear blit, which read only 2x2 pixels per output pixel.
  Above 2x most rendered samples were discarded, and 300%, 500% and 700% degenerated to a point sample with
  no anti-aliasing at all. It is now an exact area-weighted box filter that uses every sample at every level.
- The scaled scene target is released when SSAA is turned Off instead of staying allocated.
- An old config that had SSAA selected but no saved scale no longer risks failing during migration.

### Repository

- Removed accidentally committed files: decompiled Fabric lifecycle classes, `META-INF`, a vanilla
  `GameRenderer.class`, a stray attachment folder, and a duplicate icon.
