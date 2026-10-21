package utilz;

import main.Game;

public class Constants {

	public static final float GRAVITY = 0.04f * Game.SCALE;
	public static final int ANI_SPEED = 25;

	public static class ObjectConstants {

		public static final int APPLE = 1;
		public static final int SPIKE = 2;
		public static final int REVERSE_SPIKE = 3;
		
		public static final int APPLE_WIDTH_DEFAULT = 32;
		public static final int APPLE_HEIGHT_DEFAULT = 32;
		public static final int APPLE_WIDTH = (int) (Game.SCALE * APPLE_WIDTH_DEFAULT);
		public static final int APPLE_HEIGHT = (int) (Game.SCALE * APPLE_HEIGHT_DEFAULT);

		public static final int SPIKE_WIDTH_DEFAULT = 32;
		public static final int SPIKE_HEIGHT_DEFAULT = 32;
		public static final int SPIKE_WIDTH = (int)(Game.SCALE * SPIKE_WIDTH_DEFAULT);
		public static final int SPIKE_HEIGHT= (int)(Game.SCALE * SPIKE_HEIGHT_DEFAULT);
		
		public static final int REVERSE_SPIKE_WIDTH_DEFAULT = 32;
		public static final int REVERSE_SPIKE_HEIGHT_DEFAULT = 32;
		public static final int REVERSE_SPIKE_WIDTH = (int) (Game.SCALE * REVERSE_SPIKE_WIDTH_DEFAULT);
		public static final int REVERSE_SPIKE_HEIGHT = (int) (Game.SCALE * REVERSE_SPIKE_HEIGHT_DEFAULT);
		
		public static int GetSpriteAmount(int object_type) {
			switch (object_type) {
			case APPLE:
				return 17;
			case SPIKE:
				return 1;
			}
			return 1;
		}
	}

	public static class UI {
		public static class Buttons {
			public static final int B_WIDTH_DEFAULT = 140;
			public static final int B_HEIGHT_DEFAULT = 56;
			public static final int B_WIDTH = (int) (B_WIDTH_DEFAULT * Game.SCALE);
			public static final int B_HEIGHT = (int) (B_HEIGHT_DEFAULT * Game.SCALE);
		}

		public static class PauseButtons {
			public static final int SOUND_SIZE_DEFAULT = 42;
			public static final int SOUND_SIZE = (int) (SOUND_SIZE_DEFAULT * Game.SCALE);
		}

		public static class URMButtons {
			public static final int URM_DEFAULT_SIZE = 56;
			public static final int URM_SIZE = (int) (URM_DEFAULT_SIZE * Game.SCALE);

		}
	}

	public static class Directions {
		public static final int LEFT = 0;
		public static final int UP = 1;
		public static final int RIGHT = 2;
		public static final int DOWN = 3;
	}

	public static class PlayerConstants {
		public static final int IDLE = 0;
		public static final int RUNNING = 1;
		public static final int JUMP = 2;
		public static final int FALLING = 3;
		public static final int ATTACK = 4;
		public static final int HIT = 5;
		public static final int DEAD = 6;

		public static int GetSpriteAmount(int player_action) {
			switch (player_action) {
			case DEAD:
				return 8;
			case RUNNING:
				return 12;
			case IDLE:
				return 11;
			case HIT:
				return 4;
			case JUMP:
				return 1;
			case FALLING:
				return 1;
			default:
				return 1;
			}
		}
	}

}