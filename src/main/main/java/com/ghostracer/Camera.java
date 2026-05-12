package com.ghostracer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Vector3f position = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Vector3f smoothPos = new Vector3f();
    private final Vector3f smoothTarget = new Vector3f();
    private boolean freeCam = false;
    private float freePitch = -10f;
    private float freeYaw = 0f;

    // modes: 0=chase, 1=first person, 2=free
    private int mode = 0;

    // chase cam
    private static final float BASE_DIST = 7f;
    private static final float BASE_HEIGHT = 3.2f;
    private static final float FAST_DIST = 4.5f;
    private static final float FAST_HEIGHT = 1.8f;
    private static final float LOOK_AHEAD = 8f;

    // first person
    private static final float FP_HEIGHT = 1.0f;
    private static final float FP_FORWARD = 0.5f;
    private float fpYawOffset = 0;

    public void cycleMode() {
        mode = (mode + 1) % 3;
        freeCam = (mode == 2);
        fpYawOffset = 0;
    }

    public void toggleFree() { cycleMode(); }
    public boolean isFreeCam() { return mode == 2; }
    public boolean isFirstPerson() { return mode == 1; }
    public int getMode() { return mode; }

    public void updateFree(float dx, float dy) {
        if (mode == 2) {
            freeYaw += dx * 0.3f;
            freePitch += dy * 0.3f;
            freePitch = Math.max(-89f, Math.min(89f, freePitch));
        } else if (mode == 1) {
            // mouse look in first person
            fpYawOffset += dx * 0.15f;
            fpYawOffset = MathUtil.clamp(fpYawOffset, -90f, 90f);
        }
    }

    public void update(Vector3f carPos, float carYaw, float speed, float maxSpeed, float dt) {
        if (mode == 2) {
            float rad = (float) Math.toRadians(freeYaw);
            float pitchRad = (float) Math.toRadians(freePitch);
            float dist = 15f;
            position.set(
                carPos.x + (float)(Math.sin(rad) * Math.cos(pitchRad)) * dist,
                carPos.y + (float)(Math.sin(pitchRad)) * dist + 3f,
                carPos.z + (float)(Math.cos(rad) * Math.cos(pitchRad)) * dist
            );
            target.set(carPos).add(0, 0.8f, 0);
            smoothPos.set(position);
            smoothTarget.set(target);
            return;
        }

        float sf = Math.min(speed / maxSpeed, 1f);

        if (mode == 1) {
            // first person: inside car looking forward
            float fx = (float) Math.sin(carYaw);
            float fz = (float) Math.cos(carYaw);
            position.set(
                carPos.x + fx * FP_FORWARD,
                carPos.y + FP_HEIGHT,
                carPos.z + fz * FP_FORWARD
            );

            float lookDist = 10f + sf * 15f;
            float totalYaw = carYaw + (float) Math.toRadians(fpYawOffset);
            float lx = (float) Math.sin(totalYaw);
            float lz = (float) Math.cos(totalYaw);
            target.set(
                position.x + lx * lookDist,
                position.y - 0.3f,
                position.z + lz * lookDist
            );

            // tight follow in first person
            float smooth = 1.0f - (float) Math.pow(0.0001, dt);
            smoothPos.lerp(position, smooth);
            smoothTarget.lerp(target, smooth);
            return;
        }

        // chase cam — gets lower and closer at speed
        float dist = BASE_DIST + (FAST_DIST - BASE_DIST) * sf;
        float height = BASE_HEIGHT + (FAST_HEIGHT - BASE_HEIGHT) * sf;

        float behindX = -(float) Math.sin(carYaw) * dist;
        float behindZ = -(float) Math.cos(carYaw) * dist;
        position.set(carPos.x + behindX, carPos.y + height, carPos.z + behindZ);

        float lookAhead = LOOK_AHEAD * sf;
        float aheadX = (float) Math.sin(carYaw) * lookAhead;
        float aheadZ = (float) Math.cos(carYaw) * lookAhead;
        target.set(carPos.x + aheadX, carPos.y + 0.4f, carPos.z + aheadZ);

        // responsive follow — snappier
        float smooth = 1.0f - (float) Math.pow(0.001, dt);
        smoothPos.lerp(position, smooth);
        smoothTarget.lerp(target, smooth);
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(smoothPos, smoothTarget, new Vector3f(0, 1, 0));
    }

    public Vector3f getPosition() { return new Vector3f(smoothPos); }
}
