#version 150

// Grand Line compressed-air surface: procedural airflow streaks scrolling along the surface, a bright leading edge
// (v = 0), fresnel edge light supplied per vertex, and a noise dissolve for fading out. Colour comes from the vertex.

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
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(1.7, 9.2);
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
    // Airflow: stretched turbulent streaks racing along the surface.
    float flow = fbm(vec2(vUv.x * 7.0 - t * 4.0, vUv.y * 2.5 + t * 0.6));
    float streak = smoothstep(0.35, 0.85, flow);
    // Dissolve: noise eats the surface away as vFx.x rises.
    float d = fbm(vUv * vec2(10.0, 5.0) + vec2(t * 0.4, 0.0));
    if (d < vFx.x * 1.05) {
        discard;
    }
    float burn = smoothstep(vFx.x * 1.05 + 0.06, vFx.x * 1.05, d); // bright rim at the dissolving edge
    float edge = smoothstep(0.3, 0.0, vUv.y);
    float fres = vFx.y;
    float light = clamp(edge * 0.9 + fres * 0.6 + streak * 0.35 + burn * 0.5, 0.0, 1.0);
    vec3 col = mix(vColor.rgb, vec3(0.945, 0.988, 1.0), light);
    float alpha = vColor.a * mask * (0.22 + 0.5 * streak + 0.55 * edge + 0.45 * fres + burn * 0.4);
    fragColor = vec4(col, clamp(alpha, 0.0, 1.0));
}
