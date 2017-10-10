package services.moleculer.strategies;

import java.util.concurrent.atomic.AtomicInteger;

import services.moleculer.services.ActionContainer;

/**
 * Round-robin invocation strategy.
 */
public final class RoundRobinInvocationStrategy extends ArrayBasedInvocationStrategy {

	// --- NAME OF THE MOLECULER COMPONENT ---

	public String name() {
		return "Round-Robin Invocation Strategy";
	}

	// --- PROPERTIES ---

	private final AtomicInteger counter = new AtomicInteger();

	// --- GET NEXT ACTION CONTAINER ---

	@Override
	public final ActionContainer next() {
		return actions[counter.incrementAndGet() % actions.length];
	}

}