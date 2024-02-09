package cz.bliksoft.javautils.classloader;

import java.net.URI;

import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

public class StringSourceClassLoader<U> extends AbstractSourceClassLoader<String, U> {

	@Override
	public SimpleJavaFileObject getSource(String className, String src) {
		return new SimpleJavaFileObject(URI.create("file:///" + className.replace('.', '/') + ".java"), Kind.SOURCE) {
			@Override
			public CharSequence getCharContent(boolean ignoreEncErrors) {
				return src;
			}
		};
	}
}
