package cz.bliksoft.javautils.net;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPUtils {

	private IPUtils() {

	}

	public static String getHostname(String ip) throws UnknownHostException {
		InetAddress addr = InetAddress.getByName(ip);
		return addr.getHostName();
	}

	private static final String IPV4_PATTERN = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$";

	private static final Pattern pattern = Pattern.compile(IPV4_PATTERN);

	public static boolean isIPV4(final String email) {
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
	}

	public static boolean urlIsIPV4(String url) throws MalformedURLException, URISyntaxException {
		URL u;
		u = new URI(url).toURL();
		return isIPV4(u.getHost());
	}

}
