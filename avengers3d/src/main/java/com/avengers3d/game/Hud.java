package com.avengers3d.game;

import com.avengers3d.render.Camera3D;
import com.avengers3d.render.UIRenderer;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** In-game overlay: score, wave, combo, health, ability cooldowns, boss bar, radar, enemy health bars, banners. */
public final class Hud {
    private final UIRenderer ui;
    private final Vector4f v4 = new Vector4f();

    public Hud(UIRenderer ui) { this.ui = ui; }

    private void centered(String t, float w, float y, float scale, float r, float g, float b) {
        ui.text(t, (w - ui.textWidth(t, scale)) / 2f, y, scale, r, g, b);
    }

    private void bar(float x, float y, float w, float h, float frac, float r, float g, float b) {
        ui.rect(x - 2, y - 2, w + 4, h + 4, 0, 0, 0, .65f);
        ui.rect(x, y, w, h, .12f, .12f, .14f, .9f);
        ui.rect(x, y, w * Math.max(0f, Math.min(1f, frac)), h, r, g, b, 1f);
    }

    public void draw(World world, Camera3D cam, float w, float h, float s) {
        Hero p = world.player;
        if (p == null) return;
        float[] th = p.def.theme;

        // ---- top-left: score / wave
        ui.text("SCORE " + world.score, 24 * s, 18 * s, 3.2f * s, 1, 1, 1);
        String waveTxt = world.level.endless ? "WAVE " + world.wave : "WAVE " + Math.min(world.wave, world.totalWaves()) + " / " + world.totalWaves();
        ui.text(waveTxt, 24 * s, 52 * s, 2.4f * s, 1f, .85f, .3f);
        ui.text("ENEMIES " + world.enemies.size() + "    KILLS " + world.kills, 24 * s, 80 * s, 2f * s, .85f, .85f, .9f);
        if (world.combo >= 2) {
            ui.text("COMBO x" + world.combo, 24 * s, 108 * s, 3f * s, 1f, .55f + .45f * Math.min(1f, world.comboTimer / 3f), .2f);
            bar(24 * s, 138 * s, 150 * s, 5 * s, world.comboTimer / 3.2f, 1f, .6f, .2f);
        }

        // ---- top-centre: objective or boss bar
        if (world.boss != null && world.boss.alive) {
            Enemy b = world.boss;
            centered(b.name, w, 14 * s, 3f * s, 1f, .35f, .35f);
            bar(w / 2f - 280 * s, 46 * s, 560 * s, 16 * s, b.health / b.maxHealth, .9f, .15f, .2f);
        } else if (world.storyMode) {
            centered(world.level.objective, w, 14 * s, 2f * s, .9f, .9f, 1f);
        }

        // ---- bottom-left: health
        float bw = 330 * s, bh = 24 * s, bx = 24 * s, by = h - 52 * s;
        float frac = p.health / p.maxHealth;
        bar(bx, by, bw, bh, frac, 1f - frac * .7f, .25f + frac * .65f, .15f);
        ui.text(p.def.name + "   HP " + (int) Math.ceil(p.health), bx, by - 30 * s, 2.5f * s, th[0], th[1], th[2]);
        if (p.flying) ui.text("FLIGHT ON", bx, by - 58 * s, 2.2f * s, .5f, 1f, 1f);
        if (p.rageTimer > 0f) ui.text("RAGE " + (int) Math.ceil(p.rageTimer), bx, by - 58 * s, 2.4f * s, .5f, 1f, .4f);

        // ---- bottom-centre: ability slots
        float slotW = 150 * s, slotH = 58 * s, gap = 12 * s;
        float sx = w / 2f - (slotW * 3 + gap * 2) / 2f, sy = h - slotH - 24 * s;
        slot(sx, sy, slotW, slotH, "LMB", p.def.attackName, p.cooldown / p.def.attackCd, th, s);
        slot(sx + slotW + gap, sy, slotW, slotH, "Q", p.def.specialName, p.specialCooldown / p.def.specialCd, th, s);
        slot(sx + (slotW + gap) * 2, sy, slotW, slotH, "SHIFT", p.flying ? "DESCEND" : "DODGE", p.flying ? 0f : p.dodgeCooldown / 1.1f, th, s);

        radar(world, cam, w, h, s);
        enemyBars(world, cam, w, h, s);

        // ---- crosshair
        float cx = w / 2f, cy = h / 2f;
        ui.rect(cx - 12 * s, cy - 1, 8 * s, 2, 1, 1, 1, .85f);
        ui.rect(cx + 4 * s, cy - 1, 8 * s, 2, 1, 1, 1, .85f);
        ui.rect(cx - 1, cy - 12 * s, 2, 8 * s, 1, 1, 1, .85f);
        ui.rect(cx - 1, cy + 4 * s, 2, 8 * s, 1, 1, 1, .85f);

        world.numbers.draw(ui, cam, w, h, s);

        if (world.bannerTimer > 0f)
            centered(world.bannerText, w, h * .2f, 5.5f * s, 1f, .9f, .35f);
    }

    private void slot(float x, float y, float w, float h, String key, String label, float cd, float[] th, float s) {
        ui.rect(x - 2, y - 2, w + 4, h + 4, 0, 0, 0, .65f);
        ui.rect(x, y, w, h, .1f, .1f, .13f, .85f);
        boolean ready = cd <= 0.001f;
        ui.rect(x, y + h - 6 * s, w * (ready ? 1f : 1f - Math.min(1f, cd)), 6 * s, th[0], th[1], th[2], ready ? 1f : .6f);
        ui.text(key, x + 8 * s, y + 8 * s, 2f * s, .7f, .75f, .85f);
        ui.text(label, x + 8 * s, y + 30 * s, 1.9f * s, ready ? 1f : .6f, ready ? 1f : .6f, ready ? 1f : .65f);
    }

    private void radar(World world, Camera3D cam, float w, float h, float s) {
        float size = 150 * s, rx = w - size - 24 * s, ry = h - size - 24 * s, cx = rx + size / 2f, cy = ry + size / 2f;
        float range = 36f, k = (size / 2f) / range;
        ui.rect(rx - 2, ry - 2, size + 4, size + 4, .3f, .35f, .5f, .8f);
        ui.rect(rx, ry, size, size, 0, 0, .05f, .7f);
        ui.rect(cx - 1, ry, 2, size, .3f, .35f, .5f, .35f);
        ui.rect(rx, cy - 1, size, 2, .3f, .35f, .5f, .35f);
        float yaw = cam.yaw(), sy = (float) Math.sin(yaw), cyw = (float) Math.cos(yaw);
        Vector3f pp = world.player.transform().position();
        for (int i = 0; i < world.enemies.size(); i++) {
            Enemy e = world.enemies.get(i);
            Vector3f ep = e.transform().position();
            float dx = ep.x - pp.x, dz = ep.z - pp.z;
            float px = dx * cyw + dz * sy, py = dx * sy - dz * cyw;      // rotate so "up" on the radar is where the camera looks
            float ox = Math.max(-size / 2f + 3, Math.min(size / 2f - 3, px * k)), oy = Math.max(-size / 2f + 3, Math.min(size / 2f - 3, py * k));
            float d = e.boss ? 8 * s : 5 * s;
            ui.rect(cx + ox - d / 2f, cy - oy - d / 2f, d, d, e.boss ? 1f : 1f, e.boss ? .9f : .25f, e.boss ? .2f : .2f, 1f);
        }
        ui.rect(cx - 3 * s, cy - 3 * s, 6 * s, 6 * s, .4f, 1f, .5f, 1f);
    }

    private void enemyBars(World world, Camera3D cam, float w, float h, float s) {
        Vector3f eye = cam.eye();
        for (int i = 0; i < world.enemies.size(); i++) {
            Enemy e = world.enemies.get(i);
            if (e.boss || !e.alive || e.health >= e.maxHealth - .01f) continue;
            Vector3f p = e.transform().position();
            if (p.distanceSquared(eye) > 45f * 45f) continue;
            v4.set(p.x, p.y + e.barHeight + .3f, p.z, 1f);
            cam.viewProjection().transform(v4);
            if (v4.w <= .1f) continue;
            float sx = (v4.x / v4.w * .5f + .5f) * w, sy = (1f - (v4.y / v4.w * .5f + .5f)) * h;
            float bw = 46 * s;
            bar(sx - bw / 2f, sy, bw, 5 * s, e.health / e.maxHealth, 1f, .25f, .25f);
            if (e.stun > 0f) ui.text("STUN", sx - 16 * s, sy - 14 * s, 1.6f * s, .5f, .9f, 1f);
        }
    }
}
