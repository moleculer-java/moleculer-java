package services.moleculer.strategies;

/**
 * Factory of all random invocation strategies. Usage:<br>
 * <br>
 * InvocationStrategyFactory isf = InvocationStrategyFactory.xorShiftBased();
 */
public class RandomInvocationStrategyFactory implements InvocationStrategyFactory {

	public static final byte MODE_NANOSEC = 0;
	public static final byte MODE_XORSHIFT = 1;
	public static final byte MODE_RANDOM = 2;
	public static final byte MODE_SECURERANDOM = 3;

	private final byte mode;

	// --- CONSTRUCTORS ---
	
	public RandomInvocationStrategyFactory() {
		this(MODE_NANOSEC);
	}
	
	public RandomInvocationStrategyFactory(byte mode) {
		this.mode = mode;
	}

	// --- STATIC CONSTRUCTORS ---
	
	public static InvocationStrategyFactory newNanoSecBasedFactory() {
		return new RandomInvocationStrategyFactory(MODE_NANOSEC);
	}
	
	public static InvocationStrategyFactory newXORShiftBasedFactory() {
		return new RandomInvocationStrategyFactory(MODE_XORSHIFT);
	}
	
	public static InvocationStrategyFactory newRandomBasedFactory() {
		return new RandomInvocationStrategyFactory(MODE_RANDOM);
	}
	
	public static InvocationStrategyFactory newSecureRandomBasedFactory() {
		return new RandomInvocationStrategyFactory(MODE_SECURERANDOM);
	}
	
	// --- FACTORY METHOD ---
	
	@Override
	public InvocationStrategy create() {
		switch (mode) {
		case MODE_NANOSEC:
			return new NanoSecInvocationStrategy();
		case MODE_XORSHIFT:
			return new XORShiftInvocationStrategy();
		case MODE_RANDOM:
			return new RandomInvocationStrategy(false);
		case MODE_SECURERANDOM:
			return new RandomInvocationStrategy(true);
		default:
			throw new IllegalArgumentException("Invalid strategy mode (" + mode + ")!");
		}

	}

}