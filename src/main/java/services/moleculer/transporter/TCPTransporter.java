/**
 * This software is licensed under MIT license.<br>
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

import java.io.IOException;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Serverless, fast, NIO-based TCP Transporter (not implemented).
 */
@Name("TCP Transporter")
public class TCPTransporter extends Transporter {

	// --- PROPERTIES ---

	private String[] urls = new String[] { "localhost" };

	// --- CONSTUCTORS ---

	public TCPTransporter() {
		super();
	}

	public TCPTransporter(String prefix, String... urls) {
		super(prefix);
		this.urls = urls;
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
	public final void start(ServiceBroker broker, Tree config) throws Exception {

		// Process basic properties (eg. "prefix")
		super.start(broker, config);

		// Register "tcp://" protocol
		try {
			new java.net.URL("tcp://test");
		} catch (Throwable ignored) {
			java.net.URL.setURLStreamHandlerFactory((protocol) -> {
				if ("tcp".equals(protocol)) {
					return new URLStreamHandler() {

						@Override
						protected final URLConnection openConnection(java.net.URL u) throws IOException {
							throw new UnsupportedOperationException();
						}

					};
				}
				return null;
			});
		}

		// Process config
		Tree urlNode = config.get(URL);
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
				urls = new String[urlList.size()];
				urlList.toArray(urls);
			}
		}
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes all TCP channels.
	 */
	@Override
	public final void stop() {

		// Disconnect all clients
		for (TCPClient client : clients.values()) {
			client.close();
		}
		clients.clear();

	}

	// --- SUBSCRIBE ---

	@Override
	public final Promise subscribe(String channel) {

		// Do nothing
		return Promise.resolve();
	}

	// --- PUBLISH ---

	public HashMap<String, TCPClient> clients = new HashMap<>();

	@Override
	public void publish(String channel, Tree message) {

		// Create data
		// 1 byte = channel
		// 4 byte = message length
		// N byte = message
		byte[] data = new byte[0];

		// Get nodeID by channel
		String nodeID = null;

		// Send data
		if (nodeID == null) {

			// Send data to all clients
			for (TCPClient client : clients.values()) {
				client.add(data);
			}

		} else {

			// Send data to the specified node
			TCPClient client = clients.get(nodeID);
			if (client != null) {
				client.add(data);
			}

		}
	}

	// --- GETTERS / SETTERS ---

}