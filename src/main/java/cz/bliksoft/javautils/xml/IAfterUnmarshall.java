package cz.bliksoft.javautils.xml;

import jakarta.xml.bind.Unmarshaller;

public interface IAfterUnmarshall {
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent);
}
