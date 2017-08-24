package services.moleculer.cachers;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

public abstract class Cacher {

	// --- PROPERTIES ---

	protected final boolean useSharedStorage;

	// --- CONSTUCTORS ---

	/**
	 * Creates an instance of Cacher.
	 * 
	 * @param prefix
	 */
	public Cacher(boolean useSharedStorage) {
		this.useSharedStorage = useSharedStorage;
	}

	// --- STORAGE TYPE ---

	public final boolean useSharedStorage() {
		return useSharedStorage;
	}

	// --- START CACHE INSTANCE ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 */
	public void init(ServiceBroker broker) throws Exception {
	}

	// --- STOP CACHE INSTANCE ---

	/**
	 * Closes cacher.
	 */
	public void close() {
	}

	// --- GENERATE CACHE KEY ---

	/**
	 * Creates a cache key by name and params. Concatenates the name and the
	 * hashed params.
	 * 
	 * @param name
	 * @param params
	 * @param keys
	 * @return
	 */
	public String getCacheKey(String name, Tree params, boolean useSharedStorage, String... keys) {
		if (params == null) {
			return name;
		}
		StringBuilder key = new StringBuilder(128);
		key.append(name);
		key.append(':');
		if (keys == null) {
			appendToKey(key, params, useSharedStorage);
			return key.toString();
		}
		if (keys.length == 1) {
			appendToKey(key, keys[0], useSharedStorage);
			return key.toString();
		}
		if (keys.length > 1) {
			boolean first = true;
			for (String k : keys) {
				if (first) {
					first = false;
				} else {
					key.append('|');
				}
				appendToKey(key, params.get(k), useSharedStorage);
			}
		}
		return key.toString();
	}

	protected static final void appendToKey(StringBuilder key, Object object, boolean useSharedStorage) {
		if (object != null) {
			if (object instanceof Tree) {
				Tree tree = (Tree) object;
				if (tree.isPrimitive()) {
					key.append(tree.asObject());
				} else {
					String json = tree.toString(null, false, true);
					if (useSharedStorage) {

						// Create cross-platform, simplified JSON without
						// formatting characters and quotation marks
						for (char c : json.toCharArray()) {
							if (c < 33 || c == '\"' || c == '\'') {
								continue;
							}
							key.append(c);
						}
					} else {
						key.append(json);
					}
				}
			} else {
				key.append(object);
			}
		}
	}

	// --- CACHE METHODS ---

	/**
	 * Gets a cached content by a key.
	 * 
	 * @param key
	 */
	public abstract Object get(String key);

	/**
	 * Sets a content by key into the cache.
	 * 
	 * @param key
	 * @param value
	 */
	public abstract void set(String key, Object value);

	/**
	 * Deletes a content from this cache.
	 * 
	 * @param key
	 */
	public abstract void del(String key);

	/**
	 * Cleans this cache. Removes every key by a match string. The default match
	 * string is "**".
	 * 
	 * @param match
	 */
	public abstract void clean(String match);

}