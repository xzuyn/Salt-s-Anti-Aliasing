#version 330
/*
 * Salt's Anti Aliasing: SSAA resolve.
 *
 * The world is rendered at a higher resolution; this pass filters it back down to the output
 * resolution. The reconstruction filter is chosen by the Filter uniform (see SsaaResolveFilter):
 *
 *   AREA (default)  exact area-weighted box filter: every source texel contributes in proportion to
 *                   how much of it lies inside the output pixel's rectangle.
 *   NEAREST         a single point sample per output pixel. No anti-aliasing; for comparison.
 *   BOX             unweighted average of the texels whose centers fall inside the output pixel.
 *   BILINEAR        triangle (tent) filter.
 *   B_SPLINE        cubic B-spline (B = 1, C = 0): very smooth, blurs the most of the cubics.
 *   MITCHELL        Mitchell-Netravali (B = C = 1/3): the usual balance of blur and ringing.
 *   CATMULL_ROM     Catmull-Rom (B = 0, C = 1/2): sharper, slight ringing.
 *   BICUBIC         Keys cubic convolution with a = -0.75 (B = 0, C = 3/4), as used by OpenCV: sharper
 *                   than Catmull-Rom, more ringing.
 *   LANCZOS2/3      windowed sinc with 2 or 3 lobes: sharpest, most ringing.
 *
 * Every filter except NEAREST is scaled by the downscale ratio, so it spans the whole footprint of an
 * output pixel (a fixed-size kernel would read only a few texels and skip most of the rendered
 * samples above 2x). Weights are normalized to sum to exactly 1, so brightness is preserved, and
 * texels outside the image are simply left out of the sum. Filters with negative lobes can overshoot
 * around edges; the result is clamped to the displayable range.
 *
 * Averaging happens on the stored (gamma-encoded) values, matching how Minecraft blends colors.
 */

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform SsaaResolveConfig {
    float Filter;
};

in vec2 texCoord;

out vec4 fragColor;

// Must match SsaaResolveFilter.shaderIndex().
const int FILTER_NEAREST = 0;
const int FILTER_BOX = 1;
const int FILTER_AREA = 2;
const int FILTER_BILINEAR = 3;
const int FILTER_B_SPLINE = 4;
const int FILTER_MITCHELL = 5;
const int FILTER_CATMULL_ROM = 6;
const int FILTER_BICUBIC = 7;
const int FILTER_LANCZOS2 = 8;
const int FILTER_LANCZOS3 = 9;

const float PI = 3.14159265358979;

// Widest filter (Lanczos 3, 6 texels per output pixel unit) at the largest scale (8x per axis) needs
// 48 taps per axis, plus a couple for the edges.
const int MAX_TAPS = 64;

// ---- Area (exact box) -------------------------------------------------------------------------

// Length of the overlap between [lo, hi] and the texel [index, index + 1].
float overlap(float lo, float hi, int index) {
    return max(0.0, min(hi, float(index + 1)) - max(lo, float(index)));
}

vec3 resolveArea(ivec2 sourceSize, vec2 source) {
    // This output pixel's footprint, expressed in source-texel coordinates.
    vec2 halfPixel = 0.5 / max(OutSize, vec2(1.0));
    vec2 lo = (texCoord - halfPixel) * source;
    vec2 hi = (texCoord + halfPixel) * source;

    ivec2 first = max(ivec2(floor(lo)), ivec2(0));
    ivec2 last = min(ivec2(ceil(hi)) - 1, sourceSize - 1);

    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int y = first.y; y <= last.y; y++) {
        float weightY = overlap(lo.y, hi.y, y);
        for (int x = first.x; x <= last.x; x++) {
            float weight = weightY * overlap(lo.x, hi.x, x);
            sum += texelFetch(InSampler, ivec2(x, y), 0).rgb * weight;
            total += weight;
        }
    }

    // total is the footprint area; it is only zero if the footprint fell outside the image.
    return total > 0.0 ? sum / total : texture(InSampler, texCoord).rgb;
}

// ---- Kernels ----------------------------------------------------------------------------------

float sinc(float x) {
    float px = PI * x;
    return abs(px) < 1.0e-5 ? 1.0 : sin(px) / px;
}

float lanczos(float x, float lobes) {
    x = abs(x);
    return x < lobes ? sinc(x) * sinc(x / lobes) : 0.0;
}

// Mitchell-Netravali family; B = 0 gives the Keys cubics (Catmull-Rom is C = 1/2, a = -C).
float cubic(float x, float B, float C) {
    x = abs(x);
    if (x < 1.0) {
        return ((12.0 - 9.0 * B - 6.0 * C) * x * x * x + (-18.0 + 12.0 * B + 6.0 * C) * x * x + (6.0 - 2.0 * B)) / 6.0;
    }
    if (x < 2.0) {
        return ((-B - 6.0 * C) * x * x * x + (6.0 * B + 30.0 * C) * x * x + (-12.0 * B - 48.0 * C) * x + (8.0 * B + 24.0 * C)) / 6.0;
    }
    return 0.0;
}

// Half-width of the kernel, in output pixels.
float support(int f) {
    if (f == FILTER_BOX) return 0.5;
    if (f == FILTER_BILINEAR) return 1.0;
    if (f == FILTER_LANCZOS3) return 3.0;
    return 2.0; // all cubics and Lanczos 2
}

// Weight at a distance x, in output pixels, from the output pixel's center.
float kernel(int f, float x) {
    if (f == FILTER_BOX) return (x >= -0.5 && x < 0.5) ? 1.0 : 0.0;
    if (f == FILTER_BILINEAR) return max(0.0, 1.0 - abs(x));
    if (f == FILTER_B_SPLINE) return cubic(x, 1.0, 0.0);
    if (f == FILTER_MITCHELL) return cubic(x, 1.0 / 3.0, 1.0 / 3.0);
    if (f == FILTER_CATMULL_ROM) return cubic(x, 0.0, 0.5);
    if (f == FILTER_BICUBIC) return cubic(x, 0.0, 0.75);
    if (f == FILTER_LANCZOS2) return lanczos(x, 2.0);
    return lanczos(x, 3.0); // FILTER_LANCZOS3
}

// ---- Generic separable filter -----------------------------------------------------------------

vec3 resolveKernel(int f, ivec2 sourceSize, vec2 source) {
    // Source texels per output pixel on each axis; the kernel is stretched by this much.
    vec2 scale = max(source / max(OutSize, vec2(1.0)), vec2(1.0));
    vec2 center = texCoord * source;
    vec2 radius = support(f) * scale;

    // Texels whose centers (index + 0.5) lie within the kernel's reach.
    ivec2 first = max(ivec2(ceil(center - radius - 0.5)), ivec2(0));
    ivec2 last = min(ivec2(floor(center + radius - 0.5)), sourceSize - 1);
    int countX = min(last.x - first.x + 1, MAX_TAPS);
    int countY = min(last.y - first.y + 1, MAX_TAPS);

    // The horizontal weights are the same for every row, so compute them once.
    float weightsX[MAX_TAPS];
    float totalX = 0.0;
    for (int i = 0; i < countX; i++) {
        float w = kernel(f, (float(first.x + i) + 0.5 - center.x) / scale.x);
        weightsX[i] = w;
        totalX += w;
    }

    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int j = 0; j < countY; j++) {
        float weightY = kernel(f, (float(first.y + j) + 0.5 - center.y) / scale.y);
        vec3 row = vec3(0.0);
        for (int i = 0; i < countX; i++) {
            row += texelFetch(InSampler, ivec2(first.x + i, first.y + j), 0).rgb * weightsX[i];
        }
        sum += row * weightY;
        total += totalX * weightY;
    }

    return abs(total) > 1.0e-6 ? sum / total : texture(InSampler, texCoord).rgb;
}

void main() {
    ivec2 sourceSize = textureSize(InSampler, 0);
    vec2 source = vec2(sourceSize);
    int f = int(Filter + 0.5);

    vec3 color;
    if (f == FILTER_NEAREST) {
        color = texelFetch(InSampler, clamp(ivec2(floor(texCoord * source)), ivec2(0), sourceSize - 1), 0).rgb;
    } else if (f == FILTER_AREA) {
        color = resolveArea(sourceSize, source);
    } else {
        color = resolveKernel(f, sourceSize, source);
    }
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
