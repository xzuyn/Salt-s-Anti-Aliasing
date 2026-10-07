#version 330
/*
 * Salt's Anti Aliasing: SSAA resolve.
 *
 * The world is rendered at a higher resolution; this pass averages it back down to the output
 * resolution with an exact area-weighted box filter. Each output pixel covers a rectangle of the
 * high-resolution image, and every source texel contributes in proportion to how much of it lies
 * inside that rectangle. This is the standard SSAA resolve:
 *
 *   - every rendered sample is used (a hardware bilinear blit reads only 2x2 texels per pixel, which
 *     skips most samples above 2x and degenerates to a single point sample at odd integer scales),
 *   - it works for any scale, including the non-integer per-axis scales of 2x, 8x, ...,
 *   - the weights sum to exactly 1, so brightness is preserved.
 *
 * Averaging happens on the stored (gamma-encoded) values, matching how Minecraft blends colors.
 */

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

// Length of the overlap between [lo, hi] and the texel [index, index + 1].
float overlap(float lo, float hi, int index) {
    return max(0.0, min(hi, float(index + 1)) - max(lo, float(index)));
}

void main() {
    ivec2 sourceSize = textureSize(InSampler, 0);
    vec2 source = vec2(sourceSize);

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
    vec3 color = total > 0.0 ? sum / total : texture(InSampler, texCoord).rgb;
    fragColor = vec4(color, 1.0);
}
