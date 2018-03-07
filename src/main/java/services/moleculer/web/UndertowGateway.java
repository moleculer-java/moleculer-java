package services.moleculer.web;

import static services.moleculer.web.common.GatewayUtils.getSslContext;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.xnio.Options;

import io.datatree.Tree;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.web.common.LazyTree;

@Name("Undertow HTTP Server API Gateway")
public class UndertowGateway extends ApiGateway implements HttpHandler, IoCallback {

	// --- PROPERTIES ---

	protected String address;

	protected int port = 3000;

	protected boolean useHttp2;

	protected int bufferSize = 1024 * 16;

	protected int ioThreads = 1;

	// --- SSL PROPERTIES ---

	protected boolean useSSL;

	// --- JDK SSL PROPERTIES ---

	protected String keyStoreFilePath;

	protected String keyStorePassword;

	protected String keyStoreType = "jks";

	protected TrustManager[] trustManagers;

	// --- HTTP SERVER INSTANCE ---

	protected Undertow server;

	// --- COMPONENTS ---

	protected ExecutorService executor;

	// --- START HTTP SERVER INSTANCE ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Get executor
		executor = broker.getConfig().getExecutor();

		// Build new server
		if (server != null) {
			try {
				server.stop();
			} catch (Exception ignored) {
			}
		}
		Undertow.Builder builder = Undertow.builder();
		if (useSSL) {
			SSLContext sslContext = getSslContext(keyStoreFilePath, keyStorePassword, keyStoreType, trustManagers);
			builder.addHttpsListener(port, address, sslContext);
		} else {
			builder.addHttpListener(port, address);
		}
		if (useHttp2) {
			builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
		}
		builder.setBufferSize(bufferSize);
		builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false);
		builder.setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false);
		builder.setIoThreads(ioThreads);
		builder.setSocketOption(Options.BACKLOG, 10000);
		builder.setServerOption(UndertowOptions.ALWAYS_SET_DATE, false);
		builder.setServerOption(UndertowOptions.ALWAYS_SET_DATE, false);
		builder.setHandler(this);
		server = builder.build();
		server.start();
	}

	// --- REQUEST PROCESSOR ---

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		exchange.dispatch(executor, () -> {
			try {

				// Get method and path
				String httpMethod = exchange.getRequestMethod().toString();
				String path = exchange.getRequestPath();

				// Parse headers when required
				Tree reqHeaders = new LazyTree((map) -> {
					HeaderMap requestHeaders = exchange.getRequestHeaders();
					Collection<HttpString> headerNames = requestHeaders.getHeaderNames();
					for (HttpString headerName : headerNames) {
						HeaderValues list = requestHeaders.get(headerName);
						if (list != null && !list.isEmpty()) {
							if (list.size() == 1) {
								map.put(headerName.toString().toLowerCase(), list.get(0));
							} else {
								StringBuilder tmp = new StringBuilder(32);
								for (String value : list) {
									if (tmp.length() > 0) {
										tmp.append(',');
									}
									tmp.append(value);
								}
								map.put(headerName.toString().toLowerCase(), tmp.toString());
							}
						}
					}
				});

				// Get query string
				String query = exchange.getQueryString();

				// Invoke action
				exchange.getRequestReceiver().receiveFullBytes((ex, reqBody) -> {
					processRequest(httpMethod, path, reqHeaders, query, reqBody).then(rsp -> {

						// Default status
						int status = 200;
						Tree rspHeaders = null;

						// Get status code and response headers
						Tree meta = rsp.getMeta(false);
						if (meta != null) {
							status = meta.get("status", 200);
							rspHeaders = meta.get("headers");
						}

						// Convert and send body
						Class<?> type = rsp.getType();
						if (type == byte[].class) {
							sendHttpResponse(exchange, status, rspHeaders, rsp.asBytes(), null);
						} else if (type == File.class) {
							sendHttpResponse(exchange, status, rspHeaders, null, (File) rsp.asObject());
						} else {
							sendHttpResponse(exchange, status, rspHeaders, rsp.toBinary(null, false), null);
						}

					}).catchError(cause -> {
						sendHttpError(exchange, cause);
					});
				}, (ex, cause) -> {
					sendHttpError(exchange, cause);
				});
			} catch (Exception cause) {
				sendHttpError(exchange, cause);
			}
		});
	}

	protected void sendHttpError(HttpServerExchange exchange, Throwable cause) {

		// Send HTTP error response
		String message = null;
		String trace = null;
		if (cause != null) {
			message = cause.getMessage();
			StringWriter traceWriter = new StringWriter(512);
			cause.printStackTrace(new PrintWriter(traceWriter, true));
			trace = traceWriter.toString().replace('\t', ' ').replace("\r", "\\r").replace("\n", "\\n")
					.replace("\"", "\\\"").trim();
		}
		if (message != null) {
			message = message.replace('\r', ' ').replace('\t', ' ').replace('\n', ' ').replace("\"", "\\\"").trim();
		}
		if (message == null || message.isEmpty()) {
			message = "Unexpected error occured!";
		}

		// Create JSON error message
		StringBuilder json = new StringBuilder(256);
		json.append("{\r\n  \"message\":\"");
		json.append(message);
		if (trace != null) {
			json.append("\",\r\n  \"trace\":\"");
			json.append(trace);
		}
		json.append("\"\r\n}");
		byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
		sendHttpResponse(exchange, 500, null, bytes, null);
	}

	protected void sendHttpResponse(HttpServerExchange exchange, int status, Tree headers, byte[] bytes, File file) {
		try {

			// Create HTTP response
			exchange.setStatusCode(status);
			HeaderMap responseHeaders = exchange.getResponseHeaders();
			if (headers == null) {
				responseHeaders.put(Headers.CONTENT_TYPE, CONTENT_TYPE_JSON);
			} else {
				String name, value;
				boolean foundContentType = false;
				for (Tree header : headers) {
					name = header.getName();
					if (name.equals(RSP_CONTENT_LENGTH)) {
						continue;
					}
					if (!foundContentType && RSP_CONTENT_TYPE.equalsIgnoreCase(name)) {
						foundContentType = true;
					}
					value = header.asString();
					if (value != null) {
						responseHeaders.put(HttpString.tryFromString(name), value);
					}
				}
				if (!foundContentType) {
					responseHeaders.put(Headers.CONTENT_TYPE, CONTENT_TYPE_JSON);
				}
			}
			if (bytes != null && bytes.length > 0) {

				// Write HTTP headers (byte array body)
				exchange.setResponseContentLength(bytes.length);

				// Write byte array body
				exchange.getResponseSender().send(ByteBuffer.wrap(bytes), this);

			} else if (file != null) {

				// Write HTTP headers (file body)
				exchange.setResponseContentLength(file.length());

				// Write file
				FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
				exchange.getResponseSender().transferFrom(channel, this);

			} else {

				// Write HTTP headers (empty body)
				exchange.setResponseContentLength(-1);
				exchange.endExchange();
			}

		} catch (IOException closed) {
		} catch (Throwable cause) {
			logger.warn("Unable to send HTTP response!", cause);
		}
	}

	@Override
	public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
		exchange.endExchange();
	}

	@Override
	public void onComplete(HttpServerExchange exchange, Sender sender) {
		exchange.endExchange();
	}

	// --- STOP HTTP SERVER INSTANCE ---

	@Override
	public void stopped() {
		super.stopped();
		if (server != null) {
			try {
				server.stop();
			} catch (Exception cause) {
				logger.warn("Unable to stop HTTP server!", cause);
			}
			server = null;
		}
	}

	// --- PROPERTY GETTERS AND SETTERS ---

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String host) {
		this.address = host;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public String getKeyStoreFilePath() {
		return keyStoreFilePath;
	}

	public void setKeyStoreFilePath(String keyStoreFilePath) {
		this.keyStoreFilePath = keyStoreFilePath;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyStoreType() {
		return keyStoreType;
	}

	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public TrustManager[] getTrustManagers() {
		return trustManagers;
	}

	public void setTrustManagers(TrustManager[] trustManagers) {
		this.trustManagers = trustManagers;
	}

	public boolean isUseHttp2() {
		return useHttp2;
	}

	public void setUseHttp2(boolean useHttp2) {
		this.useHttp2 = useHttp2;
	}

}