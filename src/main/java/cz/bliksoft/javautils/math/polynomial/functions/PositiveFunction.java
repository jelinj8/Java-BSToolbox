package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Sign test: positive(x) returns 1.0 if x &gt; 0, else 0.0. */
public class PositiveFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("positive() requires exactly 1 argument");
		return args[0] > 0 ? 1.0 : 0.0;
	}
}
