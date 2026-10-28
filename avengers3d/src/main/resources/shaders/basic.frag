#version 330 core
in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUV;

uniform sampler2D uTexture;
uniform vec3 uTint;        // per-part colour multiplier
uniform vec3 uLightDir;    // direction the light travels, world space, normalized
uniform vec3 uCameraPos;
uniform vec3 uFogColor;
uniform float uFlash;      // 0..1 : blend toward white (hit flash / glow)

out vec4 fragColor;

void main() {
    // Lambert diffuse: intensity = max(0, N . L) where L points from surface toward the light.
    float diff = max(dot(normalize(vNormal), -uLightDir), 0.0);
    vec3 albedo = texture(uTexture, vUV).rgb * uTint;
    vec3 viewDir = normalize(uCameraPos - vWorldPos);
    vec3 halfDir = normalize(viewDir - uLightDir);
    float specular = pow(max(dot(normalize(vNormal), halfDir), 0.0), 32.0);
    vec3 lit = albedo * (0.3 + 0.8 * diff) + vec3(0.18) * specular;
    float fog = smoothstep(90.0, 260.0, distance(uCameraPos, vWorldPos));
    lit = mix(lit, uFogColor, fog * 0.35);
    fragColor = vec4(mix(lit, vec3(1.0), uFlash), 1.0);
}
