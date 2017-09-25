package services.moleculer.strategies;

/**
 * Round-robin invocation strategy factory.
 */
public class RoundRobinInvocationStrategyFactory implements InvocationStrategyFactory {

	// --- FACTORY METHOD ---
	
	@Override
	public InvocationStrategy create() {
		return new RoundRobinInvocationStrategy();
	}

}
