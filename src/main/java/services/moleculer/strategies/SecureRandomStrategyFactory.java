package services.moleculer.strategies;

import services.moleculer.services.Name;

/**
 * Factory of Java SecureRandom-based invocation strategy.
 * 
 * @see RoundRobinStrategyFactory
 * @see NanoSecRandomStrategyFactory
 * @see XORShiftRandomStrategyFactory
 */
@Name("Secure Random Strategy Factory")
public final class SecureRandomStrategyFactory extends ArrayBasedStrategyFactory {

	// --- CONSTRUCTORS ---
	
	public SecureRandomStrategyFactory() {
		super();
	}
	
	public SecureRandomStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- FACTORY METHOD ---

	@Override
	public final Strategy create() {
		return new SecureRandomStrategy(preferLocal);
	}

}