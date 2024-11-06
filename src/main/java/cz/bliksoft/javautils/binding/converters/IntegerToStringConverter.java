package cz.bliksoft.javautils.binding.converters;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.binding.interfaces.IBindingConverter;
import cz.bliksoft.javautils.binding.interfaces.IValueModel;
import cz.bliksoft.javautils.binding.models.ConverterValueModel;

/**
 * konvertor pro převod mezi java.util.Date a String
 * 
 * @author hroch
 * 
 */
public class IntegerToStringConverter implements IBindingConverter<Integer, String> {

	private IntegerToStringConverter() {

	}

	private static IntegerToStringConverter instance = null;

	public static IntegerToStringConverter getDefault() {
		if (instance == null) {
			instance = new IntegerToStringConverter();
		}
		return instance;
	}

	@Override
	public Integer sourceValue(String stringNumber) {
		if (StringUtils.isEmpty(stringNumber))
			return null;
		try {
			return Integer.valueOf(stringNumber);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public String targetValue(Integer intNumber) {
		if (intNumber == null)
			return ""; //$NON-NLS-1$
		return Integer.toString(intNumber);
	}

	public static IValueModel<String> wrapAsString(IValueModel<Integer> source) {
		return new ConverterValueModel<>(source, getDefault());
	}
}