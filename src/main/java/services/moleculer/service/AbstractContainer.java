package services.moleculer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cacher;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;

public abstract class AbstractContainer implements ActionContainer, MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- PROPERTIES ---

	protected String nodeID;
	protected String name;
	protected boolean cached;
	protected String[] cacheKeys;	
	protected int defaultTimeout;
	protected int ttl;

	// --- COMPONENTS ---

	protected ServiceBroker broker;
	private Cacher cacher;
	
	// --- CONSTRUCTOR ---
	
	AbstractContainer() {
	}
	
	// --- INIT CONTAINER ---

	/**
	 * Initializes Container instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Set name
		name = config.get("name", (String) null);

		// Set nodeID
		nodeID = config.get("nodeID", (String) null);

		// Set cache parameters
		cached = config.get("cached", false);
		cacheKeys = config.get("cacheKeys", "").split(",");
		ttl = config.get("ttl", 0);
		
		// Set default invaocation timeout
		defaultTimeout = config.get("defaultTimeout", 0);
		
		// Set components
		this.broker = broker;
		if (cached) {
			cacher = broker.components().cacher();
		}
	}

	// --- STOP CONTAINER ---

	@Override
	public void stop() {
	}

	// --- INVOKE LOCAL OR REMOTE ACTION + CACHING ---

	@Override
	public final Promise call(Tree params, CallingOptions opts, Context parent) {
		
		// Caching enabled
		if (cached) {
			String cacheKey = cacher.getCacheKey(name, params, cacheKeys);
			Promise promise = cacher.get(cacheKey);
			if (promise == null) {
				return callActionAndStore(params, opts, parent, cacheKey, ttl);
			}
			return promise.then(rsp -> {
				if (rsp == null) {
					return callActionAndStore(params, opts, parent, cacheKey, ttl);
				}
				return rsp;
			}).Catch(error -> {
				logger.warn("Unexpected error received from cacher!", error);
				return callActionNoStore(params, opts, parent);
			});
		}

		// Caching disabled
		return callActionNoStore(params, opts, parent);
	}
	
	private final Promise callActionAndStore(Tree params, CallingOptions opts, Context parent, String cacheKey, int ttl) {
		return callActionNoStore(params, opts, parent).then(result -> {
			if (result != null) {
				cacher.set(cacheKey, result, ttl);
			}
		});
	}
	
	protected abstract Promise callActionNoStore(Tree params, CallingOptions opts, Context parent);

	// --- PROPERTY GETTERS ---

	@Override
	public final String name() {
		return name;
	}

	@Override
	public final String nodeID() {
		return nodeID;
	}

	@Override
	public final boolean cached() {
		return cached;
	}

	@Override
	public final String[] cacheKeys() {
		return cacheKeys;
	}

	@Override
	public final int defaultTimeout() {
		return defaultTimeout;
	}

	@Override
	public final int ttl() {
		return ttl;
	}
	
}