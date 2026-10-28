# Marvel Avengers 3D - playable arcade prototype (Java 17+, LWJGL 3.3, JOML)

Survive endless waves of Hydra soldiers and drones as **Hulk** or **Iron Man**.
Everything is procedural (primitive meshes, generated textures, synthesized sound), so there are no asset files.

## Run
    gradle run            # Gradle 8+, JDK 17+, internet for the first dependency download
(macOS: the build already passes -XstartOnFirstThread. IDE users: add that VM option manually.)

## Controls
| Action | Key |
|---|---|
| Move / look | WASD / mouse |
| Jump (Iron Man: jump or ascend while flying) | Space |
| Attack | Left mouse or J |
| Hulk ground pound (in the air) | Left Ctrl |
| Iron Man flight on/off, descend | F, Left Shift |
| 1st / 3rd person, zoom | V, scroll |
| Pause / menu quit | Esc / Q |

## Features
- Menu, play, pause, game-over state machine; top-5 scoreboard saved in `scores.txt`
- Waves with scaling difficulty, clear bonus + healing, two enemy types (ground soldier, hovering drone)
- HUD: score, wave, enemies left, health bar, crosshair, damage flash, wave banner
- Hulk: smash (sphere-vs-AABB), double jump, ground-pound shockwave. Iron Man: flight (gravity off), repulsor bolts aimed with a camera raycast
- Procedural sound effects and looping music through OpenAL; game still runs if no audio device exists
- Manual 3D physics: gravity, jumping, AABB/sphere overlap, minimum-translation resolution, raycasts

## Layout (dependencies point downward)
    game/    GameApp (window, states, HUD, input mapping), World (rules, waves, combat), Enemy, Projectile, Block, ScoreBoard, MeshFactory
    scene/   Transform, GameObject3D (+ Parts), Character3D, Hulk, IronMan
    physics/ AABB, BoundingSphere, Physics3D
    render/  Camera3D, ShaderProgram, Mesh, Renderer, UIRenderer, Textures
    audio/   AudioManager (OpenAL), Synth
    assets/  AssetManager, ObjParser  (async OBJ + texture streaming, ready for real models)
    core/    GameLoop (fixed 60 Hz), Window, Input

## Matrix pipeline
    clip = Projection * View * Model * vec4(localPos, 1)
    Model = T*R*S, per part: ObjectModel * T(offset) * S(size)

## GC rules
Hot paths (update/render) reuse scratch vectors/matrices and indexed loops; meshes and UI batches live off-heap.
Allocation happens only at spawn time (a handful of enemies per wave).

## Known simplifications / next steps
- Characters are box-and-sphere figures: swap in OBJ models via `AssetManager`, then add skeletal animation (FBX via lwjgl-assimp)
- Enemy AI walks straight at the player (no pathfinding); render transforms are not interpolated between physics steps
- No shadows, particles, skybox or gamepad support
- Marvel names/likenesses are trademarked: use original or licensed art/audio if you publish
- This code was written without being compiled in this environment: expect to fix small API/typo issues on first build
