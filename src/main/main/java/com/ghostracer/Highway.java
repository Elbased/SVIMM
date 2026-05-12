package com.ghostracer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

public class Highway {
    private Mesh groundMesh;
    private List<Mesh> segmentMeshes = new ArrayList<>();
    private float[] segmentPositions;
    private Mesh pineMesh;
    private Mesh oakMesh;
    private Mesh bushMesh;
    private Mesh rockMesh;
    private float[][] treePattern;

    private static final float LANE_WIDTH = 3.5f;
    public static final int LANE_COUNT = 4;
    public static final float ROAD_WIDTH = LANE_WIDTH * LANE_COUNT;
    private static final float SHOULDER = 2.5f;
    private static final float SEG_LENGTH = 120f;
    private static final int SEG_COUNT = 5;
    private static final float GROUND_WIDTH = 120f;
    private static final int TREES_PER_SIDE = 20;

    public void init() {
        segmentPositions = new float[SEG_COUNT];
        for (int i = 0; i < SEG_COUNT; i++) {
            segmentMeshes.add(buildSegment());
            segmentPositions[i] = i * SEG_LENGTH;
        }
        groundMesh = buildGround();
        pineMesh = buildPine();
        oakMesh = buildOak();
        bushMesh = buildBush();
        rockMesh = buildRock();

        Random rng = new Random(42);
        treePattern = new float[TREES_PER_SIDE * 2][4]; // x, z, scale, type
        for (int i = 0; i < treePattern.length; i++) {
            float side = (i < TREES_PER_SIDE) ? -1 : 1;
            float baseX = side * (ROAD_WIDTH / 2f + SHOULDER + 3f + rng.nextFloat() * 50f);
            float zOff = rng.nextFloat() * SEG_LENGTH;
            float scale = 1.2f + rng.nextFloat() * 2.5f;
            float type = rng.nextFloat(); // 0-0.4=pine, 0.4-0.7=oak, 0.7-0.85=bush, 0.85-1.0=rock
            treePattern[i] = new float[]{baseX, zOff, scale, type};
        }
    }

    public void update(float playerZ) {
        for (int i = 0; i < SEG_COUNT; i++) {
            if (playerZ - segmentPositions[i] > SEG_LENGTH * 2) {
                float maxZ = segmentPositions[0];
                for (float z : segmentPositions) if (z > maxZ) maxZ = z;
                segmentPositions[i] = maxZ + SEG_LENGTH;
            }
        }
    }

    public static float getLaneX(int lane) {
        float roadLeft = -ROAD_WIDTH / 2f;
        return roadLeft + LANE_WIDTH * 0.5f + lane * LANE_WIDTH;
    }

    private Mesh buildSegment() {
        List<Float> v = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();

        float hw = ROAD_WIDTH / 2f;
        float sw = hw + SHOULDER;
        float h = 0.01f;
        float len = SEG_LENGTH;

        float[] asphalt = {0.13f, 0.13f, 0.15f};
        float[] shoulderCol = {0.22f, 0.20f, 0.18f};
        float[] white = {0.88f, 0.88f, 0.88f};
        float[] yellow = {0.90f, 0.75f, 0.15f};
        float[] barrierCol = {0.45f, 0.45f, 0.47f};
        float[] rumble = {0.30f, 0.15f, 0.10f};
        float[] grassEdge = {0.22f, 0.32f, 0.12f};

        // asphalt
        addQuad(v, idx, -hw, h, 0, hw, h, 0, hw, h, len, -hw, h, len, asphalt);

        // shoulders
        addQuad(v, idx, -sw, h*0.5f, 0, -hw, h*0.5f, 0, -hw, h*0.5f, len, -sw, h*0.5f, len, shoulderCol);
        addQuad(v, idx, hw, h*0.5f, 0, sw, h*0.5f, 0, sw, h*0.5f, len, hw, h*0.5f, len, shoulderCol);

        // rumble strips on shoulders
        for (float z = 0; z < len; z += 1.5f) {
            float z1 = Math.min(z + 0.6f, len);
            addQuad(v, idx, -sw, h+0.01f, z, -sw+0.5f, h+0.01f, z, -sw+0.5f, h+0.01f, z1, -sw, h+0.01f, z1, rumble);
            addQuad(v, idx, sw-0.5f, h+0.01f, z, sw, h+0.01f, z, sw, h+0.01f, z1, sw-0.5f, h+0.01f, z1, rumble);
        }

        // grass strips next to road
        float gs = sw + 6f;
        addQuad(v, idx, -gs, 0, 0, -sw, 0, 0, -sw, 0, len, -gs, 0, len, grassEdge);
        addQuad(v, idx, sw, 0, 0, gs, 0, 0, gs, 0, len, sw, 0, len, grassEdge);

        // edge lines (solid white)
        float lw = 0.15f;
        float lh = h + 0.005f;
        addQuad(v, idx, -hw, lh, 0, -hw+lw, lh, 0, -hw+lw, lh, len, -hw, lh, len, white);
        addQuad(v, idx, hw-lw, lh, 0, hw, lh, 0, hw, lh, len, hw-lw, lh, len, white);

        // yellow center line (double)
        addQuad(v, idx, -lw*1.5f, lh, 0, -lw*0.5f, lh, 0, -lw*0.5f, lh, len, -lw*1.5f, lh, len, yellow);
        addQuad(v, idx, lw*0.5f, lh, 0, lw*1.5f, lh, 0, lw*1.5f, lh, len, lw*0.5f, lh, len, yellow);

        // lane divider dashes
        for (int lane = 1; lane < LANE_COUNT; lane++) {
            float lx = -hw + lane * LANE_WIDTH;
            if (Math.abs(lx) < 0.3f) continue;
            for (float z = 0; z < len; z += 6f) {
                float z1 = Math.min(z + 3f, len);
                addQuad(v, idx, lx-lw*0.5f, lh, z, lx+lw*0.5f, lh, z,
                        lx+lw*0.5f, lh, z1, lx-lw*0.5f, lh, z1, white);
            }
        }

        // guardrails (taller, visible)
        float gh = 0.7f, gw = 0.12f;
        addQuad(v, idx, -sw-gw, 0, 0, -sw, 0, 0, -sw, gh, len, -sw-gw, gh, len, barrierCol);
        addQuad(v, idx, sw, 0, 0, sw+gw, 0, 0, sw+gw, gh, len, sw, gh, len, barrierCol);
        // guardrail top
        addQuad(v, idx, -sw-gw, gh, 0, -sw, gh, 0, -sw, gh, len, -sw-gw, gh, len, white);
        addQuad(v, idx, sw, gh, 0, sw+gw, gh, 0, sw+gw, gh, len, sw, gh, len, white);

        float[] va = new float[v.size()];
        for (int i = 0; i < va.length; i++) va[i] = v.get(i);
        int[] ia = new int[idx.size()];
        for (int i = 0; i < ia.length; i++) ia[i] = idx.get(i);
        return new Mesh(va, ia);
    }

    private Mesh buildPine() {
        List<Float> v = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        float[] trunk = {0.40f, 0.25f, 0.12f};
        float[] l1 = {0.10f, 0.38f, 0.08f};
        float[] l2 = {0.12f, 0.42f, 0.10f};
        float[] l3 = {0.08f, 0.35f, 0.07f};
        float[] l4 = {0.06f, 0.30f, 0.06f};

        addBox(v, idx, 0, 0, 0, 0.12f, 1.8f, 0.12f, trunk);
        addCone(v, idx, 0, 1.0f, 0, 1.6f, 1.2f, 10, l1);
        addCone(v, idx, 0, 1.6f, 0, 1.3f, 1.2f, 10, l2);
        addCone(v, idx, 0, 2.2f, 0, 1.0f, 1.1f, 10, l3);
        addCone(v, idx, 0, 2.8f, 0, 0.6f, 0.9f, 10, l4);

        float[] va = new float[v.size()];
        for (int i = 0; i < va.length; i++) va[i] = v.get(i);
        int[] ia = new int[idx.size()];
        for (int i = 0; i < ia.length; i++) ia[i] = idx.get(i);
        return new Mesh(va, ia);
    }

    private Mesh buildOak() {
        List<Float> v = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        float[] trunk = {0.45f, 0.28f, 0.14f};
        float[] canopy1 = {0.18f, 0.45f, 0.12f};
        float[] canopy2 = {0.15f, 0.40f, 0.10f};

        addBox(v, idx, 0, 0, 0, 0.18f, 1.5f, 0.18f, trunk);
        // round canopy = multiple flattened wide cones
        addCone(v, idx, 0, 1.3f, 0, 2.0f, 1.0f, 12, canopy1);
        addCone(v, idx, 0, 1.8f, 0, 1.6f, 1.0f, 12, canopy2);
        addCone(v, idx, 0.4f, 1.5f, 0.3f, 1.2f, 0.8f, 10, canopy1);
        addCone(v, idx, -0.3f, 1.6f, -0.2f, 1.0f, 0.9f, 10, canopy2);

        float[] va = new float[v.size()];
        for (int i = 0; i < va.length; i++) va[i] = v.get(i);
        int[] ia = new int[idx.size()];
        for (int i = 0; i < ia.length; i++) ia[i] = idx.get(i);
        return new Mesh(va, ia);
    }

    private Mesh buildBush() {
        List<Float> v = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        float[] c1 = {0.20f, 0.42f, 0.15f};
        float[] c2 = {0.16f, 0.36f, 0.12f};
        addCone(v, idx, 0, 0, 0, 1.0f, 0.6f, 10, c1);
        addCone(v, idx, 0.3f, 0, 0.2f, 0.7f, 0.5f, 8, c2);
        addCone(v, idx, -0.2f, 0, -0.15f, 0.6f, 0.55f, 8, c1);
        float[] va = new float[v.size()];
        for (int i = 0; i < va.length; i++) va[i] = v.get(i);
        int[] ia = new int[idx.size()];
        for (int i = 0; i < ia.length; i++) ia[i] = idx.get(i);
        return new Mesh(va, ia);
    }

    private Mesh buildRock() {
        List<Float> v = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        float[] c = {0.40f, 0.38f, 0.35f};
        addCone(v, idx, 0, 0, 0, 0.5f, 0.35f, 6, c);
        addCone(v, idx, 0.15f, 0, 0.1f, 0.3f, 0.25f, 5, c);
        float[] va = new float[v.size()];
        for (int i = 0; i < va.length; i++) va[i] = v.get(i);
        int[] ia = new int[idx.size()];
        for (int i = 0; i < ia.length; i++) ia[i] = idx.get(i);
        return new Mesh(va, ia);
    }

    private Mesh buildGround() {
        float w = GROUND_WIDTH;
        float[] gv = {
            -w, -0.02f, -200, 0,1,0, 0.16f,0.25f,0.10f,
             w, -0.02f, -200, 0,1,0, 0.16f,0.25f,0.10f,
             w, -0.02f,  800, 0,1,0, 0.16f,0.25f,0.10f,
            -w, -0.02f,  800, 0,1,0, 0.16f,0.25f,0.10f,
        };
        return new Mesh(gv, new int[]{0,1,2, 0,2,3});
    }

    public void render(Shader shader, Matrix4f view, Matrix4f proj, Vector3f viewPos, float playerZ) {
        shader.bind();
        shader.setMat4("view", view);
        shader.setMat4("projection", proj);
        shader.setVec3("lightDir", new Vector3f(0.3f, -0.8f, 0.5f));
        shader.setVec3("viewPos", viewPos);
        shader.setFloat("alpha", 1.0f);
        shader.setInt("useTex", 0);

        // ground follows player
        Matrix4f gm = new Matrix4f().translate(0, 0, playerZ - 200);
        shader.setMat4("model", gm);
        groundMesh.draw();

        // road segments + trees
        for (int i = 0; i < SEG_COUNT; i++) {
            Matrix4f m = new Matrix4f().translate(0, 0, segmentPositions[i]);
            shader.setMat4("model", m);
            segmentMeshes.get(i).draw();

            // vegetation along this segment
            for (float[] tp : treePattern) {
                float tx = tp[0];
                float tz = segmentPositions[i] + tp[1];
                float sc = tp[2];
                float type = tp[3];
                Matrix4f tm = new Matrix4f().translate(tx, 0, tz).scale(sc);
                shader.setMat4("model", tm);
                if (type < 0.4f) pineMesh.draw();
                else if (type < 0.7f) oakMesh.draw();
                else if (type < 0.85f) bushMesh.draw();
                else rockMesh.draw();
            }
        }
    }

    private void addQuad(List<Float> v, List<Integer> idx,
                         float x0,float y0,float z0, float x1,float y1,float z1,
                         float x2,float y2,float z2, float x3,float y3,float z3, float[] c) {
        int base = v.size() / 9;
        addV(v, x0,y0,z0, 0,1,0, c); addV(v, x1,y1,z1, 0,1,0, c);
        addV(v, x2,y2,z2, 0,1,0, c); addV(v, x3,y3,z3, 0,1,0, c);
        idx.add(base); idx.add(base+1); idx.add(base+2);
        idx.add(base); idx.add(base+2); idx.add(base+3);
    }

    private void addBox(List<Float> v, List<Integer> idx,
                        float cx, float cy, float cz, float hw, float hh, float hd, float[] c) {
        float x0=cx-hw, x1=cx+hw, y0=cy, y1=cy+hh, z0=cz-hd, z1=cz+hd;
        addQuad(v,idx, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, c);
        addQuad(v,idx, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, c);
        addQuad(v,idx, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, c);
        addQuad(v,idx, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, c);
        addQuad(v,idx, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, c);
    }

    private void addCone(List<Float> v, List<Integer> idx,
                         float cx, float cy, float cz,
                         float radius, float height, int segs, float[] c) {
        int tipIdx = v.size() / 9;
        addV(v, cx, cy+height, cz, 0,1,0, c);
        int baseStart = v.size() / 9;
        for (int i = 0; i <= segs; i++) {
            float a = (float)(i * 2 * Math.PI / segs);
            float px = cx + (float)Math.cos(a) * radius;
            float pz = cz + (float)Math.sin(a) * radius;
            float nx = (float)Math.cos(a) * 0.5f;
            float nz = (float)Math.sin(a) * 0.5f;
            addV(v, px, cy, pz, nx, 0.5f, nz, c);
            if (i > 0) {
                idx.add(tipIdx); idx.add(baseStart+i-1); idx.add(baseStart+i);
            }
        }
    }

    private void addV(List<Float> v, float x,float y,float z, float nx,float ny,float nz, float[] c) {
        v.add(x);v.add(y);v.add(z); v.add(nx);v.add(ny);v.add(nz); v.add(c[0]);v.add(c[1]);v.add(c[2]);
    }

    public void cleanup() {
        groundMesh.cleanup();
        for (Mesh m : segmentMeshes) m.cleanup();
        pineMesh.cleanup();
        oakMesh.cleanup();
        bushMesh.cleanup();
        rockMesh.cleanup();
    }
}
