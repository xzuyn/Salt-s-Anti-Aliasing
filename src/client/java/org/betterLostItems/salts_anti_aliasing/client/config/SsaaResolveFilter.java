package org.betterLostItems.salts_anti_aliasing.client.config;

/**
 * The reconstruction filter used to average the supersampled image down to the screen.
 *
 * <p>Every filter except {@link #NEAREST} is scaled by the downscale ratio, so it always covers the
 * whole footprint of an output pixel (plus its own lobes) and uses every rendered sample, rather than
 * a fixed 2x2 or 4x4 neighbourhood. Cost grows with the filter's support: {@link #AREA} and
 * {@link #BOX} are cheapest, the cubics cost roughly 16 times as much as a box at the same level and
 * Lanczos 3 roughly 36 times.</p>
 *
 * <p>{@link #shaderIndex()} must match the {@code FILTER_*} constants in
 * {@code ssaa_resolve.fsh} and the {@code Filter} value written into the {@code ssaa_resolve_*.json}
 * post effects; tests check that they do.</p>
 */
public enum SsaaResolveFilter {
    NEAREST(0, "nearest"),
    BOX(1, "box"),
    AREA(2, "area"),
    BILINEAR(3, "bilinear"),
    B_SPLINE(4, "bspline"),
    MITCHELL(5, "mitchell"),
    CATMULL_ROM(6, "catmull_rom"),
    BICUBIC(7, "bicubic"),
    LANCZOS2(8, "lanczos2"),
    LANCZOS3(9, "lanczos3");

    private final int shaderIndex;
    private final String id;

    SsaaResolveFilter(int shaderIndex, String id) {
        this.shaderIndex = shaderIndex;
        this.id = id;
    }

    /** Value of the shader's {@code Filter} uniform. */
    public int shaderIndex() {
        return shaderIndex;
    }

    /** Short name used in post effect file names and translation keys. */
    public String id() {
        return id;
    }

    /**
     * Name of the post effect (under {@code post_effect/}) that resolves with this filter and then
     * sharpens at the given setting.
     */
    public String effectName(SsaaSharpness sharpness) {
        String base = "ssaa_resolve_" + id;
        return SsaaSharpness.clamp(sharpness).enabled()
                ? base + "_sharp_" + SsaaSharpness.clamp(sharpness).name().toLowerCase(java.util.Locale.ROOT)
                : base;
    }

    /** The original exact area-weighted box filter. */
    public static SsaaResolveFilter defaultFilter() {
        return AREA;
    }

    public static SsaaResolveFilter clamp(SsaaResolveFilter filter) {
        return filter == null ? defaultFilter() : filter;
    }
}
