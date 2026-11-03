#version 330 core
in vec2 vUV;
uniform sampler2D uImage;
uniform vec2 uDir;         // (1/width, 0) or (0, 1/height)
out vec4 fragColor;
void main() {
    // 9-tap Gaussian folded into 5 bilinear fetches
    vec3 c = texture(uImage, vUV).rgb * 0.2270270270;
    c += texture(uImage, vUV + uDir * 1.3846153846).rgb * 0.3162162162;
    c += texture(uImage, vUV - uDir * 1.3846153846).rgb * 0.3162162162;
    c += texture(uImage, vUV + uDir * 3.2307692308).rgb * 0.0702702703;
    c += texture(uImage, vUV - uDir * 3.2307692308).rgb * 0.0702702703;
    fragColor = vec4(c, 1.0);
}
