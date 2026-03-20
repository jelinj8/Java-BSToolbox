/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.bliksoft.javautils.xmlfilesystem.singletons;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.exceptions.InitializationException;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.IInitializeWithFileObject;

/**
 * kontejner pro uchovávání singletonu a jeho definičního záznamu ve filesystému
 * 
 * @author hroch
 */
public class SingletonContainer {
	Logger log = LogManager.getLogger();

	private static final Object badValue = new Object();

	protected Class<?> cls;
	protected Object value;
	protected FileObject file;

	public SingletonContainer(FileObject file, Class<?> cls) {
		this.cls = cls;
		this.file = file;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SingletonContainer) {
			return file == (((SingletonContainer) obj).file);
		} else if (obj instanceof FileObject) {
			return file == obj;
		} else {
			return false;
		}
	}

	public Object getValue() {
		if (value == null) {
			Constructor<?> c = null;
			try {
				c = cls.getDeclaredConstructor(FileObject.class);
				try {
					value = c.newInstance(this.file);
					return value;
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					value = badValue;
					throw new InitializationException(
							"Failed to instantiate singleton with file " + this.file.getFullPath(), e);
				}
			} catch (NoSuchMethodException | SecurityException e) {
				try {
					value = cls.getDeclaredConstructor().newInstance();
					if (value instanceof IInitializeWithFileObject) {
						((IInitializeWithFileObject) value).initializeWithFileObject(file);
					}
					return value;
				} catch (NoSuchMethodException e1) {
					value = badValue;
					throw new InitializationException(
							"Failed to instantiate singleton from file (no default constructor) "
									+ this.file.getFullPath(),
							e1);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | SecurityException e1) {
					value = badValue;
					throw new InitializationException(
							"Failed to instantiate singleton from file " + this.file.getFullPath(), e1);
				}
			}
		}
		if (value == badValue)
			return null;

		return value;
	}
}
