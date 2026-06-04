package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

public class MeanFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length < 1)
			throw new IllegalArgumentException("mean/avg() requires at least 1 argument");
		double sum = 0;
		for (double arg : args)
			sum += arg;
		return sum / args.length;
	}

}
