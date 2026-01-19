package cz.bliksoft.javautils;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class ClasspathUtils {

	public static List<String> list(String resourceDir) throws IOException {
		String dir = resourceDir.startsWith("/") ? resourceDir.substring(1) : resourceDir;
		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		Enumeration<URL> urls = cl.getResources(dir);
		List<String> out = new ArrayList<>();

		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			String protocol = url.getProtocol();

			if ("file".equals(protocol)) {
				try {
					Path p = Paths.get(url.toURI());
					try (Stream<Path> stream = Files.list(p)) {
						stream.filter(Files::isRegularFile).forEach(f -> out.add(dir + "/" + f.getFileName()));
					}
				} catch (URISyntaxException e) {
					throw new IOException(e);
				}
			} else if ("jar".equals(protocol)) {
				// jar:file:/path/app.jar!/ui
				JarURLConnection conn = (JarURLConnection) url.openConnection();
				try (JarFile jar = conn.getJarFile()) {
					String prefix = conn.getEntryName();
					if (!prefix.endsWith("/"))
						prefix += "/";

					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						JarEntry e = entries.nextElement();
						String name = e.getName();
						if (!e.isDirectory() && name.startsWith(prefix)) {
							// only direct children (optional)
							String rest = name.substring(prefix.length());
							if (!rest.contains("/"))
								out.add(name);
						}
					}
				}
			} else {
				// e.g. "jrt" for modules, or custom classloaders
				// You can decide to ignore or add handling.
			}
		}
		return out;
	}
}
