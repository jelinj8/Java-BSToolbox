package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Natural logarithm (base e). */
public class LnFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 1)
			throw new IllegalArgumentException("ln() requires exactly 1 argument");
		return Math.log(args[0]);
	}

}
