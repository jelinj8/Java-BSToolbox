package cz.bliksoft.javautils.binding.models;

/*
 * Copyright (c) 2002-2015 JGoodies Software GmbH. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of JGoodies Software GmbH nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static cz.bliksoft.javautils.Preconditions.checkArgument;
import static cz.bliksoft.javautils.Preconditions.checkNotNull;

import java.text.Format;
import java.text.ParseException;

import cz.bliksoft.javautils.binding.interfaces.IBindingConverter;
import cz.bliksoft.javautils.binding.interfaces.IValueModel;

/**
 * A factory that vends ValueModels that convert types, for example Dates to
 * Strings. More formally, a converting ValueModel <i>VM1</i> converts the type
 * <i>T2</i> of an object being held as a value in one ValueModel <i>VM2</i>
 * into another type <i>T1</i>. When reading a value from VM1, instances of T2
 * are read from VM2 and are converted to T1. When storing a new value to VM1,
 * the type converter will perform the inverse conversion and will convert an
 * instance of T1 to T2.
 * <p>
 *
 * Type converters should be used judiciously. To bind non-Strings to a text UI
 * component you should favor to use a {@link javax.swing.JFormattedTextField}.
 * They provide a powerful means to convert Strings to objects and handle many
 * cases that arise around invalid input.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.12 $
 *
 * @see ValueModel
 * @see Format
 * @see javax.swing.JFormattedTextField
 */
public final class ConverterFactory {

	private ConverterFactory() {
		// Overrides default constructor; prevents instantiation.
	}

	// Factory Methods ********************************************************

	/**
	 * Creates and returns a ValueModel that negates Booleans and leaves
	 * <code>null</code> unchanged.
	 *
	 * @param booleanSource a Boolean ValueModel
	 * @return a ValueModel that inverts Booleans
	 *
	 * @throws NullPointerException if the {@code booleanSource} is {@code null}
	 */
	public static IValueModel<Boolean> createBooleanNegator(IValueModel<Boolean> booleanSource) {
		return new ConverterValueModel<>(booleanSource, new BooleanNegator());
	}

	/**
	 * Creates and returns a ValueModel that converts Booleans to the associated of
	 * the two specified strings, and vice versa. {@code null} is mapped to an empty
	 * string. Ignores cases when setting a text.
	 *
	 * @param booleanSubject a Boolean ValueModel
	 * @param trueText       the text associated with {@code Boolean.TRUE}
	 * @param falseText      the text associated with {@code Boolean.FALSE}
	 *
	 * @return a ValueModel that converts boolean to the associated text
	 *
	 * @throws NullPointerException     if {@code booleanSource}, {@code trueText},
	 *                                  {@code falseText} or {@code nullText} is
	 *                                  {@code null}
	 * @throws IllegalArgumentException if {@code trueText} equals {@code falseText}
	 */
	public static IValueModel<String> createBooleanToStringConverter(IValueModel<Boolean> booleanSubject,
			String trueText, String falseText) {
		return createBooleanToStringConverter(booleanSubject, trueText, falseText, "");
	}

	/**
	 * Creates and returns a ValueModel that converts Booleans to the associated of
	 * the two specified strings, and vice versa. {@code null} is mapped to the
	 * specified text. Ignores cases when setting a text.
	 *
	 * @param booleanSource a Boolean ValueModel
	 * @param trueText      the text associated with {@code Boolean.TRUE}
	 * @param falseText     the text associated with {@code Boolean.FALSE}
	 * @param nullText      the text associated with {@code null}
	 *
	 * @return a ValueModel that converts boolean to the associated text
	 *
	 * @throws NullPointerException     if {@code booleanSource}, {@code trueText},
	 *                                  {@code falseText} or {@code nullText} is
	 *                                  {@code null}
	 * @throws IllegalArgumentException if {@code trueText} equals {@code falseText}
	 */
	public static IValueModel<String> createBooleanToStringConverter(IValueModel<Boolean> booleanSource,
			String trueText, String falseText, String nullText) {
		return new ConverterValueModel<>(booleanSource, new BooleanToStringConverter(trueText, falseText, nullText));
	}

	/**
	 * Creates and returns a ValueModel that converts Doubles using the specified
	 * multiplier.
	 * <p>
	 *
	 * <strong>Examples:</strong> multiplier=100, Double(1.23) -&gt; Double(123),
	 * multiplier=1000, Double(1.23) -&gt; Double(1230)
	 *
	 * @param doubleSource a Double ValueModel
	 * @param multiplier   the multiplier used for the conversion
	 *
	 * @return a ValueModel that converts Doubles using the specified multiplier
	 *
	 * @throws NullPointerException if {@code doubleSource} is {@code null}
	 *
	 * @since 1.0.2
	 */
	public static IValueModel<Double> createDoubleConverter(IValueModel<Double> doubleSource, double multiplier) {
		return new ConverterValueModel<>(doubleSource, new DoubleConverter(multiplier));
	}

	/**
	 * Creates and returns a ValueModel that converts Doubles to Integer, and vice
	 * versa.
	 *
	 * @param doubleSource a Double ValueModel
	 *
	 * @return a ValueModel that converts Doubles to Integer
	 *
	 * @throws NullPointerException if {@code doubleSource} is {@code null}
	 */
	public static IValueModel<Integer> createDoubleToIntegerConverter(IValueModel<Double> doubleSource) {
		return createDoubleToIntegerConverter(doubleSource, 1);
	}

	/**
	 * Creates and returns a ValueModel that converts Doubles to Integer, and vice
	 * versa. The multiplier can be used to convert Doubles to percent, permill,
	 * etc. For a percentage, set the multiplier to be 100, for a permill, set the
	 * multiplier to be 1000.
	 * <p>
	 *
	 * <strong>Examples:</strong> multiplier=100, Double(1.23) -&gt; Integer(123),
	 * multiplier=1000, Double(1.23) -&gt; Integer(1230)
	 *
	 * @param doubleSource a Double ValueModel
	 * @param multiplier   the multiplier used to convert the Double to Integer
	 *
	 * @return a ValueModel that converts Doubles to Integer
	 *
	 * @throws NullPointerException if {@code doubleSource} is {@code null}
	 */
	public static IValueModel<Integer> createDoubleToIntegerConverter(IValueModel<Double> doubleSource,
			int multiplier) {
		return new ConverterValueModel<>(doubleSource, new DoubleToIntegerConverter(multiplier));
	}

	/**
	 * Creates and returns a ValueModel that converts Floats using the specified
	 * multiplier.
	 * <p>
	 *
	 * <strong>Examples:</strong> multiplier=100, Float(1.23) -&gt; Float(123),
	 * multiplier=1000, Float(1.23) -&gt; Float(1230)
	 *
	 * @param floatSource a Float ValueModel
	 * @param multiplier  the multiplier used for the conversion
	 *
	 * @return a ValueModel that converts Float using the specified multiplier
	 *
	 * @throws NullPointerException if {@code floatSource} is {@code null}
	 *
	 * @since 1.0.2
	 */
	public static IValueModel<Float> createFloatConverter(IValueModel<Float> floatSource, float multiplier) {
		return new ConverterValueModel<>(floatSource, new FloatConverter(multiplier));
	}

	/**
	 * Creates and returns a ValueModel that converts Floats to Integer, and vice
	 * versa. s
	 * 
	 * @param floatSource a Float ValueModel
	 *
	 * @return a ValueModel that converts Floats to Integer
	 *
	 * @throws NullPointerException if {@code floatSource} is {@code null}
	 */
	public static IValueModel<Integer> createFloatToIntegerConverter(IValueModel<Float> floatSource) {
		return createFloatToIntegerConverter(floatSource, 1);
	}

	/**
	 * Creates and returns a ValueModel that converts Floats to Integer, and vice
	 * versa. The multiplier can be used to convert Floats to percent, permill, etc.
	 * For a percentage, set the multiplier to be 100, for a permill, set the
	 * multiplier to be 1000.
	 *
	 * @param floatSource a Float ValueModel
	 * @param multiplier  the multiplier used to convert the Float to Integer
	 *
	 * @return a ValueModel that converts Floats to Integer
	 *
	 * @throws NullPointerException if {@code floatSource} is {@code null}
	 */
	public static IValueModel<Integer> createFloatToIntegerConverter(IValueModel<Float> floatSource, int multiplier) {
		return new ConverterValueModel<>(floatSource, new FloatToIntegerConverter(multiplier));
	}

	/**
	 * Creates and returns a ValueModel that converts Integers using the specified
	 * multiplier.
	 * <p>
	 *
	 * Examples: multiplier=100, Integer(3) -&gt; Integer(300), multiplier=1000,
	 * Integer(3) -&gt; Integer(3000)
	 *
	 * @param integerSource a Integer ValueModel
	 * @param multiplier    the multiplier used for the conversion
	 *
	 * @return a ValueModel that converts Integers using the specified multiplier
	 *
	 * @throws NullPointerException if {@code integerSource} is {@code null}
	 *
	 * @since 1.0.2
	 */
	public static IValueModel<Integer> createIntegerConverter(IValueModel<Integer> integerSource, double multiplier) {
		return new ConverterValueModel<>(integerSource, new IntegerConverter(multiplier));
	}

	/**
	 * Creates and returns a ValueModel that converts Long using the specified
	 * multiplier.
	 * <p>
	 *
	 * Examples: multiplier=100, Long(3) -&gt; Long(300), multiplier=1000, Long(3)
	 * -&gt; Long(3000)
	 *
	 * @param longSource a Long ValueModel
	 * @param multiplier the multiplier used for the conversion
	 *
	 * @return a ValueModel that converts Longs using the specified multiplier
	 *
	 * @throws NullPointerException if {@code longSource} is {@code null}
	 *
	 * @since 1.0.2
	 */
	public static IValueModel<Long> createLongConverter(IValueModel<Long> longSource, double multiplier) {
		return new ConverterValueModel<>(longSource, new LongConverter(multiplier));
	}

	/**
	 * Creates and returns a ValueModel that converts Longs to Integer and vice
	 * versa.
	 * <p>
	 *
	 * <strong>Constraints:</strong> The subject is of type {@code Long}, values
	 * written to the converter are of type {@code Integer}.
	 *
	 * @param longSubject a Long ValueModel
	 * @return a ValueModel that converts Longs to Integer
	 *
	 * @throws NullPointerException if the subject is {@code null}
	 */
	public static IValueModel<Integer> createLongToIntegerConverter(IValueModel<Long> longSubject) {
		return createLongToIntegerConverter(longSubject, 1);
	}

	/**
	 * Creates and returns a ValueModel that converts Longs to Integer and vice
	 * versa.
	 * <p>
	 *
	 * <strong>Constraints:</strong> The subject is of type {@code Long}, values
	 * written to the converter are of type {@code Integer}.
	 *
	 * @param longSource a Long ValueModel
	 * @param multiplier used to multiply the Long when converting to Integer
	 * @return a ValueModel that converts Longs to Integer
	 *
	 * @throws NullPointerException if {@code longSource} is {@code null}
	 */
	public static IValueModel<Integer> createLongToIntegerConverter(IValueModel<Long> longSource, int multiplier) {
		return new ConverterValueModel<>(longSource, new LongToIntegerConverter(multiplier));
	}

	/**
	 * Creates and returns a ValueModel that converts objects to Strings and vice
	 * versa. The conversion is performed by a {@code Format}.
	 * <p>
	 *
	 * <strong>Constraints:</strong> The source is of type {@code Object}; it must
	 * be formattable and parsable via the given {@code Format}.
	 *
	 * @param source the underlying ValueModel.
	 * @param format the {@code Format} used to format and parse
	 *
	 * @return a ValueModel that converts objects to Strings and vice versa
	 *
	 * @throws NullPointerException if {@code source} or {@code format} is
	 *                              {@code null}
	 */
	public static IValueModel<String> createStringConverter(IValueModel<Object> source, Format format) {
		return new ConverterValueModel<>(source, new StringConverter(format));
	}

	public static <T> IValueModel<T> createObjectConverter(IValueModel<Object> source) {
		return new ConverterValueModel<>(source, new TypedObjectConverter<T>());
	}

	// Converter Implementations **********************************************

	/**
	 * cast value to target type
	 * @param <T>
	 */
	public static final class TypedObjectConverter<T> implements IBindingConverter<Object, T> {

		@SuppressWarnings("unchecked")
		@Override
		public T targetValue(Object sourceValue) {
			return (T) sourceValue;
		}

		@Override
		public Object sourceValue(T targetValue) {
			return targetValue;
		}
	}
	
	/**
	 * Negates Booleans leaving null unchanged. Maps Boolean.TRUE to Boolean.FALSE,
	 * Boolean.FALSE to Boolean.TRUE, and null to null.
	 */
	public static final class BooleanNegator implements IBindingConverter<Boolean, Boolean> {

		/**
		 * Returns the negated source Boolean leaving null unchanged.
		 */
		@Override
		public Boolean targetValue(Boolean sourceValue) {
			return negate(sourceValue);
		}

		/**
		 * Returns the negated target Boolean leaving null unchanged.
		 */
		@Override
		public Boolean sourceValue(Boolean targetValue) {
			return negate(targetValue);
		}

		/**
		 * Negates Booleans leaving null unchanged. Maps Boolean.TRUE to Boolean.FALSE ,
		 * Boolean.FALSE to Boolean.TRUE, and null to null.
		 *
		 * @param value the value to invert
		 * @return the inverted Boolean value, or null if value is null
		 */
		private static Boolean negate(Boolean value) {
			return value == null ? null : Boolean.valueOf(!value.booleanValue());
		}

	}

	/**
	 * Converts Booleans to Strings and vice-versa using given texts for true,
	 * false, and null.
	 */
	public static final class BooleanToStringConverter implements IBindingConverter<Boolean, String> {

		private final String trueText;
		private final String falseText;
		private final String nullText;

		BooleanToStringConverter(String trueText, String falseText, String nullText) {
			this.trueText = checkNotNull(trueText, "The trueText must not be null.");
			this.falseText = checkNotNull(falseText, "The falseText must not be null.");
			this.nullText = checkNotNull(nullText, "The nullText must not be null.");
			checkArgument(!trueText.equals(falseText), "The trueText and falseText must be different.");
		}

		/**
		 * Returns the source value's associated text representation.
		 */
		@Override
		public String targetValue(Boolean sourceValue) {
			if (sourceValue == null) {
				return nullText;
			}
			return sourceValue.booleanValue() ? trueText : falseText;
		}

		/**
		 * Converts the given String and sets the associated Boolean as the subject's
		 * new value. In case the new value equals neither this class' trueText, nor the
		 * falseText, nor the nullText, an IllegalArgumentException is thrown.
		 *
		 * @throws IllegalArgumentException if the new value does neither match the
		 *                                  trueText nor the falseText nor the nullText
		 */
		@Override
		public Boolean sourceValue(String targetValue) {
			if (trueText.equalsIgnoreCase(targetValue)) {
				return Boolean.TRUE;
			} else if (falseText.equalsIgnoreCase(targetValue)) {
				return Boolean.FALSE;
			} else if (nullText.equalsIgnoreCase(targetValue)) {
				return null;
			} else {
				throw new IllegalArgumentException(
						"The new value must be one of: " + trueText + '/' + falseText + '/' + nullText);
			}
		}

	}

	/**
	 * Converts Doubles using a given multiplier.
	 */
	public static final class DoubleConverter implements IBindingConverter<Double, Double> {

		private final double multiplier;

		DoubleConverter(double multiplier) {
			this.multiplier = multiplier;
		}

		/**
		 * Converts the subject's value and returns a corresponding {@code Double} using
		 * the multiplier.
		 *
		 * @param sourceValue the subject's value
		 * @return the converted subjectValue
		 */
		@Override
		public Double targetValue(Double sourceValue) {
			return Double.valueOf(sourceValue.doubleValue() * multiplier);
		}

		/**
		 * Converts a {@code Double} using the multiplier and sets it as new value.
		 *
		 * @param targetValue the {@code Double} object that shall be converted
		 */
		@Override
		public Double sourceValue(Double targetValue) {
			return Double.valueOf(targetValue.doubleValue() / multiplier);
		}

	}

	/**
	 * Converts Doubles to Integers and vice-versa.
	 */
	public static final class DoubleToIntegerConverter implements IBindingConverter<Double, Integer> {

		private final int multiplier;

		DoubleToIntegerConverter(int multiplier) {
			this.multiplier = multiplier;
		}

		/**
		 * Converts the source value and returns a corresponding {@code Integer} value
		 * using the multiplier.
		 *
		 * @param sourceValue the subject's value
		 * @return the converted subjectValue
		 */
		@Override
		public Integer targetValue(Double sourceValue) {
			double doubleValue = sourceValue.doubleValue();
			if (multiplier != 1) {
				doubleValue *= multiplier;
			}
			return Integer.valueOf((int) Math.round(doubleValue));
		}

		/**
		 * Converts a {@code Double} using the multiplier and sets it as new value.
		 *
		 * @param targetValue the {@code Integer} object that shall be converted
		 */
		@Override
		public Double sourceValue(Integer targetValue) {
			double doubleValue = targetValue.doubleValue();
			if (multiplier != 1) {
				doubleValue /= multiplier;
			}
			return Double.valueOf(doubleValue);
		}

	}

	/**
	 * Converts Floats using a given multiplier.
	 */
	public static final class FloatConverter implements IBindingConverter<Float, Float> {

		private final float multiplier;

		FloatConverter(float multiplier) {
			this.multiplier = multiplier;
		}

		/**
		 * Converts the subject's value and returns a corresponding {@code Float} using
		 * the multiplier.
		 *
		 * @param sourceValue the subject's value
		 * @return the converted subjectValue
		 */
		@Override
		public Float targetValue(Float sourceValue) {
			float floatValue = sourceValue.floatValue();
			return Float.valueOf(floatValue * multiplier);
		}

		/**
		 * Converts a {@code Float} using the multiplier and sets it as new value.
		 *
		 * @param targetValue the {@code Float} object that shall be converted
		 */
		@Override
		public Float sourceValue(Float targetValue) {
			float floatValue = targetValue.floatValue();
			return Float.valueOf(floatValue / multiplier);
		}

	}

	/**
	 * Converts Floats to Integers and vice-versa.
	 */
	public static final class FloatToIntegerConverter implements IBindingConverter<Float, Integer> {

		private final int multiplier;

		FloatToIntegerConverter(int multiplier) {
			this.multiplier = multiplier;
		}

		/**
		 * @return an Integer that is the Float source value multiplied with this
		 *         converter's multiplier
		 */
		@Override
		public Integer targetValue(Float sourceValue) {
			float floatValue = sourceValue.floatValue();
			if (multiplier != 1) {
				floatValue *= multiplier;
			}
			return Integer.valueOf(Math.round(floatValue));
		}

		/**
		 * Converts a {@code Float} using the multiplier.
		 */
		@Override
		public Float sourceValue(Integer sourceValue) {
			float floatValue = sourceValue.floatValue();
			if (multiplier != 1) {
				floatValue /= multiplier;
			}
			return Float.valueOf(floatValue);
		}

	}

	/**
	 * Converts Longs using a given multiplier.
	 */
	public static final class LongConverter implements IBindingConverter<Long, Long> {

