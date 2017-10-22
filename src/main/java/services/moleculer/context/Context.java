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
package services.moleculer.context;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.service.ServiceRegistry;

/**
 * Invocation context of Actions.
 */
public final class Context {

	// --- PROPERTIES ---

	private final ServiceRegistry registry;
	private final String id;
	private final String name;
	private final Tree params;
	private final CallingOptions opts;

	// --- CONSTRUCTOR ---

	public Context(ServiceRegistry registry, String id, String name, Tree params, CallingOptions opts) {
		this.registry = registry;
		this.id = id;
		this.name = name;
		this.params = params;
		this.opts = opts;
	}

	// --- VARIABLE GETTERS ---

	public final String id() {
		return id;
	}

	public final String name() {
		return name;
	}

	public final Tree params() {
		return params;
	}

	public final CallingOptions opts() {
		return opts;
	}

	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Calls an action (local or remote)
	 */
	public Promise call(String name, Object... params) {
		Tree tree = null;
		CallingOptions opts = null;
		if (params != null) {
			if (params.length == 1) {
				if (params[0] instanceof Tree) {
					tree = (Tree) params[0];
				} else {
					tree = new Tree().setObject(params[0]);
				}
			} else {
				tree = new Tree();
				String prev = null;
				Object value;
				for (int i = 0; i < params.length; i++) {
					value = params[i];
					if (prev == null) {
						if (!(value instanceof String)) {
							if (value instanceof CallingOptions) {
								opts = (CallingOptions) value;
								continue;
							}
							if (value instanceof Context) {
								continue;
							}
							i++;
							throw new IllegalArgumentException("Parameter #" + i + " (\"" + value
									+ "\") must be String, Context, or CallingOptions!");
						}
						prev = (String) value;
						continue;
					}
					tree.putObject(prev, value);
				}
			}
		}
		return registry.getAction(name, null).call(tree, opts, this);
	}

	public Promise call(String name, Tree params) {
		return registry.getAction(name, null).call(params, (CallingOptions) null, this);
	}

	public Promise call(String name, Tree params, CallingOptions opts) {
		return registry.getAction(name, null).call(params, opts, this);
	}

	// --- EMIT EVENTS VIA EVENT BUS ---

	/**
	 * Emit an event (grouped & balanced global event)
	 * 
	 * @param name
	 * @param payload
	 * @param groups
	 */
	public final void emit(String name, Object payload, String... groups) {
	}

	/**
	 * Emit an event for all local & remote services
	 * 
	 * @param name
	 * @param payload
	 */
	public final void broadcast(String name, Object payload) {
	}

	/**
	 * Emit an event for all local services
	 * 
	 * @param name
	 * @param payload
	 */
	public final void broadcastLocal(String name, Object payload) {
	}

}