package cz.bliksoft.javautils.binding.converters;

import cz.bliksoft.javautils.binding.interfaces.IBindingConverter;

public class ObjectToFalseConverter implements IBindingConverter<Object, Boolean> {
	private ObjectToFalseConverter() {

	}

	private static ObjectToFalseConverter instance = null;

	public static ObjectToFalseConverter getDefault() {
		if (instance == null)
			instance = new ObjectToFalseConverter();
		return instance;
	}

	@Override
	public Boolean targetValue(Object sourceValue) {
		return sourceValue == null;
	}

	@Override
	public Object sourceValue(Boolean targetValue) {
		return null;
	}

}
