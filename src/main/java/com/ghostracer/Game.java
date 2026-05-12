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
    private Scenery scenery;
    private Matrix4f projection = new Matrix4f();

    private static final float BASE_FOV = 55f;
    private static final float MAX_FOV = 90f;
    private float currentFov = BASE_FOV;

    // ── Game states ──────────────────────────────────────────────────
    private enum State { MENU, INTRO, PLAYING }
    private State state = State.MENU;
    private float stateTimer = 0;
    private float menuOrbitAngle = 0;
    private static final float INTRO_DURATION = 3.0f;

    // camera interp during intro
    private final Vector3f introStartPos = new Vector3f();
    private final Vector3f introStartTarget = new Vector3f();
    private final Vector3f introEndPos = new Vector3f();
    private final Vector3f introEndTarget = new Vector3f();

    // wheel rotation
    private float wheelAngle = 0;

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

        scenery = new Scenery();
        try {
            scenery.init();
            System.out.println("Scenery initialized successfully");
        } catch (Exception e) {
            System.err.println("Scenery init failed: " + e.getMessage());
            e.printStackTrace();
        }

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

        while (!window.shouldClose()) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000.0f;
            lastTime = now;
            dt = Math.min(dt, 0.05f);

            Input.update();
            if (Input.wasPressed(GLFW_KEY_ESCAPE)) break;

            switch (state) {
                case MENU:  updateMenu(dt); break;
                case INTRO: updateIntro(dt); break;
                case PLAYING: updatePlaying(dt); break;
            }

            if (window.wasResized()) {
                glViewport(0, 0, window.getWidth(), window.getHeight());
            }
            updateProjection();
            render();
            window.swapAndPoll();
        }
    }

    // ── MENU state ───────────────────────────────────────────────────
    private void updateMenu(float dt) {
        // Car auto-drives forward slowly
        vehicle.speed = 15f;
        vehicle.position.z += vehicle.speed * dt;
        highway.update(vehicle.position.z);

        // Wheel rotation
        wheelAngle += vehicle.speed * dt * 4f;

        // Helicopter orbit camera
        menuOrbitAngle += dt * 20f; // degrees per second
        float rad = (float) Math.toRadians(menuOrbitAngle);
        float orbitDist = 25f;
        float orbitHeight = 12f;
        Vector3f camPos = new Vector3f(
            vehicle.position.x + (float) Math.sin(rad) * orbitDist,
            vehicle.position.y + orbitHeight,
            vehicle.position.z + (float) Math.cos(rad) * orbitDist
        );
        Vector3f camTarget = new Vector3f(vehicle.position).add(0, 1f, 0);
        camera.setDirect(camPos, camTarget);

        currentFov = 45f;

        // Press ENTER or SPACE to start
        if (Input.wasPressed(GLFW_KEY_ENTER) || Input.wasPressed(GLFW_KEY_SPACE)) {
            startIntro();
        }
    }

    private void startIntro() {
        state = State.INTRO;
        stateTimer = 0;

        // Save helicopter position as intro start
        introStartPos.set(camera.getPosition());
        introStartTarget.set(vehicle.position).add(0, 1f, 0);

        // Compute where the chase cam would be
        float dist = 10f;
        float height = 5.5f;
        introEndPos.set(
            vehicle.position.x,
            vehicle.position.y + height,
            vehicle.position.z - dist
        );
        introEndTarget.set(
            vehicle.position.x,
            vehicle.position.y + 0.5f,
            vehicle.position.z + 10f
        );
    }

    // ── INTRO state ──────────────────────────────────────────────────
    private void updateIntro(float dt) {
        stateTimer += dt;
        float t = Math.min(stateTimer / INTRO_DURATION, 1f);
        // Smooth ease-in-out
        float st = t * t * (3f - 2f * t);

        // Car continues forward
        vehicle.speed = 15f + t * 10f;
        vehicle.position.z += vehicle.speed * dt;
        highway.update(vehicle.position.z);
        wheelAngle += vehicle.speed * dt * 4f;

        // Update intro end position relative to current car pos
        float dist = 10f;
        float height = 5.5f;
        introEndPos.set(
            vehicle.position.x,
            vehicle.position.y + height,
            vehicle.position.z - dist
        );
        introEndTarget.set(
            vehicle.position.x,
            vehicle.position.y + 0.5f,
            vehicle.position.z + 10f
        );

        // Interpolate camera
        Vector3f pos = new Vector3f();
        Vector3f tgt = new Vector3f();
        introStartPos.lerp(introEndPos, st, pos);
        introStartTarget.lerp(introEndTarget, st, tgt);
        camera.setDirect(pos, tgt);

        currentFov = 45f + st * 10f;

        if (t >= 1f) {
            state = State.PLAYING;
        }
    }

    // ── PLAYING state ────────────────────────────────────────────────
    private void updatePlaying(float dt) {
        if (Input.wasPressed(GLFW_KEY_TAB)) camera.cycleMode();
        if (Input.wasPressed(GLFW_KEY_C)) vehicle.nextModel();

        camera.updateFree((float) Input.getMouseDx(), (float) Input.getMouseDy());

        vehicle.update(dt);
        traffic.update(dt, vehicle.position.z);
        scoring.update(dt, vehicle, traffic.getCars());
        highway.update(vehicle.position.z);

        // Wheel rotation based on speed
        wheelAngle += vehicle.speed * dt * 4f;

        camera.update(vehicle.position, vehicle.yaw, vehicle.speed, 60f, dt);

        float speedRatio = vehicle.speed / 60f;
        float targetFov = BASE_FOV + speedRatio * speedRatio * (MAX_FOV - BASE_FOV);
        if (camera.isFirstPerson()) targetFov += 10f;
        currentFov += (targetFov - currentFov) * 5.0f * dt;
    }

    // ── Rendering ────────────────────────────────────────────────────
    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix4f view = camera.getViewMatrix();
        Vector3f viewPos = camera.getPosition();

        sky.render(skyShader);

        // Set biome fog color
        float[] fogColor = scenery.getFogColor(vehicle.position.z);
        defaultShader.bind();
        defaultShader.setVec3("biomeFogColor", new Vector3f(fogColor[0], fogColor[1], fogColor[2]));
        defaultShader.setVec3("colorTint", new Vector3f(1, 1, 1));

        // Ground
        scenery.renderGround(defaultShader, view, projection, viewPos, vehicle.position.z);

        // Road
        highway.render(defaultShader, view, projection, viewPos, vehicle.position.z);

        // Scenery props
        scenery.render(defaultShader, view, projection, viewPos,
                       vehicle.position.z, highway.getSegmentPositions());

        // Player car
        defaultShader.bind();
        defaultShader.setVec3("viewPos", viewPos);
        defaultShader.setVec3("colorTint", new Vector3f(1, 1, 1));
        boolean showCar = (state == State.MENU || state == State.INTRO)
                        || !camera.isFirstPerson();
        vehicle.render(defaultShader, view, projection, showCar);

        // Traffic (only in playing and intro)
        if (state == State.PLAYING || state == State.INTRO) {
            traffic.render(defaultShader, view, projection, viewPos);
        }

        // HUD
        glDisable(GL_DEPTH_TEST);
        if (state == State.MENU) {
            hud.renderMenu(defaultShader, window.getAspect());
        } else if (state == State.PLAYING) {
            hud.render(defaultShader, vehicle.speed, scoring.score, scoring.combo,
                       scoring.lastEvent, scoring.eventTimer, window.getAspect());
        }
        glEnable(GL_DEPTH_TEST);
    }

    private void updateProjection() {
        projection.identity().perspective(
            (float) Math.toRadians(currentFov), window.getAspect(), 0.1f, 800f
        );
    }

    private void cleanup() {
        vehicle.cleanup();
        highway.cleanup();
        traffic.cleanup();
        scenery.cleanup();
        hud.cleanup();
        sky.cleanup();
        defaultShader.cleanup();
        gridShader.cleanup();
        skyShader.cleanup();
        window.cleanup();
    }
}
