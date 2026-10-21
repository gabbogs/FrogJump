package gamestates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLabel;

import Sounds.JumpSound;
import entities.Player;
import levels.LevelManager;
import main.Game;
import objects.ObjectManager;
import ui.GameOverOverlay;
import ui.LevelCompletedOverlay;
import ui.PauseOverlay;
import utilz.LoadSave;

public class Playing extends State implements Statemethods {
	private Player player;
	private LevelManager levelManager;

	private ObjectManager objectManager;
	private PauseOverlay pauseOverlay;
	private GameOverOverlay gameOverOverlay;
	private LevelCompletedOverlay levelCompletedOverlay;
	private boolean paused;

	private int yLvlOffset;
	private int topBorder = (int) (0.3 * Game.GAME_HEIGHT);
	private int bottomBorder = (int) (0.6 * Game.GAME_HEIGHT);
	private int maxLvlOffsetY;

	private BufferedImage backgroundImg;
	private JumpSound sound;

	private boolean gameOver;
	private boolean lvlCompleted;
	

	public Playing(Game game) {
		super(game);
		initClasses();
		backgroundImg = LoadSave.GetSpriteAtlas(LoadSave.FOREST_BG);
		calcLvlOffset();
		loadStartLevel();
	
	}
	public void loadNextLevel() {
		resetAll();
		levelManager.loadNextLevel();
		player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());
	}
	
	private void loadStartLevel() {
		objectManager.loadObjects(levelManager.getCurrentLevel());
	}
	
	private void calcLvlOffset() {
		maxLvlOffsetY = levelManager.getCurrentLevel().getLvlOffset();
	}
	
	private void initClasses() {
		levelManager = new LevelManager(game);
		objectManager = new ObjectManager(this);
		sound = new JumpSound();

		player = new Player(200, 200, (int) (32 * Game.SCALE), (int) (32 * Game.SCALE), this);
		player.loadLvlData(levelManager.getCurrentLevel().getLevelData());
		player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());

		pauseOverlay = new PauseOverlay(this);
		gameOverOverlay = new GameOverOverlay(this);
		levelCompletedOverlay = new LevelCompletedOverlay(this);
	}
	@Override
	
	public void update() {
		if (paused) {
			pauseOverlay.update();
		} else if (lvlCompleted) {
			levelCompletedOverlay.update();
		} else if (!gameOver) {
			levelManager.update();
			objectManager.update();
			player.update();
			checkCloseToBorder();
		} else if(gameOver) {
			gameOverOverlay.update();
		}
	}
	private void checkCloseToBorder() {
		int playerY = (int) player.getHitbox().y;
        int diff = playerY - yLvlOffset;

        if (diff > bottomBorder)
            yLvlOffset += diff - bottomBorder;
        else if (diff < topBorder)
            yLvlOffset += diff - topBorder;

        if (yLvlOffset > maxLvlOffsetY)
            yLvlOffset = maxLvlOffsetY;
        else if (yLvlOffset < 0)
            yLvlOffset = 0;
	}
	@Override
	public void draw(Graphics g) {
		g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);

		levelManager.draw(g, yLvlOffset);
		player.render(g, yLvlOffset);
		
		objectManager.draw(g, yLvlOffset);

		if (paused) {
			g.setColor(new Color(0, 0, 0, 150));
			g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
			pauseOverlay.draw(g);
		} else if (gameOver)
			gameOverOverlay.draw(g);
		else if (lvlCompleted)
			levelCompletedOverlay.draw(g);
	}
	public void resetAll() {
		gameOver = false;
		paused = false;
		lvlCompleted = false;
		player.resetAll();
		objectManager.resetAllObjects();
	}
	public void setGameOver(boolean gameOver) {
		this.gameOver = gameOver;
	}
	public void checkAppleTouched(Rectangle2D.Float hitbox) {
		objectManager.checkObjectTouched(hitbox);
	}
	public void checkSpikesTouched(Player p) {
		objectManager.checkSpikesTouched(p);	
	}
	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (gameOver)
			gameOverOverlay.keyPressed(e);
		else
			switch (e.getKeyCode()) {
		
			case KeyEvent.VK_A:
				player.setLeft(true);
				break;
			case KeyEvent.VK_D:
				player.setRight(true);
				break;
			case KeyEvent.VK_SPACE:
				jumpingWithSound();
				break;
			case KeyEvent.VK_W:
				jumpingWithSound();
				break;
			case KeyEvent.VK_ESCAPE:
				if(paused==false) {
				paused = true;
				}
				break;
			}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (!gameOver)
			switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
				player.setLeft(false);
				break;
			case KeyEvent.VK_D:
				player.setRight(false);
				break;
			case KeyEvent.VK_SPACE:
				player.setJump(false);
				break;
			case KeyEvent.VK_W:
				player.setJump(false);
			}
	}
	public void mouseDragged(MouseEvent e) {
		if (!gameOver)
			if (paused)
				pauseOverlay.mouseDragged(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (!gameOver) {
			if (paused) {
				pauseOverlay.mousePressed(e);
				}
			else if (lvlCompleted)
				levelCompletedOverlay.mousePressed(e);
		}else {
			gameOverOverlay.mousePressed(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				pauseOverlay.mouseReleased(e);
			else if (lvlCompleted)
				levelCompletedOverlay.mouseReleased(e);
		}else
			gameOverOverlay.mouseReleased(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				pauseOverlay.mouseMoved(e);
			else if (lvlCompleted)
				levelCompletedOverlay.mouseMoved(e);
		}else
			gameOverOverlay.mouseMoved(e);
	}
	
	public void jumpingWithSound() {
		player.setJump(true);
		if(player.isJump() && !player.isinAir())
			sound.startSound();
	}
	public void setLevelCompleted(boolean levelCompleted) {
		this.lvlCompleted = levelCompleted;
	}

	public void setMaxLvlOffset(int lvlOffset) {
		this.maxLvlOffsetY = lvlOffset;
	}

	public void unpauseGame() {
		paused = false;
	}

	public void windowFocusLost() {
		player.resetDirBooleans();
	}

	public Player getPlayer() {
		return player;
	}
	public LevelManager getLevelManager() {
		return levelManager;
	}

	public ObjectManager getObjectManager() {
		return objectManager;
	}
	public boolean isGameOver() {
		return gameOver;
	}
	

}