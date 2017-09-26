package services.moleculer.strategies;

import java.util.Arrays;

import services.moleculer.actions.Action;
import services.moleculer.actions.LocalAction;

public abstract class ArrayBasedInvocationStrategy extends InvocationStrategy {

	// --- ARRAY OF THE ALL ACTION INSTANCES ---

	protected Action[] actions = new Action[0];

	// --- POINTER TO A LOCAL ACTION INSTANCE ---
	
	protected Action localAction;
	
	// --- ADD ACCTION ---

	public final void add(Action action) {
		if (action instanceof LocalAction) {
			localAction = (LocalAction) action;
		}
		if (actions.length == 0) {
			actions = new Action[1];
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
	}

	// --- REMOVE ACTION ---

	public final void remove(Action action) {
		for (int i = 0; i < actions.length; i++) {
			if (actions[i].equals(action)) {
				if (actions[i].equals(localAction)) {
					localAction = null;
				}
				Action[] copy = new Action[actions.length - 1];
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
	
	public final Action get(String nodeID) {
		for (Action action: actions) {
			if (action.nodeID().equals(nodeID)) {
				return action;
			}
		}
		return null;
	}
	
	// --- CALL LOCAL OR REMOTE INSTANCE ---
	
	public final Action get(boolean preferLocal) {
		if (!preferLocal || localAction == null) {
			return next();
		}
		return localAction;
	}
	
	// --- GET NEXT REMOTE INSTANCE ---

	public abstract Action next();
	
}