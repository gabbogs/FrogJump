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

FrogJump was a course project built by a team of five. I designed the game's UI screens (main menu, pause, game over, and level complete) and the Level 2 map, which I drew pixel by pixel in GIMP.

After the course I ported the game to JavaScript so it plays in the browser, redrew the apple counter in the game's pixel style, added a mute button and a volume slider, moved levels and sounds into the classpath so the Java game runs from a JAR, compressed the background music, and replaced a copyrighted sound effect with an original one.

Character, terrain, and fruit art: [Pixel Adventure](https://pixelfrog-assets.itch.io/pixel-adventure-1) by Pixel Frog.

## Author

**Gabriel C. Bognot**: [GitHub](https://github.com/gabbogs) · [LinkedIn](https://www.linkedin.com/in/gabbogs/)
