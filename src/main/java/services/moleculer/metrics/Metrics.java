package services.moleculer.metrics;

import java.time.Duration;

public interface Metrics {

	// --- CONSTANTS ---
	
	public static final Duration ONE_SECOND = Duration.ofSeconds(1);
	
	// --- COUNTER ---
	
	public default MetricCounter increment(String name, String description, String... tags) {
		return increment(name, description, 1, tags);
	}
	
	public MetricCounter increment(String name, String description, double delta, String... tags);

	// --- GAUGE ---
	
	public void set(String name, String description, double value, String... tags);

	// --- TIMER ---
	
	public default StoppableTimer timer(String name, String description, String... tags) {
		return timer(name, description, ONE_SECOND, tags);
	}
	
	public StoppableTimer timer(String name, String description, Duration duration, String... tags);

	// --- STOP ---
	
	public void stopped();
	
}