package Sounds;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

// Loads sound effects from the classpath (/sfx/...) so they work from a JAR and in a browser.
// Returns null instead of throwing when audio is unavailable, so the game keeps running silently.
// Also holds the game's volume, which applies to every loaded sound.
public class SoundLoader {

	private static final List<Clip> clips = new ArrayList<>();
	private static float volume = 0.7f;

	public static Clip load(String fileName) {
		try {
			InputStream raw = SoundLoader.class.getResourceAsStream("/sfx/" + fileName);
			if (raw == null)
				return null;
			// AudioSystem needs mark/reset support, which BufferedInputStream provides
			AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
			Clip clip = AudioSystem.getClip();
			clip.open(audio);
			synchronized (clips) {
				clips.add(clip);
				applyVolume(clip);
			}
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

	public static float getVolume() {
		return volume;
	}

	// 0 = silent, 1 = full volume
	public static void setVolume(float newVolume) {
		synchronized (clips) {
			volume = Math.max(0, Math.min(1, newVolume));
			for (Clip clip : clips)
				applyVolume(clip);
		}
	}

	private static void applyVolume(Clip clip) {
		if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
			return;
		FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		// Loudness is logarithmic, so convert the 0-1 slider position to decibels
		float db = volume <= 0.001f ? gain.getMinimum() : (float) (20 * Math.log10(volume));
		gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
	}
}
