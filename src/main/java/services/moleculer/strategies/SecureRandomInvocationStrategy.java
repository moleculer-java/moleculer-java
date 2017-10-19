package services.moleculer.strategies;

import java.security.SecureRandom;

import services.moleculer.services.ActionContainer;
import services.moleculer.services.Name;

/**
 * Java Random/SecureRandom-based invocation strategy.
 * 
 * @see RoundRobinInvocationStrategy
 * @see NanoSecInvocationStrategy
 * @see XORShiftInvocationStrategy
 */
@Name("Secure Random Invocation Strategy")
public final class SecureRandomInvocationStrategy extends ArrayBasedInvocationStrategy {

	// --- PROPERTIES ---

	private final SecureRandom rnd = new SecureRandom();

	// --- CONSTRUCTOR ---
	
	public SecureRandomInvocationStrategy(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- GET NEXT ACTION CONTAINER ---

	@Override
	public final ActionContainer next() {
		return actions[rnd.nextInt(actions.length)];
	}

}