package services.moleculer.actions;

import java.util.concurrent.atomic.AtomicInteger;

final class RoundRobinActionInvoker extends ActionInvoker {

	// Counter
	private final AtomicInteger counter = new AtomicInteger();
	
	@Override
	final ActionContainer next() {
		return containers[counter.incrementAndGet() % containers.length];
	}

}