package cz.bliksoft.javautils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cz.bliksoft.javautils.streams.replacer.ITokenResolver;
import cz.bliksoft.javautils.streams.replacer.MapTokenResolver;
import cz.bliksoft.javautils.streams.replacer.TokenReplacingReader;

public class PropertiesUtils {

	public static void saveProperties(Properties properties, File propFile, String comment, boolean makeBackup)
			throws IOException {
		if (makeBackup) {
			Files.copy(propFile.toPath(), new File(propFile.getParent(), propFile.getName() + ".bak").toPath(), //$NON-NLS-1$
					StandardCopyOption.REPLACE_EXISTING);
		}

		try (FileOutputStream fos = new FileOutputStream(propFile)) {
			properties.store(fos, comment);
			fos.flush();
		}
	}

	public static Properties loadFromFile(File f) throws IOException {
		Properties res = null;
		try (FileInputStream fis = new FileInputStream(f)) {
			res = new Properties();
			res.load(fis);
		}
		return res;
	}

	public static Map<String, String> toMap(Properties props) {
		Map<String, String> res = new HashMap<>();
		props.forEach((k, v) -> {
			res.put(k.toString(), v.toString());
		});
		return res;
	}

	public static Properties loadFromFile(File f, Map<String, String> tokens) throws IOException {
		Properties res = null;
		ITokenResolver resolver = new MapTokenResolver(tokens);
		try (Reader fis = new TokenReplacingReader(f, resolver)) {
			res = new Properties();
			res.load(fis);
		}
		return res;
	}

}
