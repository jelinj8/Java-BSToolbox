package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Greater-than test: gt(a, b) returns 1.0 if a &gt; b, else 0.0. */
public class GtFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 2)
			throw new IllegalArgumentException("gt() requires exactly 2 arguments");
		return args[0] > args[1] ? 1.0 : 0.0;
	}
}
