package services.moleculer.metrics;

@FunctionalInterface
public interface MetricCounter {

	public default void decrement() {
		increment(-1);
	}

	public default void decrement(long value) {
		increment(-value);
	}

	public default void increment() {
		increment(1);
	}
	
	public void increment(long value);
	
}