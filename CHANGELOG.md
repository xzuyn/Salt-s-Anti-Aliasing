# Changelog

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
