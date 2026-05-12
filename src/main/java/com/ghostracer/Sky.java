package com.ghostracer;

import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import java.io.*;
import java.nio.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

public class Sky {
    private int vao;
    private int vbo;
    private int hdrTexture = 0;

    public void init() {
        float[] quad = { -1,-1, 3,-1, -1,3 };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer fb = MemoryUtil.memAllocFloat(quad.length);
        fb.put(quad).flip();
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        MemoryUtil.memFree(fb);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);

        loadHDR("/textures/sky.hdr");
    }

    private void loadHDR(String path) {
        try (InputStream is = Sky.class.getResourceAsStream(path)) {
            if (is == null) { System.err.println("sky hdr not found: " + path); return; }
            byte[] bytes = is.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();

            IntBuffer w = MemoryUtil.memAllocInt(1);
            IntBuffer h = MemoryUtil.memAllocInt(1);
            IntBuffer ch = MemoryUtil.memAllocInt(1);

            stbi_set_flip_vertically_on_load(true);
            FloatBuffer image = stbi_loadf_from_memory(buf, w, h, ch, 3);
            if (image == null) {
                System.err.println("stb hdr fail: " + stbi_failure_reason());
                MemoryUtil.memFree(buf);
                return;
            }

            hdrTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, hdrTexture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGB16F, w.get(0), h.get(0), 0, GL_RGB, GL_FLOAT, image);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            stbi_image_free(image);
            MemoryUtil.memFree(buf);
            MemoryUtil.memFree(w);
            MemoryUtil.memFree(h);
            MemoryUtil.memFree(ch);
        } catch (IOException e) { System.err.println("hdr load error: " + e.getMessage()); }
    }

    public void render(Shader shader) {
        glDepthMask(false);
        shader.bind();
        if (hdrTexture != 0) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, hdrTexture);
            shader.setInt("skyTex", 0);
            shader.setInt("useHDR", 1);
        } else {
            shader.setInt("useHDR", 0);
        }
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
        glDepthMask(true);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        if (hdrTexture != 0) glDeleteTextures(hdrTexture);
    }
}
