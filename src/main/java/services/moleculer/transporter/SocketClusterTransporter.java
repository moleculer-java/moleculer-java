/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.transporter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import io.datatree.Tree;
import io.github.sac.BasicListener;
import io.github.sac.Emitter.Listener;
import io.github.sac.Socket;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * SocketCluster Transporter. SocketCluster is an open source real-time
 * framework for Node.js. It supports both direct client-server communication
 * and group communication via pub/sub channels (website:
 * https://socketcluster.io).<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://bintray.com/sacoo7/Maven/socketcluster-client<br>
 * compile group: 'io.github.sac', name: 'SocketclusterClientJava', version:
 * '1.7.2'
 * 
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see AmqpTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
 */
@Name("SocketCluster Transporter")
public class SocketClusterTransporter extends Transporter implements Listener, BasicListener {

	// --- PROPERTIES ---

	protected String url = "127.0.0.1";
	protected String authToken;

	// --- SOCKETCLUSTER CONNECTION ---

	protected Socket client;

	// --- CONSTUCTORS ---

	public SocketClusterTransporter() {
		super();
	}

	public SocketClusterTransporter(String prefix) {
		super(prefix);
	}

	public SocketClusterTransporter(String prefix, String url) {
		super(prefix);
		this.url = url;
	}

	// --- START TRANSPORTER ---

	/**
	 * Initializes transporter instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Process basic properties (eg. "prefix")
		super.start(broker, config);

		// Process config
		Tree urlNode = config.get("url");
		if (urlNode != null) {
			List<String> urlList;
			if (urlNode.isPrimitive()) {
				urlList = new LinkedList<>();
				String url = urlNode.asString().trim();
				if (!url.isEmpty()) {
					urlList.add(url);
				}
			} else {
				urlList = urlNode.asList(String.class);
			}
			if (!urlList.isEmpty()) {
				url = urlList.get(0);
			}
		}
		authToken = config.get("authToken", authToken);
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		try {

			// Format Socketcluster server URL
			String uri = url;
			if (uri.indexOf(':') == -1) {
				uri = uri + ":80";
			}
			if (uri.indexOf("://") == -1) {
				uri = "ws://" + uri;
			}

			// Create Socketcluster client
			disconnect();
			client = new Socket(uri);

			// TODO Test reconnection strategy
			if (authToken != null) {
				client.setAuthToken(authToken);
			}
			client.setListener(this);
			if (!debug) {
				client.disableLogging();
			}
			client.connect();

		} catch (Exception cause) {
			reconnect(cause);
		}
	}

	@Override
	public void onConnected(Socket socket, Map<String, List<String>> headers) {
		logger.info("Socketcluster pub-sub connection estabilished.");
		connected();
	}

	@Override
	public void onConnectError(Socket socket, WebSocketException exception) {
		reconnect(exception);
	}

	// --- DISCONNECT ---

	@Override
	public void onDisconnected(Socket socket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
			boolean closedByServer) {
		logger.info("Socketcluster pub-sub connection aborted.");
	}

	protected void disconnect() {
		if (client != null) {
			try {
				client.disconnect();
			} catch (Throwable cause) {
				logger.warn("Unexpected error occured while closing Socketcluster client!", cause);
			} finally {
				client = null;
				disconnected();
			}
		}
	}

	// --- RECONNECT ---

	protected void reconnect(Throwable cause) {
		if (cause != null) {
			String msg = cause.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = "Unable to connect to Socketcluster server!";
			} else if (!msg.endsWith("!") && !msg.endsWith(".")) {
				msg += "!";
			}
			logger.warn(msg);
		}
		disconnect();
		logger.info("Trying to reconnect...");
		scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
	}

	// --- UNUSED CALLBACKS ---

	@Override
	public void onAuthentication(Socket socket, Boolean status) {
	}

	@Override
	public void onSetAuthToken(String token, Socket socket) {
	}

	// --- ANY I/O ERROR ---

	@Override
	protected void error(Throwable cause) {
		reconnect(cause);
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stop() {
		disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public Promise subscribe(String channel) {
		Promise promise = new Promise();
		if (client != null) {
			try {
				client.on(channel, this);
			} catch (Exception cause) {
				promise.complete(cause);
			}
		} else {
			promise.complete(new Throwable("Not connected!"));
		}
		return promise;
	}

	// --- PUBLISH ---

	@Override
	public void publish(String channel, Tree message) {
		if (client != null) {
			try {
				if (debug) {
					logger.info("Submitting message to channel \"" + channel + "\":\r\n" + message.toString());
				}
				client.emit(channel, serializer.write(message));
			} catch (Exception cause) {
				logger.warn("Unable to send message to Socketcluster server!", cause);
			}
		}
	}

	// --- RECEIVE ---

	@Override
	public void call(String name, Object data) {
		received(name, (byte[]) data);
	}

	// --- GETTERS / SETTERS ---

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

}