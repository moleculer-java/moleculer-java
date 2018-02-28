/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
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

import java.util.List;

import io.datatree.Tree;
import services.moleculer.Promise;
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

	public Action install(Action action, Tree config) {

		// Is caching enabled?
		Tree cacheNode = config.get("cache");
		if (cacheNode == null) {
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
					key.append(tree.toString(null, false, true));
				}
			} else {
				key.append(object);
			}
		} else {
			key.append("null");
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