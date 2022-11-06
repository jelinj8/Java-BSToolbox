package cz.bliksoft.javautils.net.http;

import java.text.MessageFormat;

public class MultiPart {
	public PartType type;
	public String contentType;
	public String name;
	public String filename;
	public String value;
	public byte[] bytes;

	@Override
	public String toString() {
		if (type == PartType.TEXT) {
			return MessageFormat.format("[{0}]={1}", name, value);
		} else {
			return MessageFormat.format("[{0}]=file(''{1}'',{2}), {3}B", name, filename, contentType, bytes.length);
		}
	}

	public enum PartType {
		TEXT, FILE
	}
}