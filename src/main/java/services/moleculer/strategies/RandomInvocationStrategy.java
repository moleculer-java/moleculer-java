package services.moleculer.strategies;

import java.security.SecureRandom;
import java.util.Random;

import services.moleculer.actions.Action;

/**
 * Java Random/SecureRandom-based invocation strategy.
 */
public final class RandomInvocationStrategy extends AbstractStrategy {

	// --- PROPERTIES ---
	
	private final Random rnd;
	
	RandomInvocationStrategy(boolean secure) {
		if (secure) {
			rnd = new SecureRandom();
		} else {
			rnd = new Random();
		}
	}
	
	// --- GET NEXT ACTION CONTAINER ---
	
	@Override
	public final Action next() {
		return actions[rnd.nextInt(actions.length)];
	}

}
