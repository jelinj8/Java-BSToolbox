package cz.bliksoft.javautils;

public class BooleanUtils {
	public static boolean toBoolean(Object value) {
		if (value == null)
			return false;
		if (value instanceof Boolean)
			return (Boolean) value;
		if (value instanceof Integer)
			return (Integer) value != 0;
		if (value instanceof Long)
			return (Long) value != 0;
		if (value instanceof Number) {
			double d = ((Number) value).doubleValue();
			return Math.round(d) != 0;
		}
		String s = value.toString().trim();
		if (s.isEmpty())
			return false;
		if ("true".equalsIgnoreCase(s) || "1".equals(s))
			return true;
		if ("false".equalsIgnoreCase(s) || "0".equals(s))
			return false;
		try {
			double d = Double.parseDouble(s);
			return Math.round(d) != 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
