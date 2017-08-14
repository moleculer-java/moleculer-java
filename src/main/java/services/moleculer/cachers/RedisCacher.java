package services.moleculer.cachers;

import services.moleculer.ServiceBroker;

public class RedisCacher extends Cacher {

	// --- CONSTANTS ---

	public static final String DEFAULT_URL = "redis://localhost";

	// --- VARIABLES ---

	protected final String url;

	// --- CONSTUCTORS ---

	public RedisCacher() {
		this(EMPTY_PREFIX, UNLIMITED_TTL, DEFAULT_URL);
	}

	public RedisCacher(String prefix) {
		this(prefix, UNLIMITED_TTL, DEFAULT_URL);
	}

	public RedisCacher(String prefix, long ttl) {
		this(prefix, ttl, DEFAULT_URL);
	}

	public RedisCacher(String prefix, long ttl, String url) {
		super(prefix, ttl);
		this.url = url;
	}

	// --- INIT CACHE INSTANCE ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 */
	public void init(ServiceBroker broker) {
		super.init(broker);

		// TODO Create Redis client
	}

	// --- CLOSE CACHE INSTANCE ---

	@Override
	public void close() {

	}

	// --- CACHE METHODS ---

}
