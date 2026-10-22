package Sounds;

import javax.sound.sampled.Clip;

// Looping background music. The track is loaded on a background thread so it never delays the
// game starting (it is the largest file); start/stop calls made before
// it finishes loading are remembered and applied once it's ready.
public class BackgroundMusic {
	private volatile Clip clip;
	private volatile boolean wantPlaying = false;

	public BackgroundMusic() {
		Thread loader = new Thread(() -> {
			Clip loaded = SoundLoader.load("music.wav");
			synchronized (this) {
				clip = loaded;
				if (wantPlaying)
					play();
			}
		}, "music-loader");
		loader.setDaemon(true);
		loader.start();
	}

	// Safe to call every frame: returns immediately unless the music was stopped
	public void startMusic() {
		if (wantPlaying)
			return;
		synchronized (this) {
			wantPlaying = true;
			play();
		}
	}

	public void resetMusic() {
		if (!wantPlaying)
			return;
		synchronized (this) {
			wantPlaying = false;
			if (clip != null)
				clip.stop();
		}
	}

	private void play() {
		if (clip != null && !clip.isRunning())
			clip.loop(Clip.LOOP_CONTINUOUSLY);
	}
}
