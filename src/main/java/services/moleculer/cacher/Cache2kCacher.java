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

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.cache2k.Cache2kBuilder;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.GlobMatcher;
import services.moleculer.service.Name;

/**
 * Memory cacher, based on the Cache2 API. Supports global TTL only.
 * 
 * @see MemoryCacher
 * @see OHCacher
 * @see RedisCacher
 */
@Name("Cache2k Cacher")
public final class Cache2kCacher extends Cacher {

	// --- PROPERTIES ---

	/**
	 * Maximum number of entries
	 */
	private int capacity;

	/**
	 * Expire time, in seconds (0 = never expires)
	 */
	private int ttl;

	// --- CACHE ---

	private org.cache2k.Cache<String, Tree> cache;

	// --- START CACHER ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {
		if (cache != null) {
			cache.close();
		}
		Cache2kBuilder<String, Tree> builder = new Cache2kBuilder<String, Tree>() {
		};
		builder.disableStatistics(true);
		builder.disableLastModificationTime(true);
		if (ttl > 0) {
			builder.expireAfterWrite(ttl, TimeUnit.SECONDS);
		} else {
			builder.eternal(true);
		}
		builder.entryCapacity(capacity);
		cache = builder.build();
	}

	// --- STOP CACHER ---

	@Override
	public final void stop() {
		if (cache != null) {
			cache.close();
			cache = null;
		}
	}

	// --- CACHE METHODS ---

	@Override
	public final Promise get(String key) {
		try {
			Tree value = cache.get(key);
			if (value == null) {
				return null;
			}
			return Promise.resolve(value);
		} catch (Throwable cause) {
			logger.warn("Unable to get data from cache!", cause);
		}
		return null;
	}

	@Override
	public final void set(String key, Tree value, int ttl) {
		cache.put(key, value);
	}

	@Override
	public final void del(String key) {
		cache.remove(key);
	}

	@Override
	public final void clean(String match) {
		Iterator<String> i = cache.keys().iterator();
		String key;
		while (i.hasNext()) {
			key = i.next();
			if (GlobMatcher.matches(key, match)) {
				i.remove();
			}
		}
	}

}