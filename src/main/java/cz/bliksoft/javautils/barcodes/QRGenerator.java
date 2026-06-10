package cz.bliksoft.javautils.barcodes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import cz.bliksoft.javautils.StringUtils;

/**
 * Utility class for generating QR code images using ZXing. Requires
 * {@code com.google.zxing:core} on the classpath.
 */
public class QRGenerator {

	private static final Logger log = LogManager.getLogger();

	/**
	 * Encodes {@code contents} as a QR code bit matrix.
	 *
	 * @param contents        text to encode
	 * @param correctionLevel ZXing error correction level
	 * @param margin          quiet-zone size in modules (ZXing default is 4)
	 * @return the bit matrix, or {@code null} on encoding failure
	 */
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

	/**
	 * Creates a QR code image with level-L error correction, margin 2 and given
	 * pixel modulus.
	 *
	 * @param contents text to encode
	 * @param modulus  pixels per QR module
	 */
	public static BufferedImage createQR(String contents, int modulus) {
		return createQR(contents, modulus, ErrorCorrectionLevel.L, 2);
	}

	/**
	 * Creates a QR code image with level-L error correction and configurable
	 * margin.
	 *
	 * @param contents text to encode
	 * @param modulus  pixels per QR module
	 * @param margin   quiet-zone size in modules
	 */
	public static BufferedImage createQR(String contents, int modulus, int margin) {
		return createQR(contents, modulus, ErrorCorrectionLevel.L, margin);
	}

	/**
	 * Creates a QR code image with full control over error correction and margin.
	 *
	 * @param contents        text to encode
	 * @param modulus         pixels per QR module
	 * @param correctionLevel ZXing error correction level
	 * @param margin          quiet-zone size in modules
	 */
	public static BufferedImage createQR(String contents, int modulus, ErrorCorrectionLevel correctionLevel,
			int margin) {
		return createQRImg(createQRBitMatrix(contents, correctionLevel, margin), modulus);
	}

	/**
	 * Encodes text as a QR code and writes it to a file.
	 *
	 * @param myCodeText text to encode
	 * @param filePath   destination file path
	 * @param size       maximum image dimension in pixels
	 * @param fileType   image format (e.g. {@code "png"})
	 * @throws IOException if the QR code does not fit in {@code size} pixels or
	 *                     writing fails
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

	/**
	 * Renders a QR bit matrix into a {@link BufferedImage}.
	 *
	 * @param byteMatrix the bit matrix to render
	 * @param modulus    pixels per QR module
	 * @return RGB image of the QR code
	 */
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

	/**
	 * Rasterizes a ZXing bit matrix into a black/white ARGB image with no quiet
	 * zone, replicating each module into a {@code moduleSize}×{@code moduleSize}
	 * pixel block.
	 */
	public static BufferedImage rasterize(BitMatrix matrix, int moduleSize) {
		int modules = matrix.getWidth();
		int mult = Math.max(1, moduleSize);
		int pixelSize = modules * mult;

		BufferedImage img = new BufferedImage(pixelSize, pixelSize, BufferedImage.TYPE_INT_ARGB);
		int[] row = new int[pixelSize];

		for (int y = 0; y < modules; y++) {
			for (int x = 0; x < modules; x++) {
				int argb = matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
				for (int dx = 0; dx < mult; dx++)
					row[x * mult + dx] = argb;
			}
			for (int dy = 0; dy < mult; dy++)
				img.setRGB(0, y * mult + dy, pixelSize, 1, row, 0, pixelSize);
		}
		return img;
	}

	/**
	 * Encodes {@code data} as a QR code and rasterizes it for icon-spec use, with a
	 * 2-module quiet zone. {@code errorCorrectionLevel} defaults to {@code M} (and
	 * falls back to it with a warning if unrecognized). {@code moduleSize} defaults
	 * to 2 pixels per module; if {@code targetSize} is given, the per-module pixel
	 * size is instead derived from the matrix's module count to best fit that
	 * overall size, and {@code moduleSize} is ignored.
	 */
	public static BufferedImage render(String data, String errorCorrectionLevel, Integer moduleSize, Integer targetSize)
			throws WriterException {
		return render(data, errorCorrectionLevel, moduleSize, targetSize, null);
	}

	/**
	 * Encodes {@code data} as a QR code and rasterizes it for icon-spec use.
	 * {@code errorCorrectionLevel} defaults to {@code M} (and falls back to it with
	 * a warning if unrecognized). {@code moduleSize} defaults to 2 pixels per
	 * module; if {@code targetSize} is given, the per-module pixel size is instead
	 * derived from the matrix's module count to best fit that overall size, and
	 * {@code moduleSize} is ignored. {@code border} sets the quiet-zone thickness
	 * in modules; {@code null} or {@code ≤ 0} defaults to 2.
	 */
	public static BufferedImage render(String data, String errorCorrectionLevel, Integer moduleSize, Integer targetSize,
			Integer border) throws WriterException {
		ErrorCorrectionLevel ec = ErrorCorrectionLevel.M;
		if (StringUtils.hasLength(errorCorrectionLevel)) {
			try {
				ec = ErrorCorrectionLevel.valueOf(errorCorrectionLevel.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				log.warn("Unknown QR error-correction level '{}', defaulting to M", errorCorrectionLevel);
			}
		}

		int quietZone = (border != null && border > 0) ? border : 2;

		Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
		hints.put(EncodeHintType.ERROR_CORRECTION, ec);
		hints.put(EncodeHintType.MARGIN, quietZone);
		BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 0, 0, hints);

		int moduleSz;
		if (targetSize != null && targetSize > 0)
			moduleSz = Math.max(1, targetSize / matrix.getWidth());
		else if (moduleSize != null && moduleSize > 0)
			moduleSz = moduleSize;
		else
			moduleSz = 2;

		return rasterize(matrix, moduleSz);
	}

}