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
package services.moleculer.context;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.EventBus;
import services.moleculer.service.Name;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.uid.UIDGenerator;

/**
 * Default implementation of Context Factory.
 */
@Name("Default Context Factory")
public class DefaultContextFactory extends ContextFactory {

	// --- COMPONENTS ---

	protected ServiceRegistry registry;
	protected EventBus eventbus;
	protected UIDGenerator uid;

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
	public void start(ServiceBroker broker, Tree config) throws Exception {
		registry = broker.components().registry();
		eventbus = broker.components().eventbus();
		uid = broker.components().uid();		
	}

	// --- CREATE CONTEXT ---

	@Override
	public Context create(String name, Tree params, CallingOptions.Options opts, Context parent, boolean generateID) {

		// Generate ID
		String id;
		if (generateID) {
			id = uid.nextUID();
		} else {
			id = null;
		}

		// TODO Merge meta, set parent context ID

		// Create context
		return new Context(registry, eventbus, id, name, params, opts);
	}

}