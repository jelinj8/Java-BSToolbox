package cz.bliksoft.javautils.xml.adapters;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class LocalTimeAdapter extends XmlAdapter<String, LocalTime> {
	// public static DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	public static DateTimeFormatter localTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Override
	public LocalTime unmarshal(String v) throws Exception {
		return LocalTime.parse(v);
	}

	@Override
	public String marshal(LocalTime v) throws Exception {
		if (v != null) {
			return v.toString();
		} else {
			return null;
		}
	}

	public static LocalTime toLocalTime(Timestamp ts) {
		if (ts == null)
			return null;
		return ts.toLocalDateTime().toLocalTime();
	}

	public static String toLocalTimeString(LocalTime ld) {
		if (ld == null)
			return null;
		return localTimeFormat.format(ld);
	}
}
