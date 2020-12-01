package cz.bliksoft.javautils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ResourcesLoader {

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
