package services.moleculer.cachers;

import services.moleculer.ServiceBroker;

public abstract class Cacher {

	// --- CONSTANTS ---
	
	public static final String ALL_ITEMS = "**";
	public static final long UNLIMITED_TTL = -1;
	public static final String EMPTY_PREFIX = "";
	
	// --- VARIABLES ---
	
	protected final String prefix;
	protected final long ttl;

	// --- CONSTUCTORS ---

	/**
	 * Creates an instance of Cacher.
	 * 
	 * @param prefix
	 */
	public Cacher() {
		this(EMPTY_PREFIX, UNLIMITED_TTL);
	}

	/**
	 * Creates an instance of Cacher.
	 * 
	 * @param prefix
	 */
	public Cacher(String prefix) {
		this(prefix, UNLIMITED_TTL);
	}

	/**
	 * Creates an instance of Cacher.
	 * 
	 * @param prefix
	 * @param ttl
	 */
	public Cacher(String prefix, long ttl) {
		this.prefix = prefix;
		this.ttl = ttl;
	}

	// --- INIT CACHE INSTANCE ---
	
	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 */
	public void init(ServiceBroker broker) {
	}

	// --- CLOSE CACHE INSTANCE ---
	
	/**
	 * Closes cacher.
	 */
	public void close() {
	}
	
	// --- CACHE METHODS ---
	
	/**
	 * Gets a cached content by a key.
	 * 
	 * @param key
	 */
	public Object get(String key) {
		throw new UnsupportedOperationException("Not implemented method!");
	}

	/**
	 * Sets a content by key into the cache.
	 * 
	 * @param key
	 * @param value
	 */
	public void set(String key, Object value) {
		throw new UnsupportedOperationException("Not implemented method!");
	}

	/**
	 * Deletes a content from this cache.
	 * 
	 * @param key
	 */
	public void del(String key) {
		throw new UnsupportedOperationException("Not implemented method!");
	}

	/**
	 * Removes all entries from this cache.
	 */
	public void clean() {
		clean(ALL_ITEMS);
	}
	
	/**
	 * Cleans this cache. Removes every key by a match string. The default match
	 * string is "**".
	 * 
	 * @param match
	 */
	public void clean(String match) {
		throw new UnsupportedOperationException("Not implemented method!");
	}

	// --- INTERNAL METHODS ---
	
	/**
	 * Creates a cache key by name and params. Concatenates the name and the
	 * hashed params.
	 * 
	 * @param name
	 * @param params
	 * @param keys
	 * @return
	 */
	protected String getCacheKey(String name, Object params, String... keys) {
		return null;
	}

	/**
	 * Registers this cacher as a middleware.
	 */
	protected void middleware() {

	}

}