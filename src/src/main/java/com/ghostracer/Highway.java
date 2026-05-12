package com.ghostracer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

public class Highway {
    private List<Mesh> segmentMeshes = new ArrayList<>();
    private float[] segmentPositions;

    private static final float LANE_WIDTH = 3.5f;
    public static final int LANE_COUNT = 4;
    public static final float ROAD_WIDTH = LANE_WIDTH * LANE_COUNT;
    private static final float SHOULDER = 2.5f;
    private static final float SEG_LENGTH = 120f;
    private static final int SEG_COUNT = 8;
    /** How many Z-strips per segment for curvature tessellation */
    private static final int TESS_STRIPS = 30;

    public void init() {
        segmentPositions = new float[SEG_COUNT];
        for (int i = 0; i < SEG_COUNT; i++) {
            segmentMeshes.add(buildSegment());
            // Start 3 segments behind player so road is visible behind
            segmentPositions[i] = (i - 3) * SEG_LENGTH;
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

    public static float getSegmentLength() { return SEG_LENGTH; }
    public float[] getSegmentPositions() { return segmentPositions; }

    private Mesh buildSegment() {
        List<Float> v = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();

        float hw = ROAD_WIDTH / 2f;
        float sw = hw + SHOULDER;
        float h = 0.01f;
        float len = SEG_LENGTH;
        float stripLen = len / TESS_STRIPS;

        float[] asphalt = {0.13f, 0.13f, 0.15f};
        float[] shoulderCol = {0.22f, 0.20f, 0.18f};
        float[] white = {0.88f, 0.88f, 0.88f};
        float[] yellow = {0.90f, 0.75f, 0.15f};
        float[] barrierCol = {0.45f, 0.45f, 0.47f};
        float[] rumble = {0.30f, 0.15f, 0.10f};
        float[] grassEdge = {0.22f, 0.32f, 0.12f};

        // Tessellated road surface and shoulders
        for (int s = 0; s < TESS_STRIPS; s++) {
            float z0 = s * stripLen;
            float z1 = z0 + stripLen;

            // asphalt
            addQuad(v, idx, -hw, h, z0, hw, h, z0, hw, h, z1, -hw, h, z1, asphalt);

            // shoulders
            addQuad(v, idx, -sw, h*0.5f, z0, -hw, h*0.5f, z0, -hw, h*0.5f, z1, -sw, h*0.5f, z1, shoulderCol);
            addQuad(v, idx, hw, h*0.5f, z0, sw, h*0.5f, z0, sw, h*0.5f, z1, hw, h*0.5f, z1, shoulderCol);

            // grass strips next to road
            float gs = sw + 8f;
            addQuad(v, idx, -gs, 0, z0, -sw, 0, z0, -sw, 0, z1, -gs, 0, z1, grassEdge);
            addQuad(v, idx, sw, 0, z0, gs, 0, z0, gs, 0, z1, sw, 0, z1, grassEdge);

            // guardrails (tessellated for curvature)
            float gh = 0.7f, gw = 0.12f;
            addQuad(v, idx, -sw-gw, 0, z0, -sw, 0, z0, -sw, gh, z1, -sw-gw, gh, z1, barrierCol);
            addQuad(v, idx, sw, 0, z0, sw+gw, 0, z0, sw+gw, gh, z1, sw, gh, z1, barrierCol);
        }

        // Markings (still full-length quads — they're thin so curvature is negligible)
        float lw = 0.15f;
        float lh = h + 0.005f;

        // edge lines
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

        // rumble strips
        for (float z = 0; z < len; z += 1.5f) {
            float z1 = Math.min(z + 0.6f, len);
            addQuad(v, idx, -sw, h+0.01f, z, -sw+0.5f, h+0.01f, z, -sw+0.5f, h+0.01f, z1, -sw, h+0.01f, z1, rumble);
            addQuad(v, idx, sw-0.5f, h+0.01f, z, sw, h+0.01f, z, sw, h+0.01f, z1, sw-0.5f, h+0.01f, z1, rumble);
        }

        // guardrail top
        float gww = 0.12f;
        for (int s = 0; s < TESS_STRIPS; s++) {
            float z0t = s * stripLen;
            float z1t = z0t + stripLen;
            addQuad(v, idx, -sw-gww, 0.7f, z0t, -sw, 0.7f, z0t, -sw, 0.7f, z1t, -sw-gww, 0.7f, z1t, white);
            addQuad(v, idx, sw, 0.7f, z0t, sw+gww, 0.7f, z0t, sw+gww, 0.7f, z1t, sw, 0.7f, z1t, white);
        }

        float[] va = new float[v.size()];
        for (int i = 0; i < va.length; i++) va[i] = v.get(i);
        int[] ia = new int[idx.size()];
        for (int i = 0; i < ia.length; i++) ia[i] = idx.get(i);
        return new Mesh(va, ia);
    }

    public void render(Shader shader, Matrix4f view, Matrix4f proj, Vector3f viewPos, float playerZ) {
        shader.bind();
        shader.setMat4("view", view);
        shader.setMat4("projection", proj);
        shader.setVec3("lightDir", new Vector3f(0.3f, -0.8f, 0.5f));
        shader.setVec3("viewPos", viewPos);
        shader.setFloat("alpha", 1.0f);
        shader.setInt("useTex", 0);
        shader.setVec3("colorTint", new Vector3f(1, 1, 1));

        for (int i = 0; i < SEG_COUNT; i++) {
            Matrix4f m = new Matrix4f().translate(0, 0, segmentPositions[i]);
            shader.setMat4("model", m);
            segmentMeshes.get(i).draw();
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

    private void addV(List<Float> v, float x,float y,float z, float nx,float ny,float nz, float[] c) {
        v.add(x);v.add(y);v.add(z); v.add(nx);v.add(ny);v.add(nz); v.add(c[0]);v.add(c[1]);v.add(c[2]);
    }

    public void cleanup() {
        for (Mesh m : segmentMeshes) m.cleanup();
    }
}
