package cz.bliksoft.javautils.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

import org.apache.commons.io.FileUtils;

public class FileSourceClassLoader<U> extends AbstractSourceClassLoader<File, U> {

	@Override
	public SimpleJavaFileObject getSource(String className, File src) {
		return new SimpleJavaFileObject(URI.create("file:///" + className.replace('.', '/') + ".java"), Kind.SOURCE) {
			@Override
			public CharSequence getCharContent(boolean ignoreEncErrors) {
				try {
					return FileUtils.readFileToString(src, StandardCharsets.UTF_8);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
}
