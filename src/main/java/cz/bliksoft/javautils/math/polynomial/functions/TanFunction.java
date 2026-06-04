package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Tangent of the argument (radians). */
public class TanFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("tan() requires exactly 1 argument");
		return Math.tan(args[0]);
	}

}
