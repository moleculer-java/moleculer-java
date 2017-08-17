package services.moleculer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.datatree.Tree;
import services.moleculer.cachers.Cacher;

public class ServiceBroker {

	// Local services
	private List<Service> services = new LinkedList<Service>();

	// Registered middlewares
	// private List<Object> middlewares = new LinkedList<Object>();

	public String namespace = "";
	public String nodeID;
	public Logger logger;
	public Cacher cacher;

	// --- EVENT BUS VARIABLES ---

	/**
	 * Main listener registry of the Event Bus
	 */
	private final HashMap<String, HashMap<EventHandler, Boolean>> listeners;

	/**
	 * Cache of the Event Bus
	 */
	private final io.datatree.dom.Cache<String, EventHandler[]> listenerCache;

	/**
	 * Reader lock of the Event Bus
	 */
	private final Lock readerLock;

	/**
	 * Writer lock of the Event Bus
	 */
	private final Lock writerLock;

	// --- CONSTRUCTOR ---

	/**
	 * Creates an instance of ServiceBroker.
	 * 
	 * @param options
	 */
	public ServiceBroker() {
		this.logger = this.getLogger("broker");
		if (this.nodeID == null || this.nodeID.isEmpty()) {
			try {
				this.nodeID = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				this.logger.warn("Can't resolve hostname!");
			}
		}
		
		// Init Event Bus
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
		listeners = new HashMap<>(2048);
		listenerCache = new io.datatree.dom.Cache<>(2048, true);
	}

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public void start() {
		// Call `started` of all services
		// Start transit.connect if transporter is defined

		this.logger.info("Broker started! NodeID: " + this.nodeID);
	}

	/**
	 * Stop broker. If has transporter, transporter.disconnect will be called.
	 */
	public void stop() {
		// Call `stopped` of all services
		// Start transit.disconnect if transporter is defined

		this.logger.info("Broker stopped! NodeID: " + this.nodeID);
	}

	/**
	 * Switch the console to REPL mode
	 */
	public void repl() {

	}

	/**
	 * Get a custom logger for sub-modules (service, transporter, cacher,
	 * context...etc)
	 * 
	 * @param module
	 * @return
	 */
	public Logger getLogger(String module) {
		return new Logger();
	}

	/**
	 * Get a custom logger for sub-modules (service, transporter, cacher,
	 * context...etc)
	 * 
	 * @param module
	 * @param service
	 * @param version
	 * @return
	 */
	public Logger getLogger(String module, String service, String version) {
		return new Logger();
	}

	/**
	 * Create a new service by schema
	 * 
	 * @param service
	 * @return
	 */
	public <T extends Service> T createService(T service) {
		this.services.add(service);

		return service;
	}

	/**
	 * Destroy a local service
	 * 
	 * @param service
	 */
	public void destroyService(Service service) {
		service.stopped();
		this.services.remove(service);

		// TODO: Notify all other nodes
	}

	/**
	 * Get a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public Service getService(String serviceName) {
		return null;
	}

	/**
	 * Has a local service by name
	 * 
	 * @param serviceName
	 * @return
	 */
	public boolean hasService(String serviceName) {
		return false;
	}

	/**
	 * Has an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public boolean hasAction(String actionName) {
		return false;
	}

	/**
	 * Get an action by name
	 * 
	 * @param actionName
	 * @return
	 */
	public Action getAction(String actionName) {
		return null;
	}

	/**
	 * Check has callable action handler
	 * 
	 * @param actionName
	 * @return
	 */
	public boolean isActionAvailable(String actionName) {
		return false;
	}

	/**
	 * Add a middleware to the broker
	 * 
	 * @param mws
	 */
	public void use(Object... mws) {

	}

	/**
	 * Create a new Context instance
	 * 
	 * @param action
	 * @param nodeID
	 * @param params
	 * @param opts
	 * @return
	 */
	public Context createNewContext(Action action, String nodeID, Tree params, CallingOptions opts) {
		return null;
	}

	/**
	 * Call an action (local or remote)
	 * 
	 * @param actionName
	 * @param params
	 * @param opts
	 * @return
	 */
	public Object call(String actionName, Tree params, CallingOptions opts) {
		return null;
	}

	// --- ADD EVENT LISTENER TO THE EVENT BUS ---
	
	/**
	 * Subscribe to an event
	 * 
	 * @param name
	 * @param handler
	 */
	public void on(String name, EventHandler handler) {
		register(name, handler, false);
	}

	/**
	 * Subscribe to an event once
	 * 
	 * @param name
	 * @param handler
	 */
	public void once(String name, EventHandler handler) {
		register(name, handler, true);
	}
	
	private void register(String name, EventHandler handler, boolean once) {

		// Lock getter and setter threads
		writerLock.lock();
		try {
			HashMap<EventHandler, Boolean> handlers = listeners.get(name);
			if (handlers == null) {
				handlers = new HashMap<>();
				listeners.put(name, handlers);
			}
			handlers.put(handler, once);
		} finally {
			writerLock.unlock();
		}

		// Clear cache
		listenerCache.clear();
	}
	
	// --- REMOVE EVENT LISTENER FROM THE EVENT BUS ---
	
	/**
	 * Unsubscribe from an event
	 * 
	 * @param name
	 * @param handler
	 */
	public void off(String name, Object handler) {
		
		// Check listener
		boolean found = false;

		// Lock setter threads
		readerLock.lock();
		try {
			HashMap<EventHandler, Boolean> handlers = listeners.get(name);
			if (handlers != null) {
				found = handlers.containsKey(handler);
			}
		} finally {
			readerLock.unlock();
		}

		// Remove listener
		if (found) {

			// Lock getter and setter threads
			writerLock.lock();
			try {
				HashMap<EventHandler, Boolean> handlers = listeners.get(name);
				if (handlers != null) {

					// Remove listener
					handlers.remove(handler);
					if (handlers.isEmpty()) {
						listeners.remove(name);
					}

				}
			} finally {
				writerLock.unlock();
			}

			// Clear cache
			listenerCache.clear();
		}
	}
	
	// --- EMIT EVENTS VIA EVENT BUS ---

	/**
	 * Emit an event (global & local)
	 * 
	 * @param name
	 * @param payload
	 */
	public void emit(String name, Object payload) {
		emitLocal(name, payload, null);
		
		// + Send via transporter?
	}
	
	/**
	 * Emit an event (global & local)
	 * 
	 * @param name
	 * @param payload
	 */
	public void emitLocal(String name, Object payload, String sender) {

		// Get from cache
		EventHandler[] cachedListeners = listenerCache.get(name);
		
		// If not found...
		if (cachedListeners == null) {

			// Collected handlers
			final HashSet<EventHandler> collected = new HashSet<EventHandler>();

			// Processing variables
			Entry<String, HashMap<EventHandler, Boolean>> mappedEntry;
			HashMap<EventHandler, Boolean> listenersAndOnce;
			Iterator<Entry<EventHandler, Boolean>> listenersAndOnceIterator;
			Entry<EventHandler, Boolean> listenerAndOnce;
			boolean foundOnce = false;

			// Lock getter and setter threads
			writerLock.lock();
			try {

				// Iterator of all listener mappings
				final Iterator<Entry<String, HashMap<EventHandler, Boolean>>> mappingIterator = listeners.entrySet()
						.iterator();

				// Collect listeners
				while (mappingIterator.hasNext()) {
					mappedEntry = mappingIterator.next();
					listenersAndOnce = mappedEntry.getValue();

					// TODO Matches?
					if (mappedEntry.getKey().startsWith(name)) {
						listenersAndOnceIterator = listenersAndOnce.entrySet().iterator();
						while (listenersAndOnceIterator.hasNext()) {
							listenerAndOnce = listenersAndOnceIterator.next();

							// Invoke once?
							if (listenerAndOnce.getValue()) {
								listenersAndOnceIterator.remove();
								foundOnce = true;
							}

							// Add to listener set
							collected.add(listenerAndOnce.getKey());
						}
					}

					// Empty map?
					if (listenersAndOnce.isEmpty()) {
						mappingIterator.remove();
						continue;
					}
				}

			} finally {
				writerLock.unlock();
			}

			// Convert listener set to array
			if (collected.isEmpty()) {
				cachedListeners = new EventHandler[0];
			} else {
				cachedListeners = new EventHandler[collected.size()];
				collected.toArray(cachedListeners);
			}

			// Store into cache
			if (!foundOnce) {
				listenerCache.put(name, cachedListeners);
			}
		}
		
		// Invoke one listener without looping
		if (cachedListeners.length == 1) {
			try {
				cachedListeners[0].on(payload);
			} catch (Exception cause) {
				cause.printStackTrace();
			}
			return;
		}

		// Invoke more listeners in loop
		if (cachedListeners.length > 1) {
			for (EventHandler listener : cachedListeners) {
				try {
					listener.on(payload);
				} catch (Exception cause) {
					cause.printStackTrace();
				}
			}
		}
	}
	
}
