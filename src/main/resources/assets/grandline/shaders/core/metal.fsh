#version 150

// Grand Line sharpened-steel surface (slashes, deflection rings): a hard bright cutting edge at v = 0, brushed-metal
// streaks running along the surface, a specular band sliding across it, fresnel edge light per vertex, and a noise
// dissolve with a hot spark-coloured rim. Colour from the vertex; no wind turbulence.

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

void main() {
    float t = GameTime * 1200.0;
    float mask = texture(Sampler0, vUv).a;
    if (mask < 0.004) {
        discard;
    }
    float d = noise(vUv * vec2(14.0, 6.0) + vec2(t * 0.2, 0.0));
    if (d < vFx.x * 1.05) {
        discard;
    }
    float rim = smoothstep(vFx.x * 1.05 + 0.05, vFx.x * 1.05, d);
    // Brushed metal: fine streaks along u, barely moving.
    float brushed = noise(vec2(vUv.x * 60.0, vUv.y * 3.0)) * 0.5 + noise(vec2(vUv.x * 140.0, vUv.y * 9.0)) * 0.5;
    // A specular band sweeping along the slash.
    float band = exp(-pow((fract(vUv.x * 0.8 - t * 1.5) - 0.5) * 6.0, 2.0));
    float edge = smoothstep(0.16, 0.0, vUv.y);
    float fres = vFx.y;
    vec3 steel = vColor.rgb * (0.75 + 0.35 * brushed);
    vec3 col = mix(steel, vec3(0.953, 0.973, 1.0), clamp(edge + band * 0.45 + fres * 0.35, 0.0, 1.0));
    col = mix(col, vec3(1.0, 0.77, 0.43), rim * 0.8); // hot rim as it dissolves
    float alpha = vColor.a * mask * clamp(0.18 + edge * 0.85 + band * 0.25 + fres * 0.35 + brushed * 0.12 + rim * 0.5, 0.0, 1.0);
    fragColor = vec4(col, alpha);
}
