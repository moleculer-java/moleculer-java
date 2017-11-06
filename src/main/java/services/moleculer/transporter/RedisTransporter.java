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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * 
 */
@Name("Redis Transporter")
public final class RedisTransporter extends Transporter {

	// --- PROPERTIES ---

	private String[] urls = new String[] { "localhost" };
	private String password;
	private boolean useSSL;
	private boolean startTLS;

	// --- REDIS CLIENTS ---

	private final RedisPubSubClient clientSub = new RedisPubSubClient();
	private final RedisPubSubClient clientPub = new RedisPubSubClient();

	// --- CONSTUCTORS ---

	public RedisTransporter() {
		super();
	}

	public RedisTransporter(String prefix) {
		super(prefix);
	}

	public RedisTransporter(String prefix, String... urls) {
		super(prefix);
		this.urls = urls;
	}

	public RedisTransporter(String prefix, boolean useSSL, boolean startTLS, String... urls) {
		super(prefix);
		this.useSSL = useSSL;
		this.startTLS = startTLS;
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

		// Process config
		Tree urlNode = config.get("urls");
		if (urlNode == null) {
			urlNode = config.get("url");
		}
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
		password = config.get("password", password);
		useSSL = config.get("useSSL", useSSL);
		startTLS = config.get("startTLS", startTLS);

		// Init Redis clients
		ExecutorService executorService = broker.components().executor();
		Promise subConnected = clientSub.connect(urls, password, useSSL, startTLS, executorService, (channel) -> {
			
			// Channel subscribed
			subscribed(channel);
			
		}, (channel, message) -> {
			
			// Message received
			received(channel, message, null);
			
		});
		Promise pubConnected = clientPub.connect(urls, password, useSSL, startTLS, executorService, null, null);
		
		// Wait for connections (block main thread)
		Promise.all(subConnected, pubConnected).toCompletableFuture().get();
		
		// All clients connected
		connected();
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public final void stop() {
		clientSub.disconnect();
		clientPub.disconnect();
	}

	// --- SUBSCRIBE ---

	@Override
	public final void subscribe(String channel) {
		clientSub.subscribe(channel);
	}

	// --- PUBLISH ---

	@Override
	public final void publish(String channel, Tree message) {
		if (clientPub != null) {
			try {

				// TODO
				logger.info("TO " + channel + ": " + new String(serializer.write(message)));

				clientPub.publish(channel, serializer.write(message));

			} catch (Exception cause) {
				logger.warn("Unable to send message to Redis!", cause);
			}
		}
	}

	// --- GETTERS / SETTERS ---

	public final String[] getUrls() {
		return urls;
	}

	public final void setUrls(String[] urls) {
		this.urls = urls;
	}

	public final String getPassword() {
		return password;
	}

	public final void setPassword(String password) {
		this.password = password;
	}

	public final boolean isUseSSL() {
		return useSSL;
	}

	public final void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public final boolean isStartTLS() {
		return startTLS;
	}

	public final void setStartTLS(boolean startTLS) {
		this.startTLS = startTLS;
	}

}