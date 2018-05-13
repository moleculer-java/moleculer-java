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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.service.Endpoint;
import services.moleculer.service.Name;
import services.moleculer.transporter.Transporter;
import services.moleculer.util.CommonUtils;

/**
 * Factory of lowest network latency strategy.
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
	 * Sample count
	 */
	protected int maxTries = 3;

	/**
	 * Ping period time, in SECONDS.
	 */
	protected int pingPeriod = 3;

	/**
	 * Ping period time, in MILLISECONDS.
	 */
	protected long pingTimeout = 5000L;

	// --- COMPONENTS ---

	protected ScheduledExecutorService scheduler;
	protected Transporter transporter;

	// --- CURRENT NODE ID ---

	protected String nodeID;

	// --- RESPONSE TIMES ---

	protected final ConcurrentHashMap<String, Long> responseTimes = new ConcurrentHashMap<>();

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
		pingTimer = scheduler.scheduleWithFixedDelay(this::sendNextPing, pingPeriod, pingPeriod, TimeUnit.SECONDS);
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
		if (nodeIDs == null || nodeIDs.size() < 2) {

			// No peers
			return;
		}
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
		if (!submitted) {

			// Send PING to the first node
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
		broker.ping(nextNodeID, pingTimeout).then(in -> {

			// Store the response time
			long duration = System.currentTimeMillis() - start;
			responseTimes.put(nextNodeID, duration);

		}).catchError(err -> {

			// No response / node is down
			responseTimes.remove(nextNodeID);

		});
	}

	// --- GET RESPONSE TIME OF A NODE ---

	protected long getResponseTime(String nextNodeID) {
		Long duration = responseTimes.get(nextNodeID);
		return duration == null ? Long.MAX_VALUE : duration.longValue();
	}

	// --- FACTORY METHOD ---

	@Override
	public <T extends Endpoint> Strategy<T> create() {
		if (transporter == null) {
			return new RoundRobinStrategy<T>(broker, preferLocal);
		}
		return new NetworkLatencyStrategy<T>(broker, preferLocal, maxTries, this);
	}

	// --- GETTERS / SETTERS ---

	public int getMaxTries() {
		return maxTries;
	}

	public void setMaxTries(int maxTries) {
		this.maxTries = maxTries;
	}

	public int getPingPeriod() {
		return pingPeriod;
	}

	public void setPingPeriod(int pingPeriod) {
		this.pingPeriod = pingPeriod;
	}

	public long getPingTimeout() {
		return pingTimeout;
	}

	public void setPingTimeout(long pingTimeout) {
		this.pingTimeout = pingTimeout;
	}

}