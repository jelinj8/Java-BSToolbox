package cz.bliksoft.javautils.binding.converters;

import java.time.LocalDateTime;

import cz.bliksoft.javautils.DateUtils;
import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.binding.interfaces.IBindingConverter;
import cz.bliksoft.javautils.binding.interfaces.IValueModel;
import cz.bliksoft.javautils.binding.models.ConverterValueModel;

public class LocalDateTimeToTimestampStringConverter implements IBindingConverter<LocalDateTime, String> {

	private LocalDateTimeToTimestampStringConverter() {

	}

	private static LocalDateTimeToTimestampStringConverter instance = null;

	public static LocalDateTimeToTimestampStringConverter getDefault() {
		if (instance == null) {
			instance = new LocalDateTimeToTimestampStringConverter();
		}
		return instance;
	}

	@Override
	public String targetValue(LocalDateTime sourceValue) {
		if (sourceValue == null)
			return null;
		return DateUtils.LocalDateTimeToString(sourceValue);
	}

	@Override
	public LocalDateTime sourceValue(String targetValue) {
		if (StringUtils.isEmpty(targetValue))
			return null;
		return DateUtils.LocalDateTimeFromString(targetValue);
	}

	public static IValueModel<String> wrapAsString(IValueModel<LocalDateTime> source) {
		return new ConverterValueModel<>(source, getDefault());
	}
}
