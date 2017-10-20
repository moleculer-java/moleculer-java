package services.moleculer.strategies;

import services.moleculer.services.Name;

/**
 * Factory of nanosec-based pseudorandom invocation strategy.
 * 
 * @see RoundRobinStrategyFactory
 * @see SecureRandomStrategyFactory
 * @see XORShiftRandomStrategyFactory
 */
@Name("Nanosecond-based Pseudorandom Strategy Factory")
public final class NanoSecRandomStrategyFactory extends ArrayBasedStrategyFactory {
	
	// --- CONSTRUCTORS ---
	
	public NanoSecRandomStrategyFactory() {
		super();
	}
	
	public NanoSecRandomStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- FACTORY METHOD ---

	@Override
	public final Strategy create() {
		return new NanoSecRandomStrategy(preferLocal);
	}

}