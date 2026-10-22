# FrogJump

A 2D platformer built in Java (Swing). Collect every apple, avoid the spikes, and climb to the top of each level.

**▶ Play in your browser: https://gabbogs.github.io/FrogJump/**

<img src="docs/preview.jpg" width="600" alt="FrogJump title screen" />

## Controls

| Key | Action |
| --- | --- |
| `Enter` or click **Play** | Start |
| `A` / `D` | Move left / right |
| `W` / `Space` | Jump |
| `Esc` | Pause (the pause menu has a volume slider) |
| `M` or the speaker button | Mute / unmute |

## How it works

- **Levels are images.** Each level in `res/lvls/` is a small PNG where every pixel's color tells the game what to place: the red channel picks the terrain tile, blue marks apples and spikes, and green marks the player's spawn point.
- **Game loop:** fixed update rate (200 updates/s) with separate rendering, in `main/Game.java`. Due updates run in a batch each frame, so game speed stays the same even when frames are slow.
- **States:** menu, playing, pause, game over, and level complete, each handled by its own overlay class in `gamestates/` and `ui/`.

## Run it

**On the desktop:** open the project in Eclipse (or any Java IDE) with `src` and `res` as source folders, then run `main.MainClass`. Java 8 or newer. Add `--perf` to print updates and frames per second.

**In the browser:** `docs/` holds a JavaScript port of the game (`docs/game.js`) that GitHub Pages serves. It uses the same art, levels, physics numbers and rules as the Java code, drawn on an HTML canvas that fills the window. Physics runs at the same fixed 200 steps per second, and drawing blends between steps so movement stays smooth at any screen refresh rate. It loads in about a second (around 2 MB, most of it music), and the volume is remembered between visits. The web version reads levels from `docs/assets/lvls/levels.json`, exported from the level PNGs with Java's image reader (browsers may color-correct images, which would change the tile numbers). After editing a level, run `java tools/ExportLevels.java res/lvls docs/assets/lvls/levels.json`.

## Team and credits

FrogJump started as a course project.

| Who | Role | Made |
| --- | --- | --- |
| **Matt Mariano**<br>[@mttrnmrn](https://github.com/mttrnmrn) | Developer | • **Game loop:** a fixed 200 updates per second, separate from drawing<br>• **Player:** movement, jumping, gravity, and collision with the terrain<br>• **Level system:** builds each level from an image (terrain, apples, spikes, spawn point), with a camera that scrolls up the level<br>• **Objects:** floating, animated apples to collect and spikes that end the run<br>• **Game states:** menu, playing, pause, game over, and level complete, with keyboard and mouse controls<br>• **Sound:** background music and sound effects |
| **Gabriel C. Bognot**<br>[@gabbogs](https://github.com/gabbogs) | Designer | • **UI screens:** main menu, pause, game over, and level complete<br>• **Level 2 map:** drawn pixel by pixel in GIMP<br>• **Browser version:** JavaScript port, apple counter, mute button, volume slider, compressed music, and an original sound effect replacing a copyrighted one |

Art: [Pixel Adventure](https://pixelfrog-assets.itch.io/pixel-adventure-1) by Pixel Frog.
