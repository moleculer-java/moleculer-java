package services.moleculer.strategies;

import java.util.concurrent.atomic.AtomicInteger;

import services.moleculer.services.ActionContainer;
import services.moleculer.services.Name;

/**
 * Round-robin invocation strategy.
 * 
 * @see NanoSecInvocationStrategy
 * @see SecureRandomInvocationStrategy
 * @see XORShiftInvocationStrategy
 */
@Name("Round-Robin Invocation Strategy")
public final class RoundRobinInvocationStrategy extends ArrayBasedInvocationStrategy {

	// --- PROPERTIES ---

	private final AtomicInteger counter = new AtomicInteger();

	// --- CONSTRUCTOR ---
	
	public RoundRobinInvocationStrategy(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- GET NEXT ACTION CONTAINER ---

	@Override
	public final ActionContainer next() {
		return actions[counter.incrementAndGet() % actions.length];
	}

}