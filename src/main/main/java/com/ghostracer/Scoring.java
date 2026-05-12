package com.ghostracer;

import java.util.List;

public class Scoring {
    public int score = 0;
    public int combo = 0;
    public int bestCombo = 0;
    public float multiplier = 1.0f;
    public String lastEvent = "";
    public float eventTimer = 0;
    private java.util.Set<Integer> passedCars = new java.util.HashSet<>();

    private static final float CLOSE_DIST = 2.8f;
    private static final float VERY_CLOSE_DIST = 1.8f;
    private static final float INSANE_DIST = 1.0f;
    private static final float HIT_DX = 1.4f;
    private static final float HIT_DZ = 3.0f;

    public void update(float dt, Vehicle player, List<Traffic.AICar> traffic) {
        eventTimer -= dt;
        multiplier = 1.0f + Math.min(player.speed / 12f, 5f);

        for (int i = 0; i < traffic.size(); i++) {
            Traffic.AICar c = traffic.get(i);
            if (!c.active) continue;

            float dx = Math.abs(player.position.x - c.x);
            float dz = player.position.z - c.z; // signed: positive = player ahead

            // collision
            if (dx < HIT_DX && Math.abs(dz) < HIT_DZ) {
                player.onCollision(c.x);
                onCrash();
                continue;
            }

            // near miss: player just passed this car (dz between 3 and 6, player ahead)
            if (dz > HIT_DZ && dz < 8f && !passedCars.contains(i)) {
                passedCars.add(i);
                if (dx < INSANE_DIST) {
                    addPoints(200, "insane");
                } else if (dx < VERY_CLOSE_DIST) {
                    addPoints(100, "very close");
                } else if (dx < CLOSE_DIST) {
                    addPoints(40, "close");
                }
            }
        }

        // clean up passed cars that are far behind
        passedCars.removeIf(i -> i >= traffic.size() || !traffic.get(i).active);
    }

    private void addPoints(int base, String event) {
        combo++;
        int points = (int)(base * multiplier * (1 + combo * 0.15f));
        score += points;
        lastEvent = event;
        eventTimer = 1.0f;
        if (combo > bestCombo) bestCombo = combo;
    }

    private void onCrash() {
        combo = 0;
        lastEvent = "crash";
        eventTimer = 1.5f;
    }
}
