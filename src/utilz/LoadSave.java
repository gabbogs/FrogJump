package utilz;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;



public class LoadSave {

	public static final String PLAYER_ATLAS = "frog.png";
	public static final String LEVEL_ATLAS = "Terrain (16x16).png";
	public static final String MENU_BUTTONS = "button_atlas.png";
	public static final String MENU_BACKGROUND = "menu_background.png";
	public static final String PAUSE_BACKGROUND = "pause_menu.png";
	public static final String SOUND_BUTTONS = "sound_button.png";
	public static final String URM_BUTTONS = "urm_buttons.png";
	public static final String VOLUME_BUTTONS = "volume_buttons.png";
	public static final String MENU_BACKGROUND_IMG = "autumn_sky_bg.png";
	public static final String PLAYING_BG_IMG = "cool_sky_bg.png";
	public static final String COMPLETED_IMG = "completed_sprite.png";	
	public static final String APPLE_ATLAS = "Apple.png";
	public static final String TRAP_ATLAS = "Spike_trap.png";
	public static final String DEATH_SCREEN = "death_screen.png";
	public static final String REVERSE_TRAP_ATLAS = "reverse_spike_trap";
	public static final String FOREST_BG = "forest_bg.png";

	public static BufferedImage GetSpriteAtlas(String fileName) {
		BufferedImage img = null;
		InputStream is = LoadSave.class.getResourceAsStream("/" + fileName);
		try {
			img = ImageIO.read(is);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return img;
	}

	// Loads /lvls/1.png, /lvls/2.png, ... from the classpath until one is missing.
	// Reading from the classpath (not the file system) lets the game run from a JAR or in a browser.
	public static BufferedImage[] GetAllLevels() {
		List<BufferedImage> levels = new ArrayList<>();
		for (int i = 1; ; i++) {
			InputStream is = LoadSave.class.getResourceAsStream("/lvls/" + i + ".png");
			if (is == null)
				break;
			try {
				levels.add(ImageIO.read(is));
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return levels.toArray(new BufferedImage[0]);
	}

}