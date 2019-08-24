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

import static services.moleculer.transporter.Transporter.PACKET_EVENT;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.transporter.Transporter;
import services.moleculer.util.FastBuildTree;

public class RemoteListenerEndpoint extends ListenerEndpoint {

	// --- PROPERTIES ---

	protected final String currentNodeID;

	// --- COMPONENTS ---

	protected Transporter transporter;

	// --- CONSTRUCTOR ---

	protected RemoteListenerEndpoint(Transporter transporter, String nodeID, String service, String group,
			String subscribe) {
		super(nodeID, service, group, subscribe);

		// Set properties
		currentNodeID = transporter.getBroker().getNodeID();

		// Set components
		this.transporter = transporter;
	}

	// --- INVOKE REMOTE LISTENER ---

	@Override
	public void on(String name, Tree payload, Groups groups, boolean broadcast) throws Exception {
		FastBuildTree msg = new FastBuildTree(7);
		msg.putUnsafe("ver", ServiceBroker.PROTOCOL_VERSION);
		msg.putUnsafe("sender", currentNodeID);
		msg.putUnsafe("event", name);
		msg.putUnsafe("broadcast", broadcast);
		if (groups != null) {
			String[] array = groups.groups();
			if (array != null && array.length > 0) {
				msg.putUnsafe("groups", array);
			}
		}
		
		// Add params and meta
		if (payload != null) {
			msg.putUnsafe("data", payload);
			Tree meta = payload.getMeta(false);
			if (meta != null && !meta.isEmpty()) {
				msg.putUnsafe("meta", meta.asObject());
			}
		}

		transporter.publish(PACKET_EVENT, nodeID, msg);
	}

	// --- IS A LOCAL EVENT LISTENER? ---

	public boolean isLocal() {
		return false;
	}

}