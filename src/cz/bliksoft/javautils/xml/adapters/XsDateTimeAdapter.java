package cz.bliksoft.javautils.xml.adapters;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class XsDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

	@Override
	public LocalDateTime unmarshal(String v) throws Exception {
		return LocalDateTime.from(DatatypeConverter.parseDateTime(v).toInstant());
	}

	@Override
	public String marshal(LocalDateTime v) throws Exception {
		if (v != null) {
			ZoneId zoneId = ZoneId.systemDefault();
			Date date = Date.from(v.atZone(zoneId).toInstant());
			Calendar c = Calendar.getInstance();
			c.setTime(date);
			return DatatypeConverter.printDateTime(c);
		} else {
			return null;
		}
	}
}
