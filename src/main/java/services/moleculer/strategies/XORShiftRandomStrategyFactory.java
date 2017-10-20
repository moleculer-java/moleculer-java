package services.moleculer.strategies;

import services.moleculer.services.Name;

/**
 * Factory of XORSHIFT-based pseudorandom invocation strategy.
 * 
 * @see RoundRobinStrategyFactory
 * @see NanoSecRandomStrategyFactory
 * @see SecureRandomStrategyFactory
 */
@Name("XORSHIFT Pseudorandom Strategy Factory")
public final class XORShiftRandomStrategyFactory extends ArrayBasedStrategyFactory {

	// --- CONSTRUCTORS ---
	
	public XORShiftRandomStrategyFactory() {
		super();
	}
	
	public XORShiftRandomStrategyFactory(boolean preferLocal) {
		super(preferLocal);
	}
	
	// --- FACTORY METHOD ---

	@Override
	public final Strategy create() {
		return new XORShiftRandomStrategy(preferLocal);
	}

}