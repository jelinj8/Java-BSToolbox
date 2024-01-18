package cz.bliksoft.javautils.javacode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

import org.apache.commons.io.IOUtils;

public class FileMemoryClassLoader<U> extends AbstractMemoryClassLoader<InputStream, U> {

	@Override
	public SimpleJavaFileObject getSource(String className, InputStream src) {
		return new SimpleJavaFileObject(URI.create("file:///" + className.replace('.', '/') + ".java"), Kind.SOURCE) {
			@Override
			public CharSequence getCharContent(boolean ignoreEncErrors) {
				try {
					return IOUtils.toString(src, StandardCharsets.UTF_8);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
}
