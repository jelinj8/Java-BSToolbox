package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

public class GteFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 2)
			throw new IllegalArgumentException("gte() requires exactly 2 arguments");
		return args[0] >= args[1] ? 1.0 : 0.0;
	}
}
