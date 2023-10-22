package cz.bliksoft.javautils.net.http;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

public class OKHTTPHandler extends BasicHTTPHandler {

	@Override
	public void handle(HttpExchange httpExchange, String path, String query, HttpMethod method) throws IOException {
		sendOK(httpExchange);
	}
}
