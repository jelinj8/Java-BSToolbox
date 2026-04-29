package cz.bliksoft.javautils.xmlfilesystem;

import java.util.HashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class FileLoader {
	private static final Logger log = LogManager.getLogger();

	public static String baseFileloaderPath = "/";
	public static final String FILE_LOADERS_DIR = "fileLoaders";

	public abstract Object loadObject(FileObject file);

	public abstract String getSupportedType();

	private static HashMap<String, FileLoader> loaders;

	@SuppressWarnings("unchecked")
	public static <T> T loadFile(FileObject file) {
		if (file == null)
			return null;

		if (file.getType() == null) {
			log.log(Level.WARN, "File type not specified for {}", file); //$NON-NLS-1$
			return null;
		}

		FileLoader fl = getLoader(file);
		if (fl != null) {
			return (T) fl.loadObject(file);
		} else {
			log.log(Level.ERROR, "File loader not found for {}", file); //$NON-NLS-1$
			return null;
		}
	}

	public static <T> T loadFile(FileObject parent, String fileName) {
		FileObject fileToLoad = parent.getFile(fileName);// $NON-NLS-1$
		if (fileToLoad == null)
			return null;
		return loadFile(fileToLoad);
	}

	public static FileLoader getLoader(FileObject file) {
		return getLoader(file.getType());
	}

	private static void loadFileLoaders() {
		if (loaders != null)
			return;
		loaders = new HashMap<>();

		FileObject fileLoaderFolder = FileSystem.getFile(baseFileloaderPath, FILE_LOADERS_DIR);
		if (fileLoaderFolder != null) {
			FileObjectClassLoader<FileLoader> fl = new FileObjectClassLoader<>();
			for (FileObject f : fileLoaderFolder.getChildFiles()) {
				try {
					FileLoader loader = fl.loadFile(f);
					loaders.put(loader.getSupportedType(), loader);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					log.error("Class doesn't seem to be a valid BSFramework FileLoader: {}", e.getMessage());
				}
			}
		}
	}

	public static FileLoader getLoader(String fileType) {
		loadFileLoaders();
		return loaders.get(fileType);
	}
}
