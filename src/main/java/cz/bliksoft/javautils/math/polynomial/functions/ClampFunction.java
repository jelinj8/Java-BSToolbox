package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

/** Clamps a value to [min, max]: clamp(value, min, max). */
public class ClampFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length != 3)
			throw new IllegalArgumentException("clamp() requires exactly 3 arguments");
		return Math.max(args[1], Math.min(args[2], args[0]));
	}

}
