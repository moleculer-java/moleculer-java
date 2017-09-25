package services.moleculer;

public class CircuitBreaker {

	// --- PROPERTIES ---

	/**
	 * Enable the protection
	 */
	protected final boolean enabled;
	
	/**
	 * Trip breaker after 3 failures
	 */
	protected final int maxFailures;
	
	/**
	 * Number of milliseconds to switch from open to half-open state
	 */
	protected final long halfOpenTime;
	
	/**
	 * Increment failures if the request is timed out
	 */
	protected final boolean failureOnTimeout;
	
	/**
	 * Increment failures if the request is rejected (by any Throwable)
	 */
	protected final boolean failureOnReject;

	// --- CONSTRUCTOR ---

	public CircuitBreaker() {
		this(false, 3, 1000, true, true);
	}
	
	public CircuitBreaker(boolean enabled, int maxFailures, long halfOpenTime, boolean failureOnTimeout,
			boolean failureOnReject) {
		this.enabled = enabled;
		this.maxFailures = maxFailures;
		this.halfOpenTime = halfOpenTime;
		this.failureOnTimeout = failureOnTimeout;
		this.failureOnReject = failureOnReject;
	}

}
