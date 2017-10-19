package services.moleculer.strategies;

import services.moleculer.services.Name;

/**
 * Factory of XORSHIFT-based pseudorandom invocation strategy.
 * 
 * @see RoundRobinInvocationStrategyFactory
 * @see NanoSecInvocationStrategyFactory
 * @see SecureRandomInvocationStrategyFactory
 */
@Name("XORSHIFT Pseudorandom Invocation Strategy Factory")
public final class XORShiftInvocationStrategyFactory extends ArrayBasedInvocationStrategyFactory {

	// --- CONSTRUCTORS ---
	
	public XORShiftInvocationStrategyFactory() {
		super();
	}
	
	public XORShiftInvocationStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- FACTORY METHOD ---

	@Override
	public final InvocationStrategy create() {
		return new XORShiftInvocationStrategy(preferLocal);
	}

}