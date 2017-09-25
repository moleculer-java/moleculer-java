package services.moleculer.strategies;

import java.util.concurrent.atomic.AtomicInteger;

import services.moleculer.actions.Action;

/**
 * Round-robin invocation strategy.
 */
public final class RoundRobinInvocationStrategy extends AbstractStrategy {

	// --- PROPERTIES ---
	
	private final AtomicInteger counter = new AtomicInteger();
	
	// --- GET NEXT ACTION CONTAINER ---
	
	@Override
	public final Action next() {
		return actions[counter.incrementAndGet() % actions.length];
	}

}