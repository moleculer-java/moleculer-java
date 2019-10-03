/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.datatree.Promise;
import io.datatree.Tree;
import io.datatree.dom.Config;
import io.datatree.dom.converters.DataConverterRegistry;
import services.moleculer.context.Context;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;

/**
 * Base superclass of all Cacher implementations.
 *
 * @see MemoryCacher
 * @see OHCacher
 * @see RedisCacher
 */
@Name("Cacher")
public abstract class Cacher extends Middleware {

	// --- ADD MIDDLEWARE TO ACTION ---

	@Override
	public Action install(Action action, Tree config) {

		// Is caching enabled?
		Tree cacheNode = config.get("cache");
		if (cacheNode == null || cacheNode.isNull()) {
			return null;
		}
		if (cacheNode.getType() == Boolean.class && !cacheNode.asBoolean()) {
			return null;
		}

		// Get cache keys
		Tree keyNode = cacheNode.get("keys");
		final String[] keys;
		if (keyNode == null) {
			keys = null;
		} else {
			List<String> list = keyNode.asList(String.class);
			if (list.isEmpty()) {
				keys = null;
			} else {
				keys = new String[list.size()];
				list.toArray(keys);
				for (int i = 0; i < keys.length; i++) {
					if (keys[i].startsWith("#")) {
						keys[i] = Config.META + '.' + keys[i].substring(1);
					}
				}
			}
		}

		// Get TTL (0 = use default TTL)
		final int ttl = cacheNode.get("ttl", 0);

		return new Action() {

			@Override
			public Object handler(Context ctx) throws Exception {
				String key = getCacheKey(ctx.name, ctx.params, keys);
				return new Promise(resolver -> {
					get(key).then(in -> {
						if (in == null || in.isNull()) {
							new Promise(action.handler(ctx)).then(tree -> {
								set(key, tree, ttl);
								resolver.resolve(tree);
							}).catchError(err -> {
								resolver.reject(err);
							});
						} else {
							resolver.resolve(in);
						}
					}).catchError(err -> {
						resolver.reject(err);
					});
				});
			}
		};
	}

	// --- GENERATE CACHE KEY ---

	/**
	 * Creates a cacher-specific key by name and params. Concatenates the name
	 * and params.
	 *
	 * @param name
	 *            qualified name of the action
	 * @param params
	 *            input (key) structure (~JSON)
	 * @param keys
	 *            optional array of keys (eg. "id")
	 * 
	 * @return generated cache key
	 */
	public String getCacheKey(String name, Tree params, String... keys) {
		if (params == null) {
			return name;
		}
		StringBuilder key = new StringBuilder(128);
		key.append(name);
		key.append(':');
		serializeKey(key, params, keys);
		return key.toString();
	}

	protected void serializeKey(StringBuilder key, Tree params, String... keys) {
		if (keys == null || keys.length == 0) {
			appendToKey(key, params);
			return;
		}
		if (keys.length == 1) {
			appendToKey(key, params.get(keys[0]));
			return;
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
	}

	protected void appendToKey(StringBuilder key, Tree tree) {
		if (tree == null) {
			key.append("null");
		} else {
			appendTree(key, tree.asObject());
		}
	}

	@SuppressWarnings("unchecked")
	protected void appendTree(StringBuilder key, Object source) {

		// Null value
		if (source == null) {
			key.append("null");
			return;
		}

		// String
		if (source instanceof String) {
			key.append(source);
			return;
		}

		// Primitive
		Class<?> clazz = source.getClass();
		if (clazz.isPrimitive()) {
			key.append(source);
			return;
		}

		// Map
		if (source instanceof Map) {
			Map<Object, Object> map = (Map<Object, Object>) source;
			boolean first = true;
			for (Map.Entry<Object, Object> entry : map.entrySet()) {
				if (first) {
					first = false;
				} else {
					key.append('|');
				}
				key.append(entry.getKey());
				key.append('|');
				appendTree(key, entry.getValue());
			}
			return;

		}

		// List or Set
		if (source instanceof Collection) {
			Collection<Object> collection = (Collection<Object>) source;
			boolean first = true;
			for (Object child : collection) {
				if (first) {
					first = false;
				} else {
					key.append('|');
				}
				appendTree(key, child);
			}
			return;
		}

		// Array
		if (clazz.isArray()) {
			int max = Array.getLength(source);
			boolean first = true;
			for (int i = 0; i < max; i++) {
				if (first) {
					first = false;
				} else {
					key.append('|');
				}
				appendTree(key, Array.get(source, i));
			}
			return;
		}

		// UUID, Date, etc.
		key.append(DataConverterRegistry.convert(String.class, source));
	}

	// --- CACHE METHODS ---

	/**
	 * Gets a cached content by a key.
	 *
	 * @param key
	 *            cache key
	 * 
	 * @return Promise with cached value (or null, the returned Promise also can
	 *         be null)
	 */
	public abstract Promise get(String key);

	/**
	 * Sets a content by key into the cache.
	 *
	 * @param key
	 *            cache key
	 * @param value
	 *            new value
	 * @param ttl
	 *            optional TTL of entry (0 == use default TTL)
	 * 
	 * @return Promise with empty value
	 */
	public abstract Promise set(String key, Tree value, int ttl);

	/**
	 * Deletes a content from this cache.
	 *
	 * @param key
	 *            cache key
	 * 
	 * @return Promise with empty value
	 */
	public abstract Promise del(String key);

	/**
	 * Cleans this cache. Removes every key by a match string. The default match
	 * string is "**".
	 *
	 * @param match
	 *            regex
	 * 
	 * @return Promise with empty value
	 */
	public abstract Promise clean(String match);

}