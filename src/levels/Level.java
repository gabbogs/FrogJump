package levels;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import main.Game;
import objects.Apple;
import objects.Spike;
import utilz.HelpMethods;

import static utilz.HelpMethods.GetLevelData;
import static utilz.HelpMethods.GetPlayerSpawn;

public class Level {

	private BufferedImage img;
	private int[][] lvlData;
	private ArrayList<Apple> apples;
	private ArrayList<Spike> spikes;

	private int lvlTilesHigh;
	private int maxTilesOffset;
	private int maxLvlOffsetY;
	private Point playerSpawn;
	

	public Level(BufferedImage img) {
		this.img = img;
		createLevelData();
		createPotions();	
		createSpikes();
		calcLvlOffsets();
		calcPlayerSpawn();
	}

	
	private void createSpikes() {
		spikes = HelpMethods.GetSpikes(img);
		
	}


	private void createPotions() {
		apples = HelpMethods.GetApples(img);
	}

	private void calcPlayerSpawn() {
		playerSpawn = GetPlayerSpawn(img);
	}

	private void calcLvlOffsets() {
		lvlTilesHigh = img.getHeight();
		maxTilesOffset = lvlTilesHigh - Game.TILES_IN_HEIGHT;
		maxLvlOffsetY = Game.TILES_SIZE * maxTilesOffset;
	}

	private void createLevelData() {
		lvlData = GetLevelData(img);
	}

	public int getSpriteIndex(int x, int y) {
		return lvlData[y][x];
	}

	public int[][] getLevelData() {
		return lvlData;
	}

	public int getLvlOffset() {
		return maxLvlOffsetY;
	}



	public Point getPlayerSpawn() {
		return playerSpawn;
	}

	public ArrayList<Apple> getApples() {
		return apples;
	}
	public ArrayList<Spike> getSpikes() {
		return spikes;
	}

	
}