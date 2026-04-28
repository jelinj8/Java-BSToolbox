package cz.bliksoft.javautils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;

/** Convenience wrappers around {@link java.util.Base64}. */
public class Base64Utils {

	/**
	 * Decodes a Base64-encoded string to raw bytes using the standard decoder.
	 *
	 * @param property Base64-encoded string
	 * @return decoded bytes
	 */
	public static byte[] base64Decode(String property) {
		return Base64.getDecoder().decode(property);
	}

	/**
	 * Encodes raw bytes to a Base64 string using the standard encoder.
	 *
	 * @param bytes bytes to encode
	 * @return Base64-encoded string
	 */
	public static String base64Encode(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * Reads a file and returns its contents Base64-encoded.
	 *
	 * @param fToEncode file to encode; must exist
	 * @return Base64-encoded file contents
	 * @throws FileNotFoundException if the file does not exist
	 * @throws IOException           on read error
	 */
	public static String fileToBase64(File fToEncode) throws FileNotFoundException, IOException {
		if (!fToEncode.exists())
			throw new FileNotFoundException("File to encode " + fToEncode.getAbsolutePath() + " was not found!");

		byte[] bytes = new byte[(int) fToEncode.length()];
		try (FileInputStream fis = new FileInputStream(fToEncode)) {
			fis.read(bytes);
		}

		return Base64Utils.base64Encode(bytes);
	}
}
