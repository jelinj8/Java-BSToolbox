package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Base-2 logarithm. */
public class Log2Function implements PolynomialFunction {

	private static final double LN2 = Math.log(2);

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("log2() requires exactly 1 argument");
		return Math.log(args[0]) / LN2;
	}

}
