#version 330 core
out vec4 outColor;
in vec2 uv;

uniform sampler2D skyTex;
uniform int useHDR;
uniform mat4 invViewProj;

const float PI = 3.14159265359;

// equirectangular mapping: direction -> UV
vec2 dirToEquirect(vec3 dir) {
    float phi = atan(dir.z, dir.x); // -PI..PI
    float theta = asin(clamp(dir.y, -1.0, 1.0)); // -PI/2..PI/2
    return vec2(phi / (2.0 * PI) + 0.5, theta / PI + 0.5);
}

void main() {
    if (useHDR == 1) {
        // reconstruct view direction from screen UV
        vec4 clipPos = vec4(uv * 2.0 - 1.0, 1.0, 1.0);
        vec4 worldDir = invViewProj * clipPos;
        vec3 dir = normalize(worldDir.xyz / worldDir.w);

        vec2 skyUV = dirToEquirect(dir);
        vec3 hdr = texture(skyTex, skyUV).rgb;

        // exposure + tonemap
        hdr *= 1.2;
        hdr = hdr / (hdr + vec3(1.0)); // reinhard
        hdr = pow(hdr, vec3(1.0 / 2.2)); // gamma

        // saturation boost
        float grey = dot(hdr, vec3(0.299, 0.587, 0.114));
        hdr = mix(vec3(grey), hdr, 1.4);

        outColor = vec4(hdr, 1.0);
    } else {
        // fallback procedural sky
        float t = uv.y;
        vec3 skyTop = vec3(0.15, 0.35, 0.75);
        vec3 skyMid = vec3(0.45, 0.60, 0.85);
        vec3 horizon = vec3(0.95, 0.70, 0.40);

        vec3 color = mix(horizon, skyMid, smoothstep(0.0, 0.35, t));
        color = mix(color, skyTop, smoothstep(0.35, 0.85, t));

        vec2 sunPos = vec2(0.65, 0.12);
        float sunDist = length(uv - sunPos);
        color += vec3(1.0, 0.85, 0.50) * exp(-sunDist * 18.0) * 0.8;
        color = mix(color, vec3(1.0, 0.95, 0.80), smoothstep(0.035, 0.025, sunDist));
        color += vec3(1.0, 0.85, 0.50) * exp(-sunDist * 5.0) * 0.3;

        outColor = vec4(color, 1.0);
    }
}
