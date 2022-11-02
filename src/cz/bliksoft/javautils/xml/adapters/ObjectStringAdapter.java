package cz.bliksoft.javautils.xml.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class ObjectStringAdapter extends XmlAdapter<String, Object> {

	@Override
	public Object unmarshal(String v) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public String marshal(Object v) throws Exception {
		if (v != null)
			return v.toString();
		return null;
	}

}
