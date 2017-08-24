package services.moleculer.actions;

import java.util.Arrays;

abstract class ActionSelector {

	// --- ARRAY OF THE ACTION CONTAINERS ---

	protected ActionContainer[] containers = new ActionContainer[0];

	// --- POINTER TO A LOCAL INSTANCE ---
	
	protected ActionContainer localInstance;
	
	// --- ADD CONTAINER ---

	final void add(ActionContainer container) {
		if (container instanceof LocalAction) {
			localInstance = container;
		}
		if (containers.length == 0) {
			containers = new ActionContainer[1];
			containers[0] = container;
		} else {
			for (int i = 0; i < containers.length; i++) {
				if (containers[i].equals(container)) {

					// Already registered
					return;
				}
			}

			// Add to array
			containers = Arrays.copyOf(containers, containers.length + 1);
			containers[containers.length - 1] = container;
		}
	}

	// --- REMOVE CONTAINER ---

	final void remove(ActionContainer container) {
		for (int i = 0; i < containers.length; i++) {
			if (containers[i].equals(container)) {
				if (containers[i].equals(localInstance)) {
					localInstance = null;
				}
				ActionContainer[] copy = new ActionContainer[containers.length - 1];
				System.arraycopy(containers, 0, copy, 0, i);
				System.arraycopy(containers, i + 1, copy, i, containers.length - i - 1);
				containers = copy;
				return;
			}
		}
	}

	// --- GET ACTION AT NODE ---
	
	final ActionContainer get(String nodeID) {
		for (ActionContainer container: containers) {
			if (container.nodeID.equals(nodeID)) {
				return container;
			}
		}
		return null;
	}
	
	// --- CALL LOCAL INSTANCES IF POSSIBLE ---
	
	final ActionContainer nextButPreferLocal() {
		if (localInstance == null) {
			return next();
		}
		return localInstance;
	}
	
	// --- GET NEXT ACTION CONTAINER ---

	abstract ActionContainer next();
	
}