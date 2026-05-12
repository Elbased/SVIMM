package com.ghostracer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

/**
 * Biome-based scenery system with smooth ground color blending.
 * Biomes cycle: FOREST -> DESERT -> CITY -> SNOW -> repeat.
 * Starting biome is randomized each session.
 */
public class Scenery {

    public enum BiomeType { FOREST, DESERT, CITY, SNOW }

    private static final float BIOME_LENGTH = 600f;
    private static final float BLEND_ZONE = 150f;
    private static final float CYCLE_LENGTH = BIOME_LENGTH * 4;
    private static final int NEAR_PROPS = 20, MID_PROPS = 16, FAR_PROPS = 10;

    private float biomeZOffset;

    static class Biome {
        BiomeType type; Mesh[] props; float[] propScales; boolean[] propsAreTall;
        float[] groundColor; float[] fogColor; float density;
        Biome(BiomeType t, float[] gc, float[] fc, float d) {
            type=t; groundColor=gc; fogColor=fc; density=d;
        }
    }
    static class BiomeBlend { Biome primary, secondary; float blend; }
    static class PropSlot { float xOffset, zOffset, scale, yRotation; int propIndex; }

    private Biome[] biomes;
    private PropSlot[][] patterns;
    private Random rng = new Random(42);
    private Mesh groundMesh; // single white mesh, tinted via uniform

    public void init() {
        biomeZOffset = new Random().nextInt(4) * BIOME_LENGTH
                     + new Random().nextFloat() * BIOME_LENGTH * 0.3f;
        biomes = new Biome[4];

        // FOREST
        biomes[0] = new Biome(BiomeType.FOREST,
            new float[]{0.12f, 0.26f, 0.06f}, new float[]{0.40f, 0.55f, 0.38f}, 1.2f);
        loadBiomeProps(biomes[0], new String[]{
            "/models/biome-forest/tree_oak.obj", "/models/biome-forest/tree_default.obj",
            "/models/biome-forest/tree_tall.obj", "/models/biome-forest/tree_pineDefaultA.obj",
            "/models/biome-forest/tree_pineDefaultB.obj", "/models/biome-forest/tree_pineRoundA.obj",
            "/models/biome-forest/tree_pineRoundB.obj", "/models/biome-forest/tree_detailed.obj",
            "/models/biome-forest/tree_pineTallA.obj", "/models/biome-forest/tree_pineTallB.obj",
            "/models/biome-forest/plant_bush.obj", "/models/biome-forest/plant_bushLarge.obj",
            "/models/biome-forest/rock_tallA.obj", "/models/biome-forest/rock_tallB.obj",
            "/models/biome-forest/grass.obj", "/models/biome-forest/flower_redA.obj",
            "/models/biome-forest/flower_yellowA.obj", "/models/biome-forest/log.obj",
            "/models/biome-forest/stump_round.obj",
        }, new float[]{ 6,5.5f,6.5f,6,5.5f,5.5f,6,5,7,6.5f, 3,3.5f, 3.5f,3.5f, 3,4,4,3,2.5f },
           new boolean[]{ true,true,true,true,true,true,true,true,true,true,
                          false,false, false,false, false,false,false,false,false });

        // DESERT
        biomes[1] = new Biome(BiomeType.DESERT,
            new float[]{0.62f, 0.50f, 0.32f}, new float[]{0.75f, 0.65f, 0.48f}, 0.7f);
        loadBiomeProps(biomes[1], new String[]{
            "/models/biome-desert/cactus_tall.obj", "/models/biome-desert/cactus_short.obj",
            "/models/biome-desert/tree_palm.obj", "/models/biome-desert/tree_palmBend.obj",
            "/models/biome-desert/tree_palmShort.obj", "/models/biome-desert/rock_largeA.obj",
            "/models/biome-desert/rock_largeB.obj", "/models/biome-desert/rock_tallC.obj",
            "/models/biome-desert/rock_tallD.obj", "/models/biome-desert/stone_tallA.obj",
            "/models/biome-desert/stone_tallB.obj", "/models/biome-desert/plant_flatShort.obj",
            "/models/biome-desert/plant_flatTall.obj",
        }, new float[]{ 5,4, 6,6,5, 5,5,4,4, 5,5, 3,3.5f },
           new boolean[]{ true,false, true,true,true, false,false,false,false, true,true, false,false });

        // CITY
        biomes[2] = new Biome(BiomeType.CITY,
            new float[]{0.28f, 0.27f, 0.26f}, new float[]{0.52f, 0.54f, 0.60f}, 0.9f);
        loadBiomeProps(biomes[2], new String[]{
            "/models/biome-city/building-a.obj", "/models/biome-city/building-b.obj",
            "/models/biome-city/building-c.obj", "/models/biome-city/building-d.obj",
            "/models/biome-city/building-e.obj", "/models/biome-city/building-f.obj",
            "/models/biome-city/building-g.obj", "/models/biome-city/building-h.obj",
            "/models/biome-city/building-skyscraper-a.obj", "/models/biome-city/building-skyscraper-b.obj",
            "/models/biome-city/building-skyscraper-c.obj",
            "/models/biome-city/low-detail-building-a.obj", "/models/biome-city/low-detail-building-b.obj",
            "/models/biome-city/low-detail-building-c.obj", "/models/biome-city/low-detail-building-d.obj",
            "/models/biome-city/low-detail-building-e.obj", "/models/biome-city/low-detail-building-wide-a.obj",
        }, new float[]{ 8,8,8,8,9,9,9.5f,8.5f, 12,12,11, 14,14,12,12,12,16 },
           new boolean[]{ true,true,true,true,true,true,true,true, true,true,true,
                          true,true,true,true,true,true });

        // SNOW
        biomes[3] = new Biome(BiomeType.SNOW,
            new float[]{0.78f, 0.82f, 0.88f}, new float[]{0.72f, 0.78f, 0.88f}, 1.0f);
        loadBiomeProps(biomes[3], new String[]{
            "/models/biome-snow/tree-snow-a.obj", "/models/biome-snow/tree-snow-b.obj",
            "/models/biome-snow/tree-snow-c.obj", "/models/biome-snow/tree-decorated.obj",
            "/models/biome-snow/snowman.obj", "/models/biome-snow/snowman-hat.obj",
            "/models/biome-snow/rocks-large.obj", "/models/biome-snow/rocks-medium.obj",
            "/models/biome-snow/lantern.obj", "/models/biome-snow/snow-pile.obj",
            "/models/biome-snow/candy-cane-red.obj", "/models/biome-snow/present-a-cube.obj",
            "/models/biome-snow/present-b-rectangle.obj", "/models/biome-snow/bench.obj",
        }, new float[]{ 6,6,5,6.5f, 3.5f,3.5f, 3.5f,2.5f, 4,2.5f,4,2.5f,2.5f,3 },
           new boolean[]{ true,true,true,true, false,false, false,false,
                          false,false,false,false,false,false });

        // Precompute placement patterns — DENSE
        patterns = new PropSlot[biomes.length][];
        for (int b = 0; b < biomes.length; b++) {
            if (biomes[b].props == null || biomes[b].props.length == 0) continue;
            int near = (int)(NEAR_PROPS * 2 * biomes[b].density);
            int mid  = (int)(MID_PROPS * 2 * biomes[b].density);
            int far  = (int)(FAR_PROPS * 2 * biomes[b].density);
            int total = near + mid + far;
            patterns[b] = new PropSlot[total];
            float roadEdge = Highway.ROAD_WIDTH / 2f + 4f;

            for (int i = 0; i < total; i++) {
                PropSlot s = new PropSlot();
                float side = (i % 2 == 0) ? 1f : -1f;
                if (i < near) {
                    s.xOffset = side * (roadEdge + 3f + rng.nextFloat() * 22f);
                } else if (i < near + mid) {
                    s.xOffset = side * (roadEdge + 20f + rng.nextFloat() * 35f);
                } else {
                    s.xOffset = side * (roadEdge + 50f + rng.nextFloat() * 60f);
                }
                if (biomes[b].type == BiomeType.CITY) {
                    if (i < near) s.xOffset = side * (roadEdge + 8f + rng.nextFloat() * 15f);
                    else if (i < near + mid) s.xOffset = side * (roadEdge + 25f + rng.nextFloat() * 30f);
                    else s.xOffset = side * (roadEdge + 55f + rng.nextFloat() * 50f);
                }
                s.zOffset = rng.nextFloat() * Highway.getSegmentLength();
                s.scale = 0.7f + rng.nextFloat() * 0.6f;
                s.yRotation = rng.nextFloat() * 360f;
                s.propIndex = rng.nextInt(biomes[b].props.length);
                patterns[b][i] = s;
            }
        }

        // Single white ground mesh — colored via shader tint
        groundMesh = buildGroundMesh();
    }

    private void loadBiomeProps(Biome biome, String[] paths, float[] scales, boolean[] tall) {
        List<Mesh> meshes = new ArrayList<>();
        List<Float> sl = new ArrayList<>(); List<Boolean> tl = new ArrayList<>();
        for (int i = 0; i < paths.length; i++) {
            try {
                meshes.add(ObjLoader.load(paths[i]));
                sl.add(scales[i]); tl.add(tall[i]);
            } catch (Exception e) {
                System.err.println("Scenery: skipping " + paths[i] + " (" + e.getMessage() + ")");
            }
        }
        biome.props = meshes.toArray(new Mesh[0]);
        biome.propScales = new float[sl.size()];
        biome.propsAreTall = new boolean[tl.size()];
        for (int i = 0; i < sl.size(); i++) { biome.propScales[i] = sl.get(i); biome.propsAreTall[i] = tl.get(i); }
    }

    // ── Biome queries ────────────────────────────────────────────────

    public BiomeBlend getBiomeAt(float z) {
        BiomeBlend r = new BiomeBlend();
        float pos = (((z + biomeZOffset) % CYCLE_LENGTH) + CYCLE_LENGTH) % CYCLE_LENGTH;
        int bi = (int)(pos / BIOME_LENGTH);
        float inBiome = pos - bi * BIOME_LENGTH;
        r.primary = biomes[bi % biomes.length];
        float bs = BIOME_LENGTH - BLEND_ZONE;
        if (inBiome > bs) { r.secondary = biomes[(bi+1)%biomes.length]; r.blend = (inBiome-bs)/BLEND_ZONE; }
        else { r.secondary = null; r.blend = 0; }
        return r;
    }

    public float[] getGroundColor(float z) {
        BiomeBlend bb = getBiomeAt(z);
        if (bb.secondary == null) return bb.primary.groundColor;
        float t = bb.blend;
        return new float[]{
            lerp(bb.primary.groundColor[0], bb.secondary.groundColor[0], t),
            lerp(bb.primary.groundColor[1], bb.secondary.groundColor[1], t),
            lerp(bb.primary.groundColor[2], bb.secondary.groundColor[2], t),
        };
    }

    public float[] getFogColor(float z) {
        BiomeBlend bb = getBiomeAt(z);
        if (bb.secondary == null) return bb.primary.fogColor;
        float t = bb.blend;
        return new float[]{
            lerp(bb.primary.fogColor[0], bb.secondary.fogColor[0], t),
            lerp(bb.primary.fogColor[1], bb.secondary.fogColor[1], t),
            lerp(bb.primary.fogColor[2], bb.secondary.fogColor[2], t),
        };
    }

    // ── Rendering ────────────────────────────────────────────────────

    public void render(Shader shader, Matrix4f view, Matrix4f proj,
                       Vector3f viewPos, float playerZ, float[] segmentPositions) {
        shader.bind();
        shader.setMat4("view", view);
        shader.setMat4("projection", proj);
        shader.setVec3("lightDir", new Vector3f(0.3f, -0.8f, 0.5f));
        shader.setVec3("viewPos", viewPos);
        shader.setFloat("alpha", 1.0f);
        shader.setInt("useTex", 0);
        shader.setVec3("colorTint", new Vector3f(1, 1, 1));

        for (float segZ : segmentPositions) {
            BiomeBlend bb = getBiomeAt(segZ + Highway.getSegmentLength() * 0.5f);
            renderBiomeProps(shader, bb.primary, segZ, 1.0f);
            if (bb.secondary != null && bb.blend > 0.05f)
                renderBiomeProps(shader, bb.secondary, segZ, bb.blend);
        }
    }

    private void renderBiomeProps(Shader shader, Biome biome, float segZ, float df) {
        int bi = biome.type.ordinal();
        if (patterns[bi] == null) return;
        PropSlot[] pat = patterns[bi];
        int count = Math.min((int)(pat.length * df), pat.length);
        for (int i = 0; i < count; i++) {
            PropSlot s = pat[i];
            if (s.propIndex >= biome.props.length) continue;
            float scale = biome.propScales[s.propIndex] * s.scale;
            Matrix4f m = new Matrix4f().translate(s.xOffset, 0, segZ + s.zOffset)
                .rotateY((float) Math.toRadians(s.yRotation)).scale(scale);
            shader.setMat4("model", m);
            biome.props[s.propIndex].draw();
        }
    }

    public void renderGround(Shader shader, Matrix4f view, Matrix4f proj,
                              Vector3f viewPos, float playerZ) {
        shader.bind();
        shader.setMat4("view", view);
        shader.setMat4("projection", proj);
        shader.setVec3("lightDir", new Vector3f(0.3f, -0.8f, 0.5f));
        shader.setVec3("viewPos", viewPos);
        shader.setFloat("alpha", 1.0f);
        shader.setInt("useTex", 0);

        // Smoothly blended ground color via tint uniform
        float[] gc = getGroundColor(playerZ);
        shader.setVec3("colorTint", new Vector3f(gc[0], gc[1], gc[2]));

        Matrix4f gm = new Matrix4f().translate(0, 0, playerZ - 300);
        shader.setMat4("model", gm);
        groundMesh.draw();

        // Reset tint for other rendering
        shader.setVec3("colorTint", new Vector3f(1, 1, 1));
    }

    private Mesh buildGroundMesh() {
        float halfW = 300f, zMin = -100f, zMax = 1500f;
        int xD = 24, zD = 50;
        float xS = (halfW*2)/xD, zS = (zMax-zMin)/zD;
        List<Float> v = new ArrayList<>(); List<Integer> idx = new ArrayList<>();
        float[] w = {1,1,1};
        for (int zi = 0; zi < zD; zi++) for (int xi = 0; xi < xD; xi++) {
            float x0=-halfW+xi*xS, x1=x0+xS, z0=zMin+zi*zS, z1=z0+zS;
            int b = v.size()/9;
            gv(v,x0,z0,w); gv(v,x1,z0,w); gv(v,x1,z1,w); gv(v,x0,z1,w);
            idx.add(b); idx.add(b+1); idx.add(b+2); idx.add(b); idx.add(b+2); idx.add(b+3);
        }
        float[] va=new float[v.size()]; for(int i=0;i<va.length;i++) va[i]=v.get(i);
        int[] ia=new int[idx.size()]; for(int i=0;i<ia.length;i++) ia[i]=idx.get(i);
        return new Mesh(va, ia);
    }

    private void gv(List<Float> v, float x, float z, float[] c) {
        v.add(x); v.add(-0.02f); v.add(z); v.add(0f); v.add(1f); v.add(0f);
        v.add(c[0]); v.add(c[1]); v.add(c[2]);
    }

    private float lerp(float a, float b, float t) { return a + (b-a) * t; }

    public void cleanup() {
        for (Biome b : biomes) if (b.props != null) for (Mesh m : b.props) if (m != null) m.cleanup();
        if (groundMesh != null) groundMesh.cleanup();
    }
}
