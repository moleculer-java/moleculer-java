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
package services.moleculer.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.service.Endpoint;

public abstract class ListenerEndpoint extends Endpoint {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- PROPERTIES ---

	protected final String serviceName;
	protected final String group;
	protected final String subscribe;

	protected final int hashCode;

	// --- CONSTRUCTOR ---

	protected ListenerEndpoint(String nodeID, String serviceName, String group, String subscribe) {
		super(nodeID);
		this.serviceName = serviceName;
		this.group = group;
		this.subscribe = subscribe;

		// Generate hashcode
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
		result = prime * result + ((subscribe == null) ? 0 : subscribe.hashCode());
		hashCode = result;
	}

	// --- SEND EVENT TO ENDPOINT ---

	public abstract void on(String name, Tree payload, Groups groups, boolean broadcast) throws Exception;

	// --- LOCAL LISTENER? ---

	public abstract boolean isLocal();

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
		ListenerEndpoint other = (ListenerEndpoint) obj;
		if (!nodeID.equals(other.nodeID)) {
			return false;
		}
		if (group == null) {
			if (other.group != null) {
				return false;
			}
		} else if (!group.equals(other.group)) {
			return false;
		}
		if (serviceName == null) {
			if (other.serviceName != null) {
				return false;
			}
		} else if (!serviceName.equals(other.serviceName)) {
			return false;
		}
		if (subscribe == null) {
			if (other.subscribe != null) {
				return false;
			}
		} else if (!subscribe.equals(other.subscribe)) {
			return false;
		}
		return true;
	}

	// --- PROPERTY GETTERS ---

	public String getServiceName() {
		return serviceName;
	}

	public String getGroup() {
		return group;
	}

	public String getSubscribe() {
		return subscribe;
	}

}