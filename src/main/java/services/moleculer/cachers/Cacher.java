package services.moleculer.cachers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.services.Name;

/**
 * Abstract class of all Cacher implementations.
 */
@Name("Cacher")
public abstract class Cacher implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- CONSTUCTOR ---

	public Cacher() {
	}

	// --- START CACHE INSTANCE ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
	}

	// --- STOP CACHE INSTANCE ---

	/**
	 * Closes cacher.
	 */
	@Override
	public void stop() {
	}

	// --- GENERATE CACHE KEY ---

	/**
	 * Creates a cacher-specific key by name and params. Concatenates the name
	 * and the hashed params.
	 * 
	 * @param name
	 * @param params
	 * @param keys
	 * @return
	 */
	public String getCacheKey(String name, Tree params, String... keys) {
		if (params == null) {
			return name;
		}
		StringBuilder key = new StringBuilder(128);
		key.append(name);
		key.append(':');
		if (keys == null) {
			appendToKey(key, params);
			return key.toString();
		}
		if (keys.length == 1) {
			appendToKey(key, keys[0]);
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
				appendToKey(key, params.get(k));
			}
		}
		return key.toString();
	}

	protected void appendToKey(StringBuilder key, Object object) {
		if (object != null) {
			if (object instanceof Tree) {
				Tree tree = (Tree) object;
				if (tree.isPrimitive()) {
					key.append(tree.asObject());
				} else {
					String json = tree.toString(null, false, true);

					// Create cross-platform, simplified JSON without
					// formatting characters and quotation marks
					for (char c : json.toCharArray()) {
						if (c < 33 || c == '\"' || c == '\'') {
							continue;
						}
						key.append(c);
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
	public abstract Promise get(String key);

	/**
	 * Sets a content by key into the cache.
	 * 
	 * @param key
	 * @param value
	 */
	public abstract void set(String key, Tree value);

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