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
	 * registry of singleton objects created from the FileSystem
	 */
	private static Map<FileObject, SingletonContainer> singletonObjects = null;

	private static boolean loaded = false;

	/**
	 * Guards {@link #loaded}/{@link #singletonObjects} and serializes instantiation
	 * via {@link SingletonContainer#getValue()}. An intrinsic lock is reentrant, so
	 * a singleton constructor that itself calls
	 * {@link #getSingleton(Class)}/{@link #getSingletons(Class)} on the same thread
	 * (e.g. while being instantiated during another lookup) does not deadlock.
	 */
	private static final Object LOCK = new Object();

	/**
	 * Registers singleton class metadata from the default {@value #DEFAULT_PATH}
	 * path. Does not instantiate anything - see {@link #getSingleton(Class)} /
	 * {@link #getSingletons(Class)}.
	 *
	 * <p>
	 * Idempotent and thread-safe - a second call (from anywhere, including a lazy
	 * call triggered by {@link #getSingleton(Class)}) is a no-op, so
	 * already-created instances are never discarded/re-instantiated.
	 */
	public static void loadSingletons() {
		loadSingletons(null);

	}

	/**
	 * Registers singleton class metadata from {@code alternative_path} (or
	 * {@value #DEFAULT_PATH} if {@code null}). See {@link #loadSingletons()}.
	 *
	 * @param alternative_path XmlFilesystem path to scan, or {@code null} for
	 *                         {@value #DEFAULT_PATH}
	 */
	public static void loadSingletons(String alternative_path) {
		synchronized (LOCK) {
			if (loaded)
				return;
			loaded = true;

			if (alternative_path == null) {
				alternative_path = DEFAULT_PATH;// $NON-NLS-1$
			}
			if (singletonObjects == null)
				singletonObjects = new HashMap<>();

			FileObject servicesRoot = FileSystem.getFile(alternative_path);
			if (servicesRoot != null) {
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
	}

	/**
	 * Returns the first registered singleton assignable to {@code cls},
	 * instantiating it on first access. Triggers {@link #loadSingletons()} if not
	 * loaded yet. Safe to call even if {@value #DEFAULT_PATH} doesn't exist
	 * (returns {@code null}).
	 *
	 * @param cls lookup type
	 * @return the cached/newly-created instance, or {@code null} if none is
	 *         registered
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getSingleton(Class<?> cls) {
		synchronized (LOCK) {
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
	}

	/**
	 * Returns every registered singleton assignable to {@code cls}, instantiating
	 * each on first access. Triggers {@link #loadSingletons()} if not loaded yet.
	 * Safe to call even if {@value #DEFAULT_PATH} doesn't exist (returns an empty
	 * list).
	 *
	 * @param cls lookup type
	 * @return cached/newly-created instances, possibly empty
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getSingletons(Class<?> cls) {
		synchronized (LOCK) {
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
	}

	/**
	 * Calls {@link Closeable#close()} on every singleton instantiated so far that
	 * implements {@link Closeable}. Singletons never instantiated (lazy, never
	 * looked up) are not affected.
	 */
	public static void cleanup() {
		synchronized (LOCK) {
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
}
