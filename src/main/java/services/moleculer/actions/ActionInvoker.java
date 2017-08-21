package services.moleculer.actions;

import java.util.Arrays;

abstract class ActionInvoker {

	// --- VARIABLES ---

	/**
	 * Action containers
	 */
	protected ActionContainer[] containers = new ActionContainer[0];

	// --- ADD CONTAINER ---

	final void addContainer(ActionContainer container) {
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

	final void removeContainer(ActionContainer container) {
		for (int i = 0; i < containers.length; i++) {
			if (containers[i].equals(container)) {
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
	
	// --- GET NEXT ACTION CONTAINER ---

	abstract ActionContainer next();
	
}