# Avengers 3D Java game

This directory contains the Gradle application for the Avengers 3D
procedural arcade prototype. The repository-level documentation is available
at `../README.md`; this file provides the project-specific run instructions.

## Requirements

- JDK 17 or newer
- Gradle 8 or newer
- OpenGL 3.3-compatible graphics support

## Run

From this directory:

```powershell
gradle run
```

Compile without starting the game:

```powershell
gradle compileJava
```

The game stores named top-five scores in `scores.txt`. Runtime score data is
ignored by Git.

## Controls

| Action | Control |
|---|---|
| Enter player name | Type on the main menu |
| Delete name characters | `Backspace` |
| Choose Hulk / Iron Man | `1` / `2` |
| Choose Thor | `3` |
| Choose Captain America | `4` |
| Choose Black Widow | `5` |
| Move / look | `WASD` / mouse |
| Attack | Left mouse button or `J` |
| Special power | `Q` |
| Jump | `Space` |
| Hulk ground pound | `Left Ctrl` while airborne |
| Iron Man flight | `F` |
| Iron Man descend | `Left Shift` while flying |
| Camera mode / zoom | `V` / mouse wheel |
| Pause | `Esc` |
| Quit to menu | `Q` while paused |

## Main packages

- `game`: game state, world simulation, enemies, combat, and scoreboard
- `scene`: transforms, game objects, and the expanded playable hero roster
  including Spider-Man, Scarlet Witch, Vision, Black Panther, Ant-Man, Doctor
  Strange, and Captain Marvel
- `render`: OpenGL renderer, camera, shaders, meshes, and HUD
- `physics`: collision and raycast helpers
- `audio`: OpenAL and procedural sound
- `core`: window, input, and fixed-step loop
- `assets`: procedural/OBJ asset loading helpers

See the repository [README](../README.md) for the complete feature list,
project layout, troubleshooting guidance, and legal note.
