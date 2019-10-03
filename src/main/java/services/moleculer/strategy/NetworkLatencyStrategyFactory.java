/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.strategy;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.service.Endpoint;
import services.moleculer.service.Name;
import services.moleculer.transporter.Transporter;
import services.moleculer.util.CommonUtils;

/**
 * Factory of lowest network latency strategy. This strategy comes from a random
 * strategy, but preferably communicates with the "closest" nodes (nodes with
 * the lowest response/ping time).
 * 
 * @see RoundRobinStrategyFactory
 * @see SecureRandomStrategyFactory
 * @see XorShiftRandomStrategyFactory
 * @see NanoSecRandomStrategyFactory
 * @see CpuUsageStrategyFactory
 */
@Name("Lowest Network Latency Strategy Factory")
public class NetworkLatencyStrategyFactory extends ArrayBasedStrategyFactory {

	// --- PROPERTIES ---

	/**
	 * This strategy compares number of 'sampleCount' random node.
	 */
	protected int sampleCount = 5;

	/**
	 * Ping period time, in SECONDS.
	 */
	protected int pingInterval = 10;

	/**
	 * Ping timeout time, in MILLISECONDS.
	 */
	protected long pingTimeout = 5000L;

	/**
	 * Number of samples used for average calculation.
	 */
	protected int collectCount = 5;

	// --- COMPONENTS ---

	protected ScheduledExecutorService scheduler;
	protected Transporter transporter;

	// --- CURRENT NODE ID ---

	protected String nodeID;

	// --- RESPONSE TIMES ---

	protected final ConcurrentHashMap<String, Samples> responseTimes = new ConcurrentHashMap<>();

	// --- TIMERS ---

	/**
	 * Cancelable timer of ping loop
	 */
	protected volatile ScheduledFuture<?> pingTimer;

	// --- PREVIOUS NODEID IN PING LOOP ---

	protected volatile String previousNodeID;

	// --- CONSTRUCTORS ---

	public NetworkLatencyStrategyFactory() {
		super(false);
	}

	public NetworkLatencyStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}

	// --- START INVOCATION STRATEGY ---

	/**
	 * Initializes strategy instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Store the current nodeID
		nodeID = broker.getNodeID();

		// Set components
		ServiceBrokerConfig cfg = broker.getConfig();
		scheduler = cfg.getScheduler();
		transporter = broker.getConfig().getTransporter();
		if (transporter == null) {
			logger.warn(
					CommonUtils.nameOf(this, true) + " can't work without transporter. Switched to Round-Robin mode.");
		}

		// Start loop
		pingTimer = scheduler.scheduleWithFixedDelay(this::sendNextPing, pingInterval, pingInterval, TimeUnit.SECONDS);
	}

	// --- STOP INVOCATION STRATEGY ---

	@Override
	public void stopped() {

		// Stop pinger's timer
		if (pingTimer != null) {
			pingTimer.cancel(false);
			pingTimer = null;
		}

		// Cleanup
		responseTimes.clear();
	}

	// --- PING LOOP ---

	protected void sendNextPing() {
		Set<String> nodeIDs = transporter.getAllNodeIDs();

		// Remove this node's ID
		nodeIDs.remove(nodeID);

		// Remove duplications by IP address
		int size = nodeIDs.size() * 2;
		HashSet<String> ips = new HashSet<>(size);
		Iterator<String> i = nodeIDs.iterator();
		while (i.hasNext()) {
			Tree descriptor = transporter.getDescriptor(i.next());
			if (descriptor == null) {
				continue;
			}
			Tree ipList = descriptor.get("ipList");
			if (ipList == null) {
				continue;
			}
			for (Tree ip : ipList) {
				if (ip == null) {
					continue;
				}
				String value = ip.asString();
				if (value == null || value.startsWith("127.")) {
					continue;
				}
				if (ips.contains(value)) {
					i.remove();
					break;
				}
				ips.add(value);
			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException interrupt) {
				return;
			}
		}

		// Has peers?
		if (nodeIDs.isEmpty()) {
			return;
		}

		// Find the next nodeID
		boolean submitted = false;
		if (previousNodeID != null) {
			boolean found = false;
			for (String nextNodeID : nodeIDs) {
				if (previousNodeID.equals(nextNodeID)) {
					found = true;
					continue;
				}
				if (found && !nodeID.equals(nextNodeID)) {

					// Send PING to the next node
					submitted = true;
					previousNodeID = nextNodeID;
					sendPing(nextNodeID);
					break;
				}
			}
		}

		// Send PING to the first node
		if (!submitted) {
			for (String nextNodeID : nodeIDs) {
				if (!nodeID.equals(nextNodeID)) {
					previousNodeID = nextNodeID;
					sendPing(nextNodeID);
					break;
				}
			}
		}
	}

	protected void sendPing(String nextNodeID) {
		final long start = System.currentTimeMillis();
		broker.ping(pingTimeout, nextNodeID).then(in -> {

			// Store the response time
			long duration = System.currentTimeMillis() - start;
			Samples samples = responseTimes.get(nextNodeID);
			if (samples == null) {
				samples = new Samples(collectCount);
				responseTimes.put(nextNodeID, samples);
			}
			samples.addValue(duration);

		}).catchError(err -> {

			// No response / node is down
			responseTimes.remove(nextNodeID);

		});
	}

	// --- GET RESPONSE TIME OF A NODE ---

	protected long getAverageResponseTime(String nextNodeID) {
		Samples samples = responseTimes.get(nextNodeID);
		if (samples == null) {
			return Long.MAX_VALUE;
		}
		return samples.getAverage();
	}

	// --- FACTORY METHOD ---

	@Override
	public <T extends Endpoint> Strategy<T> create() {
		if (transporter == null) {
			return new RoundRobinStrategy<T>(broker, preferLocal);
		}
		return new NetworkLatencyStrategy<T>(broker, preferLocal, sampleCount, this);
	}

	// --- SAMPLES ---

	protected static class Samples {

		protected final long[] data;

		protected volatile int pointer;
		protected volatile long average;

		protected Samples(int averageSamples) {
			data = new long[averageSamples];
			for (int i = 0; i < averageSamples; i++) {
				data[i] = -1;
			}
		}

		protected synchronized void addValue(long value) {
			pointer++;
			if (pointer >= data.length) {
				pointer = 0;
			}
			data[pointer] = value;
			long total = 0;
			int count = 0;
			for (int i = 0; i < data.length; i++) {
				if (data[i] > -1) {
					total += data[i];
					count++;
				}
			}
			average = total / count;
		}

		protected synchronized long getAverage() {
			return average;
		}

	}

	// --- GETTERS / SETTERS ---

	public int getSampleCount() {
		return sampleCount;
	}

	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}

	public int getPingInterval() {
		return pingInterval;
	}

	public void setPingInterval(int pingInterval) {
		this.pingInterval = pingInterval;
	}

	public long getPingTimeout() {
		return pingTimeout;
	}

	public void setPingTimeout(long pingTimeout) {
		this.pingTimeout = pingTimeout;
	}

	public int getCollectCount() {
		return collectCount;
	}

	public void setCollectCount(int collectCount) {
		this.collectCount = collectCount;
	}

}