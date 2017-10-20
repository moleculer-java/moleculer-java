package services.moleculer.strategies;

import java.util.concurrent.atomic.AtomicInteger;

import services.moleculer.services.ActionContainer;
import services.moleculer.services.Name;

/**
 * Round-robin invocation strategy.
 * 
 * @see NanoSecRandomStrategy
 * @see SecureRandomStrategy
 * @see XORShiftRandomStrategy
 */
@Name("Round-Robin Strategy")
public final class RoundRobinStrategy extends ArrayBasedStrategy {

	// --- PROPERTIES ---

	private final AtomicInteger counter = new AtomicInteger();

	// --- CONSTRUCTOR ---
	
	public RoundRobinStrategy(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- GET NEXT ACTION CONTAINER ---

	@Override
	public final ActionContainer next() {
		return actions[counter.incrementAndGet() % actions.length];
	}

}