package com.ghostracer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Vector3f position = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Vector3f smoothPos = new Vector3f();
    private final Vector3f smoothTarget = new Vector3f();

    // modes: 0=chase, 1=first person, 2=free
    private int mode = 0;
    private float freePitch = -10f;
    private float freeYaw = 0f;

    // chase cam orbit
    private float chaseYawOffset = 0f;
    private float chasePitchOffset = 0f;

    // chase cam
    private static final float BASE_DIST = 10f;
    private static final float BASE_HEIGHT = 5.5f;
    private static final float FAST_DIST = 7f;
    private static final float FAST_HEIGHT = 3.5f;
    private static final float LOOK_AHEAD = 10f;

    // first person
    private static final float FP_HEIGHT = 1.0f;
    private static final float FP_FORWARD = 0.5f;
    private float fpYawOffset = 0;

    public void cycleMode() {
        mode = (mode + 1) % 3;
        fpYawOffset = 0;
        chaseYawOffset = 0;
        chasePitchOffset = 0;
    }

    public void toggleFree() { cycleMode(); }
    public boolean isFreeCam() { return mode == 2; }
    public boolean isFirstPerson() { return mode == 1; }
    public int getMode() { return mode; }

    public void updateFree(float dx, float dy) {
        if (mode == 2) {
            freeYaw += dx * 0.3f;
            freePitch -= dy * 0.3f;
            freePitch = Math.max(-89f, Math.min(89f, freePitch));
        } else if (mode == 1) {
            fpYawOffset += dx * 0.15f;
            fpYawOffset = MathUtil.clamp(fpYawOffset, -120f, 120f);
        } else {
            // chase cam: orbit around car
            chaseYawOffset += dx * 0.2f;
            chasePitchOffset -= dy * 0.15f;
            chasePitchOffset = MathUtil.clamp(chasePitchOffset, -20f, 30f);
            // auto-return to center
            chaseYawOffset *= 0.97f;
            chasePitchOffset *= 0.97f;
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
            float fx = (float) Math.sin(carYaw);
            float fz = (float) Math.cos(carYaw);
            position.set(carPos.x + fx * FP_FORWARD, carPos.y + FP_HEIGHT, carPos.z + fz * FP_FORWARD);

            float lookDist = 10f + sf * 15f;
            float totalYaw = carYaw + (float) Math.toRadians(fpYawOffset);
            target.set(
                position.x + (float) Math.sin(totalYaw) * lookDist,
                position.y - 0.3f,
                position.z + (float) Math.cos(totalYaw) * lookDist
            );

            float smooth = 1.0f - (float) Math.pow(0.0001, dt);
            smoothPos.lerp(position, smooth);
            smoothTarget.lerp(target, smooth);
            return;
        }

        // chase cam — elevated behind, with mouse orbit
        float dist = BASE_DIST + (FAST_DIST - BASE_DIST) * sf;
        float height = BASE_HEIGHT + (FAST_HEIGHT - BASE_HEIGHT) * sf;
        height += chasePitchOffset * 0.1f;

        float orbitYaw = carYaw + (float) Math.toRadians(chaseYawOffset);
        float behindX = -(float) Math.sin(orbitYaw) * dist;
        float behindZ = -(float) Math.cos(orbitYaw) * dist;
        position.set(carPos.x + behindX, carPos.y + height, carPos.z + behindZ);

        // look at a point ahead of the car
        float ahead = LOOK_AHEAD * sf;
        target.set(
            carPos.x + (float) Math.sin(carYaw) * ahead,
            carPos.y + 0.5f,
            carPos.z + (float) Math.cos(carYaw) * ahead
        );

        float smooth = 1.0f - (float) Math.pow(0.002, dt);
        smoothPos.lerp(position, smooth);
        smoothTarget.lerp(target, smooth);
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(smoothPos, smoothTarget, new Vector3f(0, 1, 0));
    }

    public Vector3f getPosition() { return new Vector3f(smoothPos); }

    /** Direct control for menu/intro — bypass normal camera logic */
    public void setDirect(Vector3f pos, Vector3f target) {
        position.set(pos);
        this.target.set(target);
        smoothPos.set(pos);
        smoothTarget.set(target);
    }
}
