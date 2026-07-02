package cz.bliksoft.javautils.math.polynomial.functions;

import cz.bliksoft.javautils.math.polynomial.PolynomialFunction;

public class RandomFunction implements PolynomialFunction {

	public double apply(double... args) {
		if (args.length == 0)
			return Math.random();
		if (args.length == 1)
			return Math.random() * args[0];
		if (args.length == 2)
			return args[0] + Math.random() * (args[1] - args[0]);
		throw new IllegalArgumentException("random() requires 0, 1, or 2 arguments");
	}

}
