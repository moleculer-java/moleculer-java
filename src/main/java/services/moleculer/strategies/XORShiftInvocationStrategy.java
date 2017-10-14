package services.moleculer.strategies;

import java.util.concurrent.atomic.AtomicLong;

import services.moleculer.services.ActionContainer;
import services.moleculer.services.Name;

/**
 * XORSHIFT-based pseudorandom invocation strategy.
 */
@Name("XORSHIFT Pseudorandom Invocation Strategy")
public final class XORShiftInvocationStrategy extends ArrayBasedInvocationStrategy {

	// --- PROPERTIES ---

	private final AtomicLong rnd = new AtomicLong(System.nanoTime());

	// --- CONSTRUCTOR ---
	
	public XORShiftInvocationStrategy(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- GET NEXT ACTION CONTAINER ---

	@Override
	public final ActionContainer next() {

		// Generate pseudo random
		long start, next;
		do {
			start = rnd.get();
			next = start + 1;
			next ^= (next << 21);
			next ^= (next >>> 35);
			next ^= (next << 4);
		} while (!rnd.compareAndSet(start, next));

		// Return ActionContainer
		return actions[(int) Math.abs(next % actions.length)];
	}

}