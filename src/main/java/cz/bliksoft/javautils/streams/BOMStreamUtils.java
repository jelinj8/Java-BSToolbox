package cz.bliksoft.javautils.streams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * helper to ignore UTF8 BOM in files/streams
 */
public class BOMStreamUtils {

	/**
	 * wrapper for {@link #getReader(InputStream, Charset) getReader(InputStream,
	 * Charset) with default or detected charset}
	 * 
	 * @param source
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getReader(File source) throws IOException {
		InputStream is = new FileInputStream(source);
		return getReader(is);
	}

	/**
	 * wrapper for {@link #getReader(InputStream, Charset) getReader(InputStream,
	 * Charset)}
	 * 
	 * @param source
	 * @param chs
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getReader(File source, Charset chs) throws IOException {
		InputStream is = new FileInputStream(source);
		return getReader(is, chs);
	}

	/**
	 * wrapper for {@link #getReader(InputStream, Charset) getReader(InputStream,
	 * Charset) with default or detected charset}
	 * 
	 * @param source
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getReader(InputStream source) throws IOException {
		return getReader(source, null);
	}

	/**
	 * get BufferedReader, skipping UTF8 BOM if present.
	 * 
	 * @param source
	 * @param chs
	 *            optional specific charset, default used if null and no UTF8 BOM
	 *            present, UTF8 if null and UTF8 BOM is present
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getReader(InputStream source, Charset chs) throws IOException {
		BOMInputStream bis = BOMInputStream.builder().setInputStream(source).get();
		Reader rdr = (chs != null ? new InputStreamReader(bis, chs)
				: (bis.hasBOM(ByteOrderMark.UTF_8) ? new InputStreamReader(bis, StandardCharsets.UTF_8)
						: new InputStreamReader(bis)));
		return new BufferedReader(rdr);
	}

	/**
	 * wrapps InputStream to skip UTF8 BOM if present
	 * @param source
	 * @return
	 * @throws IOException
	 */
	public static InputStream wrap(InputStream source) throws IOException {
		return BOMInputStream.builder().setInputStream(source).get();
	}
	
	/**
	 * Checks type of BOM in file
	 * 
	 * @param source
	 * @return
	 * @throws IOException
	 */
	public static ByteOrderMark identifyBOM(File source) throws IOException {
		try (InputStream is = new FileInputStream(source)) {
			return identifyBOM(is);
		}
	}

	/**
	 * checks BOM in input stream, not closing the source stream
	 * 
	 * @param source
	 * @return
	 * @throws IOException
	 */
	public static ByteOrderMark identifyBOM(InputStream source) throws IOException {
		try (BOMInputStream bis = BOMInputStream.builder().setInputStream(CloseShieldInputStream.wrap(source)).get();) {
			return bis.getBOM();
		}
	}

}
