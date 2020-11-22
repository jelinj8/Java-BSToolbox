package cz.bliksoft.javautils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertiesUtils {
	private static final Logger log = Logger.getLogger(PropertiesUtils.class.getName());

	/**
	 * 
	 * @param properties
	 * @param fileName
	 * @param comment
	 * @param makeBackup
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void saveProperties(Properties properties, String fileName, String comment, boolean makeBackup)
			throws IOException {
		File propFile = new File(fileName);
		if (makeBackup) {
			Files.copy(new File(fileName).toPath(), new File(fileName + ".bak").toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		}

		try (FileOutputStream fos = new FileOutputStream(propFile)) {
			properties.store(fos, comment);
			fos.flush();
		}
	}

	public static Properties loadFromFile(File f) {
		Properties res = null;
		try (FileInputStream fis = new FileInputStream(f)) {
			res = new Properties();
			res.load(fis);
			log.info("Properties file " + f.getAbsolutePath() + " loaded");
		} catch (FileNotFoundException e) {
			log.log(Level.SEVERE, "Properties file " + f.getAbsolutePath() + " not found.", e);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to load properties file.", e);
		}
		return res;
	}
}
