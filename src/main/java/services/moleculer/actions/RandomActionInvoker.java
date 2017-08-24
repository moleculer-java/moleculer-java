package services.moleculer.actions;

final class RandomActionInvoker extends ActionSelector {

	// --- PROPERTIES ---
	
	private volatile long rnd = System.currentTimeMillis();
	
	// --- GET NEXT ACTION CONTAINER ---
	
	@Override
	final ActionContainer next() {
		
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
		return containers[(int) Math.abs(idx % containers.length)];
	}

}