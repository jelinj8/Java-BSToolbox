package cz.bliksoft.javautils.xmlfilesystem.singletons;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import cz.bliksoft.javautils.xmlfilesystem.IFileObjectInitializator;

public class Singletons {
	private static final Logger log = LogManager.getLogger();

	private Singletons() {
	}

	public static final String DEFAULT_PATH = "/services";
	
	/**
	 * registr poskytovatelů rozhraní/implementací
	 */
	private static Map<Class<?>, SingletonContainer> serviceProviders = new HashMap<>();

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
					if (result instanceof IFileObjectInitializator) {
						((IFileObjectInitializator) result).setFileObject(fo);
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
	 * vyhledání a vytvoření všech singletonů poskytujících dané rozhraní
	 * 
	 * @param cls              požadované rozhraní
	 * @param alternative_path cesta ve FS, na které má být vyhledáváno
	 *                         (default="/services")
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> lookupAllSingletons(Class<? extends T> cls, String alternative_path) {
		List<T> result = new ArrayList<>();
		if (alternative_path == null) {
			alternative_path = DEFAULT_PATH;//$NON-NLS-1$
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
	 * @param alternative_path cesta pro vyhledávání, default="/services"
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T lookupSingleton(Class<? extends T> cls, String alternative_path) {
		if (serviceProviders.containsKey(cls))
			return (T) serviceProviders.get(cls).getValue();

		if (alternative_path == null) {
			alternative_path = DEFAULT_PATH;//$NON-NLS-1$
		}
		FileObject servicesRoot = FileSystem.getFile(alternative_path);
		if (servicesRoot != null) {
			for (FileObject implementorFile : servicesRoot.getDirectories()) {
				if (implementorFile.getFile(cls.getName()) != null) {
					Object result = getSingletonObject(implementorFile);
					if (cls.isInstance(result)) {
						serviceProviders.put(cls, new SingletonContainer(result, implementorFile));
						return (T) result;
					}
				}
			}
		}
		return null;
	}
}
