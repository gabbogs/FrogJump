import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;

// Reads the level PNGs exactly like the Java game (utilz.HelpMethods) and writes the web version's
// levels.json. Run from the project folder after editing a level:
//   java tools/ExportLevels.java res/lvls docs/assets/lvls/levels.json
public class ExportLevels {
	public static void main(String[] args) throws Exception {
		StringBuilder out = new StringBuilder("[\n");
		for (int n = 1; ; n++) {
			File f = new File(args[0], n + ".png");
			if (!f.exists()) break;
			BufferedImage img = ImageIO.read(f);
			int w = img.getWidth(), h = img.getHeight();
			StringBuilder tiles = new StringBuilder(), apples = new StringBuilder(), spikes = new StringBuilder();
			String spawn = null;
			for (int j = 0; j < h; j++) {
				tiles.append(j == 0 ? "" : ",\n    ").append("[");
				for (int i = 0; i < w; i++) {
					int rgb = img.getRGB(i, j);
					int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
					tiles.append(i == 0 ? "" : ",").append(r >= 242 ? 0 : r);
					if (g == 100 && spawn == null) spawn = "[" + i + "," + j + "]";
					if (b == 1) apples.append(apples.length() == 0 ? "" : ",").append("[" + i + "," + j + "]");
					if (b == 2) spikes.append(spikes.length() == 0 ? "" : ",").append("[" + i + "," + j + "]");
				}
				tiles.append("]");
			}
			if (n > 1) out.append(",\n");
			out.append("  {\n  \"spawn\": ").append(spawn == null ? "[1,1]" : spawn)
				.append(",\n  \"apples\": [").append(apples).append("]")
				.append(",\n  \"spikes\": [").append(spikes).append("]")
				.append(",\n  \"tiles\": [\n    ").append(tiles).append("\n  ]\n  }");
		}
		out.append("\n]\n");
		Files.write(new File(args[1]).toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
	}
}
