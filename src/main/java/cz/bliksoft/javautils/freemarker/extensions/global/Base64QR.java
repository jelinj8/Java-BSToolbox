package cz.bliksoft.javautils.freemarker.extensions.global;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import cz.bliksoft.javautils.Base64Utils;
import cz.bliksoft.javautils.barcodes.QRGenerator;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Base64QR implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arg0) throws TemplateModelException {
		String data = String.valueOf(arg0.get(0));
		int modulus = 1;
		if (arg0.size() > 1) {
			modulus = Integer.valueOf(String.valueOf(arg0.get(1)));
		}

		BufferedImage img = QRGenerator.createQR(data, modulus);

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try {
				ImageIO.write(img, "png", baos);
			} catch (IOException e) {
				throw new TemplateModelException("Failed to encode QR code to png", e);
			}

			return Base64Utils.base64Encode(baos.toByteArray());
		} catch (IOException e1) {
			throw new TemplateModelException("Failed to cleanup ByteArrayOutputStream", e1);
		}
	}

}
