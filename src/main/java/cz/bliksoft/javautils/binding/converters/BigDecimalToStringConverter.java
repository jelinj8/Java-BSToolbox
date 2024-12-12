package cz.bliksoft.javautils.binding.converters;

import java.math.BigDecimal;

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
public class BigDecimalToStringConverter implements IBindingConverter<BigDecimal, String> {

	private BigDecimalToStringConverter() {

	}

	private static BigDecimalToStringConverter instance = null;

	public static BigDecimalToStringConverter getDefault() {
		if (instance == null) {
			instance = new BigDecimalToStringConverter();
		}
		return instance;
	}

	@Override
	public BigDecimal sourceValue(String stringNumber) {
		if (StringUtils.isEmpty(stringNumber))
			return null;
		try {
			return new BigDecimal(stringNumber);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public String targetValue(BigDecimal number) {
		if (number == null)
			return "";
		return number.toPlainString();
	}

	public static IValueModel<String> wrapAsString(IValueModel<BigDecimal> source) {
		return new ConverterValueModel<>(source, getDefault());
	}
}