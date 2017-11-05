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

import java.util.Objects;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.serializer.JsonSerializer;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.service.ServiceRegistry;

/**
 * Base superclass of all Transporter implementations.
 */
@Name("Transporter")
public abstract class Transporter implements MoleculerComponent {

	// --- CONSTANTS ---

	public static final String PACKET_EVENT = "EVENT";
	public static final String PACKET_REQUEST = "REQ";
	public static final String PACKET_RESPONSE = "RES";
	public static final String PACKET_DISCOVER = "DISCOVER";
	public static final String PACKET_INFO = "INFO";
	public static final String PACKET_DISCONNECT = "DISCONNECT";
	public static final String PACKET_HEARTBEAT = "HEARTBEAT";
	public static final String PACKET_PING = "PING";
	public static final String PACKET_PONG = "PONG";

	// --- CHANNELS ---

	public String eventChannel;
	public String requestChannel;
	public String responseChannel;
	public String discoverBroadcastChannel;
	public String discoverChannel;
	public String infoBroadcastChannel;
	public String infoChannel;
	public String disconnectChannel;
	public String heartbeatChannel;
	public String pingBroadcastChannel;
	public String pingChannel;
	public String pongChannel;

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- PROPERTIES ---

	protected String prefix;
	protected ServiceBroker broker;
	protected String nodeID;

	// --- SERIALIZER / DESERIALIZER ---

	protected Serializer serializer = new JsonSerializer();

	// --- COMPONENTS ---

	protected Executor executor;
	protected ServiceRegistry serviceRegistry;

	// --- CONSTUCTORS ---

	public Transporter() {
		this("MOL");
	}

	public Transporter(String prefix) {
		this.prefix = prefix;
	}

	public Transporter(String prefix, Serializer serializer) {
		this.prefix = prefix;
		this.serializer = serializer;
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

		// Process config
		prefix = config.get("prefix", prefix);

		// Set serializer
		Tree serializerNode = config.get("serializer");
		if (serializerNode != null) {
			String type;
			if (serializerNode.isPrimitive()) {
				type = serializerNode.asString();
			} else {
				type = serializerNode.get("type", "json");
			}

			@SuppressWarnings("unchecked")
			Class<? extends Serializer> c = (Class<? extends Serializer>) Class.forName(typeToClass(type));
			serializer = c.newInstance();
		} else {
			serializerNode = new Tree();
		}
		
		// Start serializer
		serializer.start(broker, serializerNode);

		// Get componentMap
		executor = broker.components().executor();
		serviceRegistry = broker.components().registry();

		// Get properties from broker
		this.broker = broker;
		this.nodeID = broker.nodeID();

		// Set channel names
		eventChannel = channel(PACKET_EVENT, nodeID);
		requestChannel = channel(PACKET_REQUEST, nodeID);
		responseChannel = channel(PACKET_RESPONSE, nodeID);
		discoverBroadcastChannel = channel(PACKET_DISCOVER, null);
		discoverChannel = channel(PACKET_DISCOVER, nodeID);
		infoBroadcastChannel = channel(PACKET_INFO, null);
		infoChannel = channel(PACKET_INFO, nodeID);
		disconnectChannel = channel(PACKET_DISCONNECT, null);
		heartbeatChannel = channel(PACKET_HEARTBEAT, null);
		pingBroadcastChannel = channel(PACKET_PING, null);
		pingChannel = channel(PACKET_PING, nodeID);
		pongChannel = channel(PACKET_PONG, nodeID);
	}

	protected String channel(String cmd, String nodeID) {
		StringBuilder name = new StringBuilder(64);
		name.append(prefix);
		name.append('.');
		name.append(cmd);
		if (nodeID != null && !nodeID.isEmpty()) {
			name.append('.');
			name.append(nodeID);
		}
		return name.toString();
	}

	protected String typeToClass(String type) {
		String test = type.toLowerCase();
		if ("json".equals(test)) {
			return "services.moleculer.serializer.JsonSerializer";
		}
		if ("msgpack".equals(test) || "messagepack".equals(test)) {
			return "services.moleculer.serializer.MsgPackSerializer";
		}
		if ("bson".equals(test)) {
			return "services.moleculer.serializer.BsonSerializer";
		}
		if ("cbor".equals(test)) {
			return "services.moleculer.serializer.CborSerializer";
		}
		if ("smile".equals(test)) {
			return "services.moleculer.serializer.SmileSerializer";
		}
		if ("ion".equals(test)) {
			return "services.moleculer.serializer.IonSerializer";
		}
		if (test.indexOf('.') > -1) {
			return type;
		}
		throw new IllegalArgumentException("Invalid serializer type (" + type + ")!");
	}

	// --- SERVER CONNECTED ---

	protected void connected() {
		executor.execute(() -> {

			// Subscribe channels
			subscribe(eventChannel);
			subscribe(requestChannel);
			subscribe(responseChannel);
			subscribe(discoverBroadcastChannel);
			subscribe(discoverChannel);
			subscribe(infoBroadcastChannel);
			subscribe(infoChannel);
			subscribe(disconnectChannel);
			subscribe(heartbeatChannel);
			subscribe(pingBroadcastChannel);
			subscribe(pingChannel);
			subscribe(pongChannel);

		});

		// TODO
		// - Start heartbeat timer
		// - Start checkNodes timer
	}

	// --- SERVER DISCONNECTED ---

	protected void disconnected() {

		// TODO on disconnected (move to superclass):
		// Stop heartbeat timer
		// Stop checkNodes timer
		// Call `this.tx.disconnect()`
	}

	// --- STOP TRANSPORTER ---

	/**
	 * Closes transporter.
	 */
	@Override
	public void stop() {
		// If isConnected() call `sendDisconnectPacket()`
	}

	// --- REQUEST PACKET ---

	public Tree createRequestPacket(Tree params, CallingOptions opts, Context ctx) {
		return null;
	}

	// --- PUBLISH ---

	public void publish(String cmd, String nodeID, Tree message) {
		publish(channel(cmd, nodeID), message);
	}

	public abstract void publish(String channel, Tree message);

	// --- SUBSCRIBE ---

	public void subscribe(String cmd, String nodeID) {
		subscribe(channel(cmd, nodeID));
	}

	public abstract void subscribe(String channel);

	// --- PROCESS INCOMING MESSAGE ---

	protected void received(String channel, byte[] message, Object connectionID) {
		executor.execute(() -> {

			// Parse message
			Tree data;
			try {
				data = serializer.read(message);
			} catch (Exception cause) {
				logger.warn("Unable to parse incoming message!", cause);
				failed(connectionID, cause);
				return;
			}

			// Send message to proper component
			try {
				System.out.println(channel + " -> " + data);

				// Messages of ServiceRegistry
				if (channel.equals(eventChannel) || channel.equals(requestChannel) || channel.equals(responseChannel)) {
					serviceRegistry.receive(data);
					return;
				}

			} catch (Exception cause) {
				logger.warn("Unable to process incoming message!", cause);
			}
		});
	}

	// --- SUBSCRIPTION FINISHED ---

	protected void subscribed(String channel) {
		executor.execute(() -> {
			try {
				logger.info(channel + " channel subscribed.");

				// Send INFO to all nodes
				if (channel.equals(discoverBroadcastChannel)) {
					Tree message = new Tree();
					message.put("ver", "2");
					message.put("sender", nodeID);
					publish(discoverBroadcastChannel, message);
				}

			} catch (Exception cause) {
				logger.warn("Unable to process subscription!", cause);
			}
		});
	}

	// --- OPTIONAL DISCONNECTION ON ERROR ---

	protected void failed(Object connectionID, Throwable cause) {
	}

	// --- GETTERS / SETTERS ---

	public final Serializer getSerializer() {
		return serializer;
	}

	public final void setSerializer(Serializer serializer) {
		this.serializer = Objects.requireNonNull(serializer);
	}

}