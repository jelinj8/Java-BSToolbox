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

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

public class Services {
	private static final Logger log = LogManager.getLogger();

	private Services() {
	}

	private static Map<FileObject, Object> services = new HashMap<>();

	private static boolean loaded = false;

	/**
	 * Guards {@link #loaded}/{@link #services}. An intrinsic lock is reentrant, so
	 * a service constructor that itself calls
	 * {@link #getService(Class)}/{@link #getServices(Class)} on the same thread
	 * (e.g. while being instantiated during {@link #loadServices()}) does not
	 * deadlock.
	 */
	private static final Object LOCK = new Object();

	/**
	 * Load all objects loadable from /services by registered loaders and store them
	 * in a Map&lt;&gt; to prevent GC. To be used for registering app-lifetime
	 * service objects.
	 *
	 * <p>
	 * Idempotent and thread-safe - a second call (from anywhere, including a lazy
	 * call triggered by {@link #getService(Class)}/{@link #getServices(Class)}) is
	 * a no-op, so services are never instantiated twice.
	 */
	public static void loadServices() {
		synchronized (LOCK) {
			if (loaded)
				return;
			loaded = true;

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
	}

	/**
	 * Returns the first loaded service assignable to {@code cls}, or {@code null}
	 * if none is registered. Triggers {@link #loadServices()} if not loaded yet.
	 *
	 * @param cls lookup type
	 * @return a loaded service instance, or {@code null} if none matches
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getService(Class<?> cls) {
		synchronized (LOCK) {
			if (!loaded) {
				loadServices();
			}
			for (Object value : services.values()) {
				if (cls.isAssignableFrom(value.getClass())) {
					return (T) value;
				}
			}
			return null;
		}
	}

	/**
	 * Returns all loaded services assignable to {@code cls}. Triggers
	 * {@link #loadServices()} if not loaded yet.
	 *
	 * @param cls lookup type
	 * @return loaded service instances, possibly empty
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getServices(Class<?> cls) {
		synchronized (LOCK) {
			if (!loaded) {
				loadServices();
			}
			List<T> result = new ArrayList<>();
			for (Object value : services.values()) {
				if (cls.isAssignableFrom(value.getClass())) {
					result.add((T) value);
				}
			}
			return result;
		}
	}

	/**
	 * Calls {@link Closeable#close()} on every loaded service that implements
	 * {@link Closeable}.
	 */
	public static void cleanup() {
		synchronized (LOCK) {
			for (Entry<FileObject, Object> e : services.entrySet()) {
				if (e.getValue() instanceof Closeable) {
					try {
						((Closeable) e.getValue()).close();
					} catch (IOException ex) {
						log.error("Closing service " + e.getKey(), ex);
					}
				}
			}
		}
	}
}
