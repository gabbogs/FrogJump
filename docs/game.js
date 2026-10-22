"use strict";
// FrogJump in the browser: a JavaScript port of the Java (Swing) game in src/.
// Same art, levels, physics numbers and rules. The game runs at its own resolution (1664x896)
// on a <canvas> stretched to fill the window, with physics at a fixed 200 steps per second.
// Comments name the Java class each part comes from.

// ---------- Constants (main.Game, utilz.Constants) ----------

const SCALE = 2;
const TILES_SIZE = 32 * SCALE;
const TILES_IN_WIDTH = 26, TILES_IN_HEIGHT = 14;
const GAME_WIDTH = TILES_SIZE * TILES_IN_WIDTH;
const GAME_HEIGHT = TILES_SIZE * TILES_IN_HEIGHT;
const UPS = 200;
const GRAVITY = 0.04 * SCALE;
const ANI_SPEED = 25;

const APPLE = 1, SPIKE = 2;
const APPLE_SIZE = 32 * SCALE, SPIKE_SIZE = 32 * SCALE;

const IDLE = 0, RUNNING = 1, JUMP = 2, FALLING = 3;
const PLAYER_FRAMES = { [IDLE]: 11, [RUNNING]: 12, [JUMP]: 1, [FALLING]: 1 };

const B_WIDTH_DEFAULT = 140, B_HEIGHT_DEFAULT = 56;
const B_WIDTH = B_WIDTH_DEFAULT * SCALE, B_HEIGHT = B_HEIGHT_DEFAULT * SCALE;
const URM_DEFAULT_SIZE = 56, URM_SIZE = URM_DEFAULT_SIZE * SCALE;

// Gamestate
const MENU = "menu", PLAYING = "playing", QUIT = "quit";
let gamestate = MENU;

const img = {}; // loaded images, by name
const truncate = Math.trunc; // Java's (int) cast

function intersects(a, b) {
  return a.width > 0 && a.height > 0 && b.width > 0 && b.height > 0 &&
    b.x < a.x + a.width && b.y < a.y + a.height && b.x + b.width > a.x && b.y + b.height > a.y;
}

function contains(r, x, y) {
  return x >= r.x && y >= r.y && x < r.x + r.width && y < r.y + r.height;
}

// ---------- Sound (Sounds.*) ----------
// Effects are decoded into Web Audio buffers; the music streams from an <audio> element.
// Everything goes through one gain node, which is the volume (0 = silent, 1 = full).

const sound = {
  ctx: null, master: null, buffers: {}, music: null, wantMusic: false,
  volume: 0.7, volumeBeforeMute: 0.7,

  async init() {
    try {
      const saved = parseFloat(localStorage.getItem("frogjump.volume"));
      if (saved >= 0 && saved <= 1) this.volume = saved;
    } catch (e) {}
    if (this.volume > 0) this.volumeBeforeMute = this.volume;
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return;
    this.ctx = new AC();
    this.master = this.ctx.createGain();
    this.master.gain.value = this.volume;
    this.master.connect(this.ctx.destination);

    this.music = new Audio("assets/sfx/music.mp3");
    this.music.loop = true;
    this.music.preload = "auto";
    this.ctx.createMediaElementSource(this.music).connect(this.master);

    await Promise.all(["coin", "jump", "death"].map(async (name) => {
      try {
        const data = await (await fetch(`assets/sfx/${name}.wav`)).arrayBuffer();
        this.buffers[name] = await new Promise((ok, fail) => this.ctx.decodeAudioData(data, ok, fail));
      } catch (e) { console.warn("Sound unavailable:", name, e); }
    }));
  },

  // Browsers only allow sound after the first click or key press
  unlock() {
    if (!this.ctx || this.ctx.state === "running") return;
    this.ctx.resume();
    if (this.wantMusic) this.music.play().catch(() => {});
  },

  play(name) {
    const buffer = this.buffers[name];
    if (!buffer || this.ctx.state !== "running") return;
    const src = this.ctx.createBufferSource();
    src.buffer = buffer;
    src.connect(this.master);
    src.start();
  },

  // BackgroundMusic.startMusic / resetMusic: safe to call every update
  startMusic() {
    if (this.wantMusic || !this.music) return;
    this.wantMusic = true;
    if (this.ctx.state === "running") this.music.play().catch(() => {});
  },
  stopMusic() {
    if (!this.wantMusic) return;
    this.wantMusic = false;
    this.music.pause();
  },

  setVolume(v) {
    this.volume = Math.max(0, Math.min(1, v));
    if (this.volume > 0) this.volumeBeforeMute = this.volume;
    if (this.master) this.master.gain.value = this.volume;
    try { localStorage.setItem("frogjump.volume", String(this.volume)); } catch (e) {}
  },
  isMuted() { return this.volume <= 0; },
  // If the slider was dragged nearly to zero before muting, unmute to a volume you can hear
  toggleMute() { this.setVolume(this.isMuted() ? Math.max(this.volumeBeforeMute, 0.3) : 0); },
};

// ---------- Loading (utilz.LoadSave) ----------

async function loadImage(path) {
  const blob = await (await fetch("assets/" + path)).blob();
  // Raw pixel values matter for level images, so skip color management
  return createImageBitmap(blob, { premultiplyAlpha: "none", colorSpaceConversion: "none" });
}

async function loadLevels() {
  const levels = [];
  for (let i = 1; ; i++) {
    const res = await fetch(`assets/lvls/${i}.png`);
    if (!res.ok) break;
    const bitmap = await createImageBitmap(await res.blob(), { premultiplyAlpha: "none", colorSpaceConversion: "none" });
    levels.push(new Level(bitmap));
  }
  return levels;
}

// ---------- Level (levels.Level, utilz.HelpMethods) ----------
// Each level is a small image: red = terrain tile, blue = apples (1) and spikes (2),
// green = 100 marks the player's spawn.

class Level {
  constructor(bitmap) {
    const w = bitmap.width, h = bitmap.height;
    const c = document.createElement("canvas");
    c.width = w; c.height = h;
    const cx = c.getContext("2d");
    cx.drawImage(bitmap, 0, 0);
    const px = cx.getImageData(0, 0, w, h).data;

    this.data = [];
    this.apples = [];
    this.spikes = [];
    this.spawn = null;
    for (let j = 0; j < h; j++) {
      const row = [];
      for (let i = 0; i < w; i++) {
        const k = (j * w + i) * 4;
        const r = px[k], g = px[k + 1], b = px[k + 2];
        row.push(r >= 242 ? 0 : r);
        if (g === 100 && !this.spawn) this.spawn = { x: i * TILES_SIZE, y: j * TILES_SIZE };
        if (b === APPLE) this.apples.push(new Apple(i * TILES_SIZE, j * TILES_SIZE));
        if (b === SPIKE) this.spikes.push(new Spike(i * TILES_SIZE, j * TILES_SIZE));
      }
      this.data.push(row);
    }
    if (!this.spawn) this.spawn = { x: TILES_SIZE, y: TILES_SIZE };
    this.maxLvlOffsetY = TILES_SIZE * (h - TILES_IN_HEIGHT);
    this.image = null;
  }

  // The terrain never changes, so draw it once at the atlas's 16px tiles (LevelManager.getLevelImage)
  getImage() {
    if (this.image) return this.image;
    const c = document.createElement("canvas");
    c.width = this.data[0].length * 16;
    c.height = this.data.length * 16;
    const cx = c.getContext("2d");
    for (let j = 0; j < this.data.length; j++)
      for (let i = 0; i < this.data[0].length; i++) {
        const index = this.data[j][i];
        cx.drawImage(img.terrain, (index % 22) * 16, Math.floor(index / 22) * 16, 16, 16, i * 16, j * 16, 16, 16);
      }
    return (this.image = c);
  }
}

function isSolid(x, y, lvlData) {
  const maxHeight = lvlData.length * TILES_SIZE;
  if (y < 0 || y >= maxHeight) return true;
  if (x < 0 || x >= GAME_WIDTH) return true;
  const value = lvlData[truncate(y / TILES_SIZE)][truncate(x / TILES_SIZE)];
  // Only tiles 5 and 49 are empty space
  return value >= 242 || value < 0 || (value !== 5 && value !== 49);
}

function canMoveHere(x, y, width, height, lvlData) {
  return !isSolid(x, y, lvlData) && !isSolid(x + width, y + height, lvlData) &&
    !isSolid(x + width, y, lvlData) && !isSolid(x, y + height, lvlData);
}

function getEntityXPosNextToWall(hitbox, xSpeed) {
  const currentTile = truncate(hitbox.x / TILES_SIZE);
  if (xSpeed > 0) return currentTile * TILES_SIZE + truncate(TILES_SIZE - hitbox.width) - 1;
  return currentTile * TILES_SIZE;
}

function getEntityYPosUnderRoofOrAboveFloor(hitbox, airSpeed) {
  const currentTile = truncate(hitbox.y / TILES_SIZE);
  if (airSpeed > 0) return currentTile * TILES_SIZE + truncate(TILES_SIZE - hitbox.height) - 1;
  return currentTile * TILES_SIZE;
}

function isEntityOnFloor(hitbox, lvlData) {
  return isSolid(hitbox.x, hitbox.y + hitbox.height + 1, lvlData) ||
    isSolid(hitbox.x + hitbox.width, hitbox.y + hitbox.height + 1, lvlData);
}

// ---------- Objects (objects.*) ----------

class Apple {
  constructor(x, y) {
    this.x = x; this.y = y;
    this.hitbox = { x, y, width: 16 * SCALE, height: 16 * SCALE };
    this.xDrawOffset = 10 * SCALE;
    this.yDrawOffset = 9 * SCALE;
    this.maxHoverOffset = 10 * SCALE;
    this.reset();
  }
  reset() {
    this.active = true;
    this.aniTick = 0; this.aniIndex = 0;
    this.hoverOffset = 0; this.hoverDir = 1;
    this.hitbox.y = this.y;
  }
  update() {
    if (++this.aniTick >= ANI_SPEED) {
      this.aniTick = 0;
      if (++this.aniIndex >= 17) this.aniIndex = 0;
    }
    this.hoverOffset += 0.075 * SCALE * this.hoverDir;
    if (this.hoverOffset >= this.maxHoverOffset) this.hoverDir = -1;
    else if (this.hoverOffset < 0) this.hoverDir = 1;
    this.hitbox.y = this.y + this.hoverOffset;
  }
}

class Spike {
  constructor(x, y) {
    this.yDrawOffset = 16 * SCALE;
    this.hitbox = { x, y: y + this.yDrawOffset, width: 30 * SCALE, height: 15 * SCALE };
  }
}

class ObjectManager {
  constructor(playing) {
    this.playing = playing;
    this.apples = [];
    this.spikes = [];
    this.applesLeft = 0;
  }
  loadObjects(level) {
    this.apples = level.apples;
    this.spikes = level.spikes;
    this.applesLeft = this.apples.length;
  }
  checkSpikesTouched(player) {
    for (const s of this.spikes)
      if (intersects(s.hitbox, player.hitbox)) player.kill();
  }
  checkObjectTouched(hitbox) {
    // As in the Java version, the level completes on the next check after the last apple
    let isAnyActive = false;
    for (const a of this.apples) {
      if (a.active) {
        isAnyActive = true;
        if (intersects(hitbox, a.hitbox)) {
          sound.play("coin");
          a.active = false;
          this.applesLeft--;
        }
      }
      this.playing.lvlCompleted = !isAnyActive;
    }
  }
  update() {
    for (const a of this.apples) if (a.active) a.update();
  }
  resetAllObjects() {
    for (const a of this.apples) a.reset();
    this.applesLeft = this.apples.length;
  }
  onScreen(y, off) {
    return y + TILES_SIZE >= off && y - TILES_SIZE <= off + GAME_HEIGHT;
  }
  draw(g, off) {
    for (const a of this.apples)
      if (a.active && this.onScreen(a.hitbox.y, off))
        g.drawImage(img.apple, a.aniIndex * 32, 0, 32, 32,
          truncate(a.hitbox.x - a.xDrawOffset), truncate(a.hitbox.y - a.yDrawOffset - off), APPLE_SIZE, APPLE_SIZE);
    for (const s of this.spikes)
      if (this.onScreen(s.hitbox.y, off))
        g.drawImage(img.spike, truncate(s.hitbox.x), truncate(s.hitbox.y - s.yDrawOffset - off), SPIKE_SIZE, SPIKE_SIZE);
    this.drawAppleCounter(g);
  }
  // Top-left panel showing how many apples are left, right of the mute button
  drawAppleCounter(g) {
    const textSize = 6, text = "x" + this.applesLeft;
    const iconW = 12 * 4, iconH = 14 * 4;
    const frame = 4 * PixelUi.P, pad = 6;
    const panel = {
      x: MuteButton.X + MuteButton.SIZE + 16, y: MuteButton.Y,
      width: frame + pad + iconW + 12 + PixelUi.textWidth(text, textSize) + pad + frame,
      height: frame + pad + iconH + pad + frame,
    };
    PixelUi.drawFrame(g, panel);
    const cx = panel.x + frame + pad, cy = panel.y + frame + pad;
    // The apple itself, without the empty space around it in the animation frame
    g.drawImage(img.apple, 12, 13, 12, 14, cx, cy, iconW, iconH);
    PixelUi.drawText(g, text, cx + iconW + 12, cy + (iconH - 5 * textSize) / 2, textSize);
  }
}

// ---------- Player (entities.Player) ----------

class Player {
  constructor(playing) {
    this.playing = playing;
    this.width = 32 * SCALE; this.height = 32 * SCALE;
    this.xDrawOffset = 7 * SCALE; this.yDrawOffset = 9 * SCALE;
    this.jumpSpeed = -3.25 * SCALE;
    this.fallSpeedAfterCollision = 0.5 * SCALE;
    this.walkSpeed = 1.0 * SCALE;
    this.x = 200; this.y = 200;
    this.hitbox = { x: 200, y: 200, width: 20 * SCALE, height: 23 * SCALE };
    this.state = IDLE; this.aniTick = 0; this.aniIndex = 0;
    this.left = false; this.right = false; this.jump = false;
    this.moving = false; this.inAir = false; this.airSpeed = 0;
    this.flipped = false;
    this.prevX = 200; this.prevY = 200;
  }
  setSpawn(spawn) {
    this.x = spawn.x; this.y = spawn.y;
    this.hitbox.x = this.x; this.hitbox.y = this.y;
    this.savePrev();
  }
  // Remembered so drawing can blend between the last two physics steps
  savePrev() { this.prevX = this.hitbox.x; this.prevY = this.hitbox.y; }
  loadLvlData(lvlData) {
    this.lvlData = lvlData;
    if (!isEntityOnFloor(this.hitbox, lvlData)) this.inAir = true;
  }
  update() {
    this.updatePos();
    if (this.moving) this.playing.objectManager.checkObjectTouched(this.hitbox);
    this.playing.objectManager.checkSpikesTouched(this);
    if (++this.aniTick >= ANI_SPEED) {
      this.aniTick = 0;
      if (++this.aniIndex >= PLAYER_FRAMES[this.state]) this.aniIndex = 0;
    }
    this.setAnimation();
  }
  setAnimation() {
    const start = this.state;
    this.state = this.moving ? RUNNING : IDLE;
    if (this.inAir) this.state = this.airSpeed < 0 ? JUMP : FALLING;
    if (start !== this.state) { this.aniTick = 0; this.aniIndex = 0; }
  }
  updatePos() {
    this.moving = false;
    if (this.jump && !this.inAir) { this.inAir = true; this.airSpeed = this.jumpSpeed; }
    if (!this.inAir && ((!this.left && !this.right) || (this.right && this.left))) return;

    let xSpeed = 0;
    if (this.left) { xSpeed -= this.walkSpeed; this.flipped = true; }
    if (this.right) { xSpeed += this.walkSpeed; this.flipped = false; }

    if (!this.inAir && !isEntityOnFloor(this.hitbox, this.lvlData)) this.inAir = true;

    const h = this.hitbox;
    if (this.inAir) {
      if (canMoveHere(h.x, h.y + this.airSpeed, h.width, h.height, this.lvlData)) {
        h.y += this.airSpeed;
        this.airSpeed += GRAVITY;
      } else {
        h.y = getEntityYPosUnderRoofOrAboveFloor(h, this.airSpeed);
        if (this.airSpeed > 0) { this.inAir = false; this.airSpeed = 0; }
        else this.airSpeed = this.fallSpeedAfterCollision;
      }
    }
    this.updateXPos(xSpeed);
    this.moving = true;
  }
  updateXPos(xSpeed) {
    const h = this.hitbox;
    if (canMoveHere(h.x + xSpeed, h.y, h.width, h.height, this.lvlData)) h.x += xSpeed;
    else h.x = getEntityXPosNextToWall(h, xSpeed);
  }
  render(g, lvlOffset, alpha) {
    const hx = this.prevX + (this.hitbox.x - this.prevX) * alpha;
    const hy = this.prevY + (this.hitbox.y - this.prevY) * alpha;
    const x = Math.round(hx - this.xDrawOffset), y = Math.round(hy - this.yDrawOffset - lvlOffset);
    const sx = this.aniIndex * 32, sy = this.state * 32;
    if (this.flipped) {
      g.save();
      g.translate(x + this.width, y);
      g.scale(-1, 1);
      g.drawImage(img.player, sx, sy, 32, 32, 0, 0, this.width, this.height);
      g.restore();
    } else {
      g.drawImage(img.player, sx, sy, 32, 32, x, y, this.width, this.height);
    }
  }
  resetDirBooleans() { this.left = false; this.right = false; }
  resetAll() {
    this.resetDirBooleans();
    this.inAir = false;
    this.moving = false;
    this.state = IDLE;
    this.hitbox.x = this.x; this.hitbox.y = this.y;
    this.savePrev();
  }
  kill() {
    this.playing.gameOver = true;
    sound.play("death");
  }
}

// ---------- Pixel-style panels (ui.PixelUi, ui.MuteButton, ui.VolumeSlider) ----------

const PixelUi = {
  DARK: "rgb(51,50,61)", GOLD: "rgb(223,185,82)", GOLD_SHADE: "rgb(210,167,50)",
  BRICK: "rgb(152,82,82)", BRICK_DARK: "rgb(110,56,62)", CREAM: "rgb(255,244,214)",
  P: 4,
  SPEAKER: ["...#.....", "..##..#..", "####...#.", "####.#.#.", "####...#.", "..##..#..", "...#....."],
  SPEAKER_MUTED: ["...#.....", "..##.....", "####.#.#.", "####..#..", "####.#.#.", "..##.....", "...#....."],
  GLYPHS: [
    ["###", "#.#", "#.#", "#.#", "###"], [".#.", "##.", ".#.", ".#.", "###"],
    ["###", "..#", "###", "#..", "###"], ["###", "..#", ".##", "..#", "###"],
    ["#.#", "#.#", "###", "..#", "..#"], ["###", "#..", "###", "..#", "###"],
    ["###", "#..", "###", "#.#", "###"], ["###", "..#", "..#", ".#.", ".#."],
    ["###", "#.#", "###", "#.#", "###"], ["###", "#.#", "###", "..#", "###"]],
  GLYPH_X: ["...", "#.#", ".#.", "#.#", "..."],

  fill(g, color, r, inset) {
    g.fillStyle = color;
    g.fillRect(r.x + inset, r.y + inset, r.width - 2 * inset, r.height - 2 * inset);
  },
  // Same layers as the pause menu frame: dark outline, gold border with a shaded bottom, dark inner line, brick fill
  drawFrame(g, r) {
    const P = this.P;
    this.fill(g, this.DARK, r, 0);
    this.fill(g, this.GOLD, r, P);
    g.fillStyle = this.GOLD_SHADE;
    g.fillRect(r.x + P, r.y + r.height - 3 * P, r.width - 2 * P, 2 * P);
    this.fill(g, this.DARK, r, 3 * P);
    this.fill(g, this.BRICK, r, 4 * P);
  },
  drawPixels(g, rows, x, y, size, color) {
    g.fillStyle = color;
    rows.forEach((row, j) => {
      for (let i = 0; i < row.length; i++)
        if (row[i] === "#") g.fillRect(x + i * size, y + j * size, size, size);
    });
  },
  textWidth(text, size) { return text.length * 4 * size - size; },
  drawText(g, text, x, y, size) {
    for (let i = 0; i < text.length; i++) {
      const glyph = text[i] === "x" ? this.GLYPH_X : this.GLYPHS[+text[i]];
      const gx = x + i * 4 * size;
      this.drawPixels(g, glyph, gx + 2, y + 2, size, this.DARK);
      this.drawPixels(g, glyph, gx, y, size, this.CREAM);
    }
  },
};

class MuteButton {
  static SIZE = 100; static X = 24; static Y = 24;
  constructor() { this.bounds = { x: MuteButton.X, y: MuteButton.Y, width: MuteButton.SIZE, height: MuteButton.SIZE }; }
  draw(g) {
    PixelUi.drawFrame(g, this.bounds);
    const px = 6, w = 9 * px, h = 7 * px;
    PixelUi.drawPixels(g, sound.isMuted() ? PixelUi.SPEAKER_MUTED : PixelUi.SPEAKER,
      MuteButton.X + (MuteButton.SIZE - w) / 2, MuteButton.Y + (MuteButton.SIZE - h) / 2, px, PixelUi.DARK);
  }
  mousePressed(e) {
    if (!contains(this.bounds, e.x, e.y)) return false;
    sound.toggleMute();
    return true;
  }
}

class VolumeSlider {
  constructor(centerX, y) {
    const P = PixelUi.P;
    this.panel = { x: centerX - 50 * P, y, width: 100 * P, height: 18 * P };
    this.speaker = { x: this.panel.x + 7 * P, y: this.panel.y + 5 * P, width: 9 * P, height: 7 * P };
    this.track = { x: this.panel.x + 21 * P, y: this.panel.y + 7 * P, width: 72 * P, height: 4 * P };
    this.hitArea = { x: this.track.x - 3 * P, y: this.panel.y, width: this.track.width + 6 * P, height: this.panel.height };
    this.dragging = false;
  }
  draw(g) {
    const P = PixelUi.P, t = this.track;
    PixelUi.drawFrame(g, this.panel);
    PixelUi.drawPixels(g, sound.isMuted() ? PixelUi.SPEAKER_MUTED : PixelUi.SPEAKER, this.speaker.x, this.speaker.y, P, PixelUi.DARK);
    // Track: dark outline, gold up to the current volume, dark brick after it
    PixelUi.fill(g, PixelUi.DARK, t, -P);
    g.fillStyle = PixelUi.BRICK_DARK;
    g.fillRect(t.x, t.y, t.width, t.height);
    const filled = Math.round(t.width * sound.volume);
    g.fillStyle = PixelUi.GOLD;
    g.fillRect(t.x, t.y, filled, t.height);
    // Knob
    const knob = { x: t.x + filled - 2 * P, y: t.y - 3 * P, width: 4 * P, height: t.height + 6 * P };
    PixelUi.fill(g, PixelUi.DARK, knob, 0);
    PixelUi.fill(g, PixelUi.GOLD, knob, P);
    g.fillStyle = PixelUi.GOLD_SHADE;
    g.fillRect(knob.x + P, knob.y + knob.height - 2 * P, 2 * P, P);
  }
  mousePressed(e) {
    if (contains(this.hitArea, e.x, e.y)) { this.dragging = true; this.setFromMouse(e.x); return true; }
    if (contains(this.speaker, e.x, e.y)) { sound.toggleMute(); return true; }
    return false;
  }
  mouseDragged(e) { if (this.dragging) this.setFromMouse(e.x); }
  mouseReleased() { const was = this.dragging; this.dragging = false; return was; }
  setFromMouse(x) { sound.setVolume((x - this.track.x) / this.track.width); }
}

// ---------- Buttons (ui.MenuButton, ui.UrmButton) ----------

class Button {
  constructor(bounds) { this.bounds = bounds; this.mouseOver = false; this.mousePressed = false; }
  get index() { return this.mousePressed ? 2 : this.mouseOver ? 1 : 0; }
  resetBools() { this.mouseOver = false; this.mousePressed = false; }
}

class MenuButton extends Button {
  constructor(xCenter, y, rowIndex, state) {
    super({ x: xCenter - B_WIDTH / 2, y, width: B_WIDTH, height: B_HEIGHT });
    this.rowIndex = rowIndex; this.state = state;
  }
  draw(g) {
    g.drawImage(img.menuButtons, this.index * B_WIDTH_DEFAULT, this.rowIndex * B_HEIGHT_DEFAULT, B_WIDTH_DEFAULT, B_HEIGHT_DEFAULT,
      this.bounds.x, this.bounds.y, B_WIDTH, B_HEIGHT);
  }
}

// Row 0 = play / next, 1 = replay, 2 = home
class UrmButton extends Button {
  constructor(x, y, rowIndex) { super({ x, y, width: URM_SIZE, height: URM_SIZE }); this.rowIndex = rowIndex; }
  draw(g) {
    g.drawImage(img.urm, this.index * URM_DEFAULT_SIZE, this.rowIndex * URM_DEFAULT_SIZE, URM_DEFAULT_SIZE, URM_DEFAULT_SIZE,
      this.bounds.x, this.bounds.y, URM_SIZE, URM_SIZE);
  }
}

// Shared mouse handling for a set of buttons: press, release to act, hover highlight
function pressButtons(buttons, e) {
  for (const b of buttons) if (contains(b.bounds, e.x, e.y)) { b.mousePressed = true; return; }
}
function releaseButtons(buttons, e) {
  let hit = null;
  for (const b of buttons) if (contains(b.bounds, e.x, e.y) && b.mousePressed) hit = b;
  buttons.forEach((b) => b.resetBools());
  return hit;
}
function hoverButtons(buttons, e) {
  buttons.forEach((b) => (b.mouseOver = false));
  for (const b of buttons) if (contains(b.bounds, e.x, e.y)) { b.mouseOver = true; return; }
}

// ---------- Overlays (ui.PauseOverlay, ui.GameOverOverlay, ui.LevelCompletedOverlay) ----------

class PauseOverlay {
  constructor(playing) {
    this.playing = playing;
    this.bgW = img.pauseBg.width * SCALE; this.bgH = img.pauseBg.height * SCALE;
    this.bgX = GAME_WIDTH / 2 - this.bgW / 2; this.bgY = 50 * SCALE;
    const y = 175 * SCALE;
    this.menuB = new UrmButton(313 * SCALE, y, 2);
    this.replayB = new UrmButton(387 * SCALE, y, 1);
    this.unpauseB = new UrmButton(462 * SCALE, y, 0);
    this.buttons = [this.menuB, this.replayB, this.unpauseB];
    // The pause menu image leaves room under its frame (which ends 222px down the image) for this
    this.volumeSlider = new VolumeSlider(GAME_WIDTH / 2, this.bgY + 228 * SCALE);
  }
  draw(g) {
    g.drawImage(img.pauseBg, this.bgX, this.bgY, this.bgW, this.bgH);
    this.buttons.forEach((b) => b.draw(g));
    this.volumeSlider.draw(g);
  }
  mousePressed(e) { if (!this.volumeSlider.mousePressed(e)) pressButtons(this.buttons, e); }
  mouseDragged(e) { this.volumeSlider.mouseDragged(e); }
  mouseReleased(e) {
    if (this.volumeSlider.mouseReleased()) return;
    const b = releaseButtons(this.buttons, e);
    if (b === this.menuB) { gamestate = MENU; this.playing.paused = false; }
    else if (b === this.replayB) { this.playing.resetAll(); this.playing.paused = false; }
    else if (b === this.unpauseB) this.playing.paused = false;
  }
  mouseMoved(e) { hoverButtons(this.buttons, e); }
}

// Game over and level complete share a layout: a darkened screen, an image and two buttons
class EndOverlay {
  constructor(playing, image, imgY, onNext) {
    this.playing = playing;
    this.image = image;
    this.w = image.width * SCALE; this.h = image.height * SCALE;
    this.x = GAME_WIDTH / 2 - this.w / 2; this.y = imgY;
    const y = 195 * SCALE;
    this.menu = new UrmButton(330 * SCALE, y, 2);
    this.next = new UrmButton(445 * SCALE, y, 0);
    this.buttons = [this.menu, this.next];
    this.onNext = onNext;
  }
  draw(g) {
    g.fillStyle = "rgba(0,0,0,0.784)";
    g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
    g.drawImage(this.image, this.x, this.y, this.w, this.h);
    this.buttons.forEach((b) => b.draw(g));
  }
  mousePressed(e) { pressButtons(this.buttons, e); }
  mouseReleased(e) {
    const b = releaseButtons(this.buttons, e);
    if (b === this.menu) { this.playing.resetAll(); gamestate = MENU; }
    else if (b === this.next) this.onNext();
  }
  mouseMoved(e) { hoverButtons(this.buttons, e); }
}

// ---------- Playing (gamestates.Playing, levels.LevelManager) ----------

class Playing {
  constructor(levels) {
    this.levels = levels;
    this.lvlIndex = 0;
    this.objectManager = new ObjectManager(this);
    this.player = new Player(this);
    this.player.loadLvlData(this.level.data);
    this.player.setSpawn(this.level.spawn);
    this.objectManager.loadObjects(this.level);
    this.maxLvlOffsetY = this.level.maxLvlOffsetY;
    this.yLvlOffset = 0; this.prevLvlOffset = 0;
    this.topBorder = truncate(0.3 * GAME_HEIGHT);
    this.bottomBorder = truncate(0.6 * GAME_HEIGHT);
    this.paused = false; this.gameOver = false; this.lvlCompleted = false;
    this.pauseOverlay = new PauseOverlay(this);
    this.gameOverOverlay = new EndOverlay(this, img.death, 100 * SCALE, () => this.resetAll());
    this.levelCompletedOverlay = new EndOverlay(this, img.completed, 75 * SCALE, () => this.loadNextLevel());
    this.muteButton = new MuteButton();
  }
  get level() { return this.levels[this.lvlIndex]; }