		private final double multiplier;

		LongConverter(double multiplier) {
			this.multiplier = multiplier;
		}

		/**
		 * Converts the subject's value and returns a corresponding {@code Long} using
		 * the multiplier.
		 */
		@Override
		public Long targetValue(Long sourceValue) {
			double doubleValue = sourceValue.doubleValue();
			return Long.valueOf((long) (doubleValue * multiplier));
		}

		/**
		 * Converts a {@code Long} using the multiplier and sets it as new value.
		 */
		@Override
		public Long sourceValue(Long targetValue) {
			double doubleValue = targetValue.doubleValue();
			return Long.valueOf((long) (doubleValue / multiplier));
		}

	}

	/**
	 * Converts Integers using a given multiplier.
	 */
	public static final class IntegerConverter implements IBindingConverter<Integer, Integer> {

		private final double multiplier;

		IntegerConverter(double multiplier) {
			this.multiplier = multiplier;
		}

		/**
		 * Returns an Integer that is the source Integer multiplied with this
		 * converters' multiplier.
		 */
		@Override
		public Integer targetValue(Integer sourceValue) {
			double doubleValue = sourceValue.doubleValue();
			return Integer.valueOf((int) (doubleValue * multiplier));
		}

		/**
		 * Returns an Integer that is the target Integer divided by this converters'
		 * multiplier.
		 */
		@Override
		public Integer sourceValue(Integer targetValue) {
			double doubleValue = targetValue.doubleValue();
			return Integer.valueOf((int) (doubleValue / multiplier));
		}

	}

	/**
	 * Converts Longs to Integers and vice-versa.
	 */
	public static final class LongToIntegerConverter implements IBindingConverter<Long, Integer> {

		private final int multiplier;

		LongToIntegerConverter(int multiplier) {
			this.multiplier = multiplier;
		}

		/**
		 * Returns an Integer that is the source Long multiplied with this converter's
		 * multiplier.
		 */
		@Override
		public Integer targetValue(Long sourceValue) {
			int intValue = sourceValue.intValue();
			if (multiplier != 1) {
				intValue *= multiplier;
			}
			return Integer.valueOf(intValue);
		}

		/**
		 * Returns a Long that is the target Integer divided by this converter's
		 * multiplier.
		 *
		 * @param targetValue the {@code Integer} object that represents the percent
		 *                    value
		 */
		@Override
		public Long sourceValue(Integer targetValue) {
			long longValue = targetValue.longValue();
			if (multiplier != 1) {
				longValue /= multiplier;
			}
			return Long.valueOf(longValue);
		}

	}

	/**
	 * Converts Values to Strings and vice-versa using a given Format.
	 */
	public static final class StringConverter implements IBindingConverter<Object, String> {

		/**
		 * Holds the {@code Format} used to format and parse.
		 */
		private final Format format;

		// Instance Creation **************************************************

		/**
		 * Constructs a {@code StringConverter} using the specified {@code Format}.
		 *
		 * @param format the {@code Format} used to format and parse
		 * @throws NullPointerException if the format is {@code null}.
		 */
		StringConverter(Format format) {
			this.format = checkNotNull(format, "The format must not be null.");
		}

		// Implementing Abstract Behavior *************************************

		/**
		 * Formats the source value and returns a String representation.
		 *
		 * @param sourceValue the source value
		 * @return the formatted sourceValue
		 */
		@Override
		public String targetValue(Object sourceValue) {
			return format.format(sourceValue);
		}

		/**
		 * Parses the given String encoding and returns the parsed object.
		 * {@code ParseException}s are re-thrown as IllegalArgumentException.
		 *
		 * @param targetValue the value to be converted and set as new subject value
		 */
		@Override
		public Object sourceValue(String targetValue) {
			try {
				return format.parseObject(targetValue);
			} catch (ParseException e) {
				throw new IllegalArgumentException("Cannot parse the target value " + targetValue, e);
			}
		}

	}

}
