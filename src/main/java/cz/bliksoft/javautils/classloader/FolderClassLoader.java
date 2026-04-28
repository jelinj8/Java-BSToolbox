package cz.bliksoft.javautils.classloader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * for reloading use a new instance of ReloadingClassLoader each time
 * 
 * @author jelinj8
 *
 */
public class FolderClassLoader extends ClassLoader {

	File rootFolder;

	public FolderClassLoader(File rootDir) {
		rootFolder = rootDir;
	}

	@Override
	public Class<?> loadClass(String s) {
		return findClass(s);
	}

	@Override
	public Class<?> findClass(String s) {
		try {
			byte[] bytes = loadClassData(s);
			return defineClass(s, bytes, 0, bytes.length);
		} catch (IOException ioe) {
			try {
				return super.loadClass(s);
			} catch (ClassNotFoundException ignore) {
			}
			ioe.printStackTrace(System.out);
			return null;
		}
	}

	private byte[] loadClassData(String className) throws IOException {
		File f = new File(rootFolder, className.replaceAll("\\.", "/") + ".class");
		int size = (int) f.length();
		byte buff[] = new byte[size];
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		dis.readFully(buff);
		dis.close();
		return buff;
	}
}
