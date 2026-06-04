package cz.bliksoft.javautils.math.polynomial;

/**
 * A named function usable inside a {@link PolynomialEvaluator} expression.
 * <p>
 * Implementations should validate the argument count and throw
 * {@link IllegalArgumentException} for wrong arity.
 */
@FunctionalInterface
public interface PolynomialFunction {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param args evaluated argument values passed from the expression
	 * @return the function result
	 * @throws IllegalArgumentException if the argument count is not accepted
	 */
	double apply(double... args);

}
