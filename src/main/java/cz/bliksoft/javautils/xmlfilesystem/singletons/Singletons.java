package cz.bliksoft.javautils.xmlfilesystem.singletons;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import cz.bliksoft.javautils.xmlfilesystem.IInitializeWithFileObject;

public class Singletons {
	private static final Logger log = LogManager.getLogger();

	private Singletons() {
	}

	public static final String DEFAULT_PATH = "/singletons";

	/**
	 * registr singleton objektů vytvořených z FileSystému
	 */
	private static Map<FileObject, SingletonContainer> singletonObjects = new HashMap<>();

	/**
	 * vyhledání a vytvoření singletonu dle poskytovaného rozhraní
	 * 
	 * @param fo FileObject, pro který má být vytvořen singleton
	 * @return
	 */
	public static Object getSingletonObject(FileObject fo) {
		Object result = null;
		try {
			SingletonContainer cnt = singletonObjects.get(fo);
			if (cnt != null) {
				result = cnt.value;
			} else {
				try {
					Class<?> singletonCalss = Class.forName(fo.getName());
					result = singletonCalss.getDeclaredConstructor().newInstance();
					singletonObjects.put(fo, new SingletonContainer(result, fo));
					if (result instanceof IInitializeWithFileObject) {
						((IInitializeWithFileObject) result).initializeWithFileObject(fo);
					}
				} catch (ClassNotFoundException e) {
					log.log(Level.ERROR, "Singleton class {0} not found for {1}", fo.getResourceId(), //$NON-NLS-1$
							fo.getFullPath());
					return null;
				} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
					log.log(Level.ERROR, "Failed to instantiate singleton class {0} for {1}", //$NON-NLS-1$
							fo.getResourceId(), fo.getFullPath());
					return null;
				}
			}
		} catch (InstantiationException | IllegalAccessException ex) {
			log.log(Level.ERROR,
					MessageFormat.format("Create singleton {0} defined in {1}", fo.getResourceId(), fo.getFullPath()), //$NON-NLS-1$
					ex);
		}
		return result;
	}

	/**
	 * vyhledání a vytvoření všech singletonů poskytujících dané rozhraní/třídu
	 * 
	 * @param cls              požadované rozhraní
	 * @param alternative_path cesta ve FS, na které má být vyhledáváno
	 *                         (default="/singletons")
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> lookupAllSingletons(Class<? extends T> cls, String alternative_path) {
		List<T> result = new ArrayList<>();
		if (alternative_path == null) {
			alternative_path = DEFAULT_PATH;// $NON-NLS-1$
		}
		FileObject servicesRoot = FileSystem.getFile(alternative_path);
		if (servicesRoot != null) {
			for (FileObject implementorFile : servicesRoot.getDirectories()) {
				if (implementorFile.getFile(cls.getName()) != null) {
					Object o = getSingletonObject(implementorFile);
					if (cls.isInstance(o))
						result.add((T) o);
				}
			}
		}
		return result;
	}

	/**
	 * vyhledání a vytvoření singletonu poskytujícího dané rozhraní
	 * 
	 * @param cls              požadované rozhraní
	 * @param alternative_path cesta pro vyhledávání, default="/singletons"
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T lookupSingleton(Class<? extends T> cls, String alternative_path) {

		if (alternative_path == null) {
			alternative_path = DEFAULT_PATH;// $NON-NLS-1$
		}
		FileObject servicesRoot = FileSystem.getFile(alternative_path);
		if (servicesRoot != null) {
			for (FileObject implementorFile : servicesRoot.getDirectories()) {
				if (implementorFile.getFile(cls.getName()) != null) {
					Object result = getSingletonObject(implementorFile);
					if (cls.isInstance(result)) {
						return (T) result;
					}
				}
			}
		}
		return null;
	}

	private static Map<FileObject, Object> services = new HashMap<>();

	/**
	 * Load all objects loadable from /services by registered loaders and store them
	 * in a Map<> to prevent GC. To be used for registering app-lifetime service
	 * objects.
	 */
	public static void loadServices() {
		FileObject fo = FileSystem.getFile("services");
		if (fo != null) {
			fo.streamDFAllChildren(false).forEach(f -> {
				Object o = FileLoader.loadFile(f);
				if (o != null) {
					services.put(f, o);
				}
			});
		}
	}

	public static void cleanup() {
		for (Entry<FileObject, Object> e : services.entrySet()) {
			if (e.getValue() instanceof Closeable) {
				try {
					((Closeable) e.getValue()).close();
				} catch (IOException ex) {
					log.error("Closing service " + e.getKey(), ex);
				}
			}
		}

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
