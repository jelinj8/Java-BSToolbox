package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Equality test with tolerance 1e-9: eq(a, b) returns 1.0 or 0.0. */
public class EqFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 2)
			throw new IllegalArgumentException("eq() requires exactly 2 arguments");
		return Math.abs(args[0] - args[1]) < 1e-9 ? 1.0 : 0.0;
	}
}