  loadNextLevel() {
    this.resetAll();
    if (++this.lvlIndex >= this.levels.length) {
      this.lvlIndex = 0;
      gamestate = MENU; // all levels done
    }
    this.player.loadLvlData(this.level.data);
    this.maxLvlOffsetY = this.level.maxLvlOffsetY;
    this.objectManager.loadObjects(this.level);
    this.player.setSpawn(this.level.spawn);
  }
  resetAll() {
    this.gameOver = false; this.paused = false; this.lvlCompleted = false;
    this.player.resetAll();
    this.objectManager.resetAllObjects();
  }

  update() {
    this.player.savePrev();
    this.prevLvlOffset = this.yLvlOffset;
    if (this.paused || this.lvlCompleted) return;
    if (!this.gameOver) {
      this.objectManager.update();
      this.player.update();
      sound.startMusic();
      this.checkCloseToBorder();
    } else {
      sound.stopMusic();
    }
  }
  checkCloseToBorder() {
    const diff = truncate(this.player.hitbox.y) - this.yLvlOffset;
    if (diff > this.bottomBorder) this.yLvlOffset += diff - this.bottomBorder;
    else if (diff < this.topBorder) this.yLvlOffset += diff - this.topBorder;
    this.yLvlOffset = Math.max(0, Math.min(this.maxLvlOffsetY, this.yLvlOffset));
  }

  draw(g, alpha) {
    g.drawImage(img.forest, 0, 0, GAME_WIDTH, GAME_HEIGHT);
    const off = Math.round(this.prevLvlOffset + (this.yLvlOffset - this.prevLvlOffset) * alpha);
    this.drawLevel(g, off);
    this.player.render(g, off, alpha);
    this.objectManager.draw(g, off);
    this.muteButton.draw(g);
    if (this.paused) {
      g.fillStyle = "rgba(0,0,0,0.588)";
      g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
      this.pauseOverlay.draw(g);
    } else if (this.gameOver) this.gameOverOverlay.draw(g);
    else if (this.lvlCompleted) this.levelCompletedOverlay.draw(g);
  }
  // Draws the visible slice of the pre-rendered terrain (LevelManager.draw)
  drawLevel(g, off) {
    const image = this.level.getImage(), tileScale = TILES_SIZE / 16;
    const srcY = Math.floor(off / tileScale), shift = off - srcY * tileScale;
    const srcH = Math.min(image.height - srcY, GAME_HEIGHT / tileScale + 1);
    g.drawImage(image, 0, srcY, image.width, srcH, 0, -shift, GAME_WIDTH, srcH * tileScale);
  }

  keyPressed(e) {
    if (e.code === "KeyM") { sound.toggleMute(); return; }
    if (this.gameOver) {
      if (e.code === "Escape") { this.resetAll(); gamestate = MENU; }
      return;
    }
    switch (e.code) {
      case "KeyA": this.player.left = true; break;
      case "KeyD": this.player.right = true; break;
      case "Space": case "KeyW":
        this.player.jump = true;
        if (!this.player.inAir) sound.play("jump");
        break;
      case "Escape": this.paused = true; break;
    }
  }
  keyReleased(e) {
    if (this.gameOver) return;
    switch (e.code) {
      case "KeyA": this.player.left = false; break;
      case "KeyD": this.player.right = false; break;
      case "Space": case "KeyW": this.player.jump = false; break;
    }
  }
  mousePressed(e) {
    if (this.gameOver) this.gameOverOverlay.mousePressed(e);
    else if (this.paused) this.pauseOverlay.mousePressed(e);
    else if (this.lvlCompleted) this.levelCompletedOverlay.mousePressed(e);
    else this.muteButton.mousePressed(e);
  }
  mouseReleased(e) {
    if (this.gameOver) this.gameOverOverlay.mouseReleased(e);
    else if (this.paused) this.pauseOverlay.mouseReleased(e);
    else if (this.lvlCompleted) this.levelCompletedOverlay.mouseReleased(e);
  }
  mouseMoved(e) {
    if (this.gameOver) this.gameOverOverlay.mouseMoved(e);
    else if (this.paused) this.pauseOverlay.mouseMoved(e);
    else if (this.lvlCompleted) this.levelCompletedOverlay.mouseMoved(e);
  }
  mouseDragged(e) { if (!this.gameOver && this.paused) this.pauseOverlay.mouseDragged(e); }
}

// ---------- Menu (gamestates.Menu) ----------

