package cz.bliksoft.javautils.xml.adapters.types;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({ NullObject.class, StringObject.class, IntegerObject.class, BooleanObject.class, FloatObject.class,
		LocalDateObject.class, LocalDateTimeObject.class, LocalTimeObject.class })
public abstract class AbstractXmlObject {
	public abstract Object getValue();

	public abstract AbstractXmlObject wrap(Object o);

	public static AbstractXmlObject of(Object o) {
		if (o == null) {
			return new NullObject();
		} else if (o instanceof Integer) {
			return new IntegerObject().wrap(o);
		} else if (o instanceof String) {
			return new StringObject().wrap(o);
		} else if (o instanceof Boolean) {
			return new BooleanObject().wrap(o);
		} else if (o instanceof Float) {
			return new FloatObject().wrap(o);
		} else if (o instanceof LocalDate) {
			return new LocalDateObject().wrap(o);
		} else if (o instanceof LocalDateTime) {
			return new LocalDateTimeObject().wrap(o);
		} else if (o instanceof LocalTime) {
			return new LocalTimeObject().wrap(o);
		}
		return new StringObject().wrap(o.toString());

//		throw new NotImplementedException("Class " + o.getClass().getName() + " not supported as basic object.");
	}
}
