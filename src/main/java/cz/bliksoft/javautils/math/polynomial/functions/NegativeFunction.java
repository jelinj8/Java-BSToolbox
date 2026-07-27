package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Sign test: negative(x) returns 1.0 if x &lt; 0, else 0.0. */
public class NegativeFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("negative() requires exactly 1 argument");
		return args[0] < 0 ? 1.0 : 0.0;
	}
}
