package cz.bliksoft.javautils.xmlfilesystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads typed objects from the children of a virtual-filesystem folder by
 * dispatching each child to the {@link FileLoader} registered for its
 * {@code type} attribute. Children with no matching loader are skipped. Note
 * that the loader registry is static, i.e. shared by all instances.
 */
public class DomainLoader {

	private static final Map<String, FileLoader> subLoaders = new LinkedHashMap<>();

	public DomainLoader() {

	}

	public DomainLoader(FileLoader... loader) {
		for (FileLoader fl : loader) {
			register(fl);
		}
	}

	public void register(FileLoader loader) {
		subLoaders.put(loader.getSupportedType(), loader);
	}

	public <T> List<T> loadChildrenList(FileObject folder) {
		List<T> result = new ArrayList<>();
		if (folder == null)
			return result;
		for (FileObject child : folder.getChildFiles()) {
			T loaded = loadChild(child);
			if (loaded != null)
				result.add(loaded);
		}
		return result;
	}

	public <T> Map<String, T> loadChildrenMap(FileObject folder) {
		Map<String, T> result = new HashMap<>();
		if (folder == null)
			return result;
		for (FileObject child : folder.getChildFiles()) {
			T loaded = loadChild(child);
			if (loaded != null)
				result.put(child.name, loaded);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T loadChild(FileObject child) {
		FileLoader loader = child.getType() != null ? subLoaders.get(child.getType()) : null;
		return loader != null ? (T) loader.loadObject(child) : null;
	}
}
