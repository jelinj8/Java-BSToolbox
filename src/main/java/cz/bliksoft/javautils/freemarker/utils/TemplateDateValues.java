package cz.bliksoft.javautils.freemarker.utils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Values of the {@code DATE}/{@code DATETIME} template parameters: an ISO value
 * or a date expression, evaluated at the time it is read:
 *
 * <pre>
 * expr   := base offset*
 * base   := today [('T'|' ') time] | now | yyyy-MM-dd [('T'|' ') time]     time := H[H]:mm[:ss]
 * offset := ('+'|'-') digits [d|w|M|y|h|m|s]                              (no unit = d)
 * </pre>
 *
 * Keywords ignore case, units do not ({@code M} month, {@code m} minute).
 * {@code today} is midnight, {@code now} the current time (whole seconds);
 * offsets apply left to right, e.g. {@code today+7}, {@code today-1M},
 * {@code now-1h}, {@code now+1d-2h}, {@code today 08:00+1d}. A {@code DATE}
 * takes the date part of the result. A {@code min..max} range (either side
 * optional) limits a parameter's values ({@link #parseRange(String)}).
 */
public final class TemplateDateValues {

	/** {@code 2026-10-02} */
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
	/**
	 * {@code 2026-10-02T14:30} (seconds when not zero) - the value format of an
	 * HTML {@code datetime-local} input.
	 */
	public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm",
			Locale.ROOT);
	private static final DateTimeFormatter DATETIME_SECONDS_FORMAT = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);

	/** Separator of a {@code min..max} range. */
	public static final String RANGE_SEPARATOR = "..";

	private static final Pattern EXPRESSION = Pattern.compile(
			"^\\s*(?<base>(?i:today|now)|\\d{4}-\\d{2}-\\d{2})(?:[T ]\\s*(?<time>\\d{1,2}:\\d{2}(?::\\d{2})?))?(?<offsets>(?:\\s*[+-]\\s*\\d+\\s*[dwMyhms]?)*)\\s*$");
	private static final Pattern OFFSET = Pattern.compile("([+-])\\s*(\\d+)\\s*([dwMyhms]?)");

	private static final Logger log = Logger.getLogger(TemplateDateValues.class.getName());

	private TemplateDateValues() {
	}

	/**
	 * Evaluates a date expression; {@code null} when blank.
	 *
	 * @throws IllegalArgumentException when not a valid expression
	 */
	public static LocalDateTime evaluate(String expression, Clock clock) {
		if (expression == null || expression.trim().isEmpty())
			return null;
		Matcher m = EXPRESSION.matcher(expression);
		if (!m.matches())
			throw new IllegalArgumentException("Not a date expression: " + expression);
		String base = m.group("base");
		String time = m.group("time");
		LocalDateTime result;
		try {
			if ("now".equalsIgnoreCase(base)) {
				if (time != null)
					throw new IllegalArgumentException("'now' takes no time: " + expression);
				result = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
			} else {
				LocalDate date = "today".equalsIgnoreCase(base) ? LocalDate.now(clock) : LocalDate.parse(base);
				result = time == null ? date.atStartOfDay() : date.atTime(parseTime(time));
			}
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Not a date expression: " + expression, e);
		}
		Matcher o = OFFSET.matcher(m.group("offsets"));
		while (o.find()) {
			long amount = Long.parseLong(o.group(2));
			if ("-".equals(o.group(1)))
				amount = -amount;
			result = plus(result, amount, o.group(3));
		}
		return result;
	}

	public static LocalDateTime evaluate(String expression) {
		return evaluate(expression, Clock.systemDefaultZone());
	}

	private static LocalTime parseTime(String time) {
		String[] parts = time.split(":");
		int hour = Integer.parseInt(parts[0]);
		int minute = Integer.parseInt(parts[1]);
		int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
		try {
			return LocalTime.of(hour, minute, second);
		} catch (java.time.DateTimeException e) {
			throw new DateTimeParseException(e.getMessage(), time, 0, e);
		}
	}

	private static LocalDateTime plus(LocalDateTime value, long amount, String unit) {
		switch (unit) {
		case "w":
			return value.plusWeeks(amount);
		case "M":
			return value.plusMonths(amount);
		case "y":
			return value.plusYears(amount);
		case "h":
			return value.plusHours(amount);
		case "m":
			return value.plusMinutes(amount);
		case "s":
			return value.plusSeconds(amount);
		default:
			return value.plusDays(amount);
		}
	}

	/**
	 * A {@code DATE} value: an expression's date part; {@code null} when blank.
	 *
	 * @throws IllegalArgumentException when not a valid expression
	 */
	public static LocalDate parseDate(String expression, Clock clock) {
		LocalDateTime value = evaluate(expression, clock);
		return value != null ? value.toLocalDate() : null;
	}

	public static LocalDate parseDate(String expression) {
		return parseDate(expression, Clock.systemDefaultZone());
	}

	/**
	 * A {@code DATETIME} value; {@code null} when blank.
	 *
	 * @throws IllegalArgumentException when not a valid expression
	 */
	public static LocalDateTime parseDateTime(String expression, Clock clock) {
		return evaluate(expression, clock);
	}

	public static LocalDateTime parseDateTime(String expression) {
		return evaluate(expression, Clock.systemDefaultZone());
	}

	/** {@link #parseDate(String)}, {@code null} when blank or invalid. */
	public static LocalDate tryParseDate(String expression) {
		try {
			return parseDate(expression);
		} catch (IllegalArgumentException e) {
			log.log(Level.FINE, e.getMessage());
			return null;
		}
	}

	/** {@link #parseDateTime(String)}, {@code null} when blank or invalid. */
	public static LocalDateTime tryParseDateTime(String expression) {
		try {
			return parseDateTime(expression);
		} catch (IllegalArgumentException e) {
			log.log(Level.FINE, e.getMessage());
			return null;
		}
	}

	/** ISO date ({@link #DATE_FORMAT}), {@code null} for {@code null}. */
	public static String formatDate(LocalDate date) {
		return date != null ? DATE_FORMAT.format(date) : null;
	}

	/**
	 * {@link #DATETIME_FORMAT} ({@code yyyy-MM-ddTHH:mm}, seconds only when not
	 * zero), {@code null} for {@code null}.
	 */
	public static String formatDateTime(LocalDateTime dateTime) {
		if (dateTime == null)
			return null;
		return (dateTime.getSecond() != 0 ? DATETIME_SECONDS_FORMAT : DATETIME_FORMAT).format(dateTime);
	}

	/**
	 * A date-like value as a {@code LocalDate}: {@code java.time} values,
	 * {@link Date} (incl. {@code java.sql} types), {@link Calendar},
	 * {@link Instant}, a number (epoch milliseconds) or a date expression (any
	 * other value's {@code toString()}); instants in the system time zone.
	 * {@code null} when {@code null} or not convertible.
	 */
	public static LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate)
			return (LocalDate) value;
		if (value instanceof java.sql.Date)
			return ((java.sql.Date) value).toLocalDate();
		LocalDateTime dateTime = toLocalDateTime(value);
		return dateTime != null ? dateTime.toLocalDate() : null;
	}

	/**
	 * A date-like value as a {@code LocalDateTime} (see
	 * {@link #toLocalDate(Object)}; a {@code LocalDate} is its midnight).
	 */
	public static LocalDateTime toLocalDateTime(Object value) {
		if (value == null)
			return null;
		if (value instanceof LocalDateTime)
			return (LocalDateTime) value;
		if (value instanceof LocalDate)
			return ((LocalDate) value).atStartOfDay();
		if (value instanceof java.sql.Date)
			return ((java.sql.Date) value).toLocalDate().atStartOfDay();
		if (value instanceof java.sql.Timestamp)
			return ((java.sql.Timestamp) value).toLocalDateTime();
		if (value instanceof ZonedDateTime)
			return ((ZonedDateTime) value).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
		if (value instanceof OffsetDateTime)
			return ((OffsetDateTime) value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
		if (value instanceof Instant)
			return LocalDateTime.ofInstant((Instant) value, ZoneId.systemDefault());
		if (value instanceof Date)
			return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault());
		if (value instanceof Calendar)
			return LocalDateTime.ofInstant(((Calendar) value).toInstant(), ZoneId.systemDefault());
		if (value instanceof Number)
			return LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) value).longValue()), ZoneId.systemDefault());
		return tryParseDateTime(value.toString());
	}

	/**
	 * A {@code min..max} range of a date parameter (the parameters part), both
	 * sides date expressions, either may be empty; {@code {min, max}}, entries
	 * {@code null} when not limited. No range ({@code null}) for {@code null} or
	 * text without {@value #RANGE_SEPARATOR}; an invalid side is not limited.
	 */
	public static LocalDateTime[] parseRange(String parameters, Clock clock) {
		if (parameters == null)
			return null;
		int sep = parameters.indexOf(RANGE_SEPARATOR);
		if (sep < 0)
			return null;
		return new LocalDateTime[] { tryEvaluate(parameters.substring(0, sep), clock),
				tryEvaluate(parameters.substring(sep + RANGE_SEPARATOR.length()), clock) };
	}

	public static LocalDateTime[] parseRange(String parameters) {
		return parseRange(parameters, Clock.systemDefaultZone());
	}

	private static LocalDateTime tryEvaluate(String expression, Clock clock) {
		try {
			return evaluate(expression, clock);
		} catch (IllegalArgumentException e) {
			log.log(Level.WARNING, "Invalid date range bound: " + e.getMessage());
			return null;
		}
	}
}
