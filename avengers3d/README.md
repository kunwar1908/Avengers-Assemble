# Marvel Avengers 3D  -  v2 (Java 17+, LWJGL 3.3, OpenGL 3.3)

A 3D action game with a 4-chapter **story mode**, an endless **arena**, six heroes, boss fights, procedural skeletal
animation, HDR bloom, shadow mapping, particles, 3D audio and a scoreboard. Everything is generated in code: no model,
texture or sound files.

## Run
    gradle wrapper        # once, to create gradlew (optional)
    gradle run            # needs JDK 17+; downloads LWJGL on first run
macOS: the build already passes `-XstartOnFirstThread`. Saves go to `savegame.properties` in the project folder.

## Controls
| Action | Key |
|---|---|
| Move / look | WASD / mouse |
| Jump (ascend while flying) | Space |
| Attack (hold to keep firing) | Left mouse or J |
| Special ability | Q |
| Dodge roll (descend while flying) | Left Shift |
| Iron Man flight | F |
| Hulk ground pound (mid-air) | Left Ctrl |
| 1st/3rd person, zoom | V, mouse wheel |
| Pause | Esc |
Menus: W/S or arrows, Enter, Esc. Hero select: A/D to change hero, W previews the special.

## Heroes
Iron Man (flight, repulsors, piercing Unibeam) - Hulk (smash, double jump, ground pound, Rage) - Thor (hammer, lightning storm)
- Captain America (ricocheting shield, invulnerable charge) - Black Widow (dual pistols, stun Black-out) - Hawkeye (piercing and explosive arrows).
A hero is one `HeroDef` entry in `Heroes.java`: stats, look and two lambdas. Menus and HUD adapt automatically.

## Story
Four chapters (Downtown, Foundry, Frozen Ridge, Citadel) with cinematic dialogue, a mini-boss (Commander Vex) and a final boss
(Warden Omega, 3 phases: slam, missiles, sweeping laser, summons). The villains and faction are original; hero names are Marvel
trademarks, so use original names/art if you publish.

## Architecture
    core/    GameLoop (fixed 60 Hz), Window, Input
    render/  Camera3D, Renderer (shadow + scene passes), ShadowMap, PostProcessor (HDR, bloom, ACES), SkyRenderer,
             ParticleSystem, UIRenderer, ProcTextures, Environment, Lights, ShaderProgram, Mesh
    scene/   GameObject3D (part hierarchy), Rig + Look (humanoid skeleton and animator), Character3D, Transform
    physics/ AABB, BoundingSphere, Physics3D (manual collision, MTV, raycasts)
    game/    GameApp (states, menus, story flow), World (rules), Hero/HeroDef/Heroes, Enemy (AI + bosses), Levels/Level,
             Hud, Fx, Projectile, Block, DamageNumbers, SaveData, Assets, MeshFactory
    audio/   AudioManager (OpenAL, 3D voices, 5 music tracks), Synth (procedural sound)
    assets/  AssetManager, ObjParser (async model/texture loading, ready for real assets)

## Tests
`gradle test` runs JUnit tests for the collision math (no GPU needed). CI: `.github/workflows/build.yml`.

## Status
Compile-checked and unit-tested for the pure-math parts. Shaders and OpenGL/OpenAL calls could not be run in the
environment that wrote this code, so report any first-run driver or shader errors and they can be fixed quickly.
