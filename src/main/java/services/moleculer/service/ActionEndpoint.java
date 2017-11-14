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

/**
 * Base superclass of Local or Remote actions. Sample action:<br>
 * <br>
 * &#64;Name("math")<br>
 * public class MathService extends Service {<br>
 * <br>
 * &#64;Cache(keys = { "a", "b" }, ttl = 30)<br>
 * public Action add = (ctx) -> {<br>
 * return ctx.params().get("a", 0) + ctx.params().get("b", 0);<br>
 * };<br>
 * <br>
 * }
 */
public abstract class ActionEndpoint implements MoleculerComponent {

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

	ActionEndpoint() {
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
		name = config.get(NAME, (String) null);

		// Set nodeID
		nodeID = config.get(NODE_ID, (String) null);

		// Set cache parameters
		cached = config.get(CACHE, false);
		cacheKeys = config.get(CACHE_KEYS, "").split(",");
		ttl = config.get(TTL, 0);

		// Set default invaocation timeout
		defaultTimeout = config.get(DEFAULT_TIMEOUT, 0);

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

	private final Promise callActionAndStore(Tree params, CallingOptions opts, Context parent, String cacheKey,
			int ttl) {
		return callActionNoStore(params, opts, parent).then(result -> {
			if (result != null) {
				cacher.set(cacheKey, result, ttl);
			}
		});
	}

	protected abstract Promise callActionNoStore(Tree params, CallingOptions opts, Context parent);

	// --- PROPERTY GETTERS ---

	public abstract boolean local();
	
	public final String name() {
		return name;
	}

	public final String nodeID() {
		return nodeID;
	}

	public final boolean cached() {
		return cached;
	}

	public final String[] cacheKeys() {
		return cacheKeys;
	}

	public final int defaultTimeout() {
		return defaultTimeout;
	}

	public final int ttl() {
		return ttl;
	}

	// --- EQUALS / HASHCODE ---

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((nodeID == null) ? 0 : nodeID.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ActionEndpoint other = (ActionEndpoint) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (nodeID == null) {
			if (other.nodeID != null)
				return false;
		} else if (!nodeID.equals(other.nodeID))
			return false;
		return true;
	}

}