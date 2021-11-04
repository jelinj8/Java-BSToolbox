package cz.bliksoft.javautils.binding.converters;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.binding.interfaces.IValueModel;
import cz.bliksoft.javautils.binding.models.BindingConverter;
import cz.bliksoft.javautils.binding.models.ConverterValueModel;

public class StringToHTMLConverter implements BindingConverter<String, String> {

	private StringToHTMLConverter() {

	}

	private static StringToHTMLConverter instance = null;

	public static StringToHTMLConverter getDefault() {
		if (instance == null)
			instance = new StringToHTMLConverter();
		return instance;
	}

	@Override
	public String sourceValue(String arg0) {
		// TODO provést odstranění značek
		return null;
	}

	@Override
	public String targetValue(String arg0) {
		if (arg0 == null)
			return ""; //$NON-NLS-1$
		return "<HTML>" + StringUtils.preformatForHTML(arg0) + "</HTML>"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static IValueModel<String> wrapAsHTML(IValueModel<String> source) {
		return new ConverterValueModel<>(source, getDefault());
	}
}
