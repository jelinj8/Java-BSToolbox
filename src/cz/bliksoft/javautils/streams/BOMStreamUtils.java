package cz.bliksoft.javautils.streams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

/**
 * helper to ignore UTF8 BOM in files/streams
 */
public class BOMStreamUtils {

	public static Reader getReader(File source) throws IOException {
		InputStream is = new FileInputStream(source);
		return getReader(is);
	}

	public static Reader getReader(File source, Charset chs) throws IOException {
		InputStream is = new FileInputStream(source);
		return getReader(is, chs);
	}

	public static Reader getReader(InputStream source) throws IOException {
		return getReader(source, null);
	}

	public static Reader getReader(InputStream source, Charset chs) throws IOException {
		BOMInputStream bis = BOMInputStream.builder().setInputStream(source).get();
		Reader rdr = (chs == null ? new InputStreamReader(bis) : new InputStreamReader(bis, chs));
		return new BufferedReader(rdr);
	}

	public static ByteOrderMark identifyBOM(File source) throws IOException {
		try (InputStream is = new FileInputStream(source);
				BOMInputStream bis = BOMInputStream.builder().setInputStream(is).get();) {
			return bis.getBOM();
		}
	}

}
