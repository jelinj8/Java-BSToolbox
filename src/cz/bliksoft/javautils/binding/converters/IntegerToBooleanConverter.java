package cz.bliksoft.javautils.binding.converters;

import cz.bliksoft.javautils.binding.interfaces.IBindingConverter;

public class IntegerToBooleanConverter implements IBindingConverter<Integer, Boolean> {
	private IntegerToBooleanConverter() {

	}

	private static IntegerToBooleanConverter instance = null;

	public static IntegerToBooleanConverter getDefault() {
		if (instance == null)
			instance = new IntegerToBooleanConverter();
		return instance;
	}

	@Override
	public Boolean targetValue(Integer sourceValue) {
		return ((sourceValue != null) && (sourceValue != 0));
	}

	@Override
	public Integer sourceValue(Boolean targetValue) {
		return (targetValue ? 1 : 0);
	}

}
