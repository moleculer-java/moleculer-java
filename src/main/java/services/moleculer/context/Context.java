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
import services.moleculer.ServiceBroker;

/**
 * 
 */
public final class Context extends Tree {

	// --- SERIAL VERSION UID ---
	
	private static final long serialVersionUID = 3334045159434047463L;
	
	// --- PROPERTIES ---

	private final long created = System.currentTimeMillis();
	private final ServiceBroker broker;
	private final String id;

	// --- CONSTRUCTOR ---

	public Context(ServiceBroker broker, String id) {
		this.broker = broker;
		this.id = id;
	}

	// --- VARIABLE GETTERS ---

	public final long created() {
		return created;
	}

	public final String id() {
		return id;
	}

	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Call an action (local or remote)
	 * 
	 * @param actionName
	 * @param params
	 * @param opts
	 * 
	 * @return
	 */
	public final Promise call(String actionName, Tree params, CallingOptions opts) throws Exception {
		return broker.call(actionName, params, opts);
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
		broker.emit(name, payload);
	}

	/**
	 * Emit an event for all local & remote services
	 * 
	 * @param name
	 * @param payload
	 */
	public final void broadcast(String name, Object payload) {
		broker.emit(name, payload);
	}

	/**
	 * Emit an event for all local services
	 * 
	 * @param name
	 * @param payload
	 */
	public final void broadcastLocal(String name, Object payload) {
		broker.emit(name, payload);
	}

}