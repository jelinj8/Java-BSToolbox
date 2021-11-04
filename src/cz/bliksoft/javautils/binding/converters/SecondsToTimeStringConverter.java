package cz.bliksoft.javautils.binding.converters;

import java.text.MessageFormat;

import cz.bliksoft.javautils.binding.interfaces.IValueModel;
import cz.bliksoft.javautils.binding.models.BindingConverter;
import cz.bliksoft.javautils.binding.models.ConverterValueModel;

/**
 * konvertor pro převod mezi java.util.Date a String
 * 
 * @author hroch
 * 
 */
public class SecondsToTimeStringConverter implements BindingConverter<Integer, String> {

	private SecondsToTimeStringConverter() {

	}

	private static SecondsToTimeStringConverter instance = null;

	public static SecondsToTimeStringConverter getDefault() {
		if (instance == null) {
			instance = new SecondsToTimeStringConverter();
		}
		return instance;
	}

	@Override
	public Integer sourceValue(String arg0) {
		return null;// Integer.valueOf(arg0.toString());
	}

	@Override
	public String targetValue(Integer arg0) {
		if (arg0 == null)
			return null;
//			return "-"; //$NON-NLS-1$
		int seconds = arg0 % 60;
		int minutes = (arg0 % 3600) - seconds;
		int hours = arg0 / 3600;
		if (seconds > 0) {
			return MessageFormat.format("{0}:{1,number,00}:{2,number,00}", hours, minutes, seconds);
		} else {
			return MessageFormat.format("{0}:{1,number,00}", hours, minutes);
		}
	}

	public static IValueModel<String> wrapAsString(IValueModel<Integer> source) {
		return new ConverterValueModel<>(source, getDefault());
	}
}