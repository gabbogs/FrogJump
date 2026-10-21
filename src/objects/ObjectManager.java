package objects;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import Sounds.CoinCollectSound;
import Sounds.DeathSound;
import entities.Player;
import gamestates.Playing;
import levels.Level;
import main.Game;
import ui.MuteButton;
import ui.PixelUi;
import utilz.LoadSave;
import static utilz.Constants.ObjectConstants.*;

public class ObjectManager {

	private Playing playing;
	private BufferedImage[][] appleImgs;
	private BufferedImage spikeImg;
	private BufferedImage appleIcon;

	private ArrayList<Apple> apples;
	private ArrayList<Spike> spikes;

	private CoinCollectSound coinSound;
	private int applesLeft;

	public ObjectManager(Playing playing) {
		this.playing = playing;
		coinSound = new CoinCollectSound();
		loadImgs();
	}
	public void checkSpikesTouched(Player p) {
		for(Spike s: spikes)
			if(s.getHitbox().intersects(p.getHitbox()))
				p.kill();
	}
	public void checkObjectTouched(Rectangle2D.Float hitbox) {
		boolean isAnyActive = false;
		for (Apple a : apples) {
			if (a.isActive()) {
				isAnyActive = true;
				if (hitbox.intersects(a.getHitbox())) {
					coinSound.startSound();	
					a.setActive(false);
					applesLeft--;
				}
			}
		if(!isAnyActive) {
			playing.setLevelCompleted(true);
		}else {
			playing.setLevelCompleted(false);;
		}
		}
	}
	public void loadObjects(Level newLevel) {
		apples = new ArrayList<>(newLevel.getApples());
		spikes = newLevel.getSpikes();
		applesLeft = apples.size();
	}

	private void loadImgs() {
		BufferedImage potionSprite = LoadSave.GetSpriteAtlas(LoadSave.APPLE_ATLAS);
		appleImgs = new BufferedImage[1][17];

		for (int j = 0; j < appleImgs.length; j++)
			for (int i = 0; i < appleImgs[j].length; i++)
				appleImgs[j][i] = potionSprite.getSubimage(32 * i, 32 * j, 32, 32);

		spikeImg = LoadSave.GetSpriteAtlas(LoadSave.TRAP_ATLAS);

		// The apple itself, without the empty space around it in the animation frame
		appleIcon = appleImgs[0][0].getSubimage(12, 13, 12, 14);
	}

	public void update() {
		for (Apple a : apples)
			if (a.isActive())
				a.update();
	}

	public void draw(Graphics g, int yLvlOffset) {
		drawApples(g, yLvlOffset);
		drawTraps(g, yLvlOffset);
		drawAppleCounter(g);
	}

	// Top-left panel showing how many apples are left, styled like the pause menu
	private void drawAppleCounter(Graphics g) {
		int textSize = 6;
		String text = "x" + applesLeft;
		// 4x keeps the apple's pixels whole at the browser's half resolution
		int iconW = appleIcon.getWidth() * 4, iconH = appleIcon.getHeight() * 4;
		int frame = 4 * PixelUi.P, pad = 6;
		// Right of the mute button (see ui.MuteButton)
		Rectangle panel = new Rectangle(MuteButton.X + MuteButton.SIZE + 16, MuteButton.Y,
				frame + pad + iconW + 12 + PixelUi.textWidth(text, textSize) + pad + frame,
				frame + pad + iconH + pad + frame);
		PixelUi.drawFrame(g, panel);

		int contentX = panel.x + frame + pad, contentY = panel.y + frame + pad;
		g.drawImage(appleIcon, contentX, contentY, iconW, iconH, null);
		PixelUi.drawText(g, text, contentX + iconW + 12, contentY + (iconH - 5 * textSize) / 2, textSize);
	}


	// True when something drawn at game-y `y` (one tile tall) would be visible on screen
	private boolean onScreen(float y, int yLvlOffset) {
		return y + Game.TILES_SIZE >= yLvlOffset && y - Game.TILES_SIZE <= yLvlOffset + Game.GAME_HEIGHT;
	}

	private void drawTraps(Graphics g, int yLvlOffset) {
		for(Spike s: spikes) {
			if (!onScreen(s.getHitbox().y, yLvlOffset))
				continue;
			g.drawImage(spikeImg,
					(int)(s.getHitbox().x - s.getxDrawOffset()),
					(int)(s.getHitbox().y - s.getyDrawOffset() - yLvlOffset), 
					SPIKE_WIDTH, SPIKE_HEIGHT, 
					null);
//			s.drawHitbox(g, yLvlOffset);
		}
	}
	

	private void drawApples(Graphics g, int yLvlOffset) {
		for (Apple a : apples) {
			if (a.isActive() && onScreen(a.getHitbox().y, yLvlOffset)) {
				int type = 0;
				g.drawImage(appleImgs[type][a.getAniIndex()], 
						(int) (a.getHitbox().x - a.getxDrawOffset() ),
						(int) (a.getHitbox().y - a.getyDrawOffset() - yLvlOffset),
						APPLE_WIDTH, APPLE_HEIGHT,
						 null);
			}
//			a.drawHitbox(g, yLvlOffset);
		}	
	}

	public void resetAllObjects() {
		//loadObjects(playing.getLevelManager().getCurrentLevel());
		for (Apple a: apples)
			a.reset();
		applesLeft = apples.size();
	}

}