package main;

public class MainClass {

	public static void main(String[] args) {
		// --perf prints updates and frames per second to the console
		boolean perf = false;
		for (String arg : args)
			if (arg.equals("--perf"))
				perf = true;
		new Game(perf);
	}

}
