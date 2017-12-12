/**
 * This software is licensed under MIT license.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.cacher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.service.Name;

/**
 * Base superclass of all Cacher implementations.
 * 
 * @see MemoryCacher
 * @see OHCacher
 * @see RedisCacher
 */
@Name("Cacher")
public abstract class Cacher implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

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
	 * and params.
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
	 * @param ttl
	 *            optional TTL of entry (0 == use default TTL)
	 */
	public abstract Promise set(String key, Tree value, int ttl);

	/**
	 * Deletes a content from this cache.
	 * 
	 * @param key
	 */
	public abstract Promise del(String key);

	/**
	 * Cleans this cache. Removes every key by a match string. The default match
	 * string is "**".
	 * 
	 * @param match
	 */
	public abstract Promise clean(String match);

}