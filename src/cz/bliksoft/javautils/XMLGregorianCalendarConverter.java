package cz.bliksoft.javautils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * Utility class for converting between XMLGregorianCalendar and java.util.Date
 */
public class XMLGregorianCalendarConverter {

	/**
	 * Needed to create XMLGregorianCalendar instances
	 */
	private static DatatypeFactory df = null;
	static {
		try {
			df = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException dce) {
			throw new IllegalStateException("Exception while obtaining DatatypeFactory instance", dce);
		}
	}

	/**
	 * Converts a java.util.Date into an instance of XMLGregorianCalendar
	 *
	 * @param date Instance of java.util.Date or a null reference
	 * @return XMLGregorianCalendar instance whose value is based upon the value in
	 *         the date parameter. If the date parameter is null then this method
	 *         will simply return null.
	 */
	public static XMLGregorianCalendar asXMLGregorianCalendar(java.util.Date date) {
		if (date == null) {
			return null;
		} else {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTimeInMillis(date.getTime());
			return df.newXMLGregorianCalendar(gc);
		}
	}

	public static XMLGregorianCalendar asXMLGregorianCalendar(long millis) {
		if (millis == 0) {
			return null;
		} else {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTimeInMillis(millis);
			return df.newXMLGregorianCalendar(gc);
		}
	}

	public static XMLGregorianCalendar asXMLGregorianCalendar(String date) throws ParseException {
		if (date == null) {
			return null;
		} else {
			GregorianCalendar gc = new GregorianCalendar();
			String format = null;
			switch (date.length()) {
			case 8:
				format = "yyyyMMdd";
				break;
			case 10:
				if (date.contains("-"))
					format = "yyyy-MM-dd";
				else
					format = "yyyy/MM/dd";
				break;
			case 15:
				format = "yyyyMMdd_hhmmss";
				break;
			case 19:
				if (date.contains("-"))
					format = "yyyy-MM-dd_hh:mm:ss";
				else
					format = "yyyy/MM/dd_hh:mm:ss";
				break;
			}
			DateFormat parser = new SimpleDateFormat(format);

			Date res = parser.parse(date);
			gc.setTimeInMillis(res.getTime());
			return df.newXMLGregorianCalendar(gc);
		}
	}

	private static TimeZone tzUtc = TimeZone.getTimeZone("UTC");
	private static Locale loc = Locale.getDefault();

	/**
	 * Converts an XMLGregorianCalendar to an instance of java.util.Date
	 *
	 * @param xgc Instance of XMLGregorianCalendar or a null reference
	 * @return java.util.Date instance whose value is based upon the value in the
	 *         xgc parameter. If the xgc parameter is null then this method will
	 *         simply return null.
	 */
	public static java.util.Date asDate(XMLGregorianCalendar xgc) {
		if (xgc == null) {
			return null;
		} else {
			GregorianCalendar gc = null;
			if (xgc.getTimezone() < 100)
				gc = xgc.toGregorianCalendar();
			else
				gc = xgc.toGregorianCalendar(tzUtc, loc, null);
			return gc.getTime();
		}
	}

	public static java.sql.Timestamp asTimestamp(XMLGregorianCalendar xgc) {
		if (xgc == null) {
			return null;
		} else {
			GregorianCalendar gc = null;
			if (xgc.getTimezone() < 100)
				gc = xgc.toGregorianCalendar();
			else
				gc = xgc.toGregorianCalendar(tzUtc, loc, null);
			return new java.sql.Timestamp(gc.getTimeInMillis());
		}
	}

	public static Long inMillis(XMLGregorianCalendar xgc) {
		if (xgc == null) {
			return null;
		} else {
			GregorianCalendar gc = null;
			if (xgc.getTimezone() < 100)
				gc = xgc.toGregorianCalendar();
			else
				gc = xgc.toGregorianCalendar(tzUtc, loc, null);

			return gc.getTimeInMillis();
			// return xgc.toGregorianCalendar().getTimeInMillis();
		}
	}

	public static DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	public static DateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	public static String asString(XMLGregorianCalendar xgc, DateFormat df) {
		if (xgc == null) {
			return null;
		} else {
			GregorianCalendar gc = null;
			if (xgc.getTimezone() < 100)
				gc = xgc.toGregorianCalendar();
			else
				gc = xgc.toGregorianCalendar(tzUtc, loc, null);

			Date datum = new Date(gc.getTimeInMillis());
			return df.format(datum);
		}
	}

	public static String asDateString(XMLGregorianCalendar xgc) {
		return asString(xgc, dateFormat);
	}

	public static String asDateTimeString(XMLGregorianCalendar xgc) {
		return asString(xgc, dateTimeFormat);
	}

	public static JAXBElement<XMLGregorianCalendar> asJaxbElement(String value) throws ParseException {
		return new JAXBElement<XMLGregorianCalendar>(new QName(XMLGregorianCalendar.class.getSimpleName()),
				XMLGregorianCalendar.class, asXMLGregorianCalendar(value));
	}

	public static JAXBElement<XMLGregorianCalendar> asJaxbElement(long value) {
		return new JAXBElement<XMLGregorianCalendar>(new QName(XMLGregorianCalendar.class.getSimpleName()),
				XMLGregorianCalendar.class, asXMLGregorianCalendar(value));
	}

	public static JAXBElement<XMLGregorianCalendar> asJaxbElement(Date value) {
		return new JAXBElement<XMLGregorianCalendar>(new QName(XMLGregorianCalendar.class.getSimpleName()),
				XMLGregorianCalendar.class, asXMLGregorianCalendar(value));
	}

	public static JAXBElement<XMLGregorianCalendar> asJaxbElement(XMLGregorianCalendar value) {
		return new JAXBElement<XMLGregorianCalendar>(new QName(XMLGregorianCalendar.class.getSimpleName()),
				XMLGregorianCalendar.class, value);
	}
}