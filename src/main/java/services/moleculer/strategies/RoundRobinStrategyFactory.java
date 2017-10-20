package services.moleculer.strategies;

import services.moleculer.services.Name;

/**
 * Factory of round-robin invocation strategy.
 * 
 * @see NanoSecRandomStrategyFactory
 * @see SecureRandomStrategyFactory
 * @see XORShiftRandomStrategyFactory
 */
@Name("Round-Robin Strategy Factory")
public final class RoundRobinStrategyFactory extends ArrayBasedStrategyFactory {

	// --- CONSTRUCTORS ---
	
	public RoundRobinStrategyFactory() {
		super();
	}
	
	public RoundRobinStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- FACTORY METHOD ---

	@Override
	public final Strategy create() {
		return new RoundRobinStrategy(preferLocal);
	}

}