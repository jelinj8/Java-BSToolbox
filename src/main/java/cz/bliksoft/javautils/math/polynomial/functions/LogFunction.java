package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/**
 * Base-10 logarithm (1 arg) or logarithm with custom base (2 args: value,
 * base).
 */
public class LogFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length == 1)
			return Math.log10(args[0]);
		if (args.length == 2)
			return Math.log(args[0]) / Math.log(args[1]);
		throw new IllegalArgumentException("log() requires 1 or 2 arguments");
	}

}
