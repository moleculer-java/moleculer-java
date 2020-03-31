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
package services.moleculer.service;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.context.Context;

public abstract class ActionEndpoint extends Endpoint implements Action {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(ActionEndpoint.class);

	// --- CONFIGURATION OF ACTION ---

	protected final Tree config;

	protected final int hashCode;

	protected final String name;

	protected final String service;
	
	/**
	 * Private Action; only the local Broker can access it and cannot be called
	 * remotely.
	 */
	protected final boolean privateAccess;

	// --- ACTION WITH MIDDLEWARES ---

	protected Action current;

	// --- APPLIED MIDDLEWARES ---

	protected HashSet<Middleware> checkedMiddlewares = new HashSet<>(32);

	// --- CONSTRUCTOR ---

	public ActionEndpoint(String nodeID, String service, Tree config) {
		super(nodeID);
		this.config = config;
		this.name = config.get("name", "unknown");
		this.service = service;
		
		// Private Action
		String visibility = config.get("visibility", "published");
		this.privateAccess = config.get("private", false) || "protected".equals(visibility) || "private".equals(visibility);
		
		// Generate hashcode
		this.hashCode = 31 * nodeID.hashCode() + name.hashCode();
	}

	// --- INVOKE ACTION ---

	@Override
	public Object handler(Context ctx) throws Exception {
		return current.handler(ctx);
	}

	// --- APPLY MIDDLEWARE ---

	public boolean use(Middleware middleware) {
		if (checkedMiddlewares.add(middleware)) {
			Action action = middleware.install(current, config);
			if (action != null) {
				logger.info("Middleware \"" + middleware.getName() + "\" installed to action \"" + name + "\".");
				current = action;
				return true;
			}
		}
		return false;
	}

	// --- COLLECTION HELPERS ---

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ActionEndpoint other = (ActionEndpoint) obj;
		if (!nodeID.equals(other.nodeID)) {
			return false;
		}
		return name.equals(other.name);
	}

	// --- PROPERTY GETTERS ---

	public Tree getConfig() {
		return config;
	}

	public Action getAction() {
		return current;
	}

	public boolean isPrivate() {
		return privateAccess;
	}

}