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
package services.moleculer.service;

import static services.moleculer.transporter.Transporter.PACKET_PING;
import static services.moleculer.transporter.Transporter.PACKET_RESPONSE;
import static services.moleculer.util.CommonUtils.convertAnnotations;
import static services.moleculer.util.CommonUtils.getFieldFromProxy;
import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.mergeMeta;
import static services.moleculer.util.CommonUtils.nameOf;
import static services.moleculer.util.CommonUtils.throwableToTree;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.concurrent.locks.StampedLock;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallOptions;
import services.moleculer.context.Context;
import services.moleculer.error.InvalidPacketDataError;
import services.moleculer.error.MoleculerError;
import services.moleculer.error.MoleculerErrorUtils;
import services.moleculer.error.ProtocolVersionMismatchError;
import services.moleculer.error.RequestRejectedError;
import services.moleculer.error.RequestTimeoutError;
import services.moleculer.error.ServiceNotAvailableError;
import services.moleculer.error.ServiceNotFoundError;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.metrics.MetricConstants;
import services.moleculer.metrics.MetricCounter;
import services.moleculer.metrics.MetricGauge;
import services.moleculer.metrics.Metrics;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.stream.IncomingStream;
import services.moleculer.stream.PacketListener;
import services.moleculer.stream.PacketStream;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.UidGenerator;
import services.moleculer.util.CheckedTree;
import services.moleculer.util.FastBuildTree;

/**
 * Default implementation of the Service Registry.
 */
@Name("Default Service Registry")
public class DefaultServiceRegistry extends ServiceRegistry implements MetricConstants {

	// --- REGISTERED MIDDLEWARES ---

	protected final LinkedHashSet<Middleware> middlewares = new LinkedHashSet<>(32);

	// --- REGISTERED LOCAL SERVICES ---

	protected final LinkedHashMap<String, Service> services = new LinkedHashMap<>(64);

	// --- REGISTERED STRATEGIES PER ACTIONS ---

	protected final HashMap<String, Strategy<ActionEndpoint>> strategies = new HashMap<>(256);

	// --- REGISTERED LOCAL AND REMOTE SERVICE NAMES ---

	protected final HashSet<String> names = new HashSet<>(64);

	// --- PENDING REMOTE INVOCATIONS ---

	protected final ConcurrentHashMap<String, PendingPromise> promises = new ConcurrentHashMap<>(1024);

	// --- REGISTERED STREAMS ---

	protected final HashMap<String, IncomingStream> requestStreams = new HashMap<>(1024);
	protected final HashMap<String, IncomingStream> responseStreams = new HashMap<>(1024);

	// --- STREAM LOCKS ---

	protected final ReadLock requestStreamReadLock;
	protected final WriteLock requestStreamWriteLock;

	protected final ReadLock responseStreamReadLock;
	protected final WriteLock responseStreamWriteLock;

	// --- PROPERTIES ---

	/**
	 * Invoke all local services via Thread pool (true) or directly (false)
	 */
	protected boolean asyncLocalInvocation;

	/**
	 * Check protocol version
	 */
	protected boolean checkVersion;

	/**
	 * ServiceBroker's protocol version
	 */
	protected String protocolVersion = "4";

	/**
	 * Write exceptions into the log file
	 */
	protected boolean writeErrorsToLog = true;

	/**
	 * Stream inactivity/read timeout in MILLISECONDS (0 = no timeout). It may
	 * be useful if you want to remove the wrong packages from the memory.
	 */
	protected long streamTimeout;

	// --- READ/WRITE LOCK ---

	protected final StampedLock lock = new StampedLock();

	// --- LOCAL NODE ID ---

	protected String nodeID;

	// --- COMPONENTS ---

	protected ExecutorService executor;
	protected ScheduledExecutorService scheduler;
	protected StrategyFactory strategyFactory;
	protected Transporter transporter;
	protected Eventbus eventbus;
	protected UidGenerator uidGenerator;
	protected ServiceInvoker serviceInvoker;
	protected Metrics metrics;

	// --- VARIABLES OF THE TIMEOUT HANDLER ---

	/**
	 * Cancelable timer for handling timeouts of action calls
	 */
	protected final AtomicReference<ScheduledFuture<?>> callTimeoutTimer = new AtomicReference<>();

	/**
	 * Next scheduled time to check timeouts
	 */
	protected final AtomicLong prevTimeoutAt = new AtomicLong();

	// --- WAIT FOR SERVICE(S) ---

	/**
	 * Cancelable timer for handling "wait for service" calls
	 */
	protected ScheduledFuture<?> servicesOnlineTimer;

	/**
	 * Promises of the "waitingForServices" calls
	 */
	protected final LinkedList<ServiceListener> serviceListeners = new LinkedList<>();

	// --- TIMESTAMP OF SERVICE DESCRIPTOR ---

	/**
	 * Timestamp of the service descriptor of this Moleculer Node (~=
	 * "generated at" timestamp)
	 */
	protected AtomicLong timestamp = new AtomicLong();

	// --- CACHED SERVICE DESCRIPTOR ---

	protected volatile FastBuildTree cachedDescriptor;

	// --- METRICS ---
	
	protected MetricGauge gaugeRequestsActive;
	protected MetricGauge gaugeStreamsReceiveActive;
	protected MetricCounter counterOrphanResponseTotal;
	
	// --- CONSTRUCTORS ---

	public DefaultServiceRegistry() {
		this(false);
	}

	public DefaultServiceRegistry(boolean asyncLocalInvocation) {

		// Async or direct local invocation
		this.asyncLocalInvocation = asyncLocalInvocation;

		// Init locks
		ReentrantReadWriteLock requestStreamLock = new ReentrantReadWriteLock(false);
		requestStreamReadLock = requestStreamLock.readLock();
		requestStreamWriteLock = requestStreamLock.writeLock();

		ReentrantReadWriteLock responseStreamLock = new ReentrantReadWriteLock(false);
		responseStreamReadLock = responseStreamLock.readLock();
		responseStreamWriteLock = responseStreamLock.writeLock();
	}

	// --- INIT SERVICE REGISTRY ---

	/**
	 * Initializes ServiceRegistry instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Local nodeID
		this.nodeID = broker.getNodeID();

		// Set the protocol version
		this.protocolVersion = broker.getProtocolVersion();

		// Set components
		ServiceBrokerConfig cfg = broker.getConfig();
		this.executor = cfg.getExecutor();
		this.scheduler = cfg.getScheduler();
		this.strategyFactory = cfg.getStrategyFactory();
		this.transporter = cfg.getTransporter();
		this.eventbus = cfg.getEventbus();
		this.uidGenerator = cfg.getUidGenerator();
		this.serviceInvoker = cfg.getServiceInvoker();
		if (cfg.isMetricsEnabled()) {
			metrics = cfg.getMetrics();
			if (metrics != null) {
				gaugeRequestsActive = metrics.set(MOLECULER_TRANSIT_REQUESTS_ACTIVE, MOLECULER_TRANSIT_REQUESTS_ACTIVE_DESC, 0);
				gaugeStreamsReceiveActive = metrics.set(MOLECULER_TRANSIT_STREAMS_RECEIVE_ACTIVE, MOLECULER_TRANSIT_STREAMS_RECEIVE_ACTIVE_DESC, 0);
				counterOrphanResponseTotal = metrics.increment(MOLECULER_TRANSIT_ORPHAN_RESPONSE_TOTAL, MOLECULER_TRANSIT_ORPHAN_RESPONSE_TOTAL_DESC, 0);				
			}
		}
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public void stopped() {

		// Stop timer
		ScheduledFuture<?> task = callTimeoutTimer.get();
		if (task != null) {
			task.cancel(false);
		}

		// Stop pending invocations
		Iterator<PendingPromise> pendingPromises = promises.values().iterator();
		while (pendingPromises.hasNext()) {
			PendingPromise pending = pendingPromises.next();
			pendingPromises.remove();
			try {
				pending.promise.complete(new RequestRejectedError(nodeID, pending.action));
			} catch (Throwable cause) {
				logger.warn("Unable to reject action \"" + pending.action + "\"!", cause);
			}
		}

		// Stop middlewares
		for (Middleware middleware : middlewares) {
			try {
				middleware.stopped();
			} catch (Throwable cause) {
				logger.warn("Unable to stop middleware \"" + middleware.name + "\"!", cause);
			}
		}

		// Stop registered services
		stopAllLocalServices();

		// Clear registries
		final long stamp = lock.writeLock();
		try {

			// Delete strategies (and registered actions)
			strategies.clear();

			// Delete all service names
			names.clear();

			// Delete middlewares
			middlewares.clear();

			// Delete cached node descriptor
			clearDescriptorCache();

		} finally {
			lock.unlockWrite(stamp);
		}
	}

	// --- CALL TIMEOUT CHECKER TASK ---

	protected void checkTimeouts() {
		long now = System.currentTimeMillis();

		// Check timeouted promises
		PendingPromise pending;
		Iterator<PendingPromise> i = promises.values().iterator();
		boolean removed = false;
		while (i.hasNext()) {
			pending = i.next();
			if (pending.timeoutAt > 0 && now >= pending.timeoutAt) {

				// Action is unknown at this location
				pending.promise.complete(new RequestTimeoutError(this.nodeID, pending.action));
				i.remove();
				removed = true;

				// Metrics
				if (metrics != null) {
					metrics.increment(MOLECULER_REQUEST_TIMEOUT_TOTAL, MOLECULER_REQUEST_TIMEOUT_TOTAL_DESC, "action",
							pending.action);
				}
			}
		}

		// Check timeouted request streams
		IncomingStream stream;
		long timeoutAt;
		requestStreamWriteLock.lock();
		try {
			Iterator<IncomingStream> j = requestStreams.values().iterator();
			while (j.hasNext()) {
				stream = j.next();
				timeoutAt = stream.getTimeoutAt();
				if (timeoutAt > 0 && now >= timeoutAt) {
					stream.error(new RequestTimeoutError(this.nodeID, "unknown"));
					j.remove();
					removed = true;
				}
			}
		} finally {
			requestStreamWriteLock.unlock();
		}

		// Check timeouted response streams
		try {
			Iterator<IncomingStream> j = responseStreams.values().iterator();
			while (j.hasNext()) {
				stream = j.next();
				timeoutAt = stream.getTimeoutAt();
				if (timeoutAt > 0 && now >= timeoutAt) {
					stream.error(new RequestTimeoutError(nodeID, "unknown"));
					j.remove();
					removed = true;
				}
			}
		} finally {
			responseStreamWriteLock.unlock();
		}

		// Metrics
		if (removed && metrics != null) {
			gaugeRequestsActive.set(promises.size());
			gaugeStreamsReceiveActive.set(requestStreams.size() + responseStreams.size());
		}

		// Reschedule
		if (removed) {
			scheduler.execute(() -> {
				reschedule(Long.MAX_VALUE);
			});
		} else {
			prevTimeoutAt.set(0);
		}
	}

	// --- CALL TIMEOUT HANDLING ---

	/**
	 * Recalculates the next timeout checking time.
	 * 
	 * @param minTimeoutAt
	 *            next / closest timestamp
	 */
	protected void reschedule(long minTimeoutAt) {
		if (minTimeoutAt == Long.MAX_VALUE) {
			for (PendingPromise pending : promises.values()) {
				if (pending.timeoutAt > 0 && pending.timeoutAt < minTimeoutAt) {
					minTimeoutAt = pending.timeoutAt;
				}
			}
			long timeoutAt;
			requestStreamReadLock.lock();
			try {
				for (IncomingStream stream : requestStreams.values()) {
					timeoutAt = stream.getTimeoutAt();
					if (timeoutAt > 0 && timeoutAt < minTimeoutAt) {
						minTimeoutAt = timeoutAt;
					}
				}
			} finally {
				requestStreamReadLock.unlock();
			}
			responseStreamReadLock.lock();
			try {
				for (IncomingStream stream : responseStreams.values()) {
					timeoutAt = stream.getTimeoutAt();
					if (timeoutAt > 0 && timeoutAt < minTimeoutAt) {
						minTimeoutAt = timeoutAt;
					}
				}
			} finally {
				responseStreamReadLock.unlock();
			}
		}
		long now = System.currentTimeMillis();
		if (minTimeoutAt == Long.MAX_VALUE) {
			ScheduledFuture<?> t = callTimeoutTimer.get();
			if (t != null) {
				if (prevTimeoutAt.get() > now) {
					t.cancel(false);
					prevTimeoutAt.set(0);
				} else {
					callTimeoutTimer.set(null);
					prevTimeoutAt.set(0);
				}
			}
		} else {
			minTimeoutAt = (minTimeoutAt / 100 * 100) + 100;
			long prev = prevTimeoutAt.getAndSet(minTimeoutAt);
			if (prev == minTimeoutAt) {

				// Next when not changed
				return;
			}

			// Stop previous timer
			ScheduledFuture<?> t = callTimeoutTimer.get();
			if (t != null) {
				t.cancel(false);
			}

			// Schedule next timeout timer
			long delay = Math.max(10, minTimeoutAt - now);
			callTimeoutTimer.set(scheduler.schedule(this::checkTimeouts, delay, TimeUnit.MILLISECONDS));
		}
	}

	// --- REGISTER PROMISE ---

	protected void register(String id, Promise promise, long timeoutAt, String nodeID, String action, Tree req) {

		// Register Promise
		promises.put(id, new PendingPromise(promise, timeoutAt, nodeID, action, req));

		// Metrics
		if (gaugeRequestsActive != null) {
			gaugeRequestsActive.set(promises.size());
		}

		// Reschedule
		long nextTimeoutAt = prevTimeoutAt.get();
		if (nextTimeoutAt == 0 || (timeoutAt / 100 * 100) + 100 < nextTimeoutAt || promises.size() < 3) {
			scheduler.execute(() -> {
				reschedule(timeoutAt);
			});
		}
	}

	protected void deregister(String id) {

		// Remove Promise
		PendingPromise removed = promises.remove(id);

		// Metrics
		if (gaugeRequestsActive != null && removed != null) {
			gaugeRequestsActive.set(promises.size());
		}
	}

	// --- RECEIVE REQUEST FROM REMOTE SERVICE ---

	@Override
	public void receiveRequest(Tree message) {

		// Get request's unique ID
		String id = message.get("id", (String) null);
		if (id == null || id.isEmpty()) {
			logger.warn("Missing \"id\" property!");
			return;
		}

		// Get sender's nodeID
		String sender = message.get("sender", (String) null);
		if (sender == null || sender.isEmpty()) {
			logger.warn("Missing \"sender\" property!");
			return;
		}

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!protocolVersion.equals(ver)) {
				logger.warn("Invalid protocol version (" + ver + ")!");
				transporter.publish(PACKET_RESPONSE, sender, throwableToTree(id, nodeID, protocolVersion,
						new ProtocolVersionMismatchError(nodeID, protocolVersion, ver)));
				return;
			}
		}

		// Incoming stream handling
		IncomingStream requestStream;
		requestStreamReadLock.lock();
		try {
			requestStream = requestStreams.get(id);
		} finally {
			requestStreamReadLock.unlock();
		}
		if (requestStream != null) {
			boolean remove = false;
			try {
				if (requestStream.receive(message)) {
					remove = true;
				}
			} catch (Throwable error) {
				remove = true;

				// Send error
				try {
					transporter.publish(PACKET_RESPONSE, sender, throwableToTree(id, nodeID, protocolVersion, error));
				} catch (Throwable ignored) {
					logger.debug("Unable to send response!", ignored);
				}

				// Write error to log file
				if (writeErrorsToLog) {
					logger.error("Unexpected error occurred while streaming!", error);
				}

			}
			if (remove) {
				requestStreamWriteLock.lock();
				try {
					requestStreams.remove(id);
				} finally {
					requestStreamWriteLock.unlock();
				}
			}
		} else if (message.get("stream", false) || message.get("seq", 0) > 0) {
			requestStreamWriteLock.lock();
			try {
				requestStream = requestStreams.get(id);
				if (requestStream == null) {
					requestStream = new IncomingStream(nodeID, scheduler, streamTimeout);
					requestStreams.put(id, requestStream);
				}
			} finally {
				requestStreamWriteLock.unlock();
			}
			if (requestStream.receive(message)) {
				requestStreamWriteLock.lock();
				try {
					requestStreams.remove(id);
				} finally {
					requestStreamWriteLock.unlock();
				}
			}
		}

		// Metrics
		if (gaugeStreamsReceiveActive != null && requestStream != null) {
			gaugeStreamsReceiveActive.set(requestStreams.size() + responseStreams.size());			
		}

		// Get action property
		String action = message.get("action", (String) null);
		if (action == null || action.isEmpty()) {
			if (requestStream == null) {
				logger.warn("Missing \"action\" property!\r\n" + message);
				transporter.publish(PACKET_RESPONSE, sender,
						throwableToTree(id, nodeID, protocolVersion, new InvalidPacketDataError(nodeID)));
			}
			return;
		}
		if (requestStream != null && requestStream.inited()) {

			// Action method invoked (do not invoke twice)
			return;
		}

		// Get strategy (action endpoint array) by action name
		Strategy<ActionEndpoint> strategy = null;
		long stamp = lock.tryOptimisticRead();
		if (stamp != 0) {
			try {
				strategy = strategies.get(action);
			} catch (Exception modified) {
				stamp = 0;
			}
		}
		if (!lock.validate(stamp) || stamp == 0) {
			stamp = lock.readLock();
			try {
				strategy = strategies.get(action);
			} finally {
				lock.unlockRead(stamp);
			}
		}
		if (strategy == null) {
			logger.warn("Invalid action name (" + action + ")!");
			transporter.publish(PACKET_RESPONSE, sender,
					throwableToTree(id, nodeID, protocolVersion, new ServiceNotFoundError(nodeID, action)));
			return;
		}

		// Process params and meta
		Tree params = message.get("params");
		Tree meta = message.get("meta");
		if (meta != null && !meta.isEmpty()) {
			if (params == null || params.isNull()) {
				params = new CheckedTree(new LinkedHashMap<String, Object>(), meta.asObject());
			} else {
				params = new CheckedTree(params.asObject(), meta.asObject());
			}
		}

		// Get timeout
		int timeout = message.get("timeout", 0);
		CallOptions.Options opts;
		if (timeout > 0) {
			opts = CallOptions.timeout(timeout);
		} else {
			opts = null;
		}

		// Get other properties
		int level = message.get("level", 1);
		String parentID = message.get("parentID", (String) null);
		String requestID = message.get("requestID", id);

		// Create context
		PacketStream stream = requestStream == null ? null : requestStream.getPacketStream();
		Context ctx = new Context(serviceInvoker, eventbus, uidGenerator, id, name, params, level, parentID, requestID,
				stream, opts, sender);

		// Get local action endpoint (with cache handling)
		ActionEndpoint endpoint = strategy.getEndpoint(ctx, nodeID);
		if (endpoint == null || endpoint.localOnly) {
			logger.warn("Not a local action (" + action + ")!");
			transporter.publish(PACKET_RESPONSE, sender,
					throwableToTree(id, nodeID, protocolVersion, new ServiceNotAvailableError(nodeID, action)));
			return;
		}

		// Invoke action
		try {
			new Promise(endpoint.handler(ctx)).then(data -> {

				// Send response
				FastBuildTree msg = new FastBuildTree(8);
				msg.putUnsafe("ver", protocolVersion);
				msg.putUnsafe("sender", nodeID);
				msg.putUnsafe("id", id);
				msg.putUnsafe("success", true);

				PacketStream responseStream = null;
				if (data != null) {
					Object d = data.asObject();
					if (d != null && d instanceof PacketStream) {
						msg.putUnsafe("stream", true);
						msg.putUnsafe("seq", 0);
						responseStream = (PacketStream) d;
					} else {
						msg.putUnsafe("data", d);
					}
					Tree rspMeta = data.getMeta(false);
					if (meta == null || meta.isEmpty()) {

						// Send back the new meta
						if (rspMeta != null && !rspMeta.isEmpty()) {
							msg.putUnsafe("meta", rspMeta);
						}
					} else {

						// Send back request meta
						if (rspMeta != null && !rspMeta.isEmpty()) {

							// Merge new entries
							meta.copyFrom(rspMeta);
						}
						msg.putUnsafe("meta", meta);
					}
				}
				transporter.publish(PACKET_RESPONSE, sender, msg);

				// Define sender for response stream
				if (responseStream != null) {
					responseStream.onPacket(new PacketListener() {

						// Create sequence counter
						private final AtomicLong sequence = new AtomicLong();

						@Override
						public final void onPacket(byte[] bytes, Throwable cause, boolean close) {
							if (bytes != null) {
								transporter.sendDataPacket(PACKET_RESPONSE, sender, ctx, bytes,
										sequence.incrementAndGet());
							} else if (cause != null) {
								if (writeErrorsToLog) {
									logger.error("Unexpected error occured while streaming!", cause);
								}
								transporter.sendErrorPacket(PACKET_RESPONSE, sender, ctx, cause,
										sequence.incrementAndGet());
							}
							if (close) {
								transporter.sendClosePacket(PACKET_RESPONSE, sender, ctx, sequence.incrementAndGet());
							}
						}

					});
				}

			}).catchError(error -> {

				// Send error
				transporter.publish(PACKET_RESPONSE, sender, throwableToTree(id, nodeID, protocolVersion, error));

				// Write error to log file
				if (writeErrorsToLog) {
					logger.error("Unexpected error occurred while invoking \"" + action + "\" action!", error);
				}

			});
		} catch (Throwable error) {

			// Send error
			transporter.publish(PACKET_RESPONSE, sender, throwableToTree(id, nodeID, protocolVersion, error));

			// Write error to log file
			if (writeErrorsToLog) {
				logger.error("Unexpected error occurred while invoking \"" + action + "\" action!", error);
			}

		}

	}

	// --- RECEIVE PING-PONG RESPONSE ---

	@Override
	public void receivePong(Tree message) {

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!protocolVersion.equals(ver)) {
				logger.warn("Invalid protocol version (" + ver + ")!");
				return;
			}
		}

		// Get response's unique ID
		String id = message.get("id", (String) null);
		if (id == null || id.isEmpty()) {
			logger.warn("Missing \"id\" property!", message);
			return;
		}

		// Get stored promise
		PendingPromise pending = promises.remove(id);
		if (pending == null) {
			logger.warn("Unknown (maybe timeouted) response received!", message);

			// Metrics
			if (counterOrphanResponseTotal != null) {
				counterOrphanResponseTotal.increment();
			}
			return;
		}

		// Resolve Promise
		pending.promise.complete(message);
	}

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	@Override
	public void receiveResponse(Tree message) {

		// Verify protocol version
		if (checkVersion) {
			String ver = message.get("ver", "unknown");
			if (!protocolVersion.equals(ver)) {
				logger.warn("Invalid protocol version (" + ver + ")!");
				return;
			}
		}

		// Get response's unique ID
		String id = message.get("id", (String) null);
		if (id == null || id.isEmpty()) {
			logger.warn("Missing \"id\" property!", message);
			return;
		}

		// Incoming (response) stream handling
		IncomingStream responseStream;
		responseStreamReadLock.lock();
		try {
			responseStream = responseStreams.get(id);
		} finally {
			responseStreamReadLock.unlock();
		}
		if (responseStream != null) {
			boolean remove = false;
			try {
				if (responseStream.receive(message)) {
					remove = true;
				}
			} catch (Throwable error) {
				remove = true;

				// Write error to log file
				if (writeErrorsToLog) {
					logger.error("Unexpected error occurred while streaming!", error);
				}
			}
			if (remove) {
				responseStreamWriteLock.lock();
				try {
					responseStreams.remove(id);
				} finally {
					responseStreamWriteLock.unlock();
				}
			}
			return;
		}
		if (message.get("stream", false)) {
			responseStreamWriteLock.lock();
			try {
				responseStream = responseStreams.get(id);
				if (responseStream == null) {
					responseStream = new IncomingStream(nodeID, scheduler, streamTimeout);
					responseStreams.put(id, responseStream);
				}
			} finally {
				responseStreamWriteLock.unlock();
			}
			if (responseStream.receive(message)) {
				responseStreamWriteLock.lock();
				try {
					responseStreams.remove(id);
				} finally {
					responseStreamWriteLock.unlock();
				}
			}
			message.putObject("data", responseStream.getPacketStream());
		}

		// Metrics
		if (gaugeStreamsReceiveActive != null && responseStream != null) {
			gaugeStreamsReceiveActive.set(requestStreams.size() + responseStreams.size());			
		}

		// Get stored promise
		PendingPromise pending = promises.remove(id);
		if (pending == null) {
			logger.warn("Unknown (maybe timeouted) response received!", message);

			// Metrics
			if (counterOrphanResponseTotal != null) {
				counterOrphanResponseTotal.increment();
			}
			return;
		}
		try {

			// Metrics
			if (gaugeRequestsActive != null) {
				gaugeRequestsActive.set(promises.size());
			}

			// Get response status (successed or not?)
			boolean success = message.get("success", true);
			if (success) {

				// Ok -> resolve
				Tree rsp = message.get("data");
				Tree meta = message.get("meta");
				if (meta != null && !meta.isEmpty()) {
					if (rsp == null || rsp.isNull()) {
						rsp = new CheckedTree(new LinkedHashMap<String, Object>(), meta.asObject());
					} else {
						rsp = new CheckedTree(rsp.asObject(), meta.asObject());
					}
				}
				pending.promise.complete(mergeMeta(rsp, pending.req));

			} else {

				// Failed -> reject
				Tree error = message.get("error");
				MoleculerError moleculerError;
				if (error == null) {
					moleculerError = new MoleculerError("Remote invocation failed!", null, "MoleculerError",
							pending.nodeID, false, 500, "UNKNOWN_ERROR", message);
				} else {
					moleculerError = MoleculerErrorUtils.create(error);
				}

				pending.promise.complete(moleculerError);
				logger.error(moleculerError.getMessage(), moleculerError);

				return;
			}
		} catch (Throwable cause) {
			logger.error("Unable to pass on incoming response!", cause);
		}
	}

	// --- ADD MIDDLEWARES ---

	@Override
	public void use(Collection<Middleware> middlewares) {
		LinkedList<Middleware> newMiddlewares = new LinkedList<>();
		final long stamp = lock.writeLock();
		try {

			// Register middlewares
			for (Middleware middleware : middlewares) {
				if (this.middlewares.add(middleware)) {
					newMiddlewares.add(middleware);
				}
			}

			// Apply new middlewares
			if (!newMiddlewares.isEmpty()) {
				for (Strategy<ActionEndpoint> strategy : strategies.values()) {
					List<ActionEndpoint> endpoints = strategy.getAllEndpoints();
					for (ActionEndpoint endpoint : endpoints) {
						for (Middleware middleware : newMiddlewares) {
							endpoint.use(middleware);
						}
					}
				}
			}

		} finally {
			lock.unlockWrite(stamp);
		}

		// Start new middlewares
		if (!newMiddlewares.isEmpty()) {
			for (Middleware middleware : newMiddlewares) {
				try {
					middleware.started(broker);
				} catch (MoleculerError moleculerError) {
					throw moleculerError;
				} catch (Exception cause) {
					throw new MoleculerError("Unable to start middleware!", cause, "MoleculerError", nodeID, false, 500,
							"MIDDLEWARE_ERROR");
				}
			}
		}
	}

	// --- ADD A LOCAL SERVICE ---

	@Override
	public Promise addActions(String serviceName, Service service) {
		if (serviceName == null || serviceName.isEmpty()) {
			serviceName = service.getName();
		} else {
			service.name = serviceName;
		}
		final String name = serviceName.replace(' ', '-');
		Class<? extends Service> clazz = service.getClass();
		Dependencies dependencies = clazz.getAnnotation(Dependencies.class);
		if (dependencies != null) {
			String[] services = dependencies.value();
			if (services != null && services.length > 0) {
				Promise deployed = new Promise();
				waitForServices(0, Arrays.asList(services)).then(ok -> {
					StringBuilder msg = new StringBuilder(64);
					msg.append("Starting \"");
					msg.append(name);
					msg.append("\" service because ");
					for (int i = 0; i < services.length; i++) {
						msg.append('\"');
						msg.append(services[i]);
						msg.append('\"');
						if (i < services.length - 1) {
							msg.append(", ");
						}
					}
					if (services.length == 1) {
						msg.append(" service is");
					} else {
						msg.append(" services are");
					}
					msg.append(" available...");
					logger.info(msg.toString());
					addOnlineActions(name, service);
					deployed.complete();
				}).catchError(cause -> {
					logger.error("Unable to deploy service!", cause);
					deployed.complete(cause);
				});
				return deployed;
			}
		}
		addOnlineActions(name, service);
		return Promise.resolve();
	}

	protected void addOnlineActions(String serviceName, Service service) {
		Class<?> clazz = service.getClass();
		LinkedHashMap<String, Field> fields = new LinkedHashMap<>(64);
		while (clazz != null) {
			for (Field field : clazz.getDeclaredFields()) {
				if (Action.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					fields.putIfAbsent(field.getName(), field);
				}
			}
			for (Field field : clazz.getFields()) {
				if (Action.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					fields.putIfAbsent(field.getName(), field);
				}
			}
			clazz = clazz.getSuperclass();
		}
		int actionCounter = 0;

		final long stamp = lock.writeLock();
		try {

			// Initialize actions in service
			for (Field field : fields.values()) {
				Action action = (Action) getFieldFromProxy(service, field);

				// Name of the action (eg. "service.action")
				String actionName = nameOf(serviceName, field);

				Tree actionConfig = new Tree();
				actionConfig.put("name", actionName);
				if (Modifier.isPrivate(field.getModifiers())) {
					actionConfig.put("visibility", "protected");
				}
				convertAnnotations(actionConfig, field.getAnnotations());

				// Register action
				LocalActionEndpoint endpoint = new LocalActionEndpoint(this, executor, nodeID, serviceName,
						actionConfig, action, actionName);
				Strategy<ActionEndpoint> actionStrategy = strategies.get(actionName);
				if (actionStrategy == null) {

					// Create strategy
					actionStrategy = strategyFactory.create();
					strategies.put(actionName, actionStrategy);
				}
				actionStrategy.addEndpoint(endpoint);

				// Apply middlewares
				for (Middleware middleware : middlewares) {
					endpoint.use(middleware);
				}

				// Write log about this action
				logger.info("Local action \"" + actionName + "\" registered.");
				actionCounter++;
			}
			services.put(serviceName, service);

			// Delete cached node descriptor
			clearDescriptorCache();

		} catch (Exception cause) {
			logger.error("Unable to register local service!", cause);
			return;
		} finally {
			lock.unlockWrite(stamp);
		}

		// Start service
		try {
			service.started(broker);
		} catch (Exception cause) {
			logger.error("Unable to start local service!", cause);
		}

		// Add to "names" (listened by "waitForServices")
		final long nameStamp = lock.writeLock();
		try {
			names.add(serviceName);
		} finally {
			lock.unlockWrite(nameStamp);
		}

		// Write log about this service
		StringBuilder msg = new StringBuilder(64);
		msg.append("Local service \"");
		msg.append(serviceName);
		msg.append("\" started ");
		if (actionCounter == 0) {
			msg.append("without any actions.");
		} else if (actionCounter == 1) {
			msg.append("with 1 action.");
		} else {
			msg.append("with ");
			msg.append(actionCounter);
			msg.append(" actions.");
		}
		logger.info(msg.toString());
	}

	// --- NOTIFY OTHER SERVICES ---

	protected void broadcastServicesChanged(boolean local) {
		Tree msg = new Tree();
		msg.put("localService", local);
		eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(),
				"$services.changed", msg, 1, null, null, null, null, nodeID), null, true);
	}

	// --- ADD A REMOTE SERVICE ---

	@Override
	public void addActions(String nodeID, Tree config) {
		Tree actions = config.get("actions");
		String serviceName = config.get("name", "");
		int actionCounter = 0;

		final long stamp = lock.writeLock();
		try {
			if (actions != null && actions.isMap()) {
				for (Tree actionConfig : actions) {
					actionConfig = actionConfig.clone();
					actionConfig.putObject("nodeID", nodeID, true);
					String actionName = actionConfig.get("name", "");

					// Register remote action
					RemoteActionEndpoint endpoint = new RemoteActionEndpoint(this, transporter, nodeID, serviceName,
							actionConfig, actionName);
					Strategy<ActionEndpoint> actionStrategy = strategies.get(actionName);
					if (actionStrategy == null) {
						actionStrategy = strategyFactory.create();
						strategies.put(actionName, actionStrategy);
					}
					actionStrategy.addEndpoint(endpoint);

					// Apply middlewares
					for (Middleware middleware : middlewares) {
						endpoint.use(middleware);
					}

					// Write log about this action
					logger.info("Action \"" + actionName + "\" on node \"" + nodeID + "\" registered.");
					actionCounter++;
				}
			}
			names.add(serviceName);
		} finally {
			lock.unlockWrite(stamp);
		}

		// Write log about this service
		StringBuilder msg = new StringBuilder(64);
		msg.append("Remote service \"");
		msg.append(serviceName);
		msg.append("\" registered ");
		if (actionCounter == 0) {
			msg.append("without any actions");
		} else if (actionCounter == 1) {
			msg.append("with 1 action");
		} else {
			msg.append("with ");
			msg.append(actionCounter);
			msg.append(" actions");
		}
		msg.append(" on node \"");
		msg.append(nodeID);
		msg.append("\".");
		logger.info(msg.toString());

		// Notify local listeners about the new REMOTE service
		broadcastServicesChanged(false);
	}

	// --- REMOVE ALL REMOTE SERVICES/ACTIONS OF A NODE ---

	@Override
	public void removeActions(String nodeID) {

		// Stop local services
		if (this.nodeID.equals(nodeID)) {
			stopAllLocalServices();
		}

		// Remove actions
		final long stamp = lock.writeLock();
		try {
			Iterator<Strategy<ActionEndpoint>> endpoints = strategies.values().iterator();
			while (endpoints.hasNext()) {
				Strategy<ActionEndpoint> strategy = endpoints.next();
				strategy.remove(nodeID);
				if (strategy.isEmpty()) {
					endpoints.remove();
				}
			}

			// Update service names
			names.clear();
			for (Strategy<ActionEndpoint> strategy : strategies.values()) {
				for (ActionEndpoint endpoint : strategy.getAllEndpoints()) {
					names.add(endpoint.service);
				}
			}

			// Delete cached node descriptor
			if (this.nodeID.equals(nodeID)) {
				clearDescriptorCache();
			}

		} finally {
			lock.unlockWrite(stamp);
		}

		// Reject promises
		Iterator<PendingPromise> pendingPromises = promises.values().iterator();
		boolean removed = false;
		while (pendingPromises.hasNext()) {
			PendingPromise pending = pendingPromises.next();
			if (pending.nodeID.equals(nodeID)) {
				removed = true;
				pendingPromises.remove();
				executor.execute(() -> {
					try {
						pending.promise.complete(new RequestRejectedError(nodeID, pending.action));
					} catch (Throwable cause) {
						logger.warn("Unable to reject action \"" + pending.action + "\"!", cause);
					}
				});
			}
		}

		// Reschedule timeout checker
		if (removed) {
			scheduler.execute(() -> {
				reschedule(Long.MAX_VALUE);
			});
		}

		// Notify listeners
		if (this.nodeID.equals(nodeID)) {

			// Notify local listeners (LOCAL services changed)
			broadcastServicesChanged(true);
		} else {

			// Notify local listeners (REMOTE services changed)
			broadcastServicesChanged(false);
		}
	}

	protected void stopAllLocalServices() {

		// Stop services
		for (Map.Entry<String, Service> serviceEntry : services.entrySet()) {
			String name = serviceEntry.getKey();
			try {
				serviceEntry.getValue().stopped();
				logger.info("Service \"" + name + "\" stopped.");
			} catch (Throwable cause) {
				logger.warn("Unable to stop \"" + name + "\" service!", cause);
			}
		}

		// Delete services
		final long stamp = lock.writeLock();
		try {
			services.clear();
		} finally {
			lock.unlockWrite(stamp);
		}
	}

	// --- GET LOCAL SERVICE ---

	@Override
	public Service getService(String name) {
		Service service = null;
		long stamp = lock.tryOptimisticRead();
		if (stamp != 0) {
			try {
				service = services.get(name);
			} catch (Exception modified) {
				stamp = 0;
			}
		}
		if (!lock.validate(stamp) || stamp == 0) {
			stamp = lock.readLock();
			try {
				service = services.get(name);
			} finally {
				lock.unlockRead(stamp);
			}
		}
		if (service == null) {
			throw new ServiceNotFoundError(nodeID, name);
		}
		return service;
	}

	// --- GET LOCAL OR REMOTE ACTION ---

	@Override
	public Action getAction(String name, String nodeID) {
		Strategy<ActionEndpoint> strategy = null;
		long stamp = lock.tryOptimisticRead();
		if (stamp != 0) {
			try {
				strategy = strategies.get(name);
			} catch (Exception modified) {
				stamp = 0;
			}
		}
		if (!lock.validate(stamp) || stamp == 0) {
			stamp = lock.readLock();
			try {
				strategy = strategies.get(name);
			} finally {
				lock.unlockRead(stamp);
			}
		}
		if (strategy == null) {
			throw new ServiceNotFoundError(nodeID, name);
		}
		ActionEndpoint endpoint = strategy.getEndpoint(null, nodeID);
		if (endpoint == null) {
			throw new ServiceNotAvailableError(nodeID, name);
		}
		return endpoint;
	}

	// --- WAIT FOR SERVICE(S) ---

	@Override
	public Promise waitForServices(long timeoutMillis, Collection<String> services) {
		if (services == null || services.isEmpty() || isServicesOnline(services)) {
			return Promise.resolve();
		}
		Promise promise = new Promise();
		long timeoutAt;
		if (timeoutMillis > 0) {
			timeoutAt = System.currentTimeMillis() + timeoutMillis;
		} else {
			timeoutAt = 0;
		}
		ServiceListener listener = new ServiceListener(promise, timeoutAt, services);
		synchronized (serviceListeners) {
			serviceListeners.addLast(listener);
			if (servicesOnlineTimer == null) {
				servicesOnlineTimer = scheduler.scheduleWithFixedDelay(this::checkServicesOnline, 1, 1,
						TimeUnit.SECONDS);
			}
		}
		return promise;
	}

	protected void checkServicesOnline() {
		LinkedList<ServiceListener> onlineListeners = new LinkedList<>();
		LinkedList<ServiceListener> timeoutedListeners = new LinkedList<>();
		long now = System.currentTimeMillis();
		synchronized (serviceListeners) {
			Iterator<ServiceListener> i = serviceListeners.iterator();
			while (i.hasNext()) {
				ServiceListener listener = i.next();

				// Online?
				boolean online = isServicesOnline(listener.services);
				if (online) {
					onlineListeners.addLast(listener);
					i.remove();
					continue;
				}

				// Timeouted?
				if (listener.timeoutAt > 0 && listener.timeoutAt <= now) {
					timeoutedListeners.addLast(listener);
					i.remove();
				}
			}
			if (serviceListeners.isEmpty() && servicesOnlineTimer != null) {
				servicesOnlineTimer.cancel(false);
				servicesOnlineTimer = null;
			}
		}
		if (!timeoutedListeners.isEmpty()) {
			for (ServiceListener listener : timeoutedListeners) {
				try {
					String missingService = null;
					for (String service : listener.services) {
						if (!isServicesOnline(Collections.singleton(service))) {
							missingService = service;
							break;
						}
					}
					if (missingService == null) {
						missingService = listener.services.isEmpty() ? "unknown" : listener.services.iterator().next();
					}
					listener.promise.complete(new NoSuchElementException("Missing service (" + missingService + ")!"));
				} catch (Exception ignored) {
				}
			}
		}
		for (ServiceListener listener : onlineListeners) {
			try {
				listener.promise.complete();
			} catch (Exception ignored) {
			}
		}
	}

	protected boolean isServicesOnline(Collection<String> requiredServices) {
		int foundCounter = 0;
		long stamp = lock.tryOptimisticRead();
		if (stamp != 0) {
			try {
				foundCounter = countOnlineServices(requiredServices);
			} catch (Exception modified) {
				stamp = 0;
			}
		}
		if (!lock.validate(stamp) || stamp == 0) {
			stamp = lock.readLock();
			try {
				foundCounter = countOnlineServices(requiredServices);
			} finally {
				lock.unlockRead(stamp);
			}
		}
		return foundCounter == requiredServices.size();
	}

	protected int countOnlineServices(Collection<String> requiredServices) {
		int foundCounter = 0;
		for (String service : requiredServices) {
			if (names.contains(service)) {
				foundCounter++;
				continue;
			}
			if (foundCounter == 0) {
				break;
			}
		}
		return foundCounter;
	}

	// --- PING / PONG HANDLING ---

	@Override
	public Promise ping(long timeoutMillis, String nodeID) {

		// Local node?
		if (this.nodeID.equals(nodeID)) {
			Tree rsp = new Tree();
			long time = System.currentTimeMillis();
			rsp.put("time", time);
			rsp.put("arrived", time);
			return Promise.resolve(rsp);
		}

		// Do we have a transporter?
		if (transporter == null) {
			return Promise.reject(new ServiceNotAvailableError(nodeID, "ping"));
		}

		// Create new promise
		Promise promise = new Promise();

		// Set timeout
		long timeoutAt;
		if (timeoutMillis > 0) {
			timeoutAt = System.currentTimeMillis() + timeoutMillis;
		} else {
			timeoutAt = 0;
		}

		// Register promise (timeout and response handling)
		String id = uidGenerator.nextUID();
		register(id, promise, timeoutAt, nodeID, "ping", null);

		// Send request via transporter
		Tree message = transporter.createPingPacket(id);
		transporter.publish(PACKET_PING, nodeID, message);

		// Return promise
		return promise;
	}

	// --- TIMESTAMP OF SERVICE DESCRIPTOR ---

	@Override
	public long getTimestamp() {
		return timestamp.get();
	}

	// --- GENERATE SERVICE DESCRIPTOR ---

	@Override
	public Tree getDescriptor() {
		return currentDescriptor().clone();
	}

	protected void clearDescriptorCache() {
		cachedDescriptor = null;
		timestamp.set(System.currentTimeMillis());
	}

	protected Tree currentDescriptor() {
		FastBuildTree descriptor;
		final long stamp = lock.readLock();
		try {
			descriptor = cachedDescriptor;
			if (descriptor == null) {

				// Create new descriptor block
				descriptor = new FastBuildTree(4);

				// Services array
				int serviceCount = names.size();
				Tree services = descriptor.putListUnsafe("services", serviceCount);

				// Actions map
				HashMap<String, FastBuildTree> servicesMap = new HashMap<>(serviceCount * 2);
				HashMap<String, FastBuildTree> actionsMap = new HashMap<>(serviceCount * 2);

				for (Map.Entry<String, Strategy<ActionEndpoint>> entry : strategies.entrySet()) {

					// Get action and service names
					String actionName = entry.getKey();
					String serviceName = actionName.substring(0, actionName.lastIndexOf('.'));

					// Get endpoint
					ActionEndpoint endpoint = entry.getValue().getEndpoint(null, nodeID);
					if (endpoint == null || endpoint.localOnly) {
						continue;
					}

					// Create service block
					FastBuildTree actions = actionsMap.get(serviceName);
					if (actions == null) {
						FastBuildTree service = new FastBuildTree(4);
						service.putUnsafe("name", serviceName);
						service.putUnsafe("fullName", serviceName);
						servicesMap.put(serviceName, service);

						actions = service.putMapUnsafe("actions", strategies.size());
						actionsMap.put(serviceName, actions);

						// Create event listener block
						Tree listeners = eventbus.generateListenerDescriptor(serviceName);
						if (listeners != null && !listeners.isEmpty()) {
							service.putUnsafe("events", listeners.asObject());
						}
					}

					// Create action block
					actions.putUnsafe(actionName, endpoint.getConfig());
				}

				// Add services (without actions)
				for (String serviceName : names) {
					if (!actionsMap.containsKey(serviceName)) {

						// Create event listener block
						Tree listeners = eventbus.generateListenerDescriptor(serviceName);
						if (listeners != null && !listeners.isEmpty()) {
							FastBuildTree service = new FastBuildTree(3);
							service.putUnsafe("name", serviceName);
							service.putUnsafe("fullName", serviceName);
							servicesMap.put(serviceName, service);
							actionsMap.put(serviceName, new FastBuildTree(0));
							service.putUnsafe("events", listeners.asObject());
						}
					}
				}
				for (FastBuildTree service : servicesMap.values()) {
					services.addObject(service);
				}

				// Host name
				descriptor.putUnsafe("hostname", getHostName());

				// IP array
				LinkedHashSet<String> ips = new LinkedHashSet<>();
				try {
					InetAddress local = InetAddress.getLocalHost();
					String defaultAddress = local.getHostAddress();
					if (!defaultAddress.startsWith("127.")) {
						ips.add(defaultAddress);
					}
				} catch (Exception ignored) {
				}
				try {
					Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
					while (e.hasMoreElements()) {
						NetworkInterface n = (NetworkInterface) e.nextElement();
						Enumeration<InetAddress> ee = n.getInetAddresses();
						while (ee.hasMoreElements()) {
							InetAddress i = (InetAddress) ee.nextElement();
							if (!i.isLoopbackAddress()) {
								ips.add(i.getHostAddress());
							}
						}
					}
				} catch (Exception ignored) {
				}
				Tree ipList = descriptor.putListUnsafe("ipList", ips.size());
				for (String ip : ips) {
					ipList.add(ip);
				}

				// Client descriptor
				FastBuildTree client = descriptor.putMapUnsafe("client", 3);
				client.putUnsafe("type", "java");
				client.putUnsafe("version", ServiceBroker.SOFTWARE_VERSION);
				client.putUnsafe("langVersion", System.getProperty("java.version", "1.8"));

				// Set timestamp
				timestamp.set(System.currentTimeMillis());
				cachedDescriptor = descriptor;
			}
		} finally {
			lock.unlockRead(stamp);
		}
		return descriptor;
	}

	// --- GETTERS / SETTERS ---

	public boolean isCheckVersion() {
		return checkVersion;
	}

	public void setCheckVersion(boolean checkVersion) {
		this.checkVersion = checkVersion;
	}

	public boolean isAsyncLocalInvocation() {
		return asyncLocalInvocation;
	}

	public void setAsyncLocalInvocation(boolean asyncLocalInvocation) {
		this.asyncLocalInvocation = asyncLocalInvocation;
	}

	public boolean isWriteErrorsToLog() {
		return writeErrorsToLog;
	}

	public void setWriteErrorsToLog(boolean writeErrorsToLog) {
		this.writeErrorsToLog = writeErrorsToLog;
	}

	public long getStreamTimeout() {
		return streamTimeout;
	}

	public void setStreamTimeout(long streamTimeout) {
		this.streamTimeout = streamTimeout;
	}

}
