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

	private ActionContainer localAction;

	// --- SERVICE BROKER ---

	private ServiceBroker broker;

	// --- PROPERTIES ---
	
	protected boolean preferLocal;
	
	// --- CONSTRUCTOR ---
	
	public ArrayBasedInvocationStrategy(boolean preferLocal) {
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
		this.broker = broker;
	}

	// --- ADD ACCTION ---

	@Override
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
			for (ActionContainer action : actions) {
				if (action.nodeID().equals(nodeID)) {
					return action;
				}
			}
			return null;
		}
		if (!preferLocal || localAction == null) {
			return next();
		}
		return localAction;		
	}

	// --- GET NEXT REMOTE INSTANCE ---

	public abstract ActionContainer next();

}