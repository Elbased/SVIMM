package com.ghostracer;

import org.lwjgl.system.MemoryUtil;
import java.nio.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int indexCount;

    // 9-float: pos(3)+normal(3)+color(3), no UV
    public Mesh(float[] vertices, int[] indices) {
        this(vertices, indices, false);
    }

    // 11-float: pos(3)+normal(3)+color(3)+uv(2)
    public Mesh(float[] vertices, int[] indices, boolean hasUV) {
        indexCount = indices.length;
        int stride = hasUV ? 11 * Float.BYTES : 9 * Float.BYTES;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer fb = MemoryUtil.memAllocFloat(vertices.length);
        fb.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        MemoryUtil.memFree(fb);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer ib = MemoryUtil.memAllocInt(indices.length);
        ib.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        MemoryUtil.memFree(ib);

        // position
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        // normal
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        // color
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        // uv
        if (hasUV) {
            glVertexAttribPointer(3, 2, GL_FLOAT, false, stride, 9L * Float.BYTES);
            glEnableVertexAttribArray(3);
        }

        glBindVertexArray(0);
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
}
