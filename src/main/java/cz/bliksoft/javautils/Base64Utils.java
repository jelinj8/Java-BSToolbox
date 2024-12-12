package cz.bliksoft.javautils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;

public class Base64Utils {
	public static byte[] base64Decode(String property) {
		return Base64.getDecoder().decode(property);
	}

	public static String base64Encode(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

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
