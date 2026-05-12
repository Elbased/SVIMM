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
    private float modelScale = 2.5f;

    public boolean crashed = false;
    public float crashTimer = 0;
    public float crashPushX = 0;

    private static final float MAX_SPEED = 60f;
    private static final float ACCEL = 16f;
    private static final float BRAKE_FORCE = 22f;
    private static final float HANDBRAKE_FORCE = 45f;
    private static final float DRAG = 0.12f;
    private static final float MAX_STEER = 0.7f;
    private static final float STEER_SPEED = 4.5f;
    private static final float STEER_RETURN = 6.0f;
    private static final float COAST_SPEED = 10f;

    private static final String[] CAR_MODELS = {
        "/models/sedan-sports.obj", "/models/race.obj",
        "/models/race-future.obj", "/models/hatchback-sports.obj",
        "/models/suv-luxury.obj"
    };
    private int currentModel = 0;

    public void init() {
        loadModel(currentModel);
        carTexture = TextureLoader.load("/textures/car-colormap.png");
    }

    private void loadModel(int index) {
        if (carMesh != null) carMesh.cleanup();
        carMesh = ObjLoader.load(CAR_MODELS[index]);
    }

    public void nextModel() {
        currentModel = (currentModel + 1) % CAR_MODELS.length;
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

        // responsive steering
        if (steerInput != 0) {
            steerAngle = MathUtil.approach(steerAngle, steerInput * MAX_STEER, STEER_SPEED, dt);
        } else {
            steerAngle = MathUtil.approach(steerAngle, 0, STEER_RETURN, dt);
        }

        // speed
        if (handbrake) {
            speed -= HANDBRAKE_FORCE * dt;
        } else if (throttle > 0) {
            speed += ACCEL * dt;
        } else if (throttle < 0) {
            speed -= BRAKE_FORCE * dt;
        }
        speed -= speed * DRAG * dt;

        if (speed < COAST_SPEED && throttle >= 0 && !handbrake) {
            speed = MathUtil.approach(speed, COAST_SPEED, 8f, dt);
        }
        speed = MathUtil.clamp(speed, 1f, MAX_SPEED);

        // crash recovery
        if (crashTimer > 0) {
            crashTimer -= dt;
            position.x += crashPushX * dt * 3f;
            crashPushX *= (1f - 4f * dt);
        }

        // instant yaw response — no dampening, no lag
        // turn amount scales with speed but stays responsive
        float turnRate = steerAngle * 2.5f;
        yaw += turnRate * dt;

        // gentle yaw recenter (very light, only when not steering)
        if (steerInput == 0) {
            yaw *= (1f - 1.5f * dt);
        }

        // movement
        float fwdX = (float) Math.sin(yaw);
        float fwdZ = (float) Math.cos(yaw);
        position.x += fwdX * speed * dt;
        position.z += fwdZ * speed * dt;

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
        shader.setInt("useTex", 1);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, carTexture);

        Matrix4f model = new Matrix4f()
            .translate(position)
            .rotateY(yaw)
            .scale(modelScale);
        shader.setMat4("model", model);
        carMesh.draw();
    }

    public void cleanup() {
        if (carMesh != null) carMesh.cleanup();
    }
}
