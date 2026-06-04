package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Returns -1.0, 0.0, or 1.0 depending on the sign of the argument. */
public class SignFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("sign() requires exactly 1 argument");
		return Math.signum(args[0]);
	}

}
