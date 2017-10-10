package services.moleculer.strategies;

import java.util.Arrays;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.services.Action;
import services.moleculer.services.ActionContainer;
import services.moleculer.services.LocalActionContainer;

public abstract class ArrayBasedInvocationStrategy extends InvocationStrategy {

	// --- ARRAY OF THE ALL ACTION INSTANCES ---

	protected ActionContainer[] actions = new ActionContainer[0];

	// --- POINTER TO A LOCAL ACTION INSTANCE ---

	protected ActionContainer localAction;

	// --- SERVICE BROKER ---

	protected ServiceBroker broker;

	// --- START INVOCATION STRATEGY ---

	/**
	 * Initializes instance.
	 * 
	 * @param broker
	 */
	@Override
	public void init(ServiceBroker broker) throws Exception {
		this.broker = broker;
	}

	// --- ADD ACCTION ---

	public final void add(Action action, Tree parameters) {
		if (actions.length == 0) {
			actions = new ActionContainer[1];
			actions[0] = new LocalActionContainer(broker, parameters, action);
		} else {
			for (int i = 0; i < actions.length; i++) {
				if (actions[i].equals(action)) {

					// Already registered
					return;
				}
			}

			// Add to array
			actions = Arrays.copyOf(actions, actions.length + 1);
			actions[actions.length - 1] = new LocalActionContainer(broker, parameters, action);
		}
	}

	// --- REMOVE ACTION ---

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

	public final boolean isEmpty() {
		return actions.length == 0;
	}

	// --- GET ACTION AT NODE ---

	public final ActionContainer get(String nodeID) {
		for (ActionContainer action : actions) {
			if (action.nodeID().equals(nodeID)) {
				return action;
			}
		}
		return null;
	}

	// --- CALL LOCAL OR REMOTE INSTANCE ---

	public final ActionContainer get(boolean preferLocal) {
		if (!preferLocal || localAction == null) {
			return next();
		}
		return localAction;
	}

	// --- GET NEXT REMOTE INSTANCE ---

	public abstract ActionContainer next();

}