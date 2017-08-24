package services.moleculer.actions;

import java.util.concurrent.atomic.AtomicInteger;

final class RoundRobinActionInvoker extends ActionSelector {

	// --- PROPERTIES ---
	
	private final AtomicInteger counter = new AtomicInteger();
	
	// --- GET NEXT ACTION CONTAINER ---
	
	@Override
	final ActionContainer next() {
		return containers[counter.incrementAndGet() % containers.length];
	}

}