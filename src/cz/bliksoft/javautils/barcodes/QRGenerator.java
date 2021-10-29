package cz.bliksoft.javautils.barcodes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * @author Crunchify.com Updated: 03/20/2016 - added code to narrow border size
 */

public class QRGenerator {

	public static BitMatrix createQRBitMatrix(String contents, ErrorCorrectionLevel correctionLevel, int margin) {
		try {
			Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
			hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

			// Now with zxing version 3.2.1 you could change border size (white border size
			// to just 1)
			hintMap.put(EncodeHintType.MARGIN, margin); /* default = 4 */
			hintMap.put(EncodeHintType.ERROR_CORRECTION, correctionLevel);

			QRCodeWriter qrCodeWriter = new QRCodeWriter();
			return qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, 10, 10, hintMap);
		} catch (Exception e) {
			return null;
		}
	}

	public static BufferedImage createQR(String contents, int modulus) {
		return createQR(contents, modulus, ErrorCorrectionLevel.L, 2);
	}

	public static BufferedImage createQR(String contents, int modulus, int margin) {
		return createQR(contents, modulus, ErrorCorrectionLevel.L, margin);
	}

	public static BufferedImage createQR(String contents, int modulus, ErrorCorrectionLevel correctionLevel,
			int margin) {
		return createQRImg(createQRBitMatrix(contents, correctionLevel, margin), modulus);
	}

	/**
	 * 
	 * @param myCodeText text do kódu
	 * @param filePath   cesta k souboru
	 * @param size       max velikost obrázku v pixelech
	 * @param fileType   typ souboru (např. "png");
	 * @throws IOException
	 */
	public static void createQR(String myCodeText, String filePath, int size, String fileType) throws IOException {
		File myFile = new File(filePath);
		BitMatrix matrix = createQRBitMatrix(myCodeText, ErrorCorrectionLevel.L, 2);
		int modulus = Math.floorDiv(size, matrix.getWidth());
		if (modulus == 0)
			throw new IOException("QR code won't fit size " + size);
		BufferedImage image = createQRImg(matrix, modulus);
		ImageIO.write(image, fileType, myFile);
	}

	public static BufferedImage createQRImg(BitMatrix byteMatrix, int modulus) {
		int crunchifyWidth = byteMatrix.getWidth();
		BufferedImage image = new BufferedImage(crunchifyWidth * modulus, crunchifyWidth * modulus,
				BufferedImage.TYPE_INT_RGB);
		image.createGraphics();

		Graphics2D graphics = (Graphics2D) image.getGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, crunchifyWidth * modulus, crunchifyWidth * modulus);
		graphics.setColor(Color.BLACK);

		for (int i = 0; i < crunchifyWidth; i++) {
			for (int j = 0; j < crunchifyWidth; j++) {
				if (byteMatrix.get(i, j)) {
					graphics.fillRect(i * modulus, j * modulus, modulus, modulus);
				}
			}
		}

		return image;
	}

}