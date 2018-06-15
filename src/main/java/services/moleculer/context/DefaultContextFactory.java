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
package services.moleculer.context;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.service.Name;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.uid.UidGenerator;

/**
 * Default implementation of Context Factory.
 */
@Name("Default Context Factory")
public class DefaultContextFactory extends ContextFactory {

	// --- PROPERTIES ---

	/**
	 * Max call level (0 = disabled / unlimited)
	 */
	protected int maxCallLevel = 100;

	// --- COMPONENTS ---

	protected ServiceInvoker serviceInvoker;
	protected Eventbus eventbus;
	protected UidGenerator uid;

	// --- START CONTEXT FACTORY ---

	/**
	 * Initializes Default Context Factory instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Set components
		ServiceBrokerConfig cfg = broker.getConfig();
		serviceInvoker = cfg.getServiceInvoker();
		eventbus = cfg.getEventbus();
		uid = cfg.getUidGenerator();
	}

	// --- CREATE CONTEXT ---

	@Override
	public Context create(String name, Tree params, CallOptions.Options opts, Context parent) {

		// Generate ID
		String id = uid.nextUID();

		// Create new Context
		if (parent == null) {
			return new Context(serviceInvoker, eventbus, id, name, params, opts);
		}

		// Merge meta block
		Tree parentMeta = parent.params.getMeta(false);
		if (parentMeta != null && !parentMeta.isEmpty()) {
			Tree currentMeta = params.getMeta(true);
			if (currentMeta.isEmpty()) {
				currentMeta.setObject(parentMeta.asObject());
			} else {
				currentMeta.copyFrom(parentMeta, false);
			}
		}

		// Verify call level
		if (maxCallLevel > 0 && parent.level >= maxCallLevel) {
			throw new IllegalStateException("Max call level limit reached (" + maxCallLevel + ")!");
		}

		// Create context (nested call)
		return new Context(id, name, params, opts, parent);
	}

	@Override
	public Context create(String name, Tree params, CallOptions.Options opts, String id, int level, String requestID,
			String parentID) {

		// Verify call level
		if (maxCallLevel > 0 && level > maxCallLevel) {
			throw new IllegalStateException("Max call level limit reached (" + maxCallLevel + ")!");
		}

		// Create new Context
		return new Context(serviceInvoker, eventbus, id, name, params, opts, level, requestID, parentID);
	}

	// --- PROPERTY GETTERS AND SETTERS ---

	public int getMaxCallLevel() {
		return maxCallLevel;
	}

	public void setMaxCallLevel(int maxCallLevel) {
		this.maxCallLevel = maxCallLevel;
	}

}