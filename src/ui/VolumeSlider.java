package ui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import Sounds.SoundLoader;
import static ui.PixelUi.*;

// Volume slider under the pause menu, drawn in the pause frame's colors. Click or drag the bar to
// change the volume; click the speaker to mute or unmute.
public class VolumeSlider {

	private final Rectangle panel;
	private final Rectangle speaker;
	private final Rectangle track;
	private final Rectangle hitArea;
	private boolean dragging;

	// (centerX, y) is the top center of the slider panel
	public VolumeSlider(int centerX, int y) {
		panel = new Rectangle(centerX - 50 * P, y, 100 * P, 18 * P);
		speaker = new Rectangle(panel.x + 7 * P, panel.y + 5 * P, 9 * P, 7 * P);
		track = new Rectangle(panel.x + 21 * P, panel.y + 7 * P, 72 * P, 4 * P);
		hitArea = new Rectangle(track.x - 3 * P, panel.y, track.width + 6 * P, panel.height);
	}

	public void draw(Graphics g) {
		drawFrame(g, panel);

		drawPixels(g, SoundLoader.isMuted() ? SPEAKER_MUTED : SPEAKER, speaker.x, speaker.y, P, DARK);

		// Track: dark outline, gold up to the current volume, dark brick after it
		fill(g, DARK, track, -P);
		g.setColor(BRICK_DARK);
		g.fillRect(track.x, track.y, track.width, track.height);
		int filled = Math.round(track.width * SoundLoader.getVolume());
		g.setColor(GOLD);
		g.fillRect(track.x, track.y, filled, track.height);

		// Knob
		int knobX = track.x + filled - 2 * P;
		Rectangle knob = new Rectangle(knobX, track.y - 3 * P, 4 * P, track.height + 6 * P);
		fill(g, DARK, knob, 0);
		fill(g, GOLD, knob, P);
		g.setColor(GOLD_SHADE);
		g.fillRect(knob.x + P, knob.y + knob.height - 2 * P, 2 * P, P);
	}

	// Returns true when the press was on the slider, so the pause menu can ignore it
	public boolean mousePressed(MouseEvent e) {
		if (hitArea.contains(e.getX(), e.getY())) {
			dragging = true;
			setFromMouse(e.getX());
			return true;
		}
		if (speaker.contains(e.getX(), e.getY())) {
			SoundLoader.toggleMute();
			return true;
		}
		return false;
	}

	public void mouseDragged(MouseEvent e) {
		if (dragging)
			setFromMouse(e.getX());
	}

	// Returns true when it ends a drag, so the pause menu can ignore it
	public boolean mouseReleased(MouseEvent e) {
		boolean wasDragging = dragging;
		dragging = false;
		return wasDragging;
	}

	private void setFromMouse(int x) {
		SoundLoader.setVolume((float) (x - track.x) / track.width);
	}
}
