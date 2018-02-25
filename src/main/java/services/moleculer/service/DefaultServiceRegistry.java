package services.moleculer.service;

import static services.moleculer.util.CommonUtils.nameOf;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;

/**
 * Default implementation of the Service Registry.
 */
@Name("Default Service Registry")
public class DefaultServiceRegistry extends ServiceRegistry {

	// --- REGISTERED MIDDLEWARES ---

	protected final LinkedHashSet<Middleware> middlewares = new LinkedHashSet<>(32);

	// --- REGISTERED LOCAL SERVICES ---

	protected final LinkedHashMap<String, Service> services = new LinkedHashMap<>(64);

	// --- REGISTERED STRATEGIES PER ACTIONS ---

	protected final HashMap<String, Strategy<ActionEndpoint>> strategies = new HashMap<>(256);

	// --- PENDING REMOTE INVOCATIONS ---

	protected final ConcurrentHashMap<String, PendingPromise> promises = new ConcurrentHashMap<>(1024);

	// --- PROPERTIES ---

	/**
	 * Invoke all local services via Thread pool (true) or directly (false)
	 */
	protected boolean asyncLocalInvocation;

	/**
	 * Default action invocation socketTimeout (seconds)
	 */
	protected int defaultTimeout;

	/**
	 * Timeout-checker's period delay (seconds)
	 */
	protected int cleanup = 1;

	/**
	 * Check protocol version
	 */
	protected boolean checkVersion;

	/**
	 * Reader lock of configuration
	 */
	protected final Lock readLock;

	/**
	 * Writer lock of configuration
	 */
	protected final Lock writeLock;

	// --- LOCAL NODE ID ---

	protected String nodeID;

	// --- COMPONENTS ---

	protected ServiceBroker broker;
	protected ScheduledExecutorService scheduler;
	protected StrategyFactory strategyFactory;

	// --- CONSTRUCTORS ---

	public DefaultServiceRegistry() {
		this(false);
	}

	public DefaultServiceRegistry(boolean asyncLocalInvocation) {

		// Async or direct local invocation
		this.asyncLocalInvocation = asyncLocalInvocation;

		// Create locks
		ReentrantReadWriteLock configLock = new ReentrantReadWriteLock(true);
		readLock = configLock.readLock();
		writeLock = configLock.writeLock();
	}

	// --- INIT SERVICE REGISTRY ---

	/**
	 * Initializes ServiceRegistry instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker) throws Exception {

		// Local nodeID
		this.nodeID = broker.getNodeID();

		// Set components
		this.broker = broker;
		this.scheduler = broker.getConfig().getScheduler();
		this.strategyFactory = broker.getConfig().getStrategyFactory();
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public void stop() {

		// Stop timer
		ScheduledFuture<?> task = timer.get();
		if (task != null) {
			task.cancel(false);
		}

		// Stop pending invocations
		InterruptedException error = new InterruptedException("Registry is shutting down.");
		for (PendingPromise pending : promises.values()) {
			pending.promise.complete(error);
		}

		// Stop all services
		writeLock.lock();
		try {

			// Stop registered services
			stopAllLocalServices();

			// Delete strategies (and registered actions)
			strategies.clear();

			// Stop middlewares
			for (Middleware middleware : middlewares) {
				try {
					middleware.stop();
				} catch (Throwable cause) {
					logger.warn("Unable to stop middleware!", cause);
				}
			}
			middlewares.clear();

		} finally {

			// Delete cached node descriptor
			clearDescriptorCache();

			writeLock.unlock();
		}
	}

	// --- CALL TIMEOUT CHECKER TASK ---

	protected void checkTimeouts() {
		long now = System.currentTimeMillis();
		PendingPromise pending;
		Iterator<PendingPromise> i = promises.values().iterator();
		boolean removed = false;
		Exception error = new TimeoutException("Action invocation timeouted!");
		while (i.hasNext()) {
			pending = i.next();
			if (pending.timeoutAt > 0 && now >= pending.timeoutAt) {
				pending.promise.complete(error);
				i.remove();
				removed = true;
			}
		}
		if (removed) {
			scheduler.execute(() -> {
				reschedule(Long.MAX_VALUE);
			});
		} else {
			prevTimeoutAt.set(0);
		}
	}

	// --- SCHEDULER ---

	/**
	 * Cancelable timer
	 */
	protected final AtomicReference<ScheduledFuture<?>> timer = new AtomicReference<>();

	/**
	 * Next scheduled time to check timeouts
	 */
	protected final AtomicLong prevTimeoutAt = new AtomicLong();

	/**
	 * Recalculates the next socketTimeout checking time
	 */
	protected void reschedule(long minTimeoutAt) {
		if (minTimeoutAt == Long.MAX_VALUE) {
			for (PendingPromise pending : promises.values()) {
				if (pending.timeoutAt > 0 && pending.timeoutAt < minTimeoutAt) {
					minTimeoutAt = pending.timeoutAt;
				}
			}
		}
		long now = System.currentTimeMillis();
		if (minTimeoutAt == Long.MAX_VALUE) {
			ScheduledFuture<?> t = timer.get();
			if (t != null) {
				if (prevTimeoutAt.get() > now) {
					t.cancel(false);
					prevTimeoutAt.set(0);
				} else {
					timer.set(null);
					prevTimeoutAt.set(0);
				}
			}
		} else {
			minTimeoutAt = (minTimeoutAt / 1000 * 1000) + 1000;

			long prev = prevTimeoutAt.getAndSet(minTimeoutAt);
			if (prev == minTimeoutAt) {

				// Next when not changed
				return;
			}

			// Stop previous timer
			ScheduledFuture<?> t = timer.get();
			if (t != null) {
				t.cancel(false);
			}

			// Schedule next socketTimeout timer
			long delay = Math.max(1000, minTimeoutAt - now);
			timer.set(scheduler.schedule(this::checkTimeouts, delay, TimeUnit.MILLISECONDS));
		}
	}

	// --- REGISTER PROMISE ---

	protected void register(String id, Promise promise, long timeoutAt) {
		promises.put(id, new PendingPromise(promise, timeoutAt));

		long nextTimeoutAt = prevTimeoutAt.get();
		if (nextTimeoutAt == 0 || (timeoutAt / 1000 * 1000) + 1000 < nextTimeoutAt) {
			scheduler.execute(() -> {
				reschedule(timeoutAt);
			});
		}
	}

	protected void deregister(String id) {
		promises.remove(id);
	}

	// --- RECEIVE REQUEST FROM REMOTE SERVICE ---

	@Override
	public void receiveRequest(Tree message) {
		// TODO Auto-generated method stub

	}

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	@Override
	public void receiveResponse(Tree message) {
		// TODO Auto-generated method stub

	}

	// --- ADD MIDDLEWARES ---

	@Override
	public void use(Collection<Middleware> middlewares) {
		LinkedList<Middleware> newMiddlewares = new LinkedList<>();
		writeLock.lock();
		try {

			// Register middlewares
			for (Middleware middleware : middlewares) {
				if (this.middlewares.add(middleware)) {
					newMiddlewares.add(middleware);
				}
			}

			// Start middlewares
			for (Middleware middleware : newMiddlewares) {
				try {
					middleware.start(broker);
				} catch (Exception cause) {
					throw new RuntimeException("Unable to start middleware!", cause);
				}
			}

			// Apply new middlewares
			for (Strategy<ActionEndpoint> strategy : strategies.values()) {
				List<ActionEndpoint> endpoints = strategy.getAllEndpoints();
				for (ActionEndpoint endpoint : endpoints) {
					for (Middleware middleware : newMiddlewares) {
						endpoint.use(middleware);
					}
				}
			}

		} finally {
			writeLock.unlock();
		}
	}

	// --- ADD A LOCAL SERVICE ---

	@Override
	public void addActions(String name, Service service) {
		writeLock.lock();
		try {

			// Get version
			Class<? extends Service> clazz = service.getClass();
			Version v = clazz.getAnnotation(Version.class);
			String version = null;
			if (v != null) {
				version = v.value();
				if (version != null) {
					version = version.trim();
					if (version.isEmpty()) {
						version = null;
					}
				}
				if (version != null) {
					try {
						Double.parseDouble(version);
						version = 'v' + version;
					} catch (Exception ignored) {
					}
				}
			}

			// Initialize actions in services
			Field[] fields = clazz.getFields();
			for (Field field : fields) {
				if (!Action.class.isAssignableFrom(field.getType())) {
					continue;
				}
				field.setAccessible(true);
				Action action = (Action) field.get(service);

				// Name of the action (eg. "service.action")
				String prefix = version == null ? name : version + '.' + name;
				String actionName = nameOf(prefix, field);

				Tree actionConfig = new Tree();
				actionConfig.put("name", actionName);

				Annotation[] annotations = field.getAnnotations();
				for (Annotation annotation : annotations) {

					// Create entry for annotation
					String annotationName = annotation.toString();
					int i = annotationName.lastIndexOf('.');
					if (i > -1) {
						annotationName = annotationName.substring(i + 1);
					}
					i = annotationName.indexOf('(');
					if (i > -1) {
						annotationName = annotationName.substring(0, i);
					}
					annotationName = annotationName.toLowerCase();
					if ("name".equals(annotationName) || "override".equals(annotationName)) {
						continue;
					}
					Tree annotationMap = actionConfig.putMap(annotationName);

					// Add annotation values
					Class<? extends Annotation> type = annotation.annotationType();
					Method[] members = type.getDeclaredMethods();
					for (Method member : members) {
						member.setAccessible(true);
						String propName = member.getName();
						Object propValue = member.invoke(annotation);
						annotationMap.putObject(propName, propValue);
						Tree newChild = annotationMap.get(propName);
						if (newChild.size() < 1) {
							newChild.remove();
						}
					}
					int size = annotationMap.size();
					if (size == 0) {
						annotationMap.remove();
					} else if (size == 1) {
						Tree value = annotationMap.getFirstChild();
						if (value != null && "value".equals(value.getName())) {
							annotationMap.setObject(value.asObject());
						}
					}
				}

				// Register action
				LocalActionEndpoint endpoint = new LocalActionEndpoint(nodeID, actionConfig, action);
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
			}
			services.put(name, service);
			service.started();

			// TODO Notify local listeners about the new LOCAL service
			// broadcastServicesChanged(true);

		} catch (Exception cause) {
			logger.error("Unable to register local service!", cause);
		} finally {

			// Delete cached node descriptor
			clearDescriptorCache();

			writeLock.unlock();
		}
	}

