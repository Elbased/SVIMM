package com.ghostracer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

public class Traffic {
    private static final int POOL_SIZE = 14;
    private static final float SPAWN_AHEAD = 160f;
    private static final float DESPAWN_BEHIND = 60f;
    private static final float[] LANE_SPEEDS = {14f, 17f, 21f, 25f};
    private static final float MIN_CAR_GAP = 15f;

    private final List<AICar> cars = new ArrayList<>();
    private Mesh[] models;
    private float[] modelScales;
    private float[] modelYOffsets;
    private float[] modelExtraYaws;
    private float spawnTimer = 0;

    public static class AICar {
        public float x, z, speed;
        public int lane, modelIdx;
        public boolean active = true;
    }

    // model, scale, yOffset, extraYaw(deg) — all semi-realistic pack
    private static final String[][] TRAFFIC_MODELS = {
        {"/models/traffic-BMW.obj",        "1.0", "0.0", "0"},
        {"/models/traffic-Dodge.obj",      "1.0", "0.0", "0"},
        {"/models/traffic-Ford.obj",       "1.0", "0.0", "0"},
        {"/models/traffic-Mini.obj",       "1.0", "0.0", "0"},
        {"/models/traffic-R35.obj",        "1.0", "0.0", "0"},
        {"/models/traffic-Supra.obj",      "1.0", "0.0", "0"},
        {"/models/traffic-CyberTruck.obj", "1.0", "0.0", "0"},
        {"/models/traffic-r34.obj",        "1.0", "0.0", "0"},
    };

    public void init() {
        models = new Mesh[TRAFFIC_MODELS.length];
        modelScales = new float[TRAFFIC_MODELS.length];
        modelYOffsets = new float[TRAFFIC_MODELS.length];
        modelExtraYaws = new float[TRAFFIC_MODELS.length];
        for (int i = 0; i < TRAFFIC_MODELS.length; i++) {
            models[i] = ObjLoader.load(TRAFFIC_MODELS[i][0]);
            modelScales[i] = Float.parseFloat(TRAFFIC_MODELS[i][1]);
            modelYOffsets[i] = Float.parseFloat(TRAFFIC_MODELS[i][2]);
            modelExtraYaws[i] = (float) Math.toRadians(Float.parseFloat(TRAFFIC_MODELS[i][3]));
        }
    }

    public void update(float dt, float playerZ) {
        spawnTimer += dt;
        if (spawnTimer > 0.8f) {
            spawnTimer = 0;
            trySpawn(playerZ);
        }

        for (AICar c : cars) {
            if (!c.active) continue;
            c.z += c.speed * dt;
            if (c.z < playerZ - DESPAWN_BEHIND) c.active = false;
        }

        cars.removeIf(c -> !c.active);
    }

    private void trySpawn(float playerZ) {
        if (cars.size() >= POOL_SIZE) return;

        int lane = (int)(Math.random() * Highway.LANE_COUNT);
        float targetZ = playerZ + SPAWN_AHEAD + (float)(Math.random() * 60);

        for (AICar c : cars) {
            if (c.lane == lane && Math.abs(c.z - targetZ) < MIN_CAR_GAP) return;
        }

        AICar car = new AICar();
        car.lane = lane;
        car.x = Highway.getLaneX(lane);
        car.z = targetZ;
        car.speed = LANE_SPEEDS[lane] + (float)(Math.random() * 4 - 2);
        car.modelIdx = (int)(Math.random() * models.length);
        cars.add(car);
    }

    public List<AICar> getCars() { return cars; }

    public void render(Shader shader, Matrix4f view, Matrix4f proj, Vector3f viewPos) {
        shader.bind();
        shader.setMat4("view", view);
        shader.setMat4("projection", proj);
        shader.setVec3("lightDir", new Vector3f(0.3f, -0.8f, 0.5f));
        shader.setVec3("viewPos", viewPos);
        shader.setFloat("alpha", 1.0f);
        shader.setInt("useTex", 0);

        for (AICar c : cars) {
            if (!c.active) continue;
            int mi = c.modelIdx;
            Matrix4f m = new Matrix4f()
                .translate(c.x, modelYOffsets[mi], c.z)
                .rotateY(modelExtraYaws[mi])
                .scale(modelScales[mi]);
            shader.setMat4("model", m);
            models[mi].draw();
        }
    }

    public void cleanup() {
        for (Mesh m : models) if (m != null) m.cleanup();
    }
}
