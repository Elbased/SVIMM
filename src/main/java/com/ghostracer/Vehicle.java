package com.ghostracer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Vehicle {
    public final Vector3f position = new Vector3f(0, 0, 0);
    public float yaw = 0;
    public float speed = 0;
    public float steerAngle = 0;

    private Mesh carMesh;
    private int carTexture;

    public boolean crashed = false;
    public float crashTimer = 0;
    public float crashPushX = 0;

    private static final float MAX_SPEED = 60f;
    private static final float ACCEL = 18f;
    private static final float BRAKE_FORCE = 24f;
    private static final float HANDBRAKE_FORCE = 50f;
    private static final float DRAG = 0.10f;
    private static final float MAX_STEER = 0.7f;
    private static final float STEER_SPEED = 5.0f;
    private static final float STEER_RETURN = 7.0f;
    private static final float COAST_SPEED = 10f;

    // model path, texture path, scale, yOffset, extraYaw(degrees)
    private static final String[][] CAR_DEFS = {
        {"/models/porsche-911.obj", "/textures/porsche-skin.bmp", "1.5", "0.55", "180"},
        {"/models/traffic-Bugatti.obj", "", "1.0", "0.0", "0"},
        {"/models/traffic-BMW.obj", "", "1.0", "0.0", "0"},
        {"/models/traffic-Supra.obj", "", "1.0", "0.0", "0"},
        {"/models/traffic-R35.obj", "", "1.0", "0.0", "0"},
        {"/models/traffic-Dodge.obj", "", "1.0", "0.0", "0"},
    };
    private int currentModel = 0;
    private float modelScale = 1.5f;
    private float modelYOffset = 0.55f;
    private float modelExtraYaw = (float) Math.toRadians(180);

    public void init() {
        loadModel(currentModel);
    }

    private boolean hasTexture = true;

    private void loadModel(int index) {
        if (carMesh != null) carMesh.cleanup();
        carMesh = ObjLoader.load(CAR_DEFS[index][0]);
        String texPath = CAR_DEFS[index][1];
        if (texPath != null && !texPath.isEmpty()) {
            carTexture = TextureLoader.load(texPath);
            hasTexture = true;
        } else {
            carTexture = 0;
            hasTexture = false;
        }
        modelScale = Float.parseFloat(CAR_DEFS[index][2]);
        modelYOffset = Float.parseFloat(CAR_DEFS[index][3]);
        modelExtraYaw = (float) Math.toRadians(Float.parseFloat(CAR_DEFS[index][4]));
    }

    public void nextModel() {
        currentModel = (currentModel + 1) % CAR_DEFS.length;
        loadModel(currentModel);
    }

    public void update(float dt) {
        float throttle = 0;
        if (Input.isDown(org.lwjgl.glfw.GLFW.GLFW_KEY_W)) throttle = 1;
        if (Input.isDown(org.lwjgl.glfw.GLFW.GLFW_KEY_S)) throttle = -1;
        boolean handbrake = Input.isDown(org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE);

        float steerInput = 0;
        if (Input.isDown(org.lwjgl.glfw.GLFW.GLFW_KEY_A)) steerInput = 1;
        if (Input.isDown(org.lwjgl.glfw.GLFW.GLFW_KEY_D)) steerInput = -1;

        // speed-dependent steering
        float speedFactor = 1.0f - (speed / MAX_SPEED) * 0.6f;
        float effectiveMaxSteer = MAX_STEER * speedFactor;

        if (steerInput != 0) {
            float target = steerInput * effectiveMaxSteer;
            steerAngle += (target - steerAngle) * STEER_SPEED * dt;
        } else {
            steerAngle -= steerAngle * STEER_RETURN * dt;
        }

        // speed
        if (handbrake) {
            speed -= HANDBRAKE_FORCE * dt;
        } else if (throttle > 0) {
            speed += ACCEL * (1f - speed / MAX_SPEED) * dt;
        } else if (throttle < 0) {
            speed -= BRAKE_FORCE * dt;
        }
        speed -= speed * DRAG * dt;

        if (speed < COAST_SPEED && throttle >= 0 && !handbrake) {
            speed += (COAST_SPEED - speed) * 3f * dt;
        }
        speed = MathUtil.clamp(speed, 1f, MAX_SPEED);

        // crash recovery
        if (crashTimer > 0) {
            crashTimer -= dt;
            position.x += crashPushX * dt * 3f;
            crashPushX *= (1f - 4f * dt);
        }

        // yaw
        float turnRate = steerAngle * (2.0f - speed / MAX_SPEED * 0.8f);
        yaw += turnRate * dt;

        if (Math.abs(steerInput) < 0.1f) {
            yaw -= yaw * 0.8f * dt;
        }

        // movement
        position.x += (float) Math.sin(yaw) * speed * dt;
        position.z += (float) Math.cos(yaw) * speed * dt;

        // road clamp
        float maxX = Highway.ROAD_WIDTH / 2f - 0.6f;
        if (position.x > maxX) { position.x = maxX; yaw = Math.min(yaw, 0); }
        if (position.x < -maxX) { position.x = -maxX; yaw = Math.max(yaw, 0); }
    }

    public void onCollision(float otherX) {
        crashed = true;
        crashTimer = 0.5f;
        speed *= 0.4f;
        crashPushX = (position.x - otherX) * 6f;
    }

    public void render(Shader shader, Matrix4f view, Matrix4f projection) {
        render(shader, view, projection, true);
    }

    public void render(Shader shader, Matrix4f view, Matrix4f projection, boolean visible) {
        if (!visible) return;
        shader.bind();
        shader.setMat4("view", view);
        shader.setMat4("projection", projection);
        shader.setVec3("lightDir", new Vector3f(0.3f, -0.8f, 0.5f));
        shader.setFloat("alpha", 1.0f);
        if (hasTexture) {
            shader.setInt("useTex", 1);
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, carTexture);
        } else {
            shader.setInt("useTex", 0);
        }

        Matrix4f model = new Matrix4f()
            .translate(position.x, position.y + modelYOffset, position.z)
            .rotateY(yaw + modelExtraYaw)
            .scale(modelScale);
        shader.setMat4("model", model);
        carMesh.draw();
    }

    public void cleanup() {
        if (carMesh != null) carMesh.cleanup();
    }
}
