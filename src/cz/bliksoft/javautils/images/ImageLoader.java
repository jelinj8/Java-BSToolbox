package cz.bliksoft.javautils.images;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public abstract class ImageLoader {
	public abstract List<String> getSupportedExtensions();

	public abstract Image getImage(String name, String... args) throws Exception;

	private static Map<String, ImageLoader> imageLoaders = new HashMap<>();
	private static ImageLoader defaultLoader = null;

	public static void addLoader(String extension, ImageLoader loader) {
		imageLoaders.putIfAbsent(extension, loader);
	}

	public static void setDefault(ImageLoader loader) {
		defaultLoader = loader;
	}

	public static ImageLoader getLoader(String fileName) {
		ImageLoader res = imageLoaders.get(FilenameUtils.getExtension(fileName).toLowerCase());
		if (res == null)
			res = defaultLoader;
		return res;
	}

	/**
	 * Converts a given Image into a BufferedImage
	 *
	 * @param img The Image to be converted
	 * @return The converted BufferedImage
	 */
	public static BufferedImage toBufferedImage(Image img)
	{
	    if (img instanceof BufferedImage)
	    {
	        return (BufferedImage) img;
	    }

	    // Create a buffered image with transparency
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();

	    // Return the buffered image
	    return bimage;
	}
}
