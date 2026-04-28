package cz.bliksoft.javautils.net.http;

/**
 * Standard HTTP status codes grouped by category prefix (INFO, OK, REDIR,
 * CLIENT, SERVER).
 */
public enum HTTPErrorCodes {

	// 1xx — Informational

	/**
	 * 100 Continue — server has received the request headers; client should proceed
	 * to send the body.
	 */
	INFO_CONTINUE(100),
	/**
	 * 101 Switching Protocols — server is switching to the protocol specified in
	 * the Upgrade header.
	 */
	INFO_SWITCH_PROTOCOLS(101),

	// 2xx — Success

	/** 200 OK — request succeeded. */
	OK(200),
	/** 201 Created — request succeeded and a new resource was created. */
	OK_CREATED(201),
	/**
	 * 202 Accepted — request accepted for processing, but processing is not
	 * complete.
	 */
	OK_ACCEPTED(202),
	/**
	 * 203 Non-Authoritative Information — returned metadata came from a
	 * transforming proxy, not the origin server.
	 */
	OK_NON_AUTHORITATIVE_INFORMATION(203),
	/** 204 No Content — request succeeded but the response has no body. */
	OK_NO_CONTENT(204),
	/**
	 * 205 Reset Content — request succeeded; client should reset the document view.
	 */
	OK_RESET_CONTENT(205),
	/**
	 * 206 Partial Content — server is delivering only part of the resource due to a
	 * Range header.
	 */
	OK_PARTIAL_CONTENT(206),

	// 3xx — Redirection

	/**
	 * 300 Multiple Choices — multiple representations of the resource are
	 * available.
	 */
	REDIR_MULTIPLE_CHOICES(300),
	/**
	 * 301 Moved Permanently — the resource has been permanently moved to a new URI.
	 */
	REDIR_MODEV_PERMANENTLY(301),
	/**
	 * 302 Found (Moved Temporarily) — the resource is temporarily located at a
	 * different URI.
	 */
	REDIR_MOVED_TEMPORARILY(302),
	/**
	 * 303 See Other — the response to the request can be found at another URI using
	 * GET.
	 */
	REDIR_SEE_OTHER(303),
	/**
	 * 304 Not Modified — resource has not changed since the version specified by
	 * the request headers.
	 */
	REDIIR_NOT_MODIFIED(304),
	/**
	 * 305 Use Proxy — the requested resource must be accessed through the proxy
	 * given in the Location field.
	 */
	REDIR_USE_PROXY(305),

	// 4xx — Client Error

	/**
	 * 400 Bad Request — the server cannot process the request due to a client error
	 * (e.g. malformed syntax).
	 */
	CLIENT_BAD_REQUEST(400),
	/**
	 * 401 Unauthorized — authentication is required and has failed or not been
	 * provided.
	 */
	CLIENT_UNAUTHORIZED(401),
	/** 402 Payment Required — reserved for future use. */
	CLIENT_PAYMENT_REQUIRED(402),
	/**
	 * 403 Forbidden — the server understood the request but refuses to authorise
	 * it.
	 */
	CLIENT_FORBIDDEN(403),
	/** 404 Not Found — the requested resource could not be found. */
	CLIENT_NOT_FOUND(404),
	/**
	 * 405 Method Not Allowed — the HTTP method is not supported for the requested
	 * resource.
	 */
	CLIENT_METHOD_NOT_ALLOWED(405),
	/**
	 * 406 Not Acceptable — the resource is not available in a format acceptable per
	 * the request's Accept headers.
	 */
	CLIENT_NOT_ACCEPTABLE(406),
	/**
	 * 407 Proxy Authentication Required — the client must authenticate itself with
	 * the proxy.
	 */
	CLIENT_PROXY_AUTHENTICATION_REQUIRED(407),
	/** 408 Request Timeout — the server timed out waiting for the request. */
	CLIENT_REQUEST_TIMEOUT(408),
	/**
	 * 409 Conflict — the request conflicts with the current state of the resource.
	 */
	CLIENT_CONFLICT(409),
	/**
	 * 410 Gone — the resource has been permanently removed and will not be
	 * available again.
	 */
	CLIENT_GONE(410),
	/**
	 * 411 Length Required — the request did not specify the Content-Length required
	 * by the resource.
	 */
	CLIENT_LENGTH_REQUIRED(411),
	/**
	 * 412 Precondition Failed — the server does not meet one of the preconditions
	 * specified by the client.
	 */
	CLIENT_RECONDITION_FAILED(412),
	/**
	 * 413 Content Too Large — the request body exceeds the limit the server is
	 * willing to process.
	 */
	CLIENT_REQUEST_ENTITY_TOO_LARGE(413),
	/**
	 * 414 URI Too Long — the URI provided was too long for the server to process.
	 */
	CLIENT_REQUEST_URI_TOO_LONG(414),
	/**
	 * 415 Unsupported Media Type — the request entity has a media type the server
	 * does not support.
	 */
	CLIENT_UNSUPPORTED_MEDIA_TYPE(415),

	// 5xx — Server Error

	/**
	 * 500 Internal Server Error — an unexpected condition prevented the server from
	 * fulfilling the request.
	 */
	SERVER_INTERNAL_SERVER_ERROR(500),
	/**
	 * 501 Not Implemented — the server does not support the functionality required
	 * to fulfil the request.
	 */
	SERVER_NOT_IMPLEMENTED(501),
	/**
	 * 502 Bad Gateway — the server, acting as a gateway, received an invalid
	 * response from an upstream server.
	 */
	SERVER_BAD_GATEWAY(502),
	/**
	 * 503 Service Unavailable — the server is temporarily unable to handle the
	 * request (overloaded or down for maintenance).
	 */
	SERVER_SERVICE_UNAVAILABLE(503),
	/**
	 * 504 Gateway Timeout — the server, acting as a gateway, did not receive a
	 * timely response from an upstream server.
	 */
	SERVER_GATEWAY_TIMEOUT(504),
	/**
	 * 505 HTTP Version Not Supported — the HTTP version used in the request is not
	 * supported by the server.
	 */
	SERVER_HTTP_VERSION_NOT_SUPPORTED(505);

	private int value;

	private HTTPErrorCodes(int code) {
		this.value = code;
	}

	/** Returns the numeric HTTP status code. */
	public int getValue() {
		return value;
	}
}
