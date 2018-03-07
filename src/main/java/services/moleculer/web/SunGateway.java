package services.moleculer.web;

import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.readFully;
import static services.moleculer.web.common.GatewayUtils.getSslContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManager;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.web.common.LazyTree;

/**
 * API Gateway, based on the "com.sun.net.httpserver" package. This web-server
 * can be used primarily for development and testing. This Gateway does NOT have
 * any dependencies. In production mode use the faster {@link NettyGateway}.
 * SunGateway supports Transport Layer Security (TLS).
 * 
 * @see NettyGateway
 */
@Name("Sun HTTP Server API Gateway")
@SuppressWarnings("restriction")
public class SunGateway extends ApiGateway implements HttpHandler {

	// --- PROPERTIES ---

	protected String address;

	protected int port = 3000;

	// --- SSL PROPERTIES ---

	protected boolean useSSL;

	// --- JDK SSL PROPERTIES ---

	protected String keyStoreFilePath;

	protected String keyStorePassword;

	protected String keyStoreType = "jks";

	protected TrustManager[] trustManagers;

	// --- HTTP SERVER INSTANCE ---

	protected HttpServer server;

	// --- START HTTP SERVER INSTANCE ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		InetSocketAddress socketAddress;
		if (address == null) {
			socketAddress = new InetSocketAddress(port);
		} else {
			socketAddress = new InetSocketAddress(address, port);
		}
		if (useSSL) {
			HttpsServer sslServer = HttpsServer.create(socketAddress, port);
			sslServer.setHttpsConfigurator(new HttpsConfigurator(
					getSslContext(keyStoreFilePath, keyStorePassword, keyStoreType, trustManagers)));
			server = sslServer;
		} else {
			server = HttpServer.create(socketAddress, port);
		}
		server.setExecutor(broker.getConfig().getExecutor());
		server.createContext("/", this);
		server.start();
		logger.info("HTTP server started on http://" + getHostName() + ':' + port + '.');
	}

	// --- REQUEST PROCESSOR ---

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			String httpMethod = exchange.getRequestMethod();
			URI uri = exchange.getRequestURI();
			String path = uri.getPath();

			Tree reqHeaders = new LazyTree((map) -> {
				Headers requestHeaders = exchange.getRequestHeaders();
				for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
					List<String> list = entry.getValue();
					if (list != null && !list.isEmpty()) {
						if (list.size() == 1) {
							map.put(entry.getKey().toLowerCase(), list.get(0));
						} else {
							StringBuilder tmp = new StringBuilder(32);
							for (String value : list) {
								if (tmp.length() > 0) {
									tmp.append(',');
								}
								tmp.append(value);
							}
							map.put(entry.getKey().toLowerCase(), tmp.toString());
						}
					}
				}
			});

			String query = uri.getQuery();
			byte[] reqBody = null;
			InputStream in = exchange.getRequestBody();
			if (in != null) {
				reqBody = readFully(in);
			}
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
		} catch (Exception cause) {
			sendHttpError(exchange, cause);
		}
	}

	protected void sendHttpError(HttpExchange exchange, Throwable cause) {

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

	protected void sendHttpResponse(HttpExchange exchange, int status, Tree headers, byte[] bytes, File file) {
		OutputStream out = null;
		InputStream in = null;
		try {

			// Create HTTP response
			Headers responseHeaders = exchange.getResponseHeaders();
			if (headers == null) {
				responseHeaders.set(RSP_CONTENT_TYPE, CONTENT_TYPE_JSON);
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
						responseHeaders.set(name, value);
					}
				}
				if (!foundContentType) {
					responseHeaders.set(RSP_CONTENT_TYPE, CONTENT_TYPE_JSON);
				}
			}
			if (bytes != null && bytes.length > 0) {

				// Write HTTP headers (byte array body)
				exchange.sendResponseHeaders(status, bytes.length);

				// Write body
				out = exchange.getResponseBody();
				out.write(bytes);
				out.flush();

			} else if (file != null) {

				// Write HTTP headers (file body)
				exchange.sendResponseHeaders(status, file.length());

				// Write body
				out = exchange.getResponseBody();
				in = new FileInputStream(file);
				byte[] packet = new byte[8192];
				int len;
				while ((len = in.read(packet)) > -1) {
					out.write(packet, 0, len);
					out.flush();
				}

			} else {

				// Write HTTP headers (empty body)
				exchange.sendResponseHeaders(status, -1);
			}

		} catch (IOException closed) {
		} catch (Throwable cause) {
			logger.warn("Unable to send HTTP response!", cause);
		} finally {

			// Close output stream
			if (out != null) {
				try {
					out.close();
				} catch (Exception ingored) {
				}
			}

			// Close input stream
			if (in != null) {
				try {
					in.close();
				} catch (Exception ingored) {
				}
			}

			// Close exchange
			exchange.close();
		}
	}

	// --- STOP HTTP SERVER INSTANCE ---

	@Override
	public void stopped() {
		super.stopped();
		if (server != null) {
			try {
				server.stop(0);
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

}