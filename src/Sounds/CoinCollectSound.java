package Sounds;

import javax.sound.sampled.Clip;

public class CoinCollectSound {
	private final Clip clip = SoundLoader.load("coin.wav");

	public void startSound() {
		SoundLoader.play(clip);
	}
}
