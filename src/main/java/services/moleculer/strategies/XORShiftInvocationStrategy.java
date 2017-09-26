package services.moleculer.strategies;

import services.moleculer.actions.Action;

/**
 * XORSHIFT-based pseudorandom invocation strategy.
 */
public final class XORShiftInvocationStrategy extends ArrayBasedInvocationStrategy {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	public String name() {
		return "XORSHIFT Pseudorandom Invocation Strategy";
	}
	
	// --- PROPERTIES ---
	
	private volatile long rnd = System.currentTimeMillis();
	
	// --- GET NEXT ACTION CONTAINER ---
	
	@Override
	public final Action next() {
		
		// Generate pseudo random
		long idx;
		synchronized (this) {
			idx = rnd;
			idx += 1;
			idx ^= (idx << 21);
			idx ^= (idx >>> 35);
			idx ^= (idx << 4);
			rnd = idx;
		}
		
		// Return ActionContainer
		return actions[(int) Math.abs(idx % actions.length)];
	}

}