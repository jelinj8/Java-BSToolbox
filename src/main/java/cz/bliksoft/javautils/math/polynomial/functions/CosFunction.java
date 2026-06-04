package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Cosine of the argument (radians). */
public class CosFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("cos() requires exactly 1 argument");
		return Math.cos(args[0]);
	}

}
