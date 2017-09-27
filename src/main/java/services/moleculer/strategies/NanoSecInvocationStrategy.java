package services.moleculer.strategies;

import services.moleculer.services.Action;

/**
 * Nanosec-based pseudorandom invocation strategy.
 */
public final class NanoSecInvocationStrategy extends ArrayBasedInvocationStrategy {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	public String name() {
		return "Nanosecond-based Pseudorandom Invocation Strategy";
	}
	
	// --- GET NEXT ACTION CONTAINER ---
	
	@Override
	public final Action next() {
		return actions[(int) (System.nanoTime() % actions.length)];
	}

}