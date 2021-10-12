package cz.bliksoft.javautils.freemarker.extensions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class LocalDateTimeFormatter implements TemplateMethodModelEx {

	private String format;
	DateTimeFormatter formatter;

	public LocalDateTimeFormatter(String format) {
		this.format = format;
		formatter = DateTimeFormatter.ofPattern(this.format);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List args) throws TemplateModelException {
		no.api.freemarker.java8.time.LocalDateTimeAdapter ldtAdapter = (no.api.freemarker.java8.time.LocalDateTimeAdapter) args.get(0);
		if (ldtAdapter == null)
			return "-??-";
		LocalDateTime ldt = (LocalDateTime) ldtAdapter.getAdaptedObject(LocalDateTime.class);
		if (ldt == null)
			return "-?-";
		return ldt.format(formatter);
	}

}
