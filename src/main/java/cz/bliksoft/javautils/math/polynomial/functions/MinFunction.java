package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

public class MinFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length < 1)
			throw new IllegalArgumentException("min() requires at least 1 argument");
		double result = args[0];
		for (int i = 1; i < args.length; i++)
			result = Math.min(result, args[i]);
		return result;
	}

}
