package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Less-or-equal test: lte(a, b) returns 1.0 if a &lt;= b, else 0.0. */
public class LteFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 2)
			throw new IllegalArgumentException("lte() requires exactly 2 arguments");
		return args[0] <= args[1] ? 1.0 : 0.0;
	}
}
