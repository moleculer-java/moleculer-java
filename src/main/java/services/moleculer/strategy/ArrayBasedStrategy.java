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
package services.moleculer.strategy;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

/**
 * Abstract class for Round-Robin and Random invocation strategies.
 * 
 * @see RoundRobinStrategy
 * @see NanoSecRandomStrategy
 * @see SecureRandomStrategy
 * @see XORShiftRandomStrategy
 */
public abstract class ArrayBasedStrategy<T extends Endpoint> extends Strategy<T> {

	// --- ARRAY OF ENDPOINTS ---

	protected Endpoint[] endpoints = new Endpoint[0];

	// --- POINTER TO A LOCAL ACTION INSTANCE ---

	private T localEndpoint;

	// --- PROPERTIES ---

	private final boolean preferLocal;

	// --- CONSTRUCTOR ---

	public ArrayBasedStrategy(boolean preferLocal) {
		this.preferLocal = preferLocal;
	}

	// --- START INVOCATION STRATEGY ---

	/**
	 * Initializes strategy instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {
	}

	// --- ADD A LOCAL OR REMOTE ENDPOINT ---

	@Override
	public final void addEndpoint(T endpoint) {
		if (endpoints.length == 0) {
			endpoints = new Endpoint[1];
			endpoints[0] = endpoint;
		} else {
			for (int i = 0; i < endpoints.length; i++) {
				if (endpoints[i].equals(endpoints)) {

					// Already registered
					return;
				}
			}

			// Add to array
			Endpoint[] copy = new Endpoint[endpoints.length + 1];
			System.arraycopy(endpoints, 0, copy, 0, endpoints.length);
			copy[endpoints.length] = endpoint;
			endpoints = copy;
		}

		// Store local action
		if (endpoint.local()) {
			localEndpoint = endpoint;
		}
	}

	// --- REMOVE ALL ENDPOINTS OF THE SPECIFIED NODE ---

	@Override
	public final void remove(String nodeID) {
		Endpoint endpoint;
		for (int i = 0; i < endpoints.length; i++) {
			endpoint = endpoints[i];
			if (nodeID.equals(endpoint.nodeID())) {
				if (endpoint.equals(localEndpoint)) {
					localEndpoint = null;
				}
				if (endpoints.length == 1) {
					endpoints = new Endpoint[0];
				} else {
					Endpoint[] copy = new Endpoint[endpoints.length - 1];
					System.arraycopy(endpoints, 0, copy, 0, i);
					System.arraycopy(endpoints, i + 1, copy, i, endpoints.length - i - 1);
					endpoints = copy;
					i--;
				}
			}
		}
	}

	// --- HAS ENDPOINTS ---

	@Override
	public final boolean isEmpty() {
		return endpoints.length == 0;
	}

	// --- GET LOCAL OR REMOTE ENDPOINT ---

	@SuppressWarnings("unchecked")
	@Override
	public final T getEndpoint(String nodeID) {
		if (nodeID == null) {
			if (!preferLocal || localEndpoint == null) {
				return (T) next();
			}
			return localEndpoint;
		}
		for (Endpoint endpoint : endpoints) {
			if (endpoint.nodeID().equals(nodeID)) {
				return (T) endpoint;
			}
		}
		return null;
	}

	// --- GET NEXT ENDPOINT ---

	public abstract Endpoint next();

	// --- GET LOCAL ENDPOINT ---

	@Override
	public final T getLocalEndpoint() {
		return localEndpoint;
	}

	// --- GET ALL ENDPOINTS ---

	@SuppressWarnings("unchecked")
	@Override
	public final T[] getAllEndpoints() {
		return (T[]) endpoints;
	}

}