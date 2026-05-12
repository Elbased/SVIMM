package com.ghostracer;

import org.lwjgl.opengl.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private final int program;

    public Shader(String vertPath, String fragPath) {
        String vertSrc = loadResource(vertPath);
        String fragSrc = loadResource(fragPath);

        int vert = compile(vertSrc, GL_VERTEX_SHADER);
        int frag = compile(fragSrc, GL_FRAGMENT_SHADER);

        program = glCreateProgram();
        glAttachShader(program, vert);
        glAttachShader(program, frag);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == 0)
            throw new RuntimeException("shader link failed: " + glGetProgramInfoLog(program, 1024));

        glDeleteShader(vert);
        glDeleteShader(frag);
    }

    private int compile(String src, int type) {
        int shader = glCreateShader(type);
        glShaderSource(shader, src);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("shader compile failed: " + glGetShaderInfoLog(shader, 1024));
        return shader;
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("shader not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public void bind() { glUseProgram(program); }
    public void unbind() { glUseProgram(0); }

    public void setMat4(String name, Matrix4f mat) {
        float[] buf = new float[16];
        mat.get(buf);
        glUniformMatrix4fv(glGetUniformLocation(program, name), false, buf);
    }

    public void setVec3(String name, Vector3f v) {
        glUniform3f(glGetUniformLocation(program, name), v.x, v.y, v.z);
    }

    public void setFloat(String name, float v) {
        glUniform1f(glGetUniformLocation(program, name), v);
    }

    public void setInt(String name, int v) {
        glUniform1i(glGetUniformLocation(program, name), v);
    }

    public void cleanup() { glDeleteProgram(program); }
}
