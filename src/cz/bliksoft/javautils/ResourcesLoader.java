package cz.bliksoft.javautils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

public class ResourcesLoader {

	public static String loadString(String classpathName) {
		InputStream in = ResourcesLoader.class.getResourceAsStream(classpathName);
		if (in == null)
			return null;
		try {
			return IOUtils.toString(in, Charset.forName("utf-8"));
		} catch (IOException e) {
			return null;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}

	}

}
