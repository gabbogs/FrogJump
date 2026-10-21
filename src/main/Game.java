package main;

import java.awt.Graphics;
import gamestates.Gamestate;
import gamestates.Menu;
import gamestates.Playing;
import utilz.LoadSave;

public class Game implements Runnable {

	private GameWindow gameWindow;
	private GamePanel gamePanel;
	private Thread gameThread;
	private final int FPS_SET;
	private final int UPS_SET = 200;
	// Most updates to run in one loop pass when catching up after a slow frame
	private static final int MAX_CATCH_UP = 10;
	private final boolean showPerf;

	private Playing playing;
	private Menu menu;

	public final static int TILES_DEFAULT_SIZE = 32;
	public final static float SCALE = 2f;
	public final static int TILES_IN_WIDTH = 26;
	public final static int TILES_IN_HEIGHT = 14;
	public final static int TILES_SIZE = (int) (TILES_DEFAULT_SIZE * SCALE);
	public final static int GAME_WIDTH = TILES_SIZE * TILES_IN_WIDTH;
	public final static int GAME_HEIGHT = TILES_SIZE * TILES_IN_HEIGHT;

	public Game(boolean web, boolean showPerf) {
		// Browsers refresh at 60 Hz, so drawing more often there only costs time
		FPS_SET = web ? 60 : 120;
		this.showPerf = showPerf;
		initClasses();
		gamePanel = new GamePanel(this, web);
		gameWindow = new GameWindow(gamePanel, web);
		gamePanel.setFocusable(true);
		gamePanel.requestFocus();
		startGameLoop();
	}

	private void initClasses() {
		menu = new Menu(this);
		playing = new Playing(this);
	}

	private void startGameLoop() {
		gameThread = new Thread(this);
		gameThread.start();
	}

	public void update() {
		switch (Gamestate.state) {
		case MENU:
			menu.update();
			break;
		case PLAYING:
			playing.update();
			break;
		case QUIT:
		default:
			System.exit(0);
			break;

		}
	}

	public void render(Graphics g) {
		switch (Gamestate.state) {
		case MENU:
			menu.draw(g);
			break;
		case PLAYING:
			playing.draw(g);
			break;
		default:
			break;
		}
	}

	@Override
	public void run() {

		double timePerFrame = 1000000000.0 / FPS_SET;
		double timePerUpdate = 1000000000.0 / UPS_SET;

		long previousTime = System.nanoTime();

		double deltaU = 0;
		double deltaF = 0;

		int updates = 0;
		long lastCheck = System.currentTimeMillis();

		while (true) {
			long currentTime = System.nanoTime();

			deltaU += (currentTime - previousTime) / timePerUpdate;
			deltaF += (currentTime - previousTime) / timePerFrame;
			previousTime = currentTime;

			// Run every update that's due in one batch. The loop only wakes once per frame, so several
			// fixed-size updates run back to back; game speed stays exactly UPS_SET either way.
			int ran = 0;
			while (deltaU >= 1 && ran < MAX_CATCH_UP) {
				update();
				deltaU--;
				ran++;
			}
			if (deltaU >= 1)
				deltaU = 0; // too far behind: skip ahead instead of fast-forwarding
			updates += ran;

			if (deltaF >= 1) {
				gamePanel.repaint();
				deltaF = 0; // never queue extra frames
			}

			if (showPerf && System.currentTimeMillis() - lastCheck >= 2000) {
				System.out.println("perf: " + updates / 2 + " updates/s, " + gamePanel.takeFrameCount() / 2 + " frames/s");
				updates = 0;
				lastCheck = System.currentTimeMillis();
			}

			// Sleep until the next frame is due instead of spinning. Waking up rarely matters most in the
			// browser, where every wake-up competes with drawing for the page's single main thread.
			long sleepMs = (long) ((1 - deltaF) * timePerFrame / 1000000);
			try {
				Thread.sleep(Math.max(1, sleepMs));
			} catch (InterruptedException e) {
				return;
			}
		}

	}

	public void windowFocusLost() {
		if (Gamestate.state == Gamestate.PLAYING)
			playing.getPlayer().resetDirBooleans();
	}

	public Menu getMenu() {
		return menu;
	}

	public Playing getPlaying() {
		return playing;
	}
}