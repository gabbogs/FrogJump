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
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
		g2.translate(offsetX, offsetY);
		g2.scale(scale, scale);
		g2.clipRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
		// Keep pixel art crisp when scaled
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		game.render(g2);
		g2.dispose();
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
