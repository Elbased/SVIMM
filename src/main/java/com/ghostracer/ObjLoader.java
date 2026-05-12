package com.ghostracer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjLoader {

    public static Mesh load(String resourcePath) {
        try (InputStream is = ObjLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("obj not found: " + resourcePath);
            // determine base path for loading .mtl files
            String basePath = resourcePath.substring(0, resourcePath.lastIndexOf('/') + 1);
            return parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)), basePath, resourcePath);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static Mesh load(String resourcePath, float[] defaultColor) {
        return load(resourcePath);
    }

    /**
     * Parse MTL file to extract material diffuse colors (Kd).
     * Returns a map of material name -> float[3] rgb.
     */
    private static Map<String, float[]> parseMtl(String basePath, String mtlFilename, String objPath) {
        Map<String, float[]> materials = new HashMap<>();
        String mtlPath = basePath + mtlFilename;
        try (InputStream is = ObjLoader.class.getResourceAsStream(mtlPath)) {
            if (is == null) return materials;
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            String currentMat = null;
            boolean hasTexture = false;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\s+");
                if (p[0].equals("newmtl") && p.length >= 2) {
                    currentMat = p[1];
                    hasTexture = false;
                } else if (p[0].equals("Kd") && p.length >= 4 && currentMat != null) {
                    float r = Float.parseFloat(p[1]);
                    float g = Float.parseFloat(p[2]);
                    float b = Float.parseFloat(p[3]);
                    materials.put(currentMat, new float[]{r, g, b});
                } else if (p[0].equals("map_Kd") && currentMat != null) {
                    hasTexture = true;
                    // If Kd was white (1,1,1) and we have a texture, generate a color
                    float[] existing = materials.get(currentMat);
                    if (existing != null && existing[0] > 0.98f && existing[1] > 0.98f && existing[2] > 0.98f) {
                        // Generate a building-appropriate color from the filename
                        // Use the OBJ path for unique-per-model hash
                        int hash = objPath.hashCode();
                        float hue = (hash & 0x7FFFFFFF) % 360 / 360f;
                        // Muted urban colors
                        float sat = 0.15f + (((hash >> 8) & 0xFF) / 255f) * 0.25f;
                        float val = 0.55f + (((hash >> 16) & 0xFF) / 255f) * 0.3f;
                        float[] rgb = hsvToRgb(hue, sat, val);
                        materials.put(currentMat, rgb);
                    }
                }
            }
        } catch (IOException e) { /* ignore missing mtl */ }
        return materials;
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        int hi = (int)(h * 6) % 6;
        float f = h * 6 - hi;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (hi) {
            case 0: return new float[]{v, t, p};
            case 1: return new float[]{q, v, p};
            case 2: return new float[]{p, v, t};
            case 3: return new float[]{p, q, v};
            case 4: return new float[]{t, p, v};
            default: return new float[]{v, p, q};
        }
    }

    private static Mesh parse(BufferedReader br, String basePath, String objPath) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<float[]> vertColors = new ArrayList<>();
        List<Float> outVerts = new ArrayList<>();
        List<Integer> outIndices = new ArrayList<>();
        Map<String, Integer> vertexMap = new HashMap<>();
        boolean hasUV = false;
        boolean hasVertexColors = false;

        // MTL material support
        Map<String, float[]> materials = new HashMap<>();
        float[] activeMaterialColor = {0.5f, 0.5f, 0.5f};

        // First pass: read all lines, detect features
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }

        // Check if any vertex has embedded colors (6+ values on v line)
        for (String l : lines) {
            String tl = l.trim();
            if (tl.startsWith("v ")) {
                String[] p = tl.split("\\s+");
                if (p.length >= 7) {
                    hasVertexColors = true;
                    break;
                }
            }
        }

        // Load MTL if referenced and no vertex colors
        for (String l : lines) {
            String tl = l.trim();
            if (tl.startsWith("mtllib ")) {
                String mtlFile = tl.substring(7).trim();
                materials = parseMtl(basePath, mtlFile, objPath);
                break;
            }
        }

        // Process lines
        for (String l : lines) {
            line = l.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");

            if (p[0].equals("v") && p.length >= 4) {
                positions.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                if (p.length >= 7) {
                    vertColors.add(new float[]{Float.parseFloat(p[4]), Float.parseFloat(p[5]), Float.parseFloat(p[6])});
                } else {
                    // Will be filled from material color at face time
                    vertColors.add(null);
                }
            } else if (p[0].equals("vn") && p.length >= 4) {
                normals.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
            } else if (p[0].equals("vt") && p.length >= 3) {
                texCoords.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                hasUV = true;
            } else if (p[0].equals("usemtl") && p.length >= 2) {
                String matName = p[1];
                if (materials.containsKey(matName)) {
                    activeMaterialColor = materials.get(matName);
                } else {
                    activeMaterialColor = new float[]{0.5f, 0.5f, 0.5f};
                }
            } else if (p[0].equals("f")) {
                int[] fi = new int[p.length - 1];
                for (int i = 1; i < p.length; i++) {
                    fi[i - 1] = getOrCreate(p[i], positions, normals, texCoords, vertColors,
                                             hasUV, outVerts, vertexMap, activeMaterialColor);
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
                                    List<Float> out, Map<String, Integer> map,
                                    float[] materialColor) {
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

        // Use vertex color if available AND not all-white (1,1,1)
        // All-white vertex colors typically mean "use texture" — fall back to material
        float[] col = materialColor;
        if (pi < colors.size() && colors.get(pi) != null) {
            float[] vc = colors.get(pi);
            boolean isWhite = (vc[0] > 0.98f && vc[1] > 0.98f && vc[2] > 0.98f);
            if (!isWhite) {
                col = vc;
            }
            // If white and we have a non-default material color, use that
        }

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
