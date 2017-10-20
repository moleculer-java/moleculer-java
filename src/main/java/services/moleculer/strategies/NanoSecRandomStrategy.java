package services.moleculer.strategies;

import services.moleculer.services.ActionContainer;
import services.moleculer.services.Name;

/**
 * Nanosec-based pseudorandom invocation strategy.
 * 
 * @see RoundRobinStrategy
 * @see SecureRandomStrategy
 * @see XORShiftRandomStrategy
 */
@Name("Nanosecond-based Pseudorandom Strategy")
public final class NanoSecRandomStrategy extends ArrayBasedStrategy {

	// --- CONSTRUCTOR ---
	
	public NanoSecRandomStrategy(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- GET NEXT ACTION CONTAINER ---

	@Override
	public final ActionContainer next() {
		return actions[(int) (System.nanoTime() % actions.length)];
	}

}