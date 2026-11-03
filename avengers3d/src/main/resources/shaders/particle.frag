#version 330 core
in vec2 vCorner;
in vec4 vColor;
uniform int uAdditive;
out vec4 fragColor;
void main() {
    float d = length(vCorner);
    if (d > 1.0) discard;
    float fall = (1.0 - d); fall *= fall;
    if (uAdditive == 1) fragColor = vec4(vColor.rgb * vColor.a * fall, 0.0);
    else                fragColor = vec4(vColor.rgb, vColor.a * fall);
}
