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

import java.util.Arrays;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.ActionContainer;

/**
 * Abstract class for Round-Robin and Random invocation strategies.
 * 
 * @see RoundRobinStrategy
 * @see NanoSecRandomStrategy
 * @see SecureRandomStrategy
 * @see XORShiftRandomStrategy
 */
public abstract class ArrayBasedStrategy extends Strategy {

	// --- ARRAY OF THE ALL ACTION INSTANCES ---

	protected ActionContainer[] actions = new ActionContainer[0];

	// --- POINTER TO A LOCAL ACTION INSTANCE ---

	private ActionContainer localAction;

	// --- PROPERTIES ---
	
	private boolean preferLocal;
	
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

	// --- ADD ACCTION ---

	@Override
	public final void add(ActionContainer action, Tree config) {
		if (actions.length == 0) {
			actions = new ActionContainer[1];
			actions[0] = action;
		} else {
			for (int i = 0; i < actions.length; i++) {
				if (actions[i].equals(action)) {

					// Already registered
					return;
				}
			}
			
			// Add to array
			actions = Arrays.copyOf(actions, actions.length + 1);
			actions[actions.length - 1] = action;
		}
		
		// Store local action
		if (action.local()) {
			localAction = action;
		}
	}

	// --- REMOVE ACTION ---

	@Override
	public final void remove(Action action) {
		for (int i = 0; i < actions.length; i++) {
			if (actions[i].equals(action)) {
				if (actions[i].equals(localAction)) {
					localAction = null;
				}
				ActionContainer[] copy = new ActionContainer[actions.length - 1];
				System.arraycopy(actions, 0, copy, 0, i);
				System.arraycopy(actions, i + 1, copy, i, actions.length - i - 1);
				actions = copy;
				return;
			}
		}
	}

	// --- HAS ACTIONS ---

	@Override
	public final boolean isEmpty() {
		return actions.length == 0;
	}

	// --- GET ACTION ---

	@Override
	public final ActionContainer get(String nodeID) {
		if (nodeID == null) {
			if (!preferLocal || localAction == null) {
				return next();
			}
			return localAction;
		}
		for (ActionContainer action : actions) {
			if (action.nodeID().equals(nodeID)) {
				return action;
			}
		}
		return null;
	}

	// --- GET NEXT REMOTE INSTANCE ---

	public abstract ActionContainer next();

}