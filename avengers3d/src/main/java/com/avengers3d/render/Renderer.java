package com.avengers3d.render;

import com.avengers3d.scene.GameObject3D;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/** Forward renderer: shadow pass, sky, lit scene pass. Per-part matrices come from GameObject3D.updateParts(). */
public final class Renderer implements AutoCloseable {
    private final ShaderProgram scene = ShaderProgram.load("scene.vert", "scene.frag");
    private final ShaderProgram depth = ShaderProgram.load("depth.vert", "depth.frag");
    private final SkyRenderer sky = new SkyRenderer();
    private final String[] lightPosNames = new String[Lights.MAX], lightColNames = new String[Lights.MAX];
    private final Vector3f sun = new Vector3f();

    public Renderer() {
        for (int i = 0; i < Lights.MAX; i++) {
            lightPosNames[i] = "uLightPos[" + i + "]";
            lightColNames[i] = "uLightCol[" + i + "]";
        }
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    /** Compute every part's world matrix once per frame (shared by the shadow and scene passes). */
    public void prepare(List<GameObject3D> objects) {
        for (int i = 0; i < objects.size(); i++) objects.get(i).updateParts();
    }

    public void shadowPass(List<GameObject3D> objects, ShadowMap sm, Vector3f focus) {
        sm.bind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);                       // render back faces into the map: avoids self-shadow acne
        depth.bind();
        depth.setMat4("uLightVP", sm.lightVP());
        for (int i = 0; i < objects.size(); i++) {
            GameObject3D o = objects.get(i);
            if (!o.castShadow) continue;
            Vector3f p = o.transform().position();
            float dx = p.x - focus.x, dz = p.z - focus.z;
            if (dx * dx + dz * dz > 60f * 60f) continue;
            for (int j = 0; j < o.parts.size(); j++) {
                GameObject3D.Part part = o.parts.get(j);
                if (part.mesh == null || !part.visible || !part.castShadow) continue;
                depth.setMat4("uModel", part.meshWorld);
                part.mesh.draw();
            }
        }
        glCullFace(GL_BACK);
    }

    public void scene(Camera3D cam, List<GameObject3D> objects, Environment env, Lights lights,
                      ShadowMap sm, float time) {
        sky.draw(cam, env, time);

        scene.bind();
        scene.setMat4("uViewProj", cam.viewProjection());
        scene.setMat4("uLightVP", sm.lightVP());
        scene.setVec3("uLightDir", sun.set(env.sunDir[0], env.sunDir[1], env.sunDir[2]).normalize());
        scene.setVec3("uSunColor", env.sunColor);
        scene.setVec3("uAmbientSky", env.ambientSky);
        scene.setVec3("uAmbientGround", env.ambientGround);
        scene.setVec3("uCameraPos", cam.eye());
        scene.setVec3("uFogColor", env.fog);
        scene.setFloat("uFogDensity", env.fogDensity);
        scene.setInt("uLightCount", lights.count);
        for (int i = 0; i < lights.count; i++) {
            scene.setVec4(lightPosNames[i], lights.pos[i * 4], lights.pos[i * 4 + 1], lights.pos[i * 4 + 2], lights.pos[i * 4 + 3]);
            scene.setVec3(lightColNames[i], lights.col[i * 3], lights.col[i * 3 + 1], lights.col[i * 3 + 2]);
        }
        scene.setInt("uTexture", 0);
        scene.setInt("uShadow", 1);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, sm.texture());
        glActiveTexture(GL_TEXTURE0);

        int lastTex = -2;
        for (int i = 0; i < objects.size(); i++) {
            GameObject3D o = objects.get(i);
            scene.setFloat("uFlash", o.flash);
            scene.setFloat("uRim", o.rim);
            for (int j = 0; j < o.parts.size(); j++) {
                GameObject3D.Part p = o.parts.get(j);
                if (p.mesh == null || !p.visible) continue;
                int tex = p.textureId >= 0 ? p.textureId : o.textureId;
                if (tex != lastTex) { glBindTexture(GL_TEXTURE_2D, tex); lastTex = tex; }
                scene.setMat4("uModel", p.meshWorld);
                scene.setMat3("uNormalMat", p.normalMat);
                scene.setVec3("uTint", p.r, p.g, p.b);
                scene.setFloat("uEmissive", p.emissive);
                scene.setFloat("uTexScale", p.texScale);
                scene.setFloat("uSpec", p.spec);
                p.mesh.draw();
            }
        }
    }

    @Override public void close() { scene.close(); depth.close(); sky.close(); }
}
