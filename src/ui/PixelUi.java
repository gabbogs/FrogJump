package ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

// Shared look for the in-game panels (apple counter, volume slider): the pause menu's colors and
// frame style, plus a tiny pixel font for numbers. Sizes are multiples of 2 so they stay crisp
// in the browser, where frames are drawn at half resolution.
public class PixelUi {

	public static final Color DARK = new Color(51, 50, 61);
	public static final Color GOLD = new Color(223, 185, 82);
	public static final Color GOLD_SHADE = new Color(210, 167, 50);
	public static final Color BRICK = new Color(152, 82, 82);
	public static final Color BRICK_DARK = new Color(110, 56, 62);
	public static final Color CREAM = new Color(255, 244, 214);

	// One "pixel" of the frame art, in game coordinates
	public static final int P = 4;

	// Same layers as the pause menu frame: dark outline, gold border with a shaded bottom, dark inner line, brick fill
	public static void drawFrame(Graphics g, Rectangle r) {
		fill(g, DARK, r, 0);
		fill(g, GOLD, r, P);
		g.setColor(GOLD_SHADE);
		g.fillRect(r.x + P, r.y + r.height - 3 * P, r.width - 2 * P, 2 * P);
		fill(g, DARK, r, 3 * P);
		fill(g, BRICK, r, 4 * P);
	}

	public static void fill(Graphics g, Color color, Rectangle r, int inset) {
		g.setColor(color);
		g.fillRect(r.x + inset, r.y + inset, r.width - 2 * inset, r.height - 2 * inset);
	}

	// Draws rows of '#' as square pixels of the given size
	public static void drawPixels(Graphics g, String[] rows, int x, int y, int size, Color color) {
		g.setColor(color);
		for (int row = 0; row < rows.length; row++)
			for (int col = 0; col < rows[row].length(); col++)
				if (rows[row].charAt(col) == '#')
					g.fillRect(x + col * size, y + row * size, size, size);
	}

	// Speaker icons for the volume slider and mute button
	public static final String[] SPEAKER = {
			"...#.....",
			"..##..#..",
			"####...#.",
			"####.#.#.",
			"####...#.",
			"..##..#..",
			"...#.....", };
	public static final String[] SPEAKER_MUTED = {
			"...#.....",
			"..##.....",
			"####.#.#.",
			"####..#..",
			"####.#.#.",
			"..##.....",
			"...#.....", };

	// 3x5 pixel font: digits 0-9 and 'x'
	private static final String[][] GLYPHS = {
			{ "###", "#.#", "#.#", "#.#", "###" }, { ".#.", "##.", ".#.", ".#.", "###" },
			{ "###", "..#", "###", "#..", "###" }, { "###", "..#", ".##", "..#", "###" },
			{ "#.#", "#.#", "###", "..#", "..#" }, { "###", "#..", "###", "..#", "###" },
			{ "###", "#..", "###", "#.#", "###" }, { "###", "..#", "..#", ".#.", ".#." },
			{ "###", "#.#", "###", "#.#", "###" }, { "###", "#.#", "###", "..#", "###" } };
	private static final String[] GLYPH_X = { "...", "#.#", ".#.", "#.#", "..." };

	// Width of `text` in game pixels when drawn with drawText at this pixel size
	public static int textWidth(String text, int size) {
		return text.length() * 4 * size - size;
	}

	// Draws digits and 'x' in cream with a dark drop shadow; (x, y) is the top left
	public static void drawText(Graphics g, String text, int x, int y, int size) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			String[] glyph = c == 'x' ? GLYPH_X : GLYPHS[c - '0'];
			int gx = x + i * 4 * size;
			drawPixels(g, glyph, gx + 2, y + 2, size, DARK);
			drawPixels(g, glyph, gx, y, size, CREAM);
		}
	}
}
