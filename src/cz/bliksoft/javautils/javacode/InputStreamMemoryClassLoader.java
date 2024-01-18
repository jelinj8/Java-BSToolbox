package cz.bliksoft.javautils.javacode;

import java.net.URI;

import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

public class InputStreamMemoryClassLoader<U> extends AbstractMemoryClassLoader<String, U> {

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
