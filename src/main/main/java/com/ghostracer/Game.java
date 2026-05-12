package com.ghostracer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Game {
    private Window window;
    private Shader defaultShader;
    private Shader gridShader;
    private Shader skyShader;
    private Vehicle vehicle;
    private Highway highway;
    private Traffic traffic;
    private Scoring scoring;
    private Camera camera;
    private Hud hud;
    private Sky sky;
    private Matrix4f projection = new Matrix4f();

    private static final float PHYSICS_DT = 1.0f / 60.0f;
    private static final float BASE_FOV = 55f;
    private static final float MAX_FOV = 90f;
    private float currentFov = BASE_FOV;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window = new Window();
        window.init();
        Input.init(window.getHandle());

        defaultShader = new Shader("/shaders/default.vert", "/shaders/default.frag");
        gridShader = new Shader("/shaders/grid.vert", "/shaders/grid.frag");
        skyShader = new Shader("/shaders/sky.vert", "/shaders/sky.frag");

        vehicle = new Vehicle();
        vehicle.init();
        vehicle.position.set(Highway.getLaneX(1), 0, 0);

        highway = new Highway();
        highway.init();

        traffic = new Traffic();
        traffic.init();

        scoring = new Scoring();
        camera = new Camera();
        hud = new Hud();
        hud.init();
        sky = new Sky();
        sky.init();

        updateProjection();
    }

    private void loop() {
        long lastTime = System.nanoTime();
        float accumulator = 0;

        while (!window.shouldClose()) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000.0f;
            lastTime = now;
            dt = Math.min(dt, 0.05f);

            Input.update();

            if (Input.wasPressed(GLFW_KEY_ESCAPE)) break;
            if (Input.wasPressed(GLFW_KEY_TAB)) camera.cycleMode();
            if (Input.wasPressed(GLFW_KEY_C)) vehicle.nextModel();

            // mouse look for free cam and first person
            if (camera.isFreeCam() || camera.isFirstPerson()) {
                camera.updateFree((float) Input.getMouseDx(), (float) Input.getMouseDy());
            }

            accumulator += dt;
            while (accumulator >= PHYSICS_DT) {
                vehicle.update(PHYSICS_DT);
                traffic.update(PHYSICS_DT, vehicle.position.z);
                scoring.update(PHYSICS_DT, vehicle, traffic.getCars());
                highway.update(vehicle.position.z);
                accumulator -= PHYSICS_DT;
            }

            camera.update(vehicle.position, vehicle.yaw, vehicle.speed, 60f, dt);

            // FOV: bigger range, quadratic for dramatic effect
            float speedRatio = vehicle.speed / 60f;
            float targetFov = BASE_FOV + speedRatio * speedRatio * (MAX_FOV - BASE_FOV);
            // first person gets extra FOV for immersion
            if (camera.isFirstPerson()) targetFov += 10f;
            currentFov += (targetFov - currentFov) * 5.0f * dt;

            if (window.wasResized()) {
                glViewport(0, 0, window.getWidth(), window.getHeight());
            }
            updateProjection();

            render();
            window.swapAndPoll();
        }
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix4f view = camera.getViewMatrix();
        Vector3f viewPos = camera.getPosition();

        sky.render(skyShader);

        highway.render(defaultShader, view, projection, viewPos, vehicle.position.z);

        // hide car in first person
        defaultShader.bind();
        defaultShader.setVec3("viewPos", viewPos);
        vehicle.render(defaultShader, view, projection, !camera.isFirstPerson());

        traffic.render(defaultShader, view, projection, viewPos);

        glDisable(GL_DEPTH_TEST);
        hud.render(defaultShader, vehicle.speed, scoring.score, scoring.combo,
                   scoring.lastEvent, scoring.eventTimer, window.getAspect());
        glEnable(GL_DEPTH_TEST);
    }

    private void updateProjection() {
        projection.identity().perspective(
            (float) Math.toRadians(currentFov), window.getAspect(), 0.1f, 500f
        );
    }

    private void cleanup() {
        vehicle.cleanup();
        highway.cleanup();
        traffic.cleanup();
        hud.cleanup();
        sky.cleanup();
        defaultShader.cleanup();
        gridShader.cleanup();
        skyShader.cleanup();
        window.cleanup();
    }
}
