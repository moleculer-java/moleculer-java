package services.moleculer.web.router;

public interface HttpConstants {

	// --- LOWERCASE REQUEST HEADERS ---

	public static final String REQ_CONTENT_TYPE = "content-type";
	public static final String REQ_CONTENT_LENGTH = "content-length";
	public static final String REQ_IF_NONE_MATCH = "if-none-match";
	public static final String REQ_CONNECTION = "connection";
	public static final String REQ_ACCEPT_ENCODING = "accept-encoding";
	public static final String REQ_CONTENT_ENCODING = "content-encoding";

	// --- RESPONSE HEADERS ---

	public static final String RSP_CONTENT_TYPE = "Content-Type";
	public static final String RSP_CONTENT_LENGTH = "Content-Length";
	public static final String RSP_CONNECTION = "Connection";
	public static final String RSP_ACCEPT_ENCODING = "Accept-Encoding";
	public static final String RSP_CONTENT_ENCODING = "Content-Encoding";	
	public static final String RSP_ETAG = "ETag";
	
	// --- HTTP HEADER VALUES ---
	
	public static final String DEFLATE = "deflate";
	public static final String KEEP_ALIVE = "keep-alive";
	public static final String CLOSE = "close";

	// --- META VARIABLES ---
	
	public static final String STATUS = "status";
	public static final String PATH = "path";	
	public static final String METHOD = "method";
	public static final String PATTERN = "pattern";
	public static final String HEADERS = "headers";
	
	// --- CONTENT TYPES ---
	
	public static final String CONTENT_TYPE_JSON = "application/json;charset=utf-8";
	
}