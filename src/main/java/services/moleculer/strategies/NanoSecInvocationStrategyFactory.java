package services.moleculer.strategies;

import services.moleculer.services.Name;

/**
 * Factory of nanosec-based pseudorandom invocation strategy.
 * 
 * @see RoundRobinInvocationStrategyFactory
 * @see SecureRandomInvocationStrategyFactory
 * @see XORShiftInvocationStrategyFactory
 */
@Name("Nanosecond-based Pseudorandom Invocation Strategy Factory")
public final class NanoSecInvocationStrategyFactory extends ArrayBasedInvocationStrategyFactory {
	
	// --- CONSTRUCTORS ---
	
	public NanoSecInvocationStrategyFactory() {
		super();
	}
	
	public NanoSecInvocationStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- FACTORY METHOD ---

	@Override
	public final InvocationStrategy create() {
		return new NanoSecInvocationStrategy(preferLocal);
	}

}