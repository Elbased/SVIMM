package com.ghostracer;

import org.lwjgl.system.MemoryUtil;
import java.io.*;
import java.nio.*;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

public class TextureLoader {

    public static int load(String resourcePath) {
        try (InputStream is = TextureLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("texture not found: " + resourcePath);
            byte[] bytes = is.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();

            IntBuffer w = MemoryUtil.memAllocInt(1);
            IntBuffer h = MemoryUtil.memAllocInt(1);
            IntBuffer ch = MemoryUtil.memAllocInt(1);

            stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = stbi_load_from_memory(buf, w, h, ch, 4);
            if (image == null) throw new RuntimeException("stb failed: " + stbi_failure_reason());

            int texId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            stbi_image_free(image);
            MemoryUtil.memFree(buf);
            MemoryUtil.memFree(w);
            MemoryUtil.memFree(h);
            MemoryUtil.memFree(ch);

            return texId;
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}
