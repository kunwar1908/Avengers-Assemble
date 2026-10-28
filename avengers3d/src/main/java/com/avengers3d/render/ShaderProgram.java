package com.avengers3d.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/** Compiles/links GLSL and uploads uniforms. Uniform locations are cached (glGetUniformLocation is slow). */
public final class ShaderProgram implements AutoCloseable {
    private final int program;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public ShaderProgram(String vertexSrc, String fragmentSrc) {
        int vs = compile(GL_VERTEX_SHADER, vertexSrc);
        int fs = compile(GL_FRAGMENT_SHADER, fragmentSrc);
        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0)
            throw new IllegalStateException("Link failed: " + glGetProgramInfoLog(program));
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0)
            throw new IllegalStateException("Shader compile failed: " + glGetShaderInfoLog(id));
        return id;
    }

    /** Load GLSL from the classpath (src/main/resources). */
    public static ShaderProgram fromResources(String vertPath, String fragPath) {
        try (var v = ShaderProgram.class.getResourceAsStream(vertPath);
             var f = ShaderProgram.class.getResourceAsStream(fragPath)) {
            if (v == null || f == null)
                throw new IllegalStateException("Missing shader resource: " + vertPath + " / " + fragPath);
            return new ShaderProgram(new String(v.readAllBytes(), StandardCharsets.UTF_8),
                                     new String(f.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public void bind() { glUseProgram(program); }

    private int loc(String name) {
        return uniforms.computeIfAbsent(name, n -> glGetUniformLocation(program, n));
    }

    public void setMat4(String name, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {   // stack memory: no heap garbage
            FloatBuffer fb = stack.mallocFloat(16);
            glUniformMatrix4fv(loc(name), false, m.get(fb));  // JOML is column-major, same as GL
        }
    }
    public void setMat3(String name, Matrix3f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(9);
            glUniformMatrix3fv(loc(name), false, m.get(fb));
        }
    }
    public void setVec3(String name, Vector3f v) { glUniform3f(loc(name), v.x, v.y, v.z); }
    public void setVec3(String name, float x, float y, float z) { glUniform3f(loc(name), x, y, z); }
    public void setVec2(String name, float x, float y) { glUniform2f(loc(name), x, y); }
    public void setFloat(String name, float v) { glUniform1f(loc(name), v); }
    public void setInt(String name, int v) { glUniform1i(loc(name), v); }

    @Override public void close() { glDeleteProgram(program); }
}
