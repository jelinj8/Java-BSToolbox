package cz.bliksoft.javautils.binding.converters;

import cz.bliksoft.javautils.binding.interfaces.IBindingConverter;

public class ObjectToTrueConverter implements IBindingConverter<Object, Boolean> {
	private ObjectToTrueConverter() {

	}

	private static ObjectToTrueConverter instance = null;

	public static ObjectToTrueConverter getDefault() {
		if (instance == null)
			instance = new ObjectToTrueConverter();
		return instance;
	}
	
	@Override
	public Boolean targetValue(Object sourceValue) {
		return sourceValue!=null;
	}

	@Override
	public Object sourceValue(Boolean targetValue) {
		return null;
	}

}
