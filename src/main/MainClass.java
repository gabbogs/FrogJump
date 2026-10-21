package main;

public class MainClass {

	public static void main(String[] args) {
		// The browser version (CheerpJ) passes --web to get a borderless window that fills the page.
		// --perf prints updates and frames per second to the console.
		boolean web = false, perf = false;
		for (String arg : args) {
			if (arg.equals("--web"))
				web = true;
			if (arg.equals("--perf"))
				perf = true;
		}
		new Game(web, perf);
	}

}