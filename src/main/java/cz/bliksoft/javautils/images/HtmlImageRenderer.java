package cz.bliksoft.javautils.images;

import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

import javax.swing.JEditorPane;

public class HtmlImageRenderer {
	public static BufferedImage render(String html, int width, int height) {

		JEditorPane jep = new JEditorPane("text/html", html);
		jep.setOpaque(false);
		if (width != 0 || height != 0)
			jep.setSize(width, height);

		jep.validate();
		
		BufferedImage image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration().createCompatibleImage(jep.getWidth(), jep.getHeight());

		Graphics graphics = image.createGraphics();

		jep.print(graphics);
		return image;
	}
}
