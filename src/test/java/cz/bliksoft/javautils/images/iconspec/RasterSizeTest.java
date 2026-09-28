package cz.bliksoft.javautils.images.iconspec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Raster images in a spec: {@code file|w|h|scale}, JPG included. */
class RasterSizeTest {

	@TempDir
	Path dir;
	private String file;

	@BeforeEach
	void image() throws Exception {
		BufferedImage img = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 40; y++)
			for (int x = 0; x < 80; x++)
				img.setRGB(x, y, x < 40 ? 0xFF0000 : 0x0000FF);
		File f = dir.resolve("photo.jpg").toFile();
		ImageIO.write(img, "jpg", f);
		file = IconSpecEngine.PREFIX_FILE + f.getAbsolutePath();
	}

	private BufferedImage create(String params) {
		BufferedImage img = IconSpecEngine.createImage(file + params);
		assertNotNull(img, file + params);
		return img;
	}

	private static void assertSize(int w, int h, BufferedImage img) {
		assertEquals(w + "x" + h, img.getWidth() + "x" + img.getHeight());
	}

	@Test
	void jpgAsItIs() {
		assertSize(80, 40, create(""));
	}

	@Test
	void oneSideKeepsTheAspectRatio() {
		assertSize(40, 20, create("|40"));
		assertSize(160, 80, create("||80"));
	}

	@Test
	void bothSidesAndScale() {
		assertSize(40, 10, create("|40|10"));
		assertSize(160, 80, create("|||2"));
		assertSize(20, 10, create("|40||0.5"));
	}

	@Test
	void downscaledKeepsColors() {
		BufferedImage img = create("|20");
		int left = img.getRGB(2, 5), right = img.getRGB(17, 5);
		assertEquals(true, ((left >> 16) & 0xFF) > 200 && (left & 0xFF) < 60, Integer.toHexString(left));
		assertEquals(true, (right & 0xFF) > 200 && ((right >> 16) & 0xFF) < 60, Integer.toHexString(right));
	}
}
