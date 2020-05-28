package cz.bliksoft.javautils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class PropertiesUtils {
	/**
	 * 
	 * @param properties
	 * @param fileName
	 * @param comment
	 * @param makeBackup
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void saveProperties(Properties properties, String fileName, String comment, boolean makeBackup) throws FileNotFoundException, IOException{
		File propFile = new File(fileName);
		if(makeBackup){
			Files.copy(new File(fileName).toPath(), new File(fileName + ".bak").toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		
		try (FileOutputStream fos = new FileOutputStream(propFile)) {
			properties.store(fos, comment);
			fos.flush();
		}
	}
}
