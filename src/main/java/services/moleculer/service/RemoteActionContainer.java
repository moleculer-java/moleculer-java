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

import java.util.Objects;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;

/**
 * 
 */
public final class RemoteActionContainer implements ActionContainer {

	// --- PROPERTIES ---

	private final String nodeID;
	private final String name;
	private final boolean cached;
	private final String[] cacheKeys;

	// --- SERIVCES ---

	private final ServiceRegistry registry;

	// --- CONSTRUCTOR ---

	public RemoteActionContainer(ServiceBroker broker, Tree config) {

		// Set nodeID
		nodeID = Objects.requireNonNull(config.get("nodeID", (String) null));

		// Set name
		name = Objects.requireNonNull(config.get("name", (String) null));

		// Set service registry
		registry = Objects.requireNonNull(broker.components().registry());

		// Set cache parameters
		cached = config.get("cached", false);
		cacheKeys = config.get("cacheKeys", "").split(",");
	}

	// --- INVOKE REMOTE SERVICE ---

	@Override
	public final Promise call(Tree params, CallingOptions opts) {
		return registry.send(name, params, opts);
	}

	// --- PROPERTY GETTERS ---

	@Override
	public final String name() {
		return name;
	}

	@Override
	public final String nodeID() {
		return nodeID;
	}

	@Override
	public final boolean local() {
		return false;
	}

	@Override
	public final boolean cached() {
		return cached;
	}

	@Override
	public final String[] cacheKeys() {
		return cacheKeys;
	}

}