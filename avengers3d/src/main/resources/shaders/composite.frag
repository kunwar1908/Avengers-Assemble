#version 330 core
in vec2 vUV;
uniform sampler2D uScene;
uniform sampler2D uBloom0;
uniform sampler2D uBloom1;
uniform float uExposure;
uniform float uBloom;
uniform float uFade;
uniform float uVignette;
uniform float uSaturation;
uniform vec4 uOverlay;     // rgb colour, a = strength (damage flash, heal pulse ...)
out vec4 fragColor;

// Narkowicz ACES filmic approximation: compresses HDR values into 0..1 with a cinematic toe/shoulder.
vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}
void main() {
    vec3 c = texture(uScene, vUV).rgb + (texture(uBloom0, vUV).rgb * 0.75 + texture(uBloom1, vUV).rgb * 0.95) * uBloom;
    c = aces(c * uExposure);
    c = pow(c, vec3(1.0 / 2.2));                                   // linear -> display gamma
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, uSaturation);
    vec2 q = vUV - 0.5;
    c *= clamp(1.0 - dot(q, q) * uVignette * 2.2, 0.0, 1.0);
    c = mix(c, uOverlay.rgb, uOverlay.a);
    fragColor = vec4(c * (1.0 - uFade), 1.0);
}
