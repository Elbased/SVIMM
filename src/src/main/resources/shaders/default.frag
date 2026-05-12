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

void main() {
    vec3 norm = normalize(fragNormal);
    vec3 light = normalize(-lightDir);

    float diff = max(dot(norm, light), 0.0);
    float ambient = 0.2;

    vec3 viewDir = normalize(viewPos - fragPos);
    vec3 halfDir = normalize(light + viewDir);
    float spec = pow(max(dot(norm, halfDir), 0.0), 64.0) * 0.35;

    vec3 baseColor = fragColor;
    if (useTex == 1) {
        vec4 texColor = texture(tex, fragUV);
        baseColor = texColor.rgb;
    }

    vec3 result = baseColor * (ambient + diff * 0.8) + vec3(spec);
    outColor = vec4(result, alpha);
}
