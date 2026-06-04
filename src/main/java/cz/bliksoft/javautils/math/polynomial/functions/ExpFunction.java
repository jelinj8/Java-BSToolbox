package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Euler's number e raised to the given power. */
public class ExpFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("exp() requires exactly 1 argument");
		return Math.exp(args[0]);
	}

}
