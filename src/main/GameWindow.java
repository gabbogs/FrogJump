package main;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JFrame;
import javax.swing.Timer;

public class GameWindow {
	private JFrame jframe;

	public GameWindow(GamePanel gamePanel, boolean web) {

		jframe = new JFrame();

		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.add(gamePanel);

		jframe.setResizable(false);
		// In the browser, drop the title bar and pin the game to the top-left of the page's display area
		jframe.setUndecorated(web);
		jframe.pack();
		if (web) {
			jframe.setLocation(0, 0);
			// The page's game area changes size when the browser goes fullscreen or is resized; follow it
			new Timer(250, e -> fitScreen()).start();
		} else
			jframe.setLocationRelativeTo(null);
		jframe.setVisible(true);
		jframe.addWindowFocusListener(new WindowFocusListener() {

			@Override
			public void windowLostFocus(WindowEvent e) {
				gamePanel.getGame().windowFocusLost();
			}

			@Override
			public void windowGainedFocus(WindowEvent e) {
				// TODO Auto-generated method stub

			}
		});

	}

	private void fitScreen() {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		if (!screen.equals(jframe.getSize())) {
			jframe.setSize(screen);
			jframe.validate();
		}
	}

}