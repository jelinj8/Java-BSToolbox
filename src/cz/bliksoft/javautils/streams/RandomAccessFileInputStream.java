package cz.bliksoft.javautils.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessFileInputStream extends InputStream {

	RandomAccessFile ras;
	
	public RandomAccessFileInputStream(RandomAccessFile file) {
		ras = file;
	}
	
	@Override
	public int read() throws IOException {
		return ras.read();
	}

}
