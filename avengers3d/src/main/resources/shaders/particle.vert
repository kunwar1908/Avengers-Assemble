#version 330 core
layout(location = 0) in vec4 iPosSize;   // xyz position, w size (world units)
layout(location = 1) in vec4 iColor;
uniform mat4 uViewProj;
uniform vec3 uRight;
uniform vec3 uUp;
out vec2 vCorner;
out vec4 vColor;
void main() {
    // Billboard: expand the point along the camera's right/up axes so the quad always faces the camera.
    vec2 c = vec2(float(gl_VertexID & 1) * 2.0 - 1.0, float(gl_VertexID >> 1) * 2.0 - 1.0);
    vec3 world = iPosSize.xyz + (uRight * c.x + uUp * c.y) * iPosSize.w;
    vCorner = c;
    vColor = iColor;
    gl_Position = uViewProj * vec4(world, 1.0);
}
