package cz.bliksoft.javautils.xml.adapters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {
	public static DateTimeFormatter localDateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

	@Override
	public OffsetDateTime unmarshal(String v) throws Exception {
		return OffsetDateTime.parse(v);
	}

	@Override
	public String marshal(OffsetDateTime v) throws Exception {
		if (v != null) {
			return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(v);
		} else {
			return null;
		}
	}

	public static OffsetDateTime toOffsetDateTime(Timestamp ts) {
		if (ts == null)
			return null;
		OffsetDateTime odt = OffsetDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
		return odt;
	}

	public static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
		if (localDateTime == null)
			return null;
		ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
		OffsetDateTime odt = zdt.toOffsetDateTime();
		return odt;
	}

	public static String toLocalDateTimeString(OffsetDateTime odt) {
		if (odt == null)
			return null;
		return localDateTimeFormat.format(odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
	}
}
