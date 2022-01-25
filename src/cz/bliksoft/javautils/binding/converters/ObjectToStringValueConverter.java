package cz.bliksoft.javautils.binding.converters;

import cz.bliksoft.javautils.binding.interfaces.IBindingConverter;
import cz.bliksoft.javautils.binding.interfaces.IValueModel;
import cz.bliksoft.javautils.binding.models.ConverterValueModel;
import cz.bliksoft.javautils.strings.ITranslatable;

public class ObjectToStringValueConverter implements IBindingConverter<Object, String> {

	private static ObjectToStringValueConverter instance = null;

	public static ObjectToStringValueConverter getDefault() {
		if (instance == null) {
			instance = new ObjectToStringValueConverter();
		}
		return instance;
	}

	private ObjectToStringValueConverter() {
	}

	@Override
	public Object sourceValue(String target) {
		return null;
	}

	@Override
	public String targetValue(Object source) {
		if (source == null)
			return "";
		else if (source instanceof ITranslatable)
			return ((ITranslatable) source).localizedToString();
		else
			return source.toString();
	}

	public static IValueModel<String> wrapAsString(IValueModel<Object> source) {
		return new ConverterValueModel<>(source, getDefault());
	}
}
