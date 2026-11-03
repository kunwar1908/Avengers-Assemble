#version 330 core
in vec2 vUV;
uniform mat4 uInvViewProj;
uniform vec3 uCamPos;
uniform vec3 uSkyTop;
uniform vec3 uSkyHorizon;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uFogColor;
uniform float uStars;
uniform float uAurora;
uniform float uClouds;
uniform float uTime;
out vec4 fragColor;

float hash21(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float hash31(vec3 p) { return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453); }
float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1, 0)), f.x), mix(hash21(i + vec2(0, 1)), hash21(i + vec2(1, 1)), f.x), f.y);
}
float fbm(vec2 p) { float s = 0.0, a = 0.5; for (int i = 0; i < 4; i++) { s += a * vnoise(p); p *= 2.03; a *= 0.5; } return s; }
vec3 lin(vec3 c) { return pow(c, vec3(2.2)); }

void main() {
    // Rebuild the world-space view ray for this pixel from the inverse view-projection matrix.
    vec4 farPt = uInvViewProj * vec4(vUV * 2.0 - 1.0, 1.0, 1.0);
    vec3 dir = normalize(farPt.xyz / farPt.w - uCamPos);

    vec3 hor = lin(uSkyHorizon), top = lin(uSkyTop);
    vec3 col = mix(hor, top, pow(clamp(dir.y, 0.0, 1.0), 0.55));
    if (dir.y < 0.0) col = mix(hor, lin(uFogColor) * 0.5, clamp(-dir.y * 5.0, 0.0, 1.0));

    vec3 sunPos = -normalize(uSunDir);
    float sd = max(dot(dir, sunPos), 0.0);
    col += uSunColor * (pow(sd, 900.0) * 8.0 + pow(sd, 14.0) * 0.30);     // disc + halo

    if (uClouds > 0.0 && dir.y > 0.02) {
        vec2 p = dir.xz / (dir.y + 0.15) * 1.6 + vec2(uTime * 0.012, 0.0);
        float c = smoothstep(0.48, 0.85, fbm(p));
        vec3 cloudCol = mix(hor * 1.3, vec3(0.9), 0.35) * (0.6 + 0.8 * sd);
        col = mix(col, cloudCol, c * uClouds * clamp(dir.y * 4.0, 0.0, 1.0));
    }
    if (uStars > 0.0 && dir.y > 0.0) {
        float r = hash31(floor(dir * 300.0));
        float tw = 0.55 + 0.45 * sin(uTime * 2.5 + r * 60.0);
        col += vec3(1.0, 0.95, 0.9) * step(0.9972, r) * tw * uStars * 2.5 * smoothstep(0.0, 0.25, dir.y);
    }
    if (uAurora > 0.0 && dir.y > 0.04) {
        float t = dir.x * 3.0 + sin(dir.z * 4.0 + uTime * 0.4) * 0.9 + uTime * 0.05;
        float band = smoothstep(0.1, 1.0, sin(t * 2.0) * 0.5 + 0.5);
        float vert = smoothstep(0.04, 0.3, dir.y) * smoothstep(0.95, 0.35, dir.y);
        col += mix(vec3(0.1, 1.0, 0.5), vec3(0.5, 0.3, 1.0), dir.y) * band * vert * uAurora * 0.6;
    }
    fragColor = vec4(col, 1.0);
}
