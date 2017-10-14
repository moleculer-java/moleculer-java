package services.moleculer.strategies;

/**
 * Round-robin invocation strategy factory.
 */
public final class RoundRobinInvocationStrategyFactory implements InvocationStrategyFactory {

	// --- FACTORY METHOD ---

	@Override
	public final InvocationStrategy create() {
		return new RoundRobinInvocationStrategy();
	}

}
