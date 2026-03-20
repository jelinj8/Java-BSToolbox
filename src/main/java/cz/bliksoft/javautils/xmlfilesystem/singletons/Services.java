package cz.bliksoft.javautils.xmlfilesystem.singletons;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
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
	}
}
