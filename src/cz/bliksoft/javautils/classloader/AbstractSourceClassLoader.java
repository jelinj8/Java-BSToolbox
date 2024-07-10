package cz.bliksoft.javautils.classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * https://stackoverflow.com/questions/10882952/java-load-class-from-string
 * adapted to generic source
 * For repeated loading of the same class create a new instance of this classloader.
 */

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class AbstractSourceClassLoader<T, U> extends ClassLoader {

	public AbstractSourceClassLoader() {
		super(AbstractSourceClassLoader.class.getClassLoader());
	}

	public AbstractSourceClassLoader(ClassLoader parent) {
		super(parent);
	}

	/**
	 * e.g. for String loader:
	 * 
	 * <pre>{@code
	 * return new SimpleJavaFileObject(URI.create("file:///" + className.replace('.', '/') + ".java"), Kind.SOURCE) { @Override
	 * 	public CharSequence getCharContent(boolean ignoreEncErrors) {
	 * 		return javaSource;
	 * 	}
	 * };
	 * }</pre>
	 * 
	 * @param className
	 * @param src
	 * @return
	 */
	public abstract SimpleJavaFileObject getSource(String className, T src);

	/**
	 * Load class from source
	 * 
	 * @param className  e.g. test.MyClass
	 * @param javaSource source content, type dependent on implementation (basically
	 *                   content of .java file)
	 * @return loaded {@code Class<?>}, e.g. to create instance and cast to known interface
	 *         or to use reflection calls
	 * @throws Exception
	 */
	public Class<?> compileAndLoad(String className, T javaSource) throws Exception {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null)
			throw new Exception("Couldn't get a compiler (JRE instead of JDK?)");

		try (StringWriter errorWriter = new StringWriter();
				ByteArrayOutputStream compiledBytesOutputStream = new ByteArrayOutputStream();) {

			SimpleJavaFileObject sourceFile = getSource(className, javaSource);

			SimpleJavaFileObject classFile = new SimpleJavaFileObject(
					URI.create("file:///" + className.replace('.', '/') + ".class"), Kind.CLASS) {
				@Override
				public OutputStream openOutputStream() throws IOException {
					return compiledBytesOutputStream;
				}
			};

			ForwardingJavaFileManager fileManager = new ForwardingJavaFileManager(
					compiler.getStandardFileManager(null, null, null)) {
				@Override
				public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
						JavaFileObject.Kind kind, FileObject sibling) throws IOException {
					return classFile;
				}
			};

			// compile class
			if (!compiler.getTask(errorWriter, fileManager, null, null, null, Arrays.asList(sourceFile)).call()) {
				throw new Exception(errorWriter.toString());
			}

			// load class
			byte[] bytes = compiledBytesOutputStream.toByteArray();
			return defineClass(className, bytes, 0, bytes.length);
		}
	}

	private Map<String, Class<?>> classCache = null;

	/**
	 * Creates object instance, optionally loading Class from source
	 * 
	 * @param className class name to be created (cache key)
	 * @param input     source code (if null, no loading attempt will be performed,
	 *                  calling Class.forName instead if not already cached and no
	 *                  source is provided)
	 * @return
	 * @throws Exception
	 */
	public U createNewInstance(String className, T input) throws Exception {
		Class<?> res = null;
		if (classCache == null) {
			classCache = new HashMap<>();
		} else {
			res = classCache.get(className);
		}

		if (res == null) {
			if (input != null) {
				res = compileAndLoad(className, input);
				classCache.put(className, res);
			} else {
				res = Class.forName(className);
				if (res != null)
					classCache.put(className, res);
			}
		}

		if (res != null) {
			return (U) res.newInstance();
		} else {
			return null;
		}
	}
}