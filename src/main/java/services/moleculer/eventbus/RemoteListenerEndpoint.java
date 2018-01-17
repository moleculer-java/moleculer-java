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
package services.moleculer.eventbus;

import static services.moleculer.transporter.Transporter.PACKET_EVENT;

import java.util.LinkedHashMap;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.transporter.Transporter;
import services.moleculer.util.CheckedTree;

public class RemoteListenerEndpoint extends ListenerEndpoint {

	// --- COMPONENTS ---

	protected Transporter transporter;

	// --- START ENDPOINT ---

	/**
	 * Initializes Container instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Set base properties
		super.start(broker, config);

		// Set components
		transporter = broker.components().transporter();
	}

	// --- INVOKE REMOTE LISTENER ---

	@Override
	public void on(String name, Tree payload, Groups groups, boolean emit) throws Exception {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("ver", ServiceBroker.MOLECULER_VERSION);
		map.put("sender", nodeID);
		map.put("event", name);
		if (emit) {
			map.put("groups", groups == null ? null : groups.groups());
		} else if (groups != null) {
			logger.warn("Moleculer V2 doesn't support grouped broadcast (message: " + payload.toString(false) + ")!");
		}
		if (payload != null) {
			map.put("data", payload.asObject());
		}
		transporter.publish(PACKET_EVENT, nodeID, new CheckedTree(map));
	}

	@Override
	public boolean local() {
		return false;
	}

}