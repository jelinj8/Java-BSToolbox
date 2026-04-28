package cz.bliksoft.javautils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/** Utility for loading classpath resources as strings. */
public class ResourcesLoader {

	/**
	 * Reads a classpath resource and returns its contents as a single string, using
	 * the system line separator.
	 *
	 * @param classpathName absolute classpath path (e.g.
	 *                      {@code "/cz/example/template.txt"})
	 * @return resource contents, or {@code null} if the resource was not found
	 * @throws IOException on read error
	 */
	public static String loadString(String classpathName) throws IOException {
		try (InputStream in = ResourcesLoader.class.getResourceAsStream(classpathName)) {
			if (in == null)
				return null;
			try (InputStreamReader isr = new InputStreamReader(in); BufferedReader reader = new BufferedReader(isr)) {
				return reader.lines().collect(Collectors.joining(System.lineSeparator()));
			}
		}
	}

}
