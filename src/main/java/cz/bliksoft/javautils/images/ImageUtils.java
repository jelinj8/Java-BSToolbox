package cz.bliksoft.javautils.images;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class ImageUtils {
	public static BufferedImage rotate(BufferedImage inputImage, int angle) {

		int width = inputImage.getWidth();
		int height = inputImage.getHeight();

		AffineTransform transform = new AffineTransform();
		double rotationAngle = Math.toRadians(angle);

		int newWidth;
		int newHeight;

		if (angle == 90 || angle == 270) {
			newWidth = height;
			newHeight = width;
		} else {
			newWidth = (int) Math.abs(width * Math.cos(rotationAngle))
					+ (int) Math.abs(height * Math.sin(rotationAngle));

			newHeight = (int) Math.abs(height * Math.cos(rotationAngle))
					+ (int) Math.abs(width * Math.sin(rotationAngle));
		}

		transform.rotate(rotationAngle, newWidth / 2, newHeight / 2);

		// Translate the image to keep it centered
		transform.translate((newWidth - width) / 2, (newHeight - height) / 2);

		BufferedImage outputImage = new BufferedImage(newWidth, newHeight, inputImage.getType());

		Graphics2D g2d = outputImage.createGraphics();
		g2d.setTransform(transform);
		g2d.drawImage(inputImage, 0, 0, null);
		g2d.dispose();

		return outputImage;
	}

	/**
	 *
	 * @param inputImage
	 * @param scale      (percent)
	 * @return
	 */
	public static BufferedImage scale(BufferedImage inputImage, int scale) {
		int targetW = inputImage.getWidth() * 100 / scale;
		int targetH = inputImage.getHeight() * 100 / scale;

		BufferedImage resizedImage = new BufferedImage(targetW, targetH, inputImage.getType());
		Graphics2D graphics2D = resizedImage.createGraphics();
		graphics2D.drawImage(inputImage, 0, 0, targetW, targetH, null);
		graphics2D.dispose();
		return resizedImage;
	}

	/**
	 * Checks if the image has any transparent pixels (alpha &lt; 128).
	 *
	 * @param img the image to check
	 * @return {@code true} if the image has transparent pixels
	 */
	public static boolean hasTransparency(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int argb = img.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xFF;
				if (alpha < 128) {
					return true;
				}
			}
		}
		return false;
	}
}
