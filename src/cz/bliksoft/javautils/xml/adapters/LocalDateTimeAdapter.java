package cz.bliksoft.javautils.xml.adapters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
	
//	public static DateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	public static DateTimeFormatter localDateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
	
	@Override
	public LocalDateTime unmarshal(String v) throws Exception {
		return LocalDateTime.parse(v);
	}

	@Override
	public String marshal(LocalDateTime v) throws Exception {
		if (v != null) {
			return v.toString();
		} else {
			return null;
		}
	}

	public static LocalDateTime toLocalDateTime(Timestamp ts) {
		if (ts == null)
			return null;
		return ts.toLocalDateTime();
	}
	
	public static String toLocalDateTimeString(LocalDateTime ldt) {
		return localDateTimeFormat.format(ldt);
	}
}
