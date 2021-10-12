package cz.bliksoft.javautils.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPUtils {

	private IPUtils() {
		
	}
	
	public static String getHostname(String ip) throws UnknownHostException {
		InetAddress addr = InetAddress.getByName(ip);
		return addr.getHostName();
	}

}
