package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Linear interpolation: lerp(a, b, t) = a + (b - a) * t. */
public class LerpFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 3)
			throw new IllegalArgumentException("lerp() requires exactly 3 arguments");
		return args[0] + (args[1] - args[0]) * args[2];
	}

}
