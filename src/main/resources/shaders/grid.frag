#version 330 core
in vec3 worldPos;

uniform vec3 viewPos;

out vec4 outColor;

void main() {
    vec2 coord = worldPos.xz;

    // minor grid (every 10 units)
    vec2 deriv = fwidth(coord);
    vec2 grid10 = abs(fract(coord / 10.0 - 0.5) - 0.5) / deriv * 10.0;
    float line10 = 1.0 - min(min(grid10.x, grid10.y), 1.0);

    // major grid (every 50 units)
    vec2 grid50 = abs(fract(coord / 50.0 - 0.5) - 0.5) / deriv * 50.0;
    float line50 = 1.0 - min(min(grid50.x, grid50.y), 1.0);

    // axis lines
    vec2 axisGrid = abs(coord) / deriv;
    float axisLine = 1.0 - min(min(axisGrid.x, axisGrid.y), 1.0);

    // base color
    vec3 baseColor = vec3(0.18, 0.19, 0.22);
    vec3 minorColor = vec3(0.28, 0.30, 0.34);
    vec3 majorColor = vec3(0.42, 0.44, 0.50);
    vec3 axisColor = vec3(0.55, 0.58, 0.65);

    vec3 color = baseColor;
    color = mix(color, minorColor, line10 * 0.6);
    color = mix(color, majorColor, line50 * 0.8);
    color = mix(color, axisColor, axisLine * 0.5);

    // quadrant tint
    float qx = step(0.0, worldPos.x) * 2.0 - 1.0;
    float qz = step(0.0, worldPos.z) * 2.0 - 1.0;
    vec3 tint = vec3(0.0);
    if (qx > 0.0 && qz > 0.0) tint = vec3(0.08, 0.02, 0.02);
    else if (qx < 0.0 && qz > 0.0) tint = vec3(0.02, 0.08, 0.02);
    else if (qx < 0.0 && qz < 0.0) tint = vec3(0.02, 0.02, 0.08);
    else tint = vec3(0.08, 0.06, 0.02);
    color += tint;

    // fresnel-ish reflection
    vec3 viewDir = normalize(viewPos - worldPos);
    float fresnel = pow(1.0 - max(dot(viewDir, vec3(0.0, 1.0, 0.0)), 0.0), 4.0);
    color = mix(color, vec3(0.35, 0.37, 0.42), fresnel * 0.5);

    // distance fade
    float dist = length(viewPos.xz - worldPos.xz);
    float fade = 1.0 - smoothstep(180.0, 300.0, dist);

    outColor = vec4(color, fade);
}
