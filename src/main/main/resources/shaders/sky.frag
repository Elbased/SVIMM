#version 330 core
out vec4 outColor;
in vec2 uv;

// golden hour sky
const vec3 skyTop = vec3(0.15, 0.35, 0.75);
const vec3 skyMid = vec3(0.45, 0.60, 0.85);
const vec3 horizon = vec3(0.95, 0.70, 0.40);
const vec3 sunGlow = vec3(1.0, 0.85, 0.50);
const vec3 sunCore = vec3(1.0, 0.95, 0.80);

void main() {
    float t = uv.y;

    // sky gradient
    vec3 color = mix(horizon, skyMid, smoothstep(0.0, 0.35, t));
    color = mix(color, skyTop, smoothstep(0.35, 0.85, t));

    // sun disc (low on horizon, slightly right)
    vec2 sunPos = vec2(0.65, 0.12);
    float sunDist = length(uv - sunPos);
    float sun = exp(-sunDist * 18.0);
    float sunDisc = smoothstep(0.035, 0.025, sunDist);
    color += sunGlow * sun * 0.8;
    color = mix(color, sunCore, sunDisc);

    // sun halo
    float halo = exp(-sunDist * 5.0) * 0.3;
    color += sunGlow * halo;

    // fake clouds (procedural noise bands)
    float cloud1 = smoothstep(0.48, 0.52, sin(uv.x * 12.0 + 1.5) * 0.5 + 0.5 + t * 0.3);
    float cloud2 = smoothstep(0.50, 0.54, sin(uv.x * 8.0 + 3.0) * 0.5 + 0.5 + t * 0.4);
    float cloud3 = smoothstep(0.45, 0.50, sin(uv.x * 18.0 - 1.0) * 0.5 + 0.5 + t * 0.2);
    float clouds = (cloud1 + cloud2 + cloud3) * 0.12;
    color += vec3(clouds) * 0.5;

    // bottom gradient glow (ground ambient)
    float groundGlow = exp(-t * 8.0) * 0.15;
    color += horizon * groundGlow;

    // slight vignette
    float vig = 1.0 - length(uv - 0.5) * 0.6;
    color *= vig;

    outColor = vec4(color, 1.0);
}
