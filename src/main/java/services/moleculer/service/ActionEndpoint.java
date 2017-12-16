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
import services.moleculer.strategy.Endpoint;

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
public abstract class ActionEndpoint implements MoleculerComponent, Endpoint {

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

	// --- START ENDPOINT ---

	/**
	 * Initializes enpoint instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current endpoint
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Set name
		name = config.get("name", (String) null);

		// Set nodeID
		nodeID = config.get("nodeID", (String) null);

		// Set cache parameters
		cached = config.get("cache", false);
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

	// --- STOP ENDPOINT ---

	@Override
	public final void stop() {
	}

	// --- INVOKE LOCAL OR REMOTE ACTION + CACHING ---

	public final Promise call(Tree params, CallingOptions.Options opts, Context parent) {

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

	private final Promise callActionAndStore(Tree params, CallingOptions.Options opts, Context parent, String cacheKey,
			int ttl) {
		return callActionNoStore(params, opts, parent).then(result -> {
			if (result != null) {
				cacher.set(cacheKey, result, ttl);
			}
		});
	}

	protected abstract Promise callActionNoStore(Tree params, CallingOptions.Options opts, Context parent);

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