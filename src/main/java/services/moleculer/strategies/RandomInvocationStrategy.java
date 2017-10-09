package services.moleculer.strategies;

import java.security.SecureRandom;
import java.util.Random;

import services.moleculer.services.ActionContainer;

/**
 * Java Random/SecureRandom-based invocation strategy.
 */
public final class RandomInvocationStrategy extends ArrayBasedInvocationStrategy {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	public String name() {
		return "Random Invocation Strategy";
	}
	
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
	public final ActionContainer next() {
		return actions[rnd.nextInt(actions.length)];
	}

}
