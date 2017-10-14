package services.moleculer.strategies;

import services.moleculer.services.Name;

/**
 * Factory of round-robin invocation strategy.
 */
@Name("Round-Robin Invocation Strategy Factory")
public final class RoundRobinInvocationStrategyFactory extends ArrayBasedInvocationStrategyFactory {

	// --- CONSTRUCTORS ---
	
	public RoundRobinInvocationStrategyFactory() {
		super();
	}
	
	public RoundRobinInvocationStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- FACTORY METHOD ---

	@Override
	public final InvocationStrategy create() {
		return new RoundRobinInvocationStrategy(preferLocal);
	}

}