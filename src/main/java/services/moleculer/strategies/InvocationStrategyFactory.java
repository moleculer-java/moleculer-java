package services.moleculer.strategies;

@FunctionalInterface
public interface InvocationStrategyFactory {

	// --- FACTORY METHOD ---

	public InvocationStrategy create();

}
