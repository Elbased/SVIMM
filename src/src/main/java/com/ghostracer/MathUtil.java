package com.ghostracer;

public class MathUtil {
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    public static float approach(float current, float target, float rate, float dt) {
        float diff = target - current;
        if (Math.abs(diff) < rate * dt) return target;
        return current + Math.signum(diff) * rate * dt;
    }
}
