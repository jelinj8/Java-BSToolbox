package cz.bliksoft.javautils;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateUtils {
	public static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS"); //$NON-NLS-1$
	public static SimpleDateFormat ISO8824TimestampFormat = new SimpleDateFormat("yyyyMMddHHmmss"); //$NON-NLS-1$
	
	public static DateTimeFormatter JSONTimestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	public static DateTimeFormatter dTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"); //$NON-NLS-1$

	public static LocalDateTime LocalDateTimeFromString(String value) {
		return LocalDateTime.parse(value, dTF);
	}

	public static String LocalDateTimeToString(LocalDateTime td) {
		return dTF.format(td);
	}

	public static String TimestampString() {
		return timestampFormat.format(new Date());
	}

	public static String XMLTimestampString() {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
	}

	/**
	 * dd.MM.yyyy HH:mm:ss
	 * 
	 * @param value
	 * @return
	 */
	public static String ZonedToLocalDateTimeString(String value) {
		ZonedDateTime ldt = ZonedDateTime.parse(value, dTF);
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ldt);
	}

	/*
	 * dd.MM.yyyy
	 * 
	 * @param value
	 * @return
	 */
	/*
	 * public static String XMLDateString(String value) { Date ld = new Date(value);
	 * return DateTimeFormatter.ISO_DATE.format(ldt); } else return null; }
	 */

	public static String ISO8824Timestamp() {
		return ISO8824TimestampFormat.format(new Date());
	}

	public static long millis() {
		return (new Date()).getTime();
	}

	public static String millisTimestampString(long millis) {
		return timestampFormat.format(new Date(millis));
	}

	public static String millisIntervalString(long millis) {
		long ts = Math.abs(millis);
		final long days = TimeUnit.MILLISECONDS.toDays(ts);
		ts %= 3600000 * 24;
		final long hr = TimeUnit.MILLISECONDS.toHours(ts);
		ts %= 3600000;
		final long min = TimeUnit.MILLISECONDS.toMinutes(ts);
		ts %= 60000;
		final long sec = TimeUnit.MILLISECONDS.toSeconds(ts);
		ts %= 1000;
		return (days > 0 ? days + "d " : "") + String.format("%02d:%02d:%02d.%03d", hr, min, sec, ts); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static Date date(int year, int month, int day) {
		return date(year, month, day, null);
	}

	public static Date date(int year, int month, int day, TimeZone tz) {
		Calendar working = GregorianCalendar.getInstance();
		working.clear();
		if (tz != null)
			working.setTimeZone(tz);
		working.set(year, month - 1, day, 0, 0, 0);
		return working.getTime();
	}

	public static Date date(int year, int month, int day, int hh, int mm, int ss) {
		return date(year, month, day, hh, mm, ss, null);
	}

	public static Date date(int year, int month, int day, int hh, int mm, int ss, TimeZone tz) {
		Calendar working = GregorianCalendar.getInstance();
		working.clear();
		if (tz != null)
			working.setTimeZone(tz);
		working.set(year, month - 1, day, hh, mm, ss);
		return working.getTime();
	}

	public static double toDecimalSeconds(Duration d) {
		return d.getSeconds() + d.getNano() / 1_000_000_000.0;
	}
	
}
