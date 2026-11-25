#version 150

// Grand Line explosive fire surface (Bomb): churning fbm flame turbulence rising over the surface, a heat gradient
// from a white-hot core (vUv.y = 0) through flame to a charred smoky rim (vUv.y = 1), fresnel edges that cool to smoke
// (so a fireball reads as a volume, not an opaque sphere) and a noise dissolve with glowing embers at the burning edge.
// Vertex colour tints the flame; vertex alpha fades the whole surface.

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
        p = p * 2.07 + vec2(3.1, 7.4);
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
    // Domain-warped turbulence rising through the flame.
    vec2 q = vec2(vUv.x * 6.0, vUv.y * 3.0 - t * 1.6);
    float warp = fbm(q + vec2(t * 0.3, 0.0));
    float flame = fbm(q * 1.7 + warp * 1.8);
    // Dissolve: noise burns the surface away; a hot ember rim at the edge.
    float d = fbm(vUv * vec2(9.0, 6.0) + vec2(0.0, -t * 0.5));
    float cut = vFx.x * 1.08;
    if (d < cut) {
        discard;
    }
    float ember = smoothstep(cut + 0.07, cut, d) * step(0.001, vFx.x);
    // Heat: hottest at the core and where the turbulence peaks; cools toward the rim and edge-on (fresnel).
    float heat = clamp((1.0 - vUv.y) * 0.75 + flame * 0.65 - vFx.y * 0.55, 0.0, 1.0);
    vec3 core = vec3(1.0, 0.957, 0.741);   // #FFF4BD
    vec3 hot = vec3(1.0, 0.808, 0.329);    // #FFCE54
    vec3 fire = vec3(1.0, 0.549, 0.169);   // #FF8C2B
    vec3 outer = vec3(0.851, 0.29, 0.137); // #D94A23
    vec3 smoke = vec3(0.467, 0.443, 0.424);// #77716C
    vec3 col = heat > 0.75 ? mix(hot, core, (heat - 0.75) / 0.25)
             : heat > 0.45 ? mix(fire, hot, (heat - 0.45) / 0.3)
             : heat > 0.2 ? mix(outer, fire, (heat - 0.2) / 0.25)
             : mix(smoke, outer, heat / 0.2);
    col *= mix(vec3(1.0), vColor.rgb, 0.35);
    col = mix(col, hot, ember * 0.8);
    float alpha = vColor.a * mask * clamp(0.35 + heat * 0.75 + ember * 0.5 - vFx.y * 0.35, 0.0, 1.0);
    fragColor = vec4(col, clamp(alpha, 0.0, 1.0));
}
