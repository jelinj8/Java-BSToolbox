package cz.bliksoft.javautils.xml;

import jakarta.xml.bind.Unmarshaller;

public interface IBeforeUnmarshall {
	void beforeUnmarshal(Unmarshaller unmarshaller, Object parent);
}
