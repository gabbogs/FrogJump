package main;

public class MainClass {

	public static void main(String[] args) {
		// The browser version (CheerpJ) passes --web to get a borderless window that fills the page
		boolean web = args.length > 0 && args[0].equals("--web");
		new Game(web);
	}

}