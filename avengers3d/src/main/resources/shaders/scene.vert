#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec3 aNormal;

uniform mat4 uModel;       // local -> world
uniform mat4 uViewProj;    // world -> clip
uniform mat3 uNormalMat;   // inverse-transpose of the model's 3x3

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUV;

void main() {
    vec4 world = uModel * vec4(aPos, 1.0);
    vWorldPos = world.xyz;
    vNormal = normalize(uNormalMat * aNormal);
    vUV = aUV;
    gl_Position = uViewProj * world;
}