class Menu {
  constructor() {
    this.bgW = img.menuBg.width * SCALE; this.bgH = img.menuBg.height * SCALE;
    this.bgX = GAME_WIDTH / 2 - this.bgW / 2; this.bgY = 70 * SCALE;
    this.buttons = [
      new MenuButton(GAME_WIDTH / 2, 225 * SCALE, 0, PLAYING),
      new MenuButton(GAME_WIDTH / 2, 305 * SCALE, 1, QUIT),
    ];
    this.muteButton = new MuteButton();
    sound.startMusic();
  }
  draw(g) {
    g.drawImage(img.forest, 0, 0, GAME_WIDTH, GAME_HEIGHT);
    g.drawImage(img.menuBg, this.bgX, this.bgY, this.bgW, this.bgH);
    this.buttons.forEach((b) => b.draw(g));
    this.muteButton.draw(g);
  }
  mousePressed(e) { if (!this.muteButton.mousePressed(e)) pressButtons(this.buttons, e); }
  mouseReleased(e) { const b = releaseButtons(this.buttons, e); if (b) gamestate = b.state; }
  mouseMoved(e) { hoverButtons(this.buttons, e); }
  keyPressed(e) {
    if (e.code === "Enter") gamestate = PLAYING;
    else if (e.code === "KeyM") sound.toggleMute();
  }
}

// ---------- Game loop, input, scaling (main.Game, main.GamePanel, inputs.*) ----------

async function main() {
  const canvas = document.getElementById("game");
  const g = canvas.getContext("2d", { alpha: false });

  const names = {
    player: "frog.png", terrain: "terrain.png", menuButtons: "menu_buttons.png", menuBg: "menu_background.png",
    pauseBg: "pause_menu.png", urm: "urm_buttons.png", completed: "completed.png", apple: "apple.png",
    spike: "spike.png", death: "death_screen.png", forest: "forest_bg.png",
  };
  const [, levels] = await Promise.all([
    Promise.all(Object.entries(names).map(async ([key, file]) => (img[key] = await loadImage(file)))),
    loadLevels(),
    sound.init(),
  ]);

  const menu = new Menu();
  const playing = new Playing(levels);

  // The canvas covers the window at device resolution; the game is stretched to fill it
  function resize() {
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(window.innerWidth * dpr);
    canvas.height = Math.round(window.innerHeight * dpr);
  }
  window.addEventListener("resize", resize);
  resize();

  // ---- Input ----
  const KEYS = ["KeyA", "KeyD", "KeyW", "Space", "Escape", "Enter", "KeyM"];
  window.addEventListener("keydown", (e) => {
    sound.unlock();
    if (!KEYS.includes(e.code)) return;
    e.preventDefault();
    if (gamestate === MENU) menu.keyPressed(e);
    else if (gamestate === PLAYING) playing.keyPressed(e);
  });
  window.addEventListener("keyup", (e) => {
    if (gamestate === PLAYING) playing.keyReleased(e);
  });
  window.addEventListener("blur", () => playing.player.resetDirBooleans());

  function toGame(e) {
    const r = canvas.getBoundingClientRect();
    return { x: (e.clientX - r.left) * GAME_WIDTH / r.width, y: (e.clientY - r.top) * GAME_HEIGHT / r.height };
  }
  canvas.addEventListener("pointerdown", (e) => {
    sound.unlock();
    canvas.setPointerCapture(e.pointerId);
    const p = toGame(e);
    if (gamestate === MENU) menu.mousePressed(p);
    else if (gamestate === PLAYING) playing.mousePressed(p);
    else if (gamestate === QUIT) { gamestate = MENU; document.getElementById("quit").hidden = true; }
  });
  canvas.addEventListener("pointerup", (e) => {
    const p = toGame(e);
    if (gamestate === MENU) menu.mouseReleased(p);
    else if (gamestate === PLAYING) playing.mouseReleased(p);
  });
  canvas.addEventListener("pointermove", (e) => {
    const p = toGame(e);
    if (e.buttons) { if (gamestate === PLAYING) playing.mouseDragged(p); }
    else if (gamestate === MENU) menu.mouseMoved(p);
    else if (gamestate === PLAYING) playing.mouseMoved(p);
  });

  // ---- Loop: fixed 200 physics steps per second, drawn once per screen refresh ----
  const step = 1000 / UPS;
  let last = performance.now(), acc = 0, shownState = null;
  function frame(now) {
    acc += Math.min(now - last, 250);
    last = now;
    let ran = 0;
    while (acc >= step && ran < 50) {
      if (gamestate === PLAYING) playing.update();
      acc -= step;
      ran++;
    }
    if (acc >= step) acc = 0; // far behind (tab was hidden): skip ahead

    if (gamestate !== shownState) {
      // Quit can't close a browser tab, so it shows a "thanks" screen instead
      document.getElementById("quit").hidden = gamestate !== QUIT;
      if (gamestate === QUIT) sound.stopMusic();
      shownState = gamestate;
    }

    g.setTransform(canvas.width / GAME_WIDTH, 0, 0, canvas.height / GAME_HEIGHT, 0, 0);
    g.imageSmoothingEnabled = false;
    if (gamestate === MENU) menu.draw(g);
    else if (gamestate === PLAYING) playing.draw(g, acc / step);
    else { g.fillStyle = "#000"; g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT); }
    requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);
  document.getElementById("loading").remove();
  document.getElementById("hint").hidden = false;
}

main().catch((e) => {
  console.error(e);
  document.getElementById("loading").textContent = "Couldn't load the game. Try refreshing the page.";
});
