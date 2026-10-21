package levels;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import gamestates.Gamestate;
import main.Game;
import utilz.LoadSave;

public class LevelManager {

	private Game game;
	private BufferedImage[] levelSprite;
	private ArrayList<Level> levels;
	private int lvlIndex = 0;

	public LevelManager(Game game) {
		this.game = game;
		importOutsideSprites();
		levels = new ArrayList<>();
		buildAllLevels();
	}

	public void loadNextLevel() {
		lvlIndex++;
		if (lvlIndex >= levels.size()) {
			lvlIndex = 0;
			System.out.println("No more levels! Game Completed!");
			Gamestate.state = Gamestate.MENU;
		}

		Level newLevel = levels.get(lvlIndex);
		game.getPlaying().getPlayer().loadLvlData(newLevel.getLevelData());
		game.getPlaying().setMaxLvlOffset(newLevel.getLvlOffset());
		game.getPlaying().getObjectManager().loadObjects(newLevel);
	}

	private void buildAllLevels() {
		BufferedImage[] allLevels = LoadSave.GetAllLevels();
		for (BufferedImage img : allLevels)
			levels.add(new Level(img));
	}

	private void importOutsideSprites() {
		BufferedImage img = LoadSave.GetSpriteAtlas(LoadSave.LEVEL_ATLAS);
		levelSprite = new BufferedImage[242];
		for (int j = 0; j < 11; j++)
			for (int i = 0; i < 22; i++) {
				int index = j * 22 + i;
				levelSprite[index] = img.getSubimage(i * 16, j * 16, 16, 16);
			}
	}

	// Size of one tile in the source atlas; the game draws tiles TILE_SCALE times larger
	private static final int ATLAS_TILE = 16;
	private static final int TILE_SCALE = Game.TILES_SIZE / ATLAS_TILE;
	private final Map<Level, BufferedImage> levelImages = new HashMap<>();

	// The terrain never changes, so each level is drawn once into an image at atlas resolution
	// and every frame draws only the visible slice of it (one draw call instead of hundreds).
	private BufferedImage getLevelImage(Level level) {
		BufferedImage img = levelImages.get(level);
		if (img != null)
			return img;
		int[][] data = level.getLevelData();
		img = new BufferedImage(data[0].length * ATLAS_TILE, data.length * ATLAS_TILE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		for (int j = 0; j < data.length; j++)
			for (int i = 0; i < data[0].length; i++)
				g.drawImage(levelSprite[level.getSpriteIndex(i, j)], i * ATLAS_TILE, j * ATLAS_TILE, null);
		g.dispose();
		levelImages.put(level, img);
		return img;
	}

	public void draw(Graphics g, int lvlOffset) {
		BufferedImage img = getLevelImage(levels.get(lvlIndex));
		// Source rows start at lvlOffset / TILE_SCALE; shifting by the remainder keeps scrolling pixel-smooth
		int srcY = lvlOffset / TILE_SCALE;
		int shift = lvlOffset % TILE_SCALE;
		int srcH = Math.min(img.getHeight() - srcY, Game.GAME_HEIGHT / TILE_SCALE + 1);
		g.drawImage(img,
				0, -shift, Game.GAME_WIDTH, -shift + srcH * TILE_SCALE,
				0, srcY, img.getWidth(), srcY + srcH,
				null);
	}

	public void update() {

	}

	public Level getCurrentLevel() {
		return levels.get(lvlIndex);
	}

	public int getAmountOfLevels() {
		return levels.size();
	}

}