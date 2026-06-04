package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/**
 * Raises base to the given exponent — function alias for the {@code ^}
 * operator.
 */
public class PowFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 2)
			throw new IllegalArgumentException("pow() requires exactly 2 arguments");
		return Math.pow(args[0], args[1]);
	}

}
