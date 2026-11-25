#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vColor;
out vec2 vUv;
out vec2 vFx; // x: dissolve (0 solid .. 1 gone), y: fresnel edge factor (0 face-on .. 1 edge-on)

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    vUv = UV0;
    vFx = vec2(UV2) / 240.0;
}
