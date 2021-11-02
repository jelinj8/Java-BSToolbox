package cz.bliksoft.javautils.xml.adapters;

import java.sql.Timestamp;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {
	// public static DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	public static DateTimeFormatter localDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	@Override
	public LocalDate unmarshal(String v) throws Exception {
		return LocalDate.parse(v);
	}

	@Override
	public String marshal(LocalDate v) throws Exception {
		if (v != null) {
			return v.toString();
		} else {
			return null;
		}
	}

	public static LocalDate toLocalDate(Timestamp ts) {
		if (ts == null)
			return null;
		return ts.toLocalDateTime().toLocalDate();
	}

	public static String toLocalDateString(LocalDate ld) {
		if (ld == null)
			return null;
		return localDateFormat.format(ld);
	}
}
