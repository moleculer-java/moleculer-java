package services.moleculer.web;

import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.readFully;

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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * API Gateway, based on the "com.sun.net.httpserver" package. This web-server can
 * be used primarily for development and testing.
 */
@Name("Sun HTTP Server API Gateway")
@SuppressWarnings("restriction")
public class SunGateway extends ApiGateway implements HttpHandler {

	// --- PROPERTIES ---

	protected String host;
	protected int port = 3000;

	// --- HTTP SERVER INSTANCE ---

	protected HttpServer server;

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		InetSocketAddress address;
		if (host == null) {
			address = new InetSocketAddress(port);
		} else {
			address = new InetSocketAddress(host, port);
		}
		server = HttpServer.create(address, port);
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
			Tree reqHeaders = new Tree();
			Headers requestHeaders = exchange.getRequestHeaders();
			for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
				List<String> list = entry.getValue();
				if (list != null && !list.isEmpty()) {
					reqHeaders.put(entry.getKey().toLowerCase(), list.get(0));
				}
			}
			String query = uri.getQuery();
			byte[] reqBody = null;
			InputStream in = exchange.getRequestBody();
			if (in != null) {
				reqBody = readFully(in);
			}
			processRequest(httpMethod, path, reqHeaders, query, reqBody).then(rsp -> {
				Class<?> type = rsp.getType();
				byte[] rspBody;
				if (type == byte[].class) {
					rspBody = rsp.asBytes();
				} else {
					rspBody = rsp.toBinary(null, false);
				}
				int status = 200;
				Tree meta = rsp.getMeta(false);
				Tree rspHeaders = null;
				if (meta != null) {
					status = meta.get("status", 200);
					rspHeaders = meta.get("headers");
				}
				sendHttpResponse(exchange, status, rspHeaders, rspBody);
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
		sendHttpResponse(exchange, 500, null, bytes);
	}

	protected void sendHttpResponse(HttpExchange exchange, int status, Tree headers, byte[] body) {
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
			boolean hasBody = body != null && body.length > 0;

			// Write HTTP headers
			exchange.sendResponseHeaders(status, hasBody ? body.length : -1);

			// Write body
			if (hasBody) {
				OutputStream out = exchange.getResponseBody();
				out.write(body);

				// Flush response
				out.flush();
			}
		} catch (Throwable cause) {
			String msg = String.valueOf(cause).toLowerCase();
			if (msg.contains("close") || msg.contains("abort")) {
				return;
			}
			logger.warn("Unable to send HTTP response!", cause);
		} finally {

			// Close exchange
			exchange.close();
		}
	}

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

}