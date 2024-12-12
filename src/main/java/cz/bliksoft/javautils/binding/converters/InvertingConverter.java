package cz.bliksoft.javautils.binding.converters;

import cz.bliksoft.javautils.binding.interfaces.IBindingConverter;
import cz.bliksoft.javautils.binding.interfaces.IValueModel;
import cz.bliksoft.javautils.binding.models.ConverterValueModel;

/**
 * konvertor převádějící true na false a naopak
 * 
 * @author jjelinek
 *
 */
public class InvertingConverter implements IBindingConverter<Boolean, Boolean> {

	private InvertingConverter() {

	}

	private static InvertingConverter instance = null;

	public static InvertingConverter getDefault() {
		if (instance == null) {
			instance = new InvertingConverter();
		}
		return instance;
	}

	@Override
	public Boolean sourceValue(Boolean arg0) {
		if (arg0 == null)
			return null;
		if (arg0)
			return false;
		else
			return true;
	}

	@Override
	public Boolean targetValue(Boolean arg0) {
		if (arg0 == null)
			return null;
		if (arg0)
			return false;
		else
			return true;
	}

	public static IValueModel<Boolean> wrap(IValueModel<Boolean> source) {
		return new ConverterValueModel<>(source, getDefault());
	}
}
