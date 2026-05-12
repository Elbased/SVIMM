#version 330 core
out vec4 outColor;

in vec2 uv;

void main() {
    float t = uv.y;
    vec3 top = vec3(0.04, 0.05, 0.10);
    vec3 mid = vec3(0.10, 0.12, 0.18);
    vec3 bot = vec3(0.18, 0.20, 0.25);

    vec3 color = mix(bot, mid, smoothstep(0.0, 0.5, t));
    color = mix(color, top, smoothstep(0.5, 1.0, t));

    outColor = vec4(color, 1.0);
}
