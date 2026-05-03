package cz.bliksoft.javautils.xmlfilesystem.singletons;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

public class Singletons {
	private static final Logger log = LogManager.getLogger();

	private Singletons() {
	}

	public static final String DEFAULT_PATH = "/singletons";

	/**
	 * registr singleton objektů vytvořených z FileSystému
	 */
	private static Map<FileObject, SingletonContainer> singletonObjects = null;

	public static void loadSingletons() {
		loadSingletons(null);

	}

	public static void loadSingletons(String alternative_path) {
		if (alternative_path == null) {
			alternative_path = DEFAULT_PATH;// $NON-NLS-1$
		}
		FileObject servicesRoot = FileSystem.getFile(alternative_path);
		if (servicesRoot != null) {
			if (singletonObjects == null)
				singletonObjects = new HashMap<>();

			servicesRoot.streamDFAllChildren(false).forEach(fo -> {
				String clsname = fo.getType();
				if (clsname != null) {
					try {
						Class<?> c = Class.forName(clsname);
						singletonObjects.put(fo, new SingletonContainer(fo, c));
					} catch (Exception e) {
						log.error("Singleton implementing class not found for " + fo.getFullPath(), e);
					}
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getSingleton(Class<?> cls) {
		if (singletonObjects == null) {
			loadSingletons();
		}
		for (SingletonContainer c : singletonObjects.values()) {
			if (cls.isAssignableFrom(c.cls)) {
				return (T) c.getValue();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> getSingletons(Class<?> cls) {
		if (singletonObjects == null) {
			loadSingletons();
		}
		List<T> result = new ArrayList<>();
		for (SingletonContainer c : singletonObjects.values()) {
			if (cls.isAssignableFrom(c.cls)) {
				result.add((T) c.getValue());
			}
		}
		return result;
	}

	public static void cleanup() {
		if (singletonObjects != null)
			for (Entry<FileObject, SingletonContainer> e : singletonObjects.entrySet()) {
				if (e.getValue().getValue() instanceof Closeable) {
					try {
						((Closeable) e.getValue().getValue()).close();
					} catch (IOException ex) {
						log.error("Closing singleton " + e.getKey(), ex);
					}
				}
			}
	}
}
