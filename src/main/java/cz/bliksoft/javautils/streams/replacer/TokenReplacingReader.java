package cz.bliksoft.javautils.streams.replacer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * http://tutorials.jenkov.com/java-howto/replace-strings-in-streams-arrays-files.html
 * 
 * @author Jakob Jenkov, jelinj8
 * 
 */
public class TokenReplacingReader extends Reader {
	protected PushbackReader pushbackReader = null;
	protected ITokenResolver tokenResolver = null;
	protected StringBuilder tokenNameBuffer = new StringBuilder();
	protected String tokenValue = null;
	protected int tokenValueIndex = 0;

	/**
	 * use UTF-8 as default
	 * 
	 * @param source
	 * @param resolver
	 */
	public TokenReplacingReader(InputStream source, ITokenResolver resolver) {
		this(new InputStreamReader(source, StandardCharsets.UTF_8), resolver);
	}

	/**
	 * specify input charset
	 * 
	 * @param source
	 * @param charset
	 * @param resolver
	 */
	public TokenReplacingReader(InputStream source, Charset charset, ITokenResolver resolver) {
		this(new InputStreamReader(source, charset), resolver);
	}

	public TokenReplacingReader(File source, ITokenResolver resolver) throws FileNotFoundException {
		this(new FileReader(source), resolver);
	}

	public TokenReplacingReader(char[] source, ITokenResolver resolver) {
		this(new CharArrayReader(source), resolver);
	}

	public TokenReplacingReader(String source, ITokenResolver resolver) {
		this(new StringReader(source), resolver);
	}

	public TokenReplacingReader(Reader source, ITokenResolver resolver) {
		this.pushbackReader = new PushbackReader(source, 2);
		this.tokenResolver = resolver;
	}

	public int read(CharBuffer target) throws IOException {
		throw new RuntimeException("Operation Not Supported");
	}

	public int read() throws IOException {
		if (this.tokenValue != null) {
			if (this.tokenValueIndex < this.tokenValue.length()) {
				return this.tokenValue.charAt(this.tokenValueIndex++);
			}
			if (this.tokenValueIndex == this.tokenValue.length()) {
				this.tokenValue = null;
				this.tokenValueIndex = 0;
			}
		}

		int data = this.pushbackReader.read();
		if (data != '$')
			return data;

		data = this.pushbackReader.read();
		if (data != '{') {
			this.pushbackReader.unread(data);
			return '$';
		}
		this.tokenNameBuffer.delete(0, this.tokenNameBuffer.length());

		data = this.pushbackReader.read();
		while (data != '}') {
			this.tokenNameBuffer.append((char) data);
			data = this.pushbackReader.read();
		}

		this.tokenValue = this.tokenResolver.resolveToken(this.tokenNameBuffer.toString());

		if (this.tokenValue == null) {
			this.tokenValue = "${" + this.tokenNameBuffer.toString() + "}";
		}
		if (this.tokenValue.length() == 0) {
			return read();
		}
		return this.tokenValue.charAt(this.tokenValueIndex++);

	}

	public int read(char cbuf[]) throws IOException {
		return read(cbuf, 0, cbuf.length);
	}

	public int read(char cbuf[], int off, int len) throws IOException {
		int charsRead = 0;
		for (int i = 0; i < len; i++) {
			int nextChar = read();
			if (nextChar == -1) {
				if (charsRead == 0) {
					charsRead = -1;
				}
				break;
			}
			charsRead = i + 1;
			cbuf[off + i] = (char) nextChar;
		}
		return charsRead;
	}

	public void close() throws IOException {
		this.pushbackReader.close();
	}

	public long skip(long n) throws IOException {
		throw new RuntimeException("Operation Not Supported");
	}

	public boolean ready() throws IOException {
		return this.pushbackReader.ready();
	}

	public boolean markSupported() {
		return false;
	}

	public void mark(int readAheadLimit) throws IOException {
		throw new RuntimeException("Operation Not Supported");
	}

	public void reset() throws IOException {
		throw new RuntimeException("Operation Not Supported");
	}

	public String readAsString() throws IOException {
		try (BufferedReader buffer = new BufferedReader(this)) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
	}

	public List<String> readAllLines() throws IOException {
		try (BufferedReader buffer = new BufferedReader(this)) {
			List<String> lines = new LinkedList<>();
			buffer.lines().forEach(l -> {
				lines.add(l);
			});
			return lines;
		}
	}

	public InputStream toInputStream(Charset charset) throws IOException {
		return new ByteArrayInputStream(readAsString().getBytes(charset));
	}
}
