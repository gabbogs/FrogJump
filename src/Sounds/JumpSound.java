package Sounds;

import javax.sound.sampled.Clip;

public class JumpSound {
	private final Clip clip = SoundLoader.load("jump.wav");

	public void startSound() {
		SoundLoader.play(clip);
	}
}
