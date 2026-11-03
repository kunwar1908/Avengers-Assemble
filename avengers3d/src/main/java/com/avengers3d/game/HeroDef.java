package com.avengers3d.game;

import com.avengers3d.render.Camera3D;
import com.avengers3d.scene.Look;
import com.avengers3d.scene.Rig;

/** Data describing one playable hero. Adding a hero = adding one HeroDef to Heroes.ALL; menus and HUD adapt automatically. */
public final class HeroDef {
    /** One ability: runs when the player presses the key. Cooldowns and animation are handled by Hero. */
    public interface Ability { void use(World w, Hero h, Camera3D cam); }

    public final String id, name;
    public String tagline = "", attackName = "ATTACK", specialName = "SPECIAL";
    public String[] perks = {};
    public float health = 100f, walkSpeed = 7f, jumpSpeed = 11f, attackCd = .5f, specialCd = 8f;
    public int airJumps;
    public boolean canFly, groundPound;
    public float[] theme = {1f, 1f, 1f};
    public final Look look = new Look();
    public Ability attack, special;
    public int attackAnim = Rig.ATK_SHOOT, specialAnim = Rig.ATK_CAST;
    public float attackTime = .35f, specialTime = .6f;

    public HeroDef(String id, String name) { this.id = id; this.name = name; }
}
