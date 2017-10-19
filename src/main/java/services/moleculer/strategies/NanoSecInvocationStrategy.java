package services.moleculer.strategies;

import services.moleculer.services.ActionContainer;
import services.moleculer.services.Name;

/**
 * Nanosec-based pseudorandom invocation strategy.
 * 
 * @see RoundRobinInvocationStrategy
 * @see SecureRandomInvocationStrategy
 * @see XORShiftInvocationStrategy
 */
@Name("Nanosecond-based Pseudorandom Invocation Strategy")
public final class NanoSecInvocationStrategy extends ArrayBasedInvocationStrategy {

	// --- CONSTRUCTOR ---
	
	public NanoSecInvocationStrategy(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- GET NEXT ACTION CONTAINER ---

	@Override
	public final ActionContainer next() {
		return actions[(int) (System.nanoTime() % actions.length)];
	}

}