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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lambdaworks.redis.event.DefaultEventBus;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * Base superclass of all Event Bus implementations.
 * 
 * @see DefaultEventBus
 */
@Name("Event Bus")
public abstract class Eventbus implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- START EVENT BUS ---

	/**
	 * Initializes internal EventBus instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void start(ServiceBroker broker) throws Exception {
	}

	// --- STOP EVENT BUS ---

	@Override
	public void stop() {
	}

	// --- RECEIVE EVENT FROM REMOTE SERVICE ---

	public abstract void receiveEvent(Tree message);

	// --- ADD LISTENERS OF A LOCAL SERVICE ---

	public abstract void addListeners(Service service, Tree config) throws Exception;

	// --- ADD LISTENERS OF A REMOTE SERVICE ---

	public abstract void addListeners(Tree config) throws Exception;

	// --- REMOVE ALL LISTENERS OF A NODE ---

	public abstract void removeListeners(String nodeID);

	// --- SEND EVENT TO ONE LISTENER IN THE SPECIFIED GROUP ---

	public abstract void emit(String name, Tree payload, Groups groups, boolean local);

	// --- SEND EVENT TO ALL LISTENERS IN THE SPECIFIED GROUP ---

	public abstract void broadcast(String name, Tree payload, Groups groups, boolean local);

	// --- GENERATE LISTENER DESCRIPTOR ---

	public abstract Tree generateListenerDescriptor(String service);

}