package services.moleculer.services;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cache;
import services.moleculer.logger.AsyncLoggerFactory;

public final class DefaultServiceRegistry extends ServiceRegistry {

	// --- NAME OF THE MOLECULER COMPONENT ---

	@Override
	public final String name() {
		return "Default Service Registry";
	}

	// --- INTERNAL COMPONENTS ---

	private final HashMap<String, Service> serviceMap = new HashMap<>(256);

	private final Logger logger = AsyncLoggerFactory.getLogger(name());

	private ServiceBroker broker;

	// --- VARIABLES ---

	/**
	 * Reader lock
	 */
	private final Lock readerLock;

	/**
	 * Writer lock
	 */
	private final Lock writerLock;

	// --- CONSTRUCTOR ---

	public DefaultServiceRegistry() {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
	}

	// --- INIT SERVICE REGISTRY ---

	@Override
	public final void init(ServiceBroker broker) throws Exception {
		this.broker = broker;
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public final void close() {
		writerLock.lock();
		try {
			for (Service service : serviceMap.values()) {
				String info = service.name();
				if (info == null || info.isEmpty()) {
					info = service.getClass().toString();
				}
				try {
					service.close();
					logger.info("Service \"" + info + "\" stopped.");
				} catch (Throwable cause) {
					logger.warn("Unable to stop \"" + info + "\" service!", cause);
				}
			}
			serviceMap.clear();
		} finally {
			writerLock.unlock();
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

			// Get annotations of actions
			Class<? extends Service> clazz = service.getClass();
			Field[] fields = clazz.getFields();
			for (Field field : fields) {
				if (Action.class.isAssignableFrom(field.getType())) {

					// Name of the action (eg. "v2.service.add")
					String name = service.name + '.' + field.getName();

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
			
					// TODO register actions
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
	public final void addAction(String nodeID, String name, Tree parameters) throws Exception {
		
		// TODO register action
		
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
	public final boolean removeService(String name) {
		writerLock.lock();
		try {
			Service service = serviceMap.remove(name);
			if (service == null) {
				return false;
			}
			try {
				service.close();
				logger.info("Service \"" + name + "\" stopped.");
			} catch (Exception cause) {
				logger.warn("Service removed, but it threw an exception in the \"close\" method!", cause);
			}
			return true;
		} finally {
			writerLock.unlock();
		}
	}
	
	// --- GET ACTION ---
	
	@Override
	public final Action getAction(String nodeID, String name) {
		readerLock.lock();
		try {
			
			// TODO find action
			return null;
			
		} finally {
			readerLock.unlock();
		}
	}

}