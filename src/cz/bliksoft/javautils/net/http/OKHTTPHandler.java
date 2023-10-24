package cz.bliksoft.javautils.net.http;

import java.io.IOException;

public class OKHTTPHandler extends BasicHTTPHandler {

	@Override
	public void handle(BSHttpContext context) throws IOException {
		sendOK(context.httpExchange);
	}
}
