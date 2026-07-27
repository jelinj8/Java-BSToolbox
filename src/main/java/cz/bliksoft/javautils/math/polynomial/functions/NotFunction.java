package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Logical negation: not(x) returns 1.0 if x is exactly 0.0, else 0.0. */
public class NotFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("not() requires exactly 1 argument");
		return args[0] == 0.0 ? 1.0 : 0.0;
	}
}
