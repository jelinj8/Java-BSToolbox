package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Sine of the argument (radians). */
public class SinFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("sin() requires exactly 1 argument");
		return Math.sin(args[0]);
	}

}
