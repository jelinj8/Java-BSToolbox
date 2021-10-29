package cz.bliksoft.javautils.xml;

import javax.xml.bind.Unmarshaller;

public interface IAfterUnmarshall {
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent);
}
