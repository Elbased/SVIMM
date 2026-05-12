package com.ghostracer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjLoader {

    public static Mesh load(String resourcePath) {
        try (InputStream is = ObjLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("obj not found: " + resourcePath);
            return parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static Mesh load(String resourcePath, float[] defaultColor) {
        return load(resourcePath);
    }

    private static Mesh parse(BufferedReader br) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<float[]> vertColors = new ArrayList<>();
        List<Float> outVerts = new ArrayList<>();
        List<Integer> outIndices = new ArrayList<>();
        Map<String, Integer> vertexMap = new HashMap<>();
        boolean hasUV = false;

        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");

            if (p[0].equals("v") && p.length >= 4) {
                positions.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                if (p.length >= 7) {
                    vertColors.add(new float[]{Float.parseFloat(p[4]), Float.parseFloat(p[5]), Float.parseFloat(p[6])});
                } else {
                    vertColors.add(new float[]{0.5f, 0.5f, 0.5f});
                }
            } else if (p[0].equals("vn") && p.length >= 4) {
                normals.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
            } else if (p[0].equals("vt") && p.length >= 3) {
                texCoords.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                hasUV = true;
            } else if (p[0].equals("f")) {
                int[] fi = new int[p.length - 1];
                for (int i = 1; i < p.length; i++) {
                    fi[i - 1] = getOrCreate(p[i], positions, normals, texCoords, vertColors, hasUV, outVerts, vertexMap);
                }
                for (int i = 1; i < fi.length - 1; i++) {
                    outIndices.add(fi[0]); outIndices.add(fi[i]); outIndices.add(fi[i + 1]);
                }
            }
        }

        if (normals.isEmpty()) computeNormals(outVerts, outIndices, hasUV);

        float[] va = new float[outVerts.size()];
        for (int i = 0; i < va.length; i++) va[i] = outVerts.get(i);
        int[] ia = new int[outIndices.size()];
        for (int i = 0; i < ia.length; i++) ia[i] = outIndices.get(i);
        return new Mesh(va, ia, hasUV);
    }

    private static int getOrCreate(String fv, List<float[]> pos, List<float[]> norms,
                                    List<float[]> uvs, List<float[]> colors, boolean hasUV,
                                    List<Float> out, Map<String, Integer> map) {
        Integer existing = map.get(fv);
        if (existing != null) return existing;

        String[] p = fv.split("/");
        int pi = Integer.parseInt(p[0]) - 1;
        int ti = -1, ni = -1;
        if (p.length >= 2 && !p[1].isEmpty()) ti = Integer.parseInt(p[1]) - 1;
        if (p.length >= 3 && !p[2].isEmpty()) ni = Integer.parseInt(p[2]) - 1;

        float[] position = pos.get(pi);
        float nx = 0, ny = 1, nz = 0;
        if (ni >= 0 && ni < norms.size()) { nx = norms.get(ni)[0]; ny = norms.get(ni)[1]; nz = norms.get(ni)[2]; }
        float[] col = (pi < colors.size()) ? colors.get(pi) : new float[]{0.5f, 0.5f, 0.5f};

        int idx = out.size() / (hasUV ? 11 : 9);
        out.add(position[0]); out.add(position[1]); out.add(position[2]);
        out.add(nx); out.add(ny); out.add(nz);
        out.add(col[0]); out.add(col[1]); out.add(col[2]);
        if (hasUV) {
            if (ti >= 0 && ti < uvs.size()) { out.add(uvs.get(ti)[0]); out.add(uvs.get(ti)[1]); }
            else { out.add(0f); out.add(0f); }
        }

        map.put(fv, idx);
        return idx;
    }

    private static void computeNormals(List<Float> v, List<Integer> idx, boolean hasUV) {
        int stride = hasUV ? 11 : 9;
        for (int i = 0; i < v.size(); i += stride) { v.set(i+3, 0f); v.set(i+4, 0f); v.set(i+5, 0f); }
        for (int i = 0; i < idx.size(); i += 3) {
            int a = idx.get(i)*stride, b = idx.get(i+1)*stride, c = idx.get(i+2)*stride;
            float ax = v.get(b)-v.get(a), ay = v.get(b+1)-v.get(a+1), az = v.get(b+2)-v.get(a+2);
            float bx = v.get(c)-v.get(a), by = v.get(c+1)-v.get(a+1), bz = v.get(c+2)-v.get(a+2);
            float nx = ay*bz-az*by, ny = az*bx-ax*bz, nz = ax*by-ay*bx;
            for (int j : new int[]{a,b,c}) { v.set(j+3,v.get(j+3)+nx); v.set(j+4,v.get(j+4)+ny); v.set(j+5,v.get(j+5)+nz); }
        }
        for (int i = 0; i < v.size(); i += stride) {
            float nx=v.get(i+3), ny=v.get(i+4), nz=v.get(i+5);
            float l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            if (l>0) { v.set(i+3,nx/l); v.set(i+4,ny/l); v.set(i+5,nz/l); }
        }
    }
}
