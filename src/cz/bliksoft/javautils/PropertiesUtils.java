package cz.bliksoft.javautils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class PropertiesUtils {

	public static void saveProperties(Properties properties, File propFile, String comment, boolean makeBackup)
			throws IOException {
		if (makeBackup) {
			Files.copy(propFile.toPath(), new File(propFile.getParent(), propFile.getName() + ".bak").toPath(),
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
}
