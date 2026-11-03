#version 330 core
out vec2 vUV;
void main() {
    // One oversized triangle covers the screen: ids 0,1,2 -> (0,0) (2,0) (0,2) in UV space.
    vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
    vUV = p;
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}
