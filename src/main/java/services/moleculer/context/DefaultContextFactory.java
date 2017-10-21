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
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.uid.UIDGenerator;

/**
 * 
 */
@Name("Default Context Factory")
public final class DefaultContextFactory extends ContextFactory {

	// --- VARIABLES ---

	private String nodeID;

	// --- INTERNAL COMPONENTS ---

	private ServiceBroker broker;
	private UIDGenerator uid;

	// --- START CONTEXT FACTORY ---

	/**
	 * Initializes Default Context Factory instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {
		this.nodeID = broker.nodeID();
		this.broker = broker;
		this.uid = broker.components().uid();
	}

	// --- CREATE CONTEXT ---

	public final Context create(Tree params, CallingOptions opts) {

		// Target node
		String targetID = opts == null ? null : opts.nodeID();

		// Generate context ID (for remote services only)
		String id;
		if (targetID != null && !targetID.equals(nodeID)) {
			id = uid.nextUID();
		} else {
			id = null;
		}

		// TODO Create or join meta
		// ps: primitive values has no meta node!

		// Create context
		return new Context(broker, id);
	}

}