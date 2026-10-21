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
| `Esc` | Pause |

## How it works

- **Levels are images.** Each level in `res/lvls/` is a small PNG where every pixel's color tells the game what to place: the red channel picks the terrain tile, blue marks apples and spikes, and green marks the player's spawn point.
- **Game loop:** fixed update rate (200 updates/s) with separate rendering (120 FPS), in `main/Game.java`.
- **States:** menu, playing, pause, game over, and level complete, each handled by its own overlay class in `gamestates/` and `ui/`.

## Run it

**In the browser:** the game in `docs/` runs through [CheerpJ](https://cheerpj.com/), which runs the original Java program in the browser with WebAssembly. Passing `--web` gives a borderless window that scales the game to fit the page.

**On the desktop:** open the project in Eclipse (or any Java IDE) with `src` and `res` as source folders, then run `main.MainClass`. Java 8 or newer.

## Team and credits

FrogJump was a course project built by a team of five. I designed the game's UI screens (main menu, pause, game over, and level complete) and the Level 2 map, which I drew pixel by pixel in GIMP.

For the web version I moved levels and sounds into the classpath so the game runs from a JAR, added the browser mode, removed the background music, and replaced a copyrighted sound effect with an original one.

Character, terrain, and fruit art: [Pixel Adventure](https://pixelfrog-assets.itch.io/pixel-adventure-1) by Pixel Frog.

## Author

**Gabriel C. Bognot**: [GitHub](https://github.com/gabbogs) · [LinkedIn](https://www.linkedin.com/in/gabbogs/)
