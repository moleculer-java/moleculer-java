package services.moleculer.strategies;

import services.moleculer.services.Name;

/**
 * Factory of Java SecureRandom-based invocation strategy.
 */
@Name("Secure Random Invocation Strategy Factory")
public final class SecureRandomInvocationStrategyFactory extends ArrayBasedInvocationStrategyFactory {

	// --- CONSTRUCTORS ---
	
	public SecureRandomInvocationStrategyFactory() {
		super();
	}
	
	public SecureRandomInvocationStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- FACTORY METHOD ---

	@Override
	public final InvocationStrategy create() {
		return new SecureRandomInvocationStrategy(preferLocal);
	}

}