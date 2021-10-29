package cz.bliksoft.javautils.xml;

import javax.xml.bind.Unmarshaller;

public interface IBeforeUnmarshall {
	void beforeUnmarshal(Unmarshaller unmarshaller, Object parent);
}
