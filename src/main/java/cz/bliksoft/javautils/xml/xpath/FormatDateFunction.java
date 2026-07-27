package cz.bliksoft.javautils.xml.xpath;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import cz.bliksoft.javautils.xml.XmlUtils;

/**
 * {@code formatDate(value, pattern)} - formats a date/time value with a
 * {@link DateTimeFormatter} pattern.
 *
 * <p>
 * The value may be a {@link TemporalAccessor}, a {@link Date}, a number
 * (interpreted as epoch milliseconds, e.g. the result of {@code now()}), or a
 * string parseable as ISO zoned/local date-time, local date, local time,
 * instant, or epoch milliseconds. Returns an empty string for a null or empty
 * value.
 */
public class FormatDateFunction implements XPathFunction {

	@Override
	public Object evaluate(@SuppressWarnings("rawtypes") List args) throws XPathFunctionException {
		if (args.size() < 2)
			throw new XPathFunctionException("formatDate requires 2 arguments: value, pattern");
		try {
			Object raw = args.get(0);
			String pattern = XmlUtils.getResultText(args.get(1));
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
			TemporalAccessor temporal = toTemporal(raw);
			if (temporal == null)
				return "";
			return formatter.format(temporal);
		} catch (Exception e) {
			throw new XPathFunctionException("formatDate failed: " + e.getMessage());
		}
	}

	private static TemporalAccessor toTemporal(Object raw) throws Exception {
		if (raw == null)
			return null;
		if (raw instanceof TemporalAccessor)
			return (TemporalAccessor) raw;
		if (raw instanceof Date)
			return ((Date) raw).toInstant().atZone(ZoneId.systemDefault());
		// a numeric value (e.g. now(), or a Long extracted in a flow) is epoch millis
		if (raw instanceof Number)
			return Instant.ofEpochMilli(((Number) raw).longValue()).atZone(ZoneId.systemDefault());
		String s = raw instanceof String ? (String) raw : XmlUtils.getResultText(raw);
		if (s == null || s.isEmpty())
			return null;

		try {
			return ZonedDateTime.parse(s);
		} catch (Exception ignored) {
		}
		try {
			return LocalDateTime.parse(s);
		} catch (Exception ignored) {
		}
		try {
			return LocalDate.parse(s);
		} catch (Exception ignored) {
		}
		try {
			return LocalTime.parse(s);
		} catch (Exception ignored) {
		}
		try {
			return Instant.parse(s).atZone(ZoneId.systemDefault());
		} catch (Exception ignored) {
		}
		try {
			long millis = Long.parseLong(s);
			return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
		} catch (Exception ignored) {
		}
		return null;
	}
}