	// --- ADD A REMOTE SERVICE ---

	@Override
	public void addActions(Tree config) {
		Tree actions = config.get("actions");
		if (actions != null && actions.isMap()) {
			String nodeID = Objects.requireNonNull(config.get("nodeID", (String) null));
			writeLock.lock();
			try {
				for (Tree actionConfig : actions) {
					actionConfig.putObject("nodeID", nodeID, true);
					String actionName = actionConfig.get("name", "");

					// Register remote action
					RemoteActionEndpoint endpoint = new RemoteActionEndpoint(nodeID, actionConfig);
					Strategy<ActionEndpoint> actionStrategy = strategies.get(actionName);
					if (actionStrategy == null) {
						actionStrategy = strategyFactory.create();
						strategies.put(actionName, actionStrategy);
					}
					actionStrategy.addEndpoint(endpoint);
				}
			} finally {
				writeLock.unlock();
			}

			// Notify local listeners about the new REMOTE service
			// broadcastServicesChanged(false);
		}
	}

	// --- REMOVE ALL REMOTE SERVICES/ACTIONS OF A NODE ---

	@Override
	public void removeActions(String nodeID) {
		writeLock.lock();
		try {
			Iterator<Strategy<ActionEndpoint>> endpoints = strategies.values().iterator();
			while (endpoints.hasNext()) {
				Strategy<ActionEndpoint> strategy = endpoints.next();
				strategy.remove(nodeID);
				if (strategy.isEmpty()) {
					endpoints.remove();
				}
			}
			if (this.nodeID.equals(nodeID)) {

				// Stop local services
				try {
					stopAllLocalServices();
				} finally {

					// Delete cached node descriptor
					clearDescriptorCache();
				}

				// Notify local listeners (LOCAL services changed)
				// broadcastServicesChanged(true);

			} else {

				// Notify local listeners (REMOTE services changed)
				// broadcastServicesChanged(false);
			}
		} finally {
			writeLock.unlock();
		}
	}

	protected void stopAllLocalServices() {
		for (Map.Entry<String, Service> serviceEntry : services.entrySet()) {
			String name = serviceEntry.getKey();
			try {
				serviceEntry.getValue().stopped();
				logger.info("Service \"" + name + "\" stopped.");
			} catch (Throwable cause) {
				logger.warn("Unable to stop \"" + name + "\" service!", cause);
			}
		}
		services.clear();
	}

	// --- GET LOCAL SERVICE ---

	@Override
	public Service getService(String name) {
		Service service;
		readLock.lock();
		try {
			service = services.get(name);
		} finally {
			readLock.unlock();
		}
		if (service == null) {
			throw new NoSuchElementException("Invalid service name (" + name + ")!");
		}
		return service;
	}

	// --- GET LOCAL OR REMOTE ACTION ---

	@Override
	public Action getAction(String name, String nodeID) {
		Strategy<ActionEndpoint> strategy;
		readLock.lock();
		try {
			strategy = strategies.get(name);
		} finally {
			readLock.unlock();
		}
		if (strategy == null) {
			throw new NoSuchElementException("Unknown action name (" + name + ")!");
		}
		ActionEndpoint endpoint = strategy.getEndpoint(nodeID);
		if (endpoint == null) {
			throw new NoSuchElementException("Unknown nodeID (" + nodeID + ")!");
		}
		return endpoint;
	}

	// --- TIMESTAMP OF SERVICE DESCRIPTOR ---

	private AtomicLong timestamp = new AtomicLong();

	public long getTimestamp() {
		return timestamp.get();
	}

	// --- GENERATE SERVICE DESCRIPTOR ---

	private volatile Tree descriptor;

	@Override
	public Tree getDescriptor() {
		return currentDescriptor().clone();
	}

	protected synchronized void clearDescriptorCache() {
		descriptor = null;
	}

	protected synchronized Tree currentDescriptor() {
		if (descriptor == null) {

			// Create new descriptor block
			descriptor = new Tree();

			// TODO ...

			// Set timestamp
			timestamp.set(System.currentTimeMillis());
		}
		return descriptor;
	}

}