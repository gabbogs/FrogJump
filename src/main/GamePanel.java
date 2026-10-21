package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
import inputs.KeyboardInputs;
import inputs.MouseInputs;

import static main.Game.GAME_HEIGHT;
import static main.Game.GAME_WIDTH;


public class GamePanel extends JPanel {

	private MouseInputs mouseInputs;
	private Game game;

	// Web mode fills the browser area: the game is drawn scaled by `scale` and centered at (offsetX, offsetY).
	// On the desktop these stay at 1 and 0, so nothing changes.
	private double scale = 1;
	private int offsetX = 0, offsetY = 0;

	// Web mode: the frame is drawn at this fraction of the game's resolution, then scaled up (see paintViaBackBuffer)
	private static final double BUFFER_SCALE = 0.5;
	private BufferedImage backBuffer;

	// Frames actually painted since the last takeFrameCount() call (for the --perf readout)
	private final AtomicInteger frames = new AtomicInteger();

	public int takeFrameCount() {
		return frames.getAndSet(0);
	}

	public GamePanel(Game game, boolean web) {
		mouseInputs = new MouseInputs(this);
		this.game = game;
		setPanelSize(web);

		addKeyListener(new KeyboardInputs(this));
		add(game.getPlaying().getObjectManager().coinCounterLabel);
		ScaledMouse scaledMouse = new ScaledMouse();
		addMouseListener(scaledMouse);
		addMouseMotionListener(scaledMouse);
	}

	private void setPanelSize(boolean web) {
		if (!web) {
			setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
			return;
		}
		// In the browser the "screen" is the page's game area; fit the game inside it
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		scale = Math.min((double) screen.width / GAME_WIDTH, (double) screen.height / GAME_HEIGHT);
		offsetX = (int) ((screen.width - GAME_WIDTH * scale) / 2);
		offsetY = (int) ((screen.height - GAME_HEIGHT * scale) / 2);
		setPreferredSize(screen);
		setBackground(Color.BLACK);
		backBuffer = new BufferedImage((int) (GAME_WIDTH * BUFFER_SCALE), (int) (GAME_HEIGHT * BUFFER_SCALE), BufferedImage.TYPE_INT_RGB);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (backBuffer != null)
			paintViaBackBuffer(g);
		else
			game.render(g);
		frames.incrementAndGet();
	}

	// Browser drawing cost grows with the number of pixels touched, so in web mode the game is drawn
	// into a half-resolution image (a quarter of the pixels) and copied to the screen in one scaled draw.
	// Every sprite is pixel art at a multiple of 2, so nothing is lost at half size.
	private void paintViaBackBuffer(Graphics g) {
		Graphics2D bg = backBuffer.createGraphics();
		bg.scale(BUFFER_SCALE, BUFFER_SCALE);
		bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		game.render(bg);
		bg.dispose();

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2.drawImage(backBuffer, offsetX, offsetY, (int) (GAME_WIDTH * scale), (int) (GAME_HEIGHT * scale), null);
	}

	public Game getGame() {
		return game;
	}

	// Converts mouse positions from screen pixels back to game coordinates before the game sees them
	private class ScaledMouse implements MouseListener, MouseMotionListener {

		private MouseEvent toGame(MouseEvent e) {
			int x = (int) ((e.getX() - offsetX) / scale);
			int y = (int) ((e.getY() - offsetY) / scale);
			return new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiersEx(), x, y,
					e.getClickCount(), e.isPopupTrigger(), e.getButton());
		}

		public void mouseClicked(MouseEvent e) { mouseInputs.mouseClicked(toGame(e)); }
		public void mousePressed(MouseEvent e) { mouseInputs.mousePressed(toGame(e)); }
		public void mouseReleased(MouseEvent e) { mouseInputs.mouseReleased(toGame(e)); }
		public void mouseEntered(MouseEvent e) { mouseInputs.mouseEntered(toGame(e)); }
		public void mouseExited(MouseEvent e) { mouseInputs.mouseExited(toGame(e)); }
		public void mouseDragged(MouseEvent e) { mouseInputs.mouseDragged(toGame(e)); }
		public void mouseMoved(MouseEvent e) { mouseInputs.mouseMoved(toGame(e)); }
	}

}
