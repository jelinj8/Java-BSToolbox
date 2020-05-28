package cz.bliksoft.javautils.streams;

import java.io.IOException;
import java.io.OutputStream;

public class NoCloseOutputStream extends OutputStream {

	private OutputStream os;
	
	public NoCloseOutputStream(OutputStream target) {
		os = target;
	}

	@Override
	public void write(int b) throws IOException {
		os.write(b);
	}

	@Override
	public void close() throws IOException {
		flush();
	}
}
