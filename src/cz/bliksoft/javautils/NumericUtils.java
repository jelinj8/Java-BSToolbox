package cz.bliksoft.javautils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class NumericUtils {

	private NumericUtils() {
	}

	public static Double roundToNonzeroDecimals(Double value, int decimals) {
		return roundToBigDecimal(value, decimals).doubleValue();
	}

	public static BigDecimal roundToBigDecimal(Double value, int decimals) {
		return roundToBigDecimal(BigDecimal.valueOf(value), decimals);
	}

	public static BigDecimal roundToBigDecimal(BigDecimal value, int decimals) {
//		BigDecimal wholeNumber = value.round(new MathContext(0, RoundingMode.DOWN));
		BigDecimal wholeNumber = value.setScale(0, RoundingMode.DOWN);
		BigDecimal decimalPart = value.subtract(wholeNumber);
		return wholeNumber.add(decimalPart.round(new MathContext(decimals)));
	}

}
