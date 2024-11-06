package cz.bliksoft.javautils.net.http;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Cookie {
	public enum SameSite {
		Strict, Lax, None
	}

	private String name;
	private String value;
	private Date expires;
	private Integer maxAge;
	private String domain;
	private String path;
	private boolean secure;
	private boolean httpOnly;
	private SameSite sameSite;

	private Cookie(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public Cookie(String name, String value, Date expires, Integer maxAge, String domain, String path, boolean secure,
			boolean httpOnly, SameSite sameSite) {
		this.name = name;
		this.value = value;
		this.expires = expires;
		this.maxAge = maxAge;
		this.domain = domain;
		this.path = path;
		this.secure = secure;
		this.httpOnly = httpOnly;
		this.sameSite = sameSite;
	}

	public static Cookie create(String name, String value) {
		return new Cookie(name, value);

	}

	public Cookie withExpiry(Date expires) {
		this.expires = expires;
		return this;
	}

	public Cookie withMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
		return this;
	}

	public Cookie withDomain(String domain) {
		this.domain = domain;
		return this;
	}

	public Cookie withPath(String path) {
		this.path = path;
		return this;
	}

	public Cookie withSecure(boolean secure) {
		this.secure = secure;
		return this;
	}

	public Cookie withHTTPOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
		return this;
	}

	public Cookie withSameSite(SameSite sameSite) {
		this.sameSite = sameSite;
		return this;
	}

	public String toString() {
		StringBuffer s = new StringBuffer();

		s.append(name + "=" + value);

		if (expires != null) {
			SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			s.append("; Expires=" + fmt.format(expires) + " GMT");
		}

		if (maxAge != null) {
			s.append("; Max-Age=" + maxAge);
		}

		if (domain != null) {
			s.append("; Domain=" + domain);
		}

		if (path != null) {
			s.append("; Path=" + path);
		}

		if (secure) {
			s.append("; Secure");
		}

		if (httpOnly) {
			s.append("; HttpOnly");
		}

		if (sameSite != null) {
			s.append("; SameSite=" + sameSite);
		}

		return s.toString();
	}
}
