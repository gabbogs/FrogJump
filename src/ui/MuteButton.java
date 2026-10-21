package ui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import Sounds.SoundLoader;
import static ui.PixelUi.*;

// Square mute button in the top-left corner of the menu and the game. M does the same.
public class MuteButton {

	// Same height as the apple counter, so they line up side by side
	public static final int SIZE = 100;
	public static final int X = 24, Y = 24;

	private static final int ICON_PIXEL = 6;
	private final Rectangle bounds = new Rectangle(X, Y, SIZE, SIZE);

	public void draw(Graphics g) {
		drawFrame(g, bounds);
		int iconW = 9 * ICON_PIXEL, iconH = 7 * ICON_PIXEL;
		drawPixels(g, SoundLoader.isMuted() ? SPEAKER_MUTED : SPEAKER,
				X + (SIZE - iconW) / 2, Y + (SIZE - iconH) / 2, ICON_PIXEL, DARK);
	}

	// Returns true when the press was on the button, so the screen behind it can ignore it
	public boolean mousePressed(MouseEvent e) {
		if (!bounds.contains(e.getX(), e.getY()))
			return false;
		SoundLoader.toggleMute();
		return true;
	}
}
