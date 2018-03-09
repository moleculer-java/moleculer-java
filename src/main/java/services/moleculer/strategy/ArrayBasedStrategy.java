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
package services.moleculer.strategy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.datatree.dom.Cache;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Endpoint;

/**
 * Abstract class for Round-Robin and Random invocation strategies.
 *
 * @see RoundRobinStrategy
 * @see NanoSecRandomStrategy
 * @see SecureRandomStrategy
 * @see XORShiftRandomStrategy
 * @see CpuUsageStrategy
 */
public abstract class ArrayBasedStrategy<T extends Endpoint> extends Strategy<T> {

	// --- ARRAY OF ENDPOINTS ---

	protected Endpoint[] endpoints = new Endpoint[0];

	// --- CACHE ---

	protected final Cache<String, Endpoint[]> endpointCache = new Cache<>(1024, true);

	// --- PROPERTIES ---

	protected final String nodeID;
	protected final boolean preferLocal;

	// --- CONSTRUCTOR ---

	public ArrayBasedStrategy(ServiceBroker broker, boolean preferLocal) {
		this.nodeID = broker.getNodeID();
		this.preferLocal = preferLocal;
	}

	// --- ADD A LOCAL OR REMOTE ENDPOINT ---

	@Override
	public void addEndpoint(T endpoint) {
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

		// Remove from cache
		endpointCache.remove(endpoint.getNodeID());
	}

	// --- REMOVE ALL ENDPOINTS OF THE SPECIFIED NODE ---

	@Override
	public boolean remove(String nodeID) {
		Endpoint endpoint;
		boolean found = false;
		for (int i = 0; i < endpoints.length; i++) {
			endpoint = endpoints[i];
			if (nodeID.equals(endpoint.getNodeID())) {
				found = true;
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

		// Remove from cache
		if (found) {
			endpointCache.remove(nodeID);
		}
		return found;
	}

	// --- HAS ENDPOINTS ---

	@Override
	public boolean isEmpty() {
		return endpoints.length == 0;
	}

	// --- GET LOCAL OR REMOTE ENDPOINT ---

	@SuppressWarnings("unchecked")
	@Override
	public T getEndpoint(String nodeID) {
		Endpoint[] array;
		if (nodeID == null && preferLocal) {
			array = getEndpointsByNodeID(this.nodeID);
			if (array.length == 0) {
				array = endpoints;
			}
		} else {
			array = getEndpointsByNodeID(nodeID);
		}
		if (array.length == 0) {
			return null;
		}
		if (array.length == 1) {
			return (T) array[0];
		}
		return (T) next(array);
	}

	protected Endpoint[] getEndpointsByNodeID(String nodeID) {
		if (nodeID == null) {
			return endpoints;
		}
		Endpoint[] array = endpointCache.get(nodeID);
		if (array == null) {
			LinkedList<Endpoint> list = new LinkedList<>();
			for (Endpoint endpoint : endpoints) {
				if (endpoint.getNodeID().equals(nodeID)) {
					list.addLast(endpoint);
				}
			}
			int size = list.size();
			if (size == 0) {
				return new Endpoint[0];
			}
			array = new Endpoint[size];
			list.toArray(array);
			endpointCache.put(nodeID, array);
		}
		return array;
	}

	// --- GET NEXT ENDPOINT ---

	public abstract Endpoint next(Endpoint[] array);

	// --- GET ALL ENDPOINTS ---

	@SuppressWarnings("unchecked")
	@Override
	public List<T> getAllEndpoints() {
		ArrayList<T> list = new ArrayList<>(endpoints.length);
		for (int i = 0; i < endpoints.length; i++) {
			list.add((T) endpoints[i]);
		}
		return list;
	}

}