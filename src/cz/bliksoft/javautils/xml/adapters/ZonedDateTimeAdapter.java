package cz.bliksoft.javautils.xml.adapters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class ZonedDateTimeAdapter extends XmlAdapter<String, ZonedDateTime> {
	public static DateTimeFormatter localDateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

	@Override
	public ZonedDateTime unmarshal(String v) throws Exception {
		return ZonedDateTime.parse(v);
	}

	@Override
	public String marshal(ZonedDateTime v) throws Exception {
		if (v != null) {
			return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(v);
		} else {
			return null;
		}
	}

	public static ZonedDateTime toZonedDateTime(Timestamp ts) {
		if (ts == null)
			return null;
		ZonedDateTime zdt = ZonedDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
		return zdt;
	}

	public static ZonedDateTime toZonedDateTime(LocalDateTime localDateTime) {
		if (localDateTime == null)
			return null;
		ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
		return zdt;
	}

	public static String toLocalDateTimeString(ZonedDateTime zdt) {
		if (zdt == null)
			return null;
		return localDateTimeFormat.format(zdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
	}
}
