package services.moleculer.strategies;

import services.moleculer.actions.Action;

/**
 * Nanosec-based pseudo-random invocation strategy.
 */
public final class NanoSecInvocationStrategy extends AbstractStrategy {

	// --- GET NEXT ACTION CONTAINER ---
	
	@Override
	public final Action next() {
		return actions[(int) (System.nanoTime() % actions.length)];
	}

}