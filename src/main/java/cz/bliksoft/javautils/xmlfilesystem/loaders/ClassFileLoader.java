package cz.bliksoft.javautils.xmlfilesystem.loaders;

import cz.bliksoft.javautils.exceptions.InitializationException;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.IInitializeWithFileObject;

/**
 * loads an object, if it has a constructor accepting {@link FileObject}, it is
 * used. Otherwise a no-args constructor is required. THen it can implement
 * {@link IInitializeWithFileObject}.
 */
public class ClassFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {

		String controlClass = file.getAttribute("class");
		Class<?> c;
		try {
			c = Class.forName(controlClass);
			try {
				return c.getDeclaredConstructor(FileObject.class).newInstance(file);
			} catch (NoSuchMethodException nsme) {
				Object result = c.getDeclaredConstructor().newInstance();
				if (result instanceof IInitializeWithFileObject) {
					((IInitializeWithFileObject) result).initializeWithFileObject(file);
				}
				return result;
			}
		} catch (Exception e) {
			throw new InitializationException("Failed to load Class " + controlClass, e);
		}
	}

	@Override
	public String getSupportedType() {
		return "Class";
	}

}
