#version 330 core
in vec3 fragPos;
in vec3 fragNormal;
in vec3 fragColor;
in vec2 fragUV;

uniform vec3 lightDir;
uniform vec3 viewPos;
uniform float alpha;
uniform sampler2D tex;
uniform int useTex;

out vec4 outColor;

// warm golden sun color
const vec3 sunColor = vec3(1.0, 0.9, 0.65);
const vec3 skyAmbient = vec3(0.35, 0.45, 0.65);
const vec3 fogColor = vec3(0.55, 0.65, 0.85);
const float fogDensity = 0.004;

vec3 saturate(vec3 c, float amount) {
    float grey = dot(c, vec3(0.299, 0.587, 0.114));
    return mix(vec3(grey), c, amount);
}

void main() {
    vec3 norm = normalize(fragNormal);
    vec3 light = normalize(-lightDir);
    vec3 viewDir = normalize(viewPos - fragPos);

    // diffuse — warm sun
    float diff = max(dot(norm, light), 0.0);
    float wrap = max(dot(norm, light) * 0.5 + 0.5, 0.0); // half-lambert

    // specular — sharp highlights
    vec3 halfDir = normalize(light + viewDir);
    float spec = pow(max(dot(norm, halfDir), 0.0), 128.0) * 0.5;

    // rim light (backlight glow)
    float rim = 1.0 - max(dot(viewDir, norm), 0.0);
    rim = pow(rim, 3.0) * 0.3;

    // base color
    vec3 baseColor = fragColor;
    if (useTex == 1) {
        vec4 texColor = texture(tex, fragUV);
        baseColor = texColor.rgb;
    }

    // lighting
    vec3 ambient = skyAmbient * 0.25;
    vec3 diffuse = sunColor * wrap * 0.75;
    vec3 specular = sunColor * spec;
    vec3 rimLight = sunColor * rim;

    vec3 result = baseColor * (ambient + diffuse) + specular + rimLight;

    // high saturation boost
    result = saturate(result, 1.45);

    // tone mapping (simple reinhard)
    result = result / (result + vec3(1.0));

    // gamma correction
    result = pow(result, vec3(1.0 / 2.2));

    // distance fog
    float dist = length(viewPos - fragPos);
    float fog = 1.0 - exp(-dist * fogDensity * dist * fogDensity);
    fog = clamp(fog, 0.0, 1.0);
    result = mix(result, fogColor, fog);

    outColor = vec4(result, alpha);
}
