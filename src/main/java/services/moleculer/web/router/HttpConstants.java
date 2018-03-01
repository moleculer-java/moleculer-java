package services.moleculer.web.router;

public interface HttpConstants {

	// --- LIST OF HTTP STATUS CODES ---

	public static final String STATUS_100 = "100 Continue";
	public static final String STATUS_101 = "101 Switching Protocols";
	public static final String STATUS_102 = "102 Processing";
	public static final String STATUS_200 = "200 OK";
	public static final String STATUS_201 = "201 Created";
	public static final String STATUS_202 = "202 Accepted";
	public static final String STATUS_203 = "203 Non-authoritative Information";
	public static final String STATUS_204 = "204 No Content";
	public static final String STATUS_205 = "205 Reset Content";
	public static final String STATUS_206 = "206 Partial Content";
	public static final String STATUS_207 = "207 Multi-Status";
	public static final String STATUS_208 = "208 Already Reported";
	public static final String STATUS_226 = "226 IM Used";
	public static final String STATUS_300 = "300 Multiple Choices";
	public static final String STATUS_301 = "301 Moved Permanently";
	public static final String STATUS_302 = "302 Found";
	public static final String STATUS_303 = "303 See Other";
	public static final String STATUS_304 = "304 Not Modified";
	public static final String STATUS_305 = "305 Use Proxy";
	public static final String STATUS_307 = "307 Temporary Redirect";
	public static final String STATUS_308 = "308 Permanent Redirect";
	public static final String STATUS_400 = "400 Bad Request";
	public static final String STATUS_401 = "401 Unauthorized";
	public static final String STATUS_402 = "402 Payment Required";
	public static final String STATUS_403 = "403 Forbidden";
	public static final String STATUS_404 = "404 Not Found";
	public static final String STATUS_405 = "405 Method Not Allowed";
	public static final String STATUS_406 = "406 Not Acceptable";
	public static final String STATUS_407 = "407 Proxy Authentication Required";
	public static final String STATUS_408 = "408 Request Timeout";
	public static final String STATUS_409 = "409 Conflict";
	public static final String STATUS_410 = "410 Gone";
	public static final String STATUS_411 = "411 Length Required";
	public static final String STATUS_412 = "412 Precondition Failed";
	public static final String STATUS_413 = "413 Payload Too Large";
	public static final String STATUS_414 = "414 Request-URI Too Long";
	public static final String STATUS_415 = "415 Unsupported Media Type";
	public static final String STATUS_416 = "416 Requested Range Not Satisfiable";
	public static final String STATUS_417 = "417 Expectation Failed";
	public static final String STATUS_418 = "418 I'm a teapot";
	public static final String STATUS_421 = "421 Misdirected Request";
	public static final String STATUS_422 = "422 Unprocessable Entity";
	public static final String STATUS_423 = "423 Locked";
	public static final String STATUS_424 = "424 Failed Dependency";
	public static final String STATUS_426 = "426 Upgrade Required";
	public static final String STATUS_428 = "428 Precondition Required";
	public static final String STATUS_429 = "429 Too Many Requests";
	public static final String STATUS_431 = "431 Request Header Fields Too Large";
	public static final String STATUS_444 = "444 Connection Closed Without Response";
	public static final String STATUS_451 = "451 Unavailable For Legal Reasons";
	public static final String STATUS_499 = "499 Client Closed Request";
	public static final String STATUS_500 = "500 Internal Server Error";
	public static final String STATUS_501 = "501 Not Implemented";
	public static final String STATUS_502 = "502 Bad Gateway";
	public static final String STATUS_503 = "503 Service Unavailable";
	public static final String STATUS_504 = "504 Gateway Timeout";
	public static final String STATUS_505 = "505 HTTP Version Not Supported";
	public static final String STATUS_506 = "506 Variant Also Negotiates";
	public static final String STATUS_507 = "507 Insufficient Storage";
	public static final String STATUS_508 = "508 Loop Detected";
	public static final String STATUS_510 = "510 Not Extended";
	public static final String STATUS_511 = "511 Network Authentication Required";
	public static final String STATUS_599 = "599 Network Connect Timeout Error";

	// --- REQUEST HEADERS ---

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