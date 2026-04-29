package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.net.URIBuilder;

public class BasicHttpClient<T> {
	private String baseURI;

	public BasicHttpClient(String baseURI) throws MalformedURLException {
		this.baseURI = baseURI;
	}

	public enum Method {
		GET, POST, DELETE, PUT
	};

	public T call(String url, String path, Method method, Map<String, String> headers,
			Map<String, String> getParameters, Map<String, String> postParameters, ContentType ct, String data,
			AbstractHttpClientResponseHandler<T> responseHandler) throws IOException, URISyntaxException {
		try (CloseableHttpClient client = HttpClients.createDefault()) {

			URIBuilder builder = new URIBuilder((url == null ? baseURI : url));
			if (path != null)
				builder.setPath(path);

			if (getParameters != null) {
				for (Entry<String, String> p : getParameters.entrySet()) {
					builder.addParameter(p.getKey(), p.getValue());
				}
			}

			URI uri = builder.build();

			HttpUriRequestBase req = null;

			switch (method) {
			case GET:
				req = new HttpGet(uri);
				break;
			case POST:
				req = new HttpPost(uri);
				break;
			case DELETE:
				req = new HttpDelete(uri);
				break;
			case PUT:
				req = new HttpPut(uri);
				break;
			}

			if (headers != null) {
				for (Entry<String, String> h : headers.entrySet()) {
					req.addHeader(h.getKey(), h.getValue());
				}
			}

			if (method == Method.POST) {
				if (postParameters == null) {
					if (ct != null && data != null) {
						req.setEntity(new ByteArrayEntity(data.getBytes(ct.getCharset()), ct));
					}
				} else {
					MultipartEntityBuilder mpBuilder = MultipartEntityBuilder.create();

					for (Entry<String, String> p : postParameters.entrySet()) {
						mpBuilder.addTextBody(p.getKey(), p.getValue());
					}

					if (ct != null && data != null) {
						mpBuilder.addTextBody("data", data, ct);
					}
					req.setEntity(mpBuilder.build());
				}
			} else {
				if (ct != null && data != null) {
					req.setEntity(new ByteArrayEntity(data.getBytes(ct.getCharset()), ct));
				}
			}
			return client.execute(req, responseHandler);
		}
	}

}
