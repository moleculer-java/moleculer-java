package services.moleculer.web;

import static services.moleculer.web.common.FileUtils.getFileURL;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.xnio.Options;
import org.xnio.channels.StreamSourceChannel;

import io.datatree.Tree;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.web.common.LazyTree;

@Name("Undertow HTTP Server API Gateway")
public class UndertowGateway extends ApiGateway implements HttpHandler {

	// --- PROPERTIES ---

	protected String address;

	protected int port = 3000;

	protected boolean useHttp2;

	protected int bufferSize = 1024 * 16;

	protected int ioThreads = 2;

	// --- SSL PROPERTIES ---

	protected boolean useSSL;

	// --- JDK SSL PROPERTIES ---

	protected String keyStoreFilePath;

	protected String keyStorePassword;

	protected String keyStoreType = "jks";

	protected TrustManager[] trustManagers;

	// --- HTTP SERVER INSTANCE ---

	protected Undertow server;

	// --- START HTTP SERVER INSTANCE ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		if (server != null) {
			try {
				server.stop();
			} catch (Exception ignored) {
			}
		}
		Undertow.Builder builder = Undertow.builder();
		if (useSSL) {
			SSLContext sslContext = getSslContext();
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
		builder.setServerOption(UndertowOptions.ALWAYS_SET_DATE, true);
		builder.setHandler(this);
		server = builder.build();
		server.start();
	}

	// --- REQUEST PROCESSOR ---

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		try {
			String httpMethod = exchange.getRequestMethod().toString();
			String path = exchange.getRequestPath();

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

			String query = exchange.getQueryString();
			StreamSourceChannel in = exchange.getRequestChannel();

			// TODO read request
			byte[] reqBody = null;
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

		// TODO send response
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

	// --- CREATE SSL CONTEXT ---

	protected SSLContext getSslContext() throws Exception {

		// Load KeyStore
		KeyStore keyStore = KeyStore.getInstance(keyStoreType);
		InputStream keyStoreInputStream = getFileURL(keyStoreFilePath).openStream();
		keyStore.load(keyStoreInputStream, keyStorePassword == null ? null : keyStorePassword.toCharArray());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.getProvider();
		keyManagerFactory.init(keyStore, keyStorePassword == null ? null : keyStorePassword.toCharArray());

		// Create TrustManager
		TrustManager[] mgrs;
		if (trustManagers == null) {
			mgrs = new TrustManager[] { new X509TrustManager() {

				@Override
				public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
						throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

			} };
		} else {
			mgrs = trustManagers;
		}

		// Create SSL context
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagerFactory.getKeyManagers(), mgrs, null);
		return sslContext;
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