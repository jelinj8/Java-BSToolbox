package cz.bliksoft.javautils.freemarker.extensions.global;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import cz.bliksoft.javautils.Base64Utils;
import cz.bliksoft.javautils.images.iconspec.IconSpecEngine;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * Resolves an icon-spec string (see {@link IconSpecEngine}) server-side — no
 * JavaFX required — and returns the rendered image as a base64-encoded PNG,
 * suitable for embedding directly into generated HTML/XML
 * ({@code data:image/png;base64,...}).
 */
public class Base64IconSpec implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arg0) throws TemplateModelException {
		String spec = String.valueOf(arg0.get(0));

		BufferedImage img = IconSpecEngine.createImage(spec);
		if (img == null)
			throw new TemplateModelException("Failed to resolve iconspec: " + spec);

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try {
				ImageIO.write(img, "png", baos);
			} catch (IOException e) {
				throw new TemplateModelException("Failed to encode iconspec image to png", e);
			}

			return Base64Utils.base64Encode(baos.toByteArray());
		} catch (IOException e1) {
			throw new TemplateModelException("Failed to cleanup ByteArrayOutputStream", e1);
		}
	}

}
