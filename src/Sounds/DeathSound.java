package Sounds;

import javax.sound.sampled.Clip;

public class DeathSound {
	private final Clip clip = SoundLoader.load("death.wav");

	public void startSound() {
		SoundLoader.play(clip);
	}
}
