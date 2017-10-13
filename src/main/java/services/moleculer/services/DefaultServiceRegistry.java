package services.moleculer.services;

import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cache;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.transporters.Transporter;

@Name("Default Service Registry")
public final class DefaultServiceRegistry extends ServiceRegistry {

	// --- SERVICE MAP ---

	private final HashMap<String, Service> serviceMap = new HashMap<>(256);

	// --- COMPONENTS ---

	private ServiceBroker broker;
	private Executor executor;
	private ContextFactory contextFactory;
	private Transporter transporter;

	// --- PROPERTIES ---

	/**
	 * Invoke local service via ExecutorService
	 */
	private final boolean asyncLocalInvocation;

	/**
	 * Reader lock
	 */
	private final Lock readerLock;

	/**
	 * Writer lock
	 */
	private final Lock writerLock;

	// --- PROMISES OF REMOTE ACTION INVOCATIONS ---

	private final ConcurrentHashMap<String, Promise> pendingPromises = new ConcurrentHashMap<>();

	// --- CONSTRUCTORS ---

	public DefaultServiceRegistry() {
		this(false);
	}

	public DefaultServiceRegistry(boolean asyncLocalInvocation) {

		// Async or direct local invocation
		this.asyncLocalInvocation = asyncLocalInvocation;

		// Create locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
	}

	// --- INIT SERVICE REGISTRY ---

	@Override
	public final void init(ServiceBroker broker) throws Exception {

		// Parent service broker
		this.broker = broker;
		Objects.nonNull(broker);

		// Async or direct local invocation
		if (asyncLocalInvocation) {
			executor = broker.components().executor();
		} else {
			executor = null;
		}

		// Set context factory
		contextFactory = broker.components().contextFactory();
		Objects.nonNull(contextFactory);

		// Set transporter (can be null)
		transporter = broker.components().transporter();
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public final void close() {
		writerLock.lock();
		try {
			for (Service service : serviceMap.values()) {
				try {
					service.close();
					logger.info("Service \"" + service.name + "\" stopped.");
				} catch (Throwable cause) {
					logger.warn("Unable to stop \"" + service.name + "\" service!", cause);
				}
			}
			serviceMap.clear();
			pendingPromises.clear();
		} finally {
			writerLock.unlock();
		}
	}

	// --- CALL LOCAL SERVICE ---

	@Override
	public final Promise call(Action action, Tree params, CallingOptions opts) {

		// Create new context
		final Context ctx = contextFactory.create(params, opts);

		// A.) Invoke local action via thread pool
		if (asyncLocalInvocation) {
			return new Promise(CompletableFuture.supplyAsync(() -> {
				try {
					return action.handler(ctx);
				} catch (Throwable error) {
					return error;
				}
			}, executor));
		}

		// B.) In-process (direct) action invocation
		try {
			return new Promise(action.handler(ctx));
		} catch (Throwable error) {
			return Promise.reject(error);
		}
	}

	// --- SEND REQUEST TO REMOTE SERVICE ---

	@Override
	public Promise send(String name, Tree params, CallingOptions opts) {
		Context ctx = contextFactory.create(params, opts);
		String id = ctx.id();
		if (id == null) {

			// Local service
			ActionContainer actionContainer = getAction(null, name);
			if (actionContainer == null) {
				return Promise.reject(new IllegalArgumentException("Invalid action name (\"" + name + "\")!"));
			}
			return actionContainer.call(params, opts);
		}

		// TODO Create Tree by context
		Tree message = new Tree();
		message.put("id", id);
		message.put("name", name);

		String targetNodeID = null;
		if (opts != null) {
			targetNodeID = opts.nodeID();
			message.put("nodeID", targetNodeID);
		}

		if (params != null) {
			message.putObject("params", params);
		}

		Tree meta = ctx.meta();
		if (meta != null) {
			message.putObject("meta", meta);
		}

		// Store promise (context ID -> promise)
		Promise p = new Promise();
		pendingPromises.put(id, p);

		// Send to transporter
		transporter.publish(Transporter.PACKET_REQUEST, targetNodeID, message);

		// Return promise
		return p;
	}

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	@Override
	public void receive(Tree message) {
		String id = message.get("id", "");
		if (id.isEmpty()) {
			logger.warn("Missing \"id\" property!", message);
			return;
		}
		Promise promise = pendingPromises.remove(id);
		if (promise == null) {
			logger.warn("Missing (maybe timeouted) message!", message);
			return;
		}

		// TODO Convert Tree to Object or Exception
		try {
			String error = message.get("error", "");
			if (!error.isEmpty()) {

				// Error response
				promise.complete(new RemoteException(error));
				return;
			}
			Tree response = message.get("response");
			promise.complete(response);
		} catch (Throwable cause) {
			promise.complete(cause);
		}
	}

	// --- ADD LOCAL SERVICE(S) ---

	@Override
	public final void addService(Service... services) throws Exception {
		writerLock.lock();
		try {
			Service[] initedServices = new Service[services.length];
			Service service = null;
			Exception blocker = null;

			// Initialize services
			for (int i = 0; i < services.length; i++) {
				try {
					service = services[i];
					service.init(broker);
					serviceMap.put(service.name, service);
					initedServices[i] = service;
				} catch (Exception cause) {
					blocker = cause;
					logger.error("Unable to initialize service \"" + service.name + "\"!", cause);
					break;
				}
			}

			// Initialize actions in services
			for (int i = 0; i < services.length; i++) {
				String name = null;
				try {
					service = services[i];

					// Get annotations of actions
					Class<? extends Service> clazz = service.getClass();
					Field[] fields = clazz.getFields();
					for (Field field : fields) {
						if (Action.class.isAssignableFrom(field.getType())) {
							Tree parameters = new Tree();

							// Name of the action (eg. "v2.service.add")
							name = service.name + '.' + field.getName();
							parameters.put("name", name);

							// Process "Cache" annotation
							Cache cache = field.getAnnotation(Cache.class);
							boolean cached = false;
							String[] keys = null;
							if (cache != null) {
								cached = true;
								if (cached) {
									keys = cache.value();
									if (keys != null && keys.length == 0) {
										keys = null;
									}
								}
							}
							parameters.put("cached", cached);
							if (keys != null && keys.length > 0) {
								parameters.put("cacheKeys", String.join(",", keys));
							}

							// TODO register actions
							LocalActionContainer container = new LocalActionContainer(broker, parameters,
									(Action) field.get(service));
						}
					}
				} catch (Exception cause) {
					blocker = cause;
					logger.error("Unable to initialize action \"" + name + "\"!", cause);
					break;
				}
			}

			// Start services
			if (blocker == null) {
				for (int i = 0; i < services.length; i++) {
					try {
						service = services[i];
						service.started();
						logger.info("Service \"" + service.name + "\" started.");
					} catch (Exception cause) {
						blocker = cause;
						logger.error("Unable to start service \"" + service.name + "\"!", cause);
						break;
					}
				}
			}

			// Stop initialized services on error
			if (blocker != null) {
				for (int i = 0; i < services.length; i++) {
					service = initedServices[i];
					if (service == null) {
						break;
					}
					try {
						serviceMap.remove(service.name);
						service.close();
						logger.info("Service \"" + service.name + "\" stopped.");
					} catch (Exception cause) {
						logger.warn("Service removed, but it threw an exception in the \"close\" method!", cause);
					}
				}
				throw blocker;
			}
		} finally {
			writerLock.unlock();
		}
	}

	// --- ADD REMOTE ACTION ---

	@Override
	public final void addAction(Tree parameters) throws Exception {

		// TODO register action
		RemoteActionContainer container = new RemoteActionContainer(broker, parameters);
	}

	// --- GET SERVICE ---

	@Override
	public final Service getService(String name) {
		readerLock.lock();
		try {
			return serviceMap.get(name);
		} finally {
			readerLock.unlock();
		}
	}

	// --- REMOVE SERVICE ---

	@Override
	public final void removeService(Service... services) {
		writerLock.lock();
		try {
			for (Service service : services) {
				Service removed = serviceMap.remove(service.name);
				if (removed != null) {
					try {
						removed.close();
						logger.info("Service \"" + removed.name + "\" stopped.");
					} catch (Exception cause) {
						logger.warn("Service removed, but it threw an exception in the \"close\" method!", cause);
					}
				}
			}
		} finally {
			writerLock.unlock();
		}
	}

	// --- GET ACTION ---

	@Override
	public final ActionContainer getAction(String nodeID, String name) {
		readerLock.lock();
		try {

			// TODO find action
			return null;

		} finally {
			readerLock.unlock();
		}
	}

}