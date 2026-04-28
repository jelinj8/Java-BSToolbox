package cz.bliksoft.javautils.streams;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link OutputStream} wrapper that suppresses {@link #close()} calls, flushing
 * instead. Useful when passing a stream to a library that closes it on
 * completion but the caller still needs the underlying stream open.
 */
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
