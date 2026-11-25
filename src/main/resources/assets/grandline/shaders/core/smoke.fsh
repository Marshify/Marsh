#version 150

// Grand Line smoke surface (Smoke Fruit and any smoke effect): billowing domain-warped fbm density that churns and
// rises, cool-gray self-shading from inner shadow to highlight, soft edges (fresnel-faded, so every shell reads as
// a volume rather than a solid ball), density layers by vUv.y (0 dense core .. 1 thin wisps) and a noise dissolve
// without any bright rim (smoke thins out, it does not burn). Vertex colour tints, vertex alpha fades.

uniform sampler2D Sampler0;
uniform float GameTime;

in vec4 vColor;
in vec2 vUv;
in vec2 vFx;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.02 + vec2(5.3, 1.7);
        a *= 0.5;
    }
    return v;
}

void main() {
    float t = GameTime * 1200.0; // seconds
    float mask = texture(Sampler0, vUv).a;
    if (mask < 0.004) {
        discard;
    }
    vec2 q = vec2(vUv.x * 5.0 + t * 0.15, vUv.y * 2.5 - t * 0.35);
    vec2 warp = vec2(fbm(q + vec2(0.0, t * 0.2)), fbm(q + vec2(4.1, -t * 0.15)));
    float density = fbm(q * 1.4 + warp * 2.2);
    // Layer: the dense core holds together, the outer layers break into wisps.
    float layer = vUv.y;
    float body = smoothstep(0.18 + layer * 0.35, 0.62 + layer * 0.2, density);
    float cut = vFx.x * 1.05;
    float d = fbm(vUv * vec2(7.0, 4.0) + warp + vec2(t * 0.1, 0.0));
    if (d < cut || body < 0.02) {
        discard;
    }
    float thin = smoothstep(cut, cut + 0.15, d);
    // Self-shading: crevices dark, billows light (cool gray palette), slightly lighter toward the top of a shell.
    vec3 shadow = vec3(0.314, 0.349, 0.396);   // #505965
    vec3 dense = vec3(0.451, 0.490, 0.545);    // #737D8B
    vec3 mainc = vec3(0.627, 0.655, 0.698);    // #A0A7B2
    vec3 light = vec3(0.886, 0.898, 0.922);    // #E2E5EB
    float shade = clamp(density * 1.3 - 0.2 + (warp.x - 0.5) * 0.4, 0.0, 1.0);
    vec3 col = shade < 0.4 ? mix(shadow, dense, shade / 0.4) : shade < 0.75 ? mix(dense, mainc, (shade - 0.4) / 0.35) : mix(mainc, light, (shade - 0.75) / 0.25);
    col *= mix(vec3(1.0), vColor.rgb, 0.5);
    float soft = 1.0 - vFx.y * 0.85; // edge-on parts fade: no hard silhouette
    float alpha = vColor.a * mask * body * soft * thin * (0.95 - layer * 0.35);
    fragColor = vec4(col, clamp(alpha, 0.0, 1.0));
}
