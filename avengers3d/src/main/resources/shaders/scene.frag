#version 330 core
in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUV;

uniform sampler2D uTexture;          // rgb = colour, alpha = emissive mask
uniform sampler2DShadow uShadow;
uniform mat4 uLightVP;
uniform vec3 uTint;
uniform float uEmissive;
uniform float uTexScale;             // > 0: world-space (triplanar) texture with this many metres per tile
uniform float uSpec;
uniform float uFlash;
uniform float uRim;
uniform vec3 uLightDir;              // direction the sun light travels
uniform vec3 uSunColor;
uniform vec3 uAmbientSky;
uniform vec3 uAmbientGround;
uniform vec3 uCameraPos;
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform int uLightCount;
uniform vec4 uLightPos[8];           // xyz position, w radius
uniform vec3 uLightCol[8];

out vec4 fragColor;

vec3 toLinear(vec3 c) { return pow(c, vec3(2.2)); }

// 3x3 percentage-closer filtering: averages 9 hardware depth comparisons -> soft shadow edges.
float shadowFactor(vec3 N) {
    vec4 ls = uLightVP * vec4(vWorldPos + N * 0.08, 1.0);
    vec3 p = ls.xyz / ls.w * 0.5 + 0.5;
    if (p.z > 1.0 || p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0) return 1.0;
    float texel = 1.0 / 2048.0;
    float s = 0.0;
    for (int x = -1; x <= 1; x++)
        for (int y = -1; y <= 1; y++)
            s += texture(uShadow, vec3(p.xy + vec2(x, y) * texel, p.z - 0.0008));
    return s / 9.0;
}

void main() {
    vec3 N = normalize(vNormal);
    vec2 uv = vUV;
    if (uTexScale > 0.0) {            // project the texture along the dominant axis of the normal
        vec3 a = abs(N);
        if (a.y >= a.x && a.y >= a.z) uv = vWorldPos.xz / uTexScale;
        else if (a.x >= a.z)          uv = vec2(vWorldPos.z, vWorldPos.y) / uTexScale;
        else                          uv = vec2(vWorldPos.x, vWorldPos.y) / uTexScale;
    }
    vec4 texel = texture(uTexture, uv);
    vec3 albedo = toLinear(texel.rgb * uTint);

    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 L = -uLightDir;
    float ndl = max(dot(N, L), 0.0);
    float sh = ndl > 0.0 ? shadowFactor(N) : 1.0;

    vec3 hemi = mix(uAmbientGround, uAmbientSky, N.y * 0.5 + 0.5);       // sky/ground ambient
    vec3 color = albedo * (hemi + uSunColor * ndl * sh);

    vec3 H = normalize(L + V);                                           // Blinn-Phong highlight
    color += uSunColor * pow(max(dot(N, H), 0.0), 48.0) * uSpec * sh * step(0.0, ndl);

    color += uAmbientSky * 2.2 * pow(1.0 - max(dot(N, V), 0.0), 3.0) * uRim;   // rim light

    for (int i = 0; i < uLightCount; i++) {                              // dynamic point lights
        vec3 d = uLightPos[i].xyz - vWorldPos;
        float dist = length(d);
        float att = clamp(1.0 - dist / uLightPos[i].w, 0.0, 1.0);
        color += albedo * uLightCol[i] * max(dot(N, d / max(dist, 0.001)), 0.0) * att * att;
    }

    color += albedo * uEmissive + toLinear(texel.rgb) * texel.a * 3.0;   // glow (values > 1 feed the bloom)

    float dist = length(uCameraPos - vWorldPos);
    float fog = 1.0 - exp(-pow(dist * uFogDensity, 2.0));
    color = mix(color, toLinear(uFogColor), clamp(fog, 0.0, 1.0));
    color = mix(color, vec3(2.5), uFlash * 0.85);
    fragColor = vec4(color, 1.0);
}
