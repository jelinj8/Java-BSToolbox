package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Less-than test: lt(a, b) returns 1.0 if a &lt; b, else 0.0. */
public class LtFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 2)
			throw new IllegalArgumentException("lt() requires exactly 2 arguments");
		return args[0] < args[1] ? 1.0 : 0.0;
	}
}
