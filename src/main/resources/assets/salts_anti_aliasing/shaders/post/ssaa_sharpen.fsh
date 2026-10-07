#version 330
/*
 * Salt's Anti Aliasing: SSAA resolve sharpening.
 *
 * Averaging a supersampled image down to the screen is an anti-aliasing filter, so it also softens
 * fine detail a little. This pass gives some of that detail back with contrast adaptive sharpening
 * in the style of AMD's FidelityFX CAS: the sharpening weight is derived from the local minimum and
 * maximum of the 3x3 neighbourhood. Flat areas are left unchanged, and the weight shrinks to zero as
 * the local range approaches pure black or white, so sharpening never clips. Like any sharpening it
 * can overshoot a little around edges (a faint halo that grows with Strength); the result is always
 * clamped to the displayable range.
 *
 * Runs on the stored (gamma-encoded) colors, which is the space CAS is designed for. The strength
 * range is deliberately gentle: a correct supersampled resolve only loses a few percent of texture
 * contrast compared with unfiltered rendering, so the aim is to win that back, not to exceed it.
 */

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform SsaaSharpenConfig {
    float Strength;
};

in vec2 texCoord;

out vec4 fragColor;

vec3 fetch(ivec2 pixel, ivec2 size) {
    return texelFetch(InSampler, clamp(pixel, ivec2(0), size - 1), 0).rgb;
}

void main() {
    ivec2 size = textureSize(InSampler, 0);
    ivec2 p = clamp(ivec2(texCoord * vec2(size)), ivec2(0), size - 1);

    //  a b c
    //  d e f
    //  g h i
    vec3 a = fetch(p + ivec2(-1, -1), size);
    vec3 b = fetch(p + ivec2( 0, -1), size);
    vec3 c = fetch(p + ivec2( 1, -1), size);
    vec3 d = fetch(p + ivec2(-1,  0), size);
    vec3 e = fetch(p, size);
    vec3 f = fetch(p + ivec2( 1,  0), size);
    vec3 g = fetch(p + ivec2(-1,  1), size);
    vec3 h = fetch(p + ivec2( 0,  1), size);
    vec3 i = fetch(p + ivec2( 1,  1), size);

    // Soft min/max: the cross plus the full 3x3, summed (so both are scaled by 2).
    vec3 mn = min(min(min(d, e), min(f, b)), h);
    mn += min(mn, min(min(a, c), min(g, i)));
    vec3 mx = max(max(max(d, e), max(f, b)), h);
    mx += max(mx, max(max(a, c), max(g, i)));

    // How much headroom the local range leaves before clipping, relative to its brightness.
    vec3 amp = sqrt(clamp(min(mn, 2.0 - mx) / max(mx, vec3(1.0e-4)), 0.0, 1.0));

    // Negative weight on the four neighbours. AMD's CAS never goes below -1/8, which is tuned for
    // much softer content than Minecraft's high-contrast pixel art, so Strength 1.0 here is CAS's
    // gentlest setting and the useful range is below it. Strength 0.0 leaves the image unchanged.
    float peak = -0.125 * clamp(Strength, 0.0, 1.0);
    vec3 w = amp * peak;

    vec3 color = (w * (b + d + f + h) + e) / (4.0 * w + 1.0);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
