#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec3 aNormal;

uniform mat4 uModel;       // local -> world
uniform mat4 uViewProj;    // world -> clip  (P * V, computed once per frame on the CPU)
uniform mat3 uNormalMat;   // transpose(inverse(mat3(model))): keeps normals perpendicular under non-uniform scale

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUV;

void main() {
    vec4 world = uModel * vec4(aPos, 1.0);
    vWorldPos = world.xyz;
    vNormal   = normalize(uNormalMat * aNormal);
    vUV       = aUV;
    gl_Position = uViewProj * world;
}
