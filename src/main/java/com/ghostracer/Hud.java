package com.ghostracer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

public class Hud {
    private Mesh[] digitMeshes = new Mesh[10];
    private Mesh dotMesh;

    private static final float SW = 0.008f;
    private static final float SL = 0.022f;
    private static final float DH = 0.055f;
    private static final float DW = 0.035f;

    private static final boolean[][] SEGS = {
        {true,true,true,true,true,true,false},
        {false,true,true,false,false,false,false},
        {true,true,false,true,true,false,true},
        {true,true,true,true,false,false,true},
        {false,true,true,false,false,true,true},
        {true,false,true,true,false,true,true},
        {true,false,true,true,true,true,true},
        {true,true,true,false,false,false,false},
        {true,true,true,true,true,true,true},
        {true,true,true,true,false,true,true},
    };

    public void init() {
        for (int d = 0; d < 10; d++) digitMeshes[d] = buildDigit(d);
        List<Float> dv = new ArrayList<>();
        List<Integer> di = new ArrayList<>();
        addSeg(dv, di, 0, 0, SW, SW, new float[]{1,1,1});
        dotMesh = toMesh(dv, di);

        // Build a bar mesh for menu background
        List<Float> bv = new ArrayList<>(); List<Integer> bi = new ArrayList<>();
        addSeg(bv, bi, 0, 0, 1.2f, 0.05f, new float[]{0.1f, 0.1f, 0.12f});
        titleBarMesh = toMesh(bv, bi);

        // Build "press enter" blinking bar
        List<Float> pv = new ArrayList<>(); List<Integer> pi = new ArrayList<>();
        addSeg(pv, pi, 0, 0, 0.25f, 0.018f, new float[]{0.9f, 0.9f, 0.9f});
        promptBarMesh = toMesh(pv, pi);
    }

    private Mesh titleBarMesh;
    private Mesh promptBarMesh;

    public void render(Shader shader, float speed, int score, int combo,
                       String event, float eventTimer, float aspect) {
        shader.bind();
        shader.setInt("useTex", 0);
        Matrix4f proj = new Matrix4f().ortho(-aspect, aspect, -1, 1, -1, 1);
        Matrix4f view = new Matrix4f();
        shader.setMat4("view", view);
        shader.setMat4("projection", proj);
        shader.setVec3("lightDir", new Vector3f(0, 0, -1));
        shader.setVec3("viewPos", new Vector3f(0, 0, 1));

        // === SCORE — top center, large ===
        shader.setFloat("alpha", 0.95f);
        drawNum(shader, score, 0, 0.90f, 1.8f);

        // === SPEED km/h — bottom center ===
        int kmh = (int)(speed * 3.6f);
        drawNum(shader, kmh, 0, -0.88f, 1.5f);

        // === COMBO — top left ===
        if (combo > 1) {
            shader.setFloat("alpha", 0.9f);
            // "x" dot
            Matrix4f xm = new Matrix4f().translate(-aspect + 0.08f, 0.90f, 0).scale(1.5f);
            shader.setMat4("model", xm);
            dotMesh.draw();
            drawNum(shader, combo, -aspect + 0.18f, 0.88f, 1.4f);
        }

        // === EVENT FLASH — center screen ===
        if (eventTimer > 0 && !event.equals("crash")) {
            float a = Math.min(eventTimer * 3f, 1f);
            shader.setFloat("alpha", a * 0.9f);

            // tier indicator dots: 1=close, 3=very close, 5=insane
            int dots = event.equals("insane") ? 7 : event.equals("very close") ? 4 : 2;
            float dotSpacing = 0.05f;
            float startX = -(dots - 1) * dotSpacing / 2f;
            for (int i = 0; i < dots; i++) {
                Matrix4f dm = new Matrix4f().translate(startX + i * dotSpacing, 0.25f, 0).scale(2.5f);
                shader.setMat4("model", dm);
                dotMesh.draw();
            }
        }

        // === CRASH — red flash at center ===
        if (event.equals("crash") && eventTimer > 0) {
            shader.setFloat("alpha", eventTimer * 0.4f);
            for (int i = 0; i < 3; i++) {
                Matrix4f cm = new Matrix4f().translate(-0.05f + i * 0.05f, 0.25f, 0).scale(3f);
                shader.setMat4("model", cm);
                dotMesh.draw();
            }
        }
    }

    private void drawNum(Shader shader, int num, float cx, float cy, float scale) {
        String s = String.valueOf(num);
        float digitSpace = 0.045f * scale;
        float totalW = s.length() * digitSpace;
        float x = cx - totalW / 2f + digitSpace / 2f;
        for (int i = 0; i < s.length(); i++) {
            int d = s.charAt(i) - '0';
            if (d < 0 || d > 9) continue;
            Matrix4f m = new Matrix4f().translate(x + i * digitSpace, cy, 0).scale(scale);
            shader.setMat4("model", m);
            digitMeshes[d].draw();
        }
    }

    private Mesh buildDigit(int d) {
        List<Float> v = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        float[] col = {0.95f, 0.95f, 0.95f};
        boolean[] s = SEGS[d];
        float hw = DW * 0.5f;
        if (s[0]) addSeg(v, idx, 0, DH, SL, SW, col);
        if (s[1]) addSeg(v, idx, hw, DH*0.75f, SW, SL, col);
        if (s[2]) addSeg(v, idx, hw, DH*0.25f, SW, SL, col);
        if (s[3]) addSeg(v, idx, 0, 0, SL, SW, col);
        if (s[4]) addSeg(v, idx, -hw, DH*0.25f, SW, SL, col);
        if (s[5]) addSeg(v, idx, -hw, DH*0.75f, SW, SL, col);
        if (s[6]) addSeg(v, idx, 0, DH*0.5f, SL, SW, col);
        return toMesh(v, idx);
    }

    private void addSeg(List<Float> v, List<Integer> idx,
                        float cx, float cy, float hw, float hh, float[] c) {
        int base = v.size() / 9;
        aV(v, cx-hw, cy-hh, c); aV(v, cx+hw, cy-hh, c);
        aV(v, cx+hw, cy+hh, c); aV(v, cx-hw, cy+hh, c);
        idx.add(base); idx.add(base+1); idx.add(base+2);
        idx.add(base); idx.add(base+2); idx.add(base+3);
    }

    private void aV(List<Float> v, float x, float y, float[] c) {
        v.add(x);v.add(y);v.add(0f); v.add(0f);v.add(0f);v.add(1f); v.add(c[0]);v.add(c[1]);v.add(c[2]);
    }

    private Mesh toMesh(List<Float> v, List<Integer> idx) {
        float[] va = new float[v.size()];
        for (int i = 0; i < va.length; i++) va[i] = v.get(i);
        int[] ia = new int[idx.size()];
        for (int i = 0; i < ia.length; i++) ia[i] = idx.get(i);
        return new Mesh(va, ia);
    }

    public void renderMenu(Shader shader, float aspect) {
        shader.bind();
        shader.setInt("useTex", 0);
        Matrix4f proj = new Matrix4f().ortho(-aspect, aspect, -1, 1, -1, 1);
        Matrix4f view = new Matrix4f();
        shader.setMat4("view", view);
        shader.setMat4("projection", proj);
        shader.setVec3("lightDir", new Vector3f(0, 0, -1));
        shader.setVec3("viewPos", new Vector3f(0, 0, 1));

        // Semi-transparent title background bar
        shader.setFloat("alpha", 0.7f);
        Matrix4f barM = new Matrix4f().translate(0, 0.3f, 0).scale(1.5f);
        shader.setMat4("model", barM);
        titleBarMesh.draw();

        // Title "SVIMM" spelled with dots pattern — stylized
        shader.setFloat("alpha", 1.0f);
        // S
        float tx = -0.28f, ty = 0.32f;
        float ds = 0.04f;
        float[][] sDots = {{0,2},{1,2},{0,1},{1,0},{0,0},{-1,2},{-1,1},{1,1},{-1,0}};
        for (float[] d : sDots) {
            Matrix4f dm = new Matrix4f().translate(tx + d[0]*ds*0.5f, ty + d[1]*ds*0.5f - ds, 0).scale(3.5f);
            shader.setMat4("model", dm);
            dotMesh.draw();
        }
        // V
        tx = -0.14f;
        float[][] vDots = {{-1,2},{1,2},{-0.7f,1},{0.7f,1},{-0.3f,0},{0.3f,0},{0,-.5f}};
        for (float[] d : vDots) {
            Matrix4f dm = new Matrix4f().translate(tx + d[0]*ds*0.5f, ty + d[1]*ds*0.5f - ds, 0).scale(3.5f);
            shader.setMat4("model", dm);
            dotMesh.draw();
        }
        // I
        tx = 0f;
        float[][] iDots = {{0,2},{0,1},{0,0},{0,-0.5f}};
        for (float[] d : iDots) {
            Matrix4f dm = new Matrix4f().translate(tx + d[0]*ds*0.5f, ty + d[1]*ds*0.5f - ds, 0).scale(3.5f);
            shader.setMat4("model", dm);
            dotMesh.draw();
        }
        // M
        tx = 0.12f;
        float[][] mDots = {{-1,2},{-1,1},{-1,0},{-1,-.5f},{-.5f,1.5f},{0,1},{.5f,1.5f},{1,2},{1,1},{1,0},{1,-.5f}};
        for (float[] d : mDots) {
            Matrix4f dm = new Matrix4f().translate(tx + d[0]*ds*0.5f, ty + d[1]*ds*0.5f - ds, 0).scale(3.5f);
            shader.setMat4("model", dm);
            dotMesh.draw();
        }
        // M (second)
        tx = 0.28f;
        for (float[] d : mDots) {
            Matrix4f dm = new Matrix4f().translate(tx + d[0]*ds*0.5f, ty + d[1]*ds*0.5f - ds, 0).scale(3.5f);
            shader.setMat4("model", dm);
            dotMesh.draw();
        }

        // Blinking "press enter" prompt
        float blink = (float) Math.sin(System.currentTimeMillis() / 400.0) * 0.5f + 0.5f;
        shader.setFloat("alpha", 0.5f + blink * 0.5f);
        Matrix4f pm = new Matrix4f().translate(0, -0.4f, 0);
        shader.setMat4("model", pm);
        promptBarMesh.draw();

        // Two dots as visual accent
        shader.setFloat("alpha", 0.4f + blink * 0.3f);
        for (float dx : new float[]{-0.3f, 0.3f}) {
            Matrix4f adm = new Matrix4f().translate(dx, -0.4f, 0).scale(2f);
            shader.setMat4("model", adm);
            dotMesh.draw();
        }
    }

    public void cleanup() {
        for (Mesh m : digitMeshes) if (m != null) m.cleanup();
        if (dotMesh != null) dotMesh.cleanup();
        if (titleBarMesh != null) titleBarMesh.cleanup();
        if (promptBarMesh != null) promptBarMesh.cleanup();
    }
}
