package com.avengers3d.render;

import com.avengers3d.scene.GameObject3D;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/** Minimal forward renderer. Upgrade path: sort by mesh/texture, then glDrawElementsInstanced. */
public final class Renderer {
    private final ShaderProgram shader;
    private final Matrix3f normalMat = new Matrix3f();          // scratch objects: no per-frame allocation
    private final Matrix4f partModel = new Matrix4f();
    private final Vector3f lightDir = new Vector3f(-0.4f, -1f, -0.3f).normalize();

    public Renderer(ShaderProgram shader) {
        this.shader = shader;
        glEnable(GL_DEPTH_TEST);   // z-buffer: nearest fragment wins
        glEnable(GL_CULL_FACE);    // skip back faces (counter-clockwise = front)
    }

    public void render(Camera3D camera, List<? extends GameObject3D> objects) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        shader.bind();
        shader.setMat4("uViewProj", camera.viewProjection());   // once per frame
        shader.setVec3("uLightDir", lightDir);
        shader.setVec3("uCameraPos", camera.eye());
        shader.setVec3("uFogColor", 0.45f, 0.65f, 0.9f);
        shader.setInt("uTexture", 0);

        for (int i = 0; i < objects.size(); i++) {               // indexed loops: no Iterator garbage
            GameObject3D o = objects.get(i);
            if (o.parts.isEmpty()) continue;
            Matrix4f model = o.transform().modelMatrix();
            shader.setFloat("uFlash", o.flash);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, o.textureId);

            for (int j = 0; j < o.parts.size(); j++) {
                GameObject3D.Part p = o.parts.get(j);
                // Part matrix = Model * T(offset) * S(size): scale, then offset, then the object's own transform.
                partModel.set(model).translate(p.ox, p.oy, p.oz).scale(p.sx, p.sy, p.sz);
                shader.setMat4("uModel", partModel);
                partModel.normal(normalMat);                     // transpose(inverse(upper-left 3x3))
                shader.setMat3("uNormalMat", normalMat);
                shader.setVec3("uTint", p.r, p.g, p.b);
                p.mesh.draw();
            }
        }
    }
}
