#version 330 core
in vec2 vUV;
uniform sampler2D uScene;
uniform vec2 uTexel;       // size of one SOURCE texel
uniform float uThreshold;
out vec4 fragColor;
void main() {
    vec3 c = texture(uScene, vUV + uTexel * vec2( 1.0,  1.0)).rgb
           + texture(uScene, vUV + uTexel * vec2(-1.0,  1.0)).rgb
           + texture(uScene, vUV + uTexel * vec2( 1.0, -1.0)).rgb
           + texture(uScene, vUV + uTexel * vec2(-1.0, -1.0)).rgb;
    c = min(c * 0.25, vec3(30.0));
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    fragColor = vec4(c * (max(l - uThreshold, 0.0) / max(l, 0.0001)), 1.0);
}
