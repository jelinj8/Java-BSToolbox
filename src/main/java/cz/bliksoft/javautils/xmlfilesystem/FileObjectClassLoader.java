package cz.bliksoft.javautils.xmlfilesystem;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class FileObjectClassLoader<T> {

	public T loadFile(FileObject fo) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, SecurityException {
		@SuppressWarnings("unchecked")
		Class<T> cls = (Class<T>) Class.forName(fo.getName());
		try {
			return cls.getDeclaredConstructor().newInstance();
		} catch (NoSuchMethodException | InvocationTargetException e) {
			return null;
		}
	}

	public List<T> loadFiles(FileObject fo) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, SecurityException {
		List<T> result = new ArrayList<>();
		for (FileObject ch : fo.getChildFiles()) {
			result.add(loadFile(ch));
		}
		return result;
	}
}
