/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.service.Name;

/**
 * This is a Transporter that can connect multiple ServiceBrokers running in the
 * same JVM. The calls are made in separate Threads, so call timeouts can be
 * used. Used primarily for testing Serializers. Usage:
 * 
 * <pre>
 * ServiceBroker broker1 = ServiceBroker.builder().nodeID("node1").transporter(new InternalTransporter()).build();
 * ServiceBroker broker2 = ServiceBroker.builder().nodeID("node2").transporter(new InternalTransporter()).build();
 * </pre>
 * 
 * @see TcpTransporter
 * @see RedisTransporter
 * @see NatsTransporter
 * @see NatsStreamingTransporter
 * @see MqttTransporter
 * @see KafkaTransporter
 * @see AmqpTransporter
 * @see JmsTransporter
 */
@Name("Internal Transporter")
public class InternalTransporter extends Transporter {

	// --- SHARED STATIC INSTANCE ---

	protected static final Subscriptions sharedInstance = new Subscriptions();

	// --- SUBSCRIPTION HANDLER ---

	protected final Subscriptions subscriptions;

	// --- REGISTERED CHANNELS ---

	protected HashSet<String> channels = new HashSet<>();

	// --- CONSTRUCTOR ---

	public InternalTransporter() {
		this(sharedInstance);
	}

	public InternalTransporter(Subscriptions subscriptions) {
		this.subscriptions = Objects.requireNonNull(subscriptions);
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		connected();
	}

	// --- REGISTER SUBSCRIPTION ---

	@Override
	public Promise subscribe(String channel) {
		subscriptions.register(channel, this);
		synchronized (channels) {
			channels.add(channel);
		}
		return Promise.resolve();
	}

	// --- DEREGISTER CHANNELS ---

	@Override
	public void stopped() {
		boolean notify = deregister();
		super.stopped();

		// Notify internal listeners
		if (notify) {
			broadcastTransporterDisconnected();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		deregister();
	}

	protected boolean deregister() {
		synchronized (channels) {
			if (!channels.isEmpty()) {
				for (String channel : channels) {
					subscriptions.deregister(channel, this, !channel.endsWith('.' + nodeID));
				}
				channels.clear();
				return true;
			}
		}
		return false;
	}

	// --- SEND DATA ---

	@Override
	public void publish(String channel, Tree message) {
		try {

			// Metrics
			byte[] bytes = serializer.write(message);
			if (metrics != null) {
				counterTransporterPacketsSentTotal.increment();
				counterTransporterPacketsSentBytes.increment(bytes.length);
			}

			// Send bytes
			subscriptions.send(channel, bytes);
		} catch (Exception cause) {
			logger.warn("Unable to publish message!", cause);
		}
	}

	// --- SUBSCRIPTION HANDLER ---

	public static class Subscriptions {

		// --- SUBSCRIPTIONS PER CHANNEL ---

		protected final HashMap<String, SubscriptionSet> sets = new HashMap<>(64);

		// --- READ/WRITE LOCK ---

		protected final ReadLock readLock;
		protected final WriteLock writeLock;

		// --- CONSTRUCTOR ---

		public Subscriptions() {

			// Create locks
			ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
			readLock = lock.readLock();
			writeLock = lock.writeLock();
		}

		// --- METHODS ---

		protected void register(String channel, InternalTransporter transporter) {
			SubscriptionSet set = getSubscriptionSet(channel);
			if (set == null) {
				set = new SubscriptionSet(channel);
				SubscriptionSet previous;
				writeLock.lock();
				try {
					previous = sets.putIfAbsent(channel, set);
				} finally {
					writeLock.unlock();
				}
				if (previous != null) {
					set = previous;
				}
			}
			set.register(transporter);
		}

		protected void deregister(String channel, InternalTransporter transporter, boolean shared) {
			SubscriptionSet set = getSubscriptionSet(channel);
			if (set == null) {
				return;
			}
			if (shared) {
				set.deregister(transporter);
			} else {
				writeLock.lock();
				try {
					sets.remove(channel);
				} finally {
					writeLock.unlock();
				}
			}
		}

		protected void send(String channel, byte[] message) throws Exception {
			SubscriptionSet set = getSubscriptionSet(channel);
			if (set != null) {
				set.send(message);
			}
		}

		protected SubscriptionSet getSubscriptionSet(String channel) {
			SubscriptionSet set = null;
			readLock.lock();
			try {
				set = sets.get(channel);
			} finally {
				readLock.unlock();
			}
			return set;
		}

	}

	protected static class SubscriptionSet {

		// --- REGISTERED TRANSPORTERS ---

		protected final WeakHashMap<InternalTransporter, InternalTransporter> set = new WeakHashMap<InternalTransporter, InternalTransporter>(
				64);

		// --- READ/WRITE LOCK ---

		protected final ReadLock readLock;
		protected final WriteLock writeLock;

		// --- CHANNEL ---

		protected final String channel;

		// --- CONSTRUCTOR ---

		protected SubscriptionSet(String channel) {
			ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
			this.readLock = lock.readLock();
			this.writeLock = lock.writeLock();
			this.channel = channel;
		}

		protected void register(InternalTransporter transporter) {
			writeLock.lock();
			try {
				set.put(transporter, transporter);
			} finally {
				writeLock.unlock();
			}
		}

		protected void deregister(InternalTransporter transporter) {
			writeLock.lock();
			try {
				set.remove(transporter);
			} finally {
				writeLock.unlock();
			}
		}

		protected void send(byte[] message) throws Exception {
			readLock.lock();
			try {
				if (set.isEmpty()) {
					return;
				}
				for (InternalTransporter transporter : set.keySet()) {
					if (transporter != null) {
						transporter.executor.execute(() -> {
							transporter.processReceivedMessage(channel, message);
						});
					}
				}
			} finally {
				readLock.unlock();
			}

		}

	}

}