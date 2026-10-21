package Sounds;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

// Loads sound effects from the classpath (/sfx/...) so they work from a JAR and in a browser.
// Returns null instead of throwing when audio is unavailable, so the game keeps running silently.
public class SoundLoader {

	public static Clip load(String fileName) {
		try {
			InputStream raw = SoundLoader.class.getResourceAsStream("/sfx/" + fileName);
			if (raw == null)
				return null;
			// AudioSystem needs mark/reset support, which BufferedInputStream provides
			AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
			Clip clip = AudioSystem.getClip();
			clip.open(audio);
			return clip;
		} catch (Exception e) {
			System.err.println("Sound unavailable: " + fileName + " (" + e + ")");
			return null;
		}
	}

	public static void play(Clip clip) {
		if (clip == null)
			return;
		clip.stop();
		clip.setFramePosition(0);
		clip.start();
	}
}
