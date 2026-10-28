# Avengers 3D

Avengers 3D is a Java 3D arcade prototype built with LWJGL 3 and JOML. Choose
from five heroes, survive increasingly difficult enemy waves, and record high
scores using a player name.

The game is a procedural prototype: meshes, textures, and sound effects are
generated at runtime, so no external art assets are required.

## Project layout

```text
avengers3d/
├── avengers3d/              # Gradle Java project
│   ├── src/main/java/       # Game source
│   ├── src/main/resources/  # GLSL shaders
│   ├── build.gradle
│   └── README.md            # Detailed project documentation
├── .gitignore
└── avengers3d.code-workspace
```

## Requirements

- Java Development Kit (JDK) 17 or newer
- Gradle 8 or newer
- OpenGL 3.3-compatible graphics support
- A working audio device is recommended but not required
- Internet access the first time Gradle downloads dependencies

The Gradle build requests Java 17 through its toolchain configuration. A
newer JDK can run the project when the Gradle toolchain is configured to use
that installed JDK.

## Run

Open PowerShell in the Java project directory:

```powershell
cd "avengers3d"
gradle run
```

The game writes runtime scores to `scores.txt`. This file is intentionally
ignored by Git.

If Gradle is not on `PATH`, run the project with a local Gradle installation
or use the Gradle wrapper if one is added to the project:

```powershell
.\gradlew.bat run
```

## Controls

| Action | Control |
|---|---|
| Enter player name | Type on the main menu |
| Delete player-name characters | `Backspace` |
| Choose Hulk | `1` |
| Choose Iron Man | `2` |
| Choose Thor | `3` |
| Choose Captain America | `4` |
| Choose Black Widow | `5` |
| Move | `W`, `A`, `S`, `D` |
| Look / aim | Mouse |
| Attack / repulsors | Left mouse button or `J` |
| Special power | `Q` |
| Jump | `Space` |
| Hulk ground pound | `Left Ctrl` while airborne |
| Iron Man flight toggle | `F` |
| Iron Man ascend | `Space` while flying |
| Iron Man descend | `Left Shift` while flying |
| First-/third-person camera | `V` |
| Camera zoom | Mouse wheel |
| Pause / resume | `Esc` |
| Quit to menu while paused | `Q` |

## Features

- Hulk and Iron Man playable characters
- Thor, Captain America, and Black Widow playable characters
- Additional heroes including Hawkeye, Spider-Man, Scarlet Witch, Vision,
  Black Panther, Ant-Man, Winter Soldier, Doctor Strange, and Captain Marvel
- Third-person and first-person camera modes
- Endless enemy waves with increasing difficulty
- Ground soldiers and hovering drones
- Hulk smash, double jump, and ground pound
- Iron Man flight and camera-aimed repulsor bolts
- Thor lightning strike, Captain America shield throw, and Black Widow rapid
  dual shots
- Shared Q-key special powers with per-hero cooldowns and HUD feedback
- Manual 3D physics with gravity, collisions, overlaps, and raycasts
- Health, wave, score, enemy-count, crosshair, and damage-flash HUD
- Persistent top-five scores with player names
- Procedural meshes, textures, music, and sound effects
- OpenGL 3.3 rendering with lighting, specular highlights, and distance fog
- 4x MSAA and vertical synchronization

## Source structure

```text
src/main/java/com/avengers3d/
├── assets/    Runtime asset and OBJ loading helpers
├── audio/     OpenAL audio and synthesized sound
├── core/      Main loop, input, and window management
├── game/      Game state, world rules, combat, enemies, and scores
├── physics/   AABB, sphere, collision, and raycast helpers
├── render/    Camera, meshes, shaders, renderer, textures, and HUD
└── scene/     Transforms, game objects, and playable characters
```

## Build and validation

Compile without launching the window:

```powershell
gradle compileJava
```

Run the application:

```powershell
gradle run
```

The project has no automated test suite yet. A successful `compileJava` task
checks Java compilation; `run` additionally verifies GLFW, OpenGL, and OpenAL
initialization on the local machine.

## Troubleshooting

### `gradle` is not recognized

Install Gradle or add it to `PATH`. Alternatively, use the Gradle wrapper:

```powershell
.\gradlew.bat run
```

### Java toolchain 17 cannot be found

Install JDK 17 and configure `JAVA_HOME`, or configure Gradle to use an
installed newer JDK for this Java-17-compatible project.

### The window does not open

Check that the graphics driver supports OpenGL 3.3 and that another process
is not holding the display context. Run from PowerShell to see initialization
errors.

### No sound

The game can continue without an audio device. Check the system output device
and OpenAL installation if audio is required.

## Legal note

This is an educational prototype. Marvel names, characters, and likenesses
are trademarks or copyrighted properties of their respective owners. Use
original or properly licensed characters and assets before distributing a
public or commercial version.
