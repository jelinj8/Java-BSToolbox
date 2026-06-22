package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

public class PositiveFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("positive() requires exactly 1 argument");
		return args[0] > 0 ? 1.0 : 0.0;
	}
}
