package services.moleculer.metrics;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

public class DefaultMetrics extends CompositeMeterRegistry implements Metrics {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(DefaultMetrics.class);

	// --- INTERNAL VARIABLES ---

	protected final HashMap<String, Object> registry = new HashMap<>(128);

	protected final StampedLock lock = new StampedLock();

	protected DropwizardReporters reporters;

	// --- METRIC REGISTRY FUNCTIONS ---

	@Override
	public MetricCounter increment(String name, String description, double delta, String... tags) {
		Counter counter = getMetric(name, tags, () -> {
			return Counter.builder(name).description(description).tags(tags).register(this);
		});
		counter.increment(delta);
		return (value) -> {
			counter.increment(value);
		};
	}

	@Override
	public void set(String name, String description, double value, String... tags) {
		AtomicReference<Double> store = getMetric(name, tags, () -> {
			AtomicReference<Double> ref = new AtomicReference<>(0d);
			Gauge.builder(name, ref, d -> d.get()).description(description).tags(tags).register(this);
			return ref;
		});
		store.set(value);
	}

	@Override
	public StoppableTimer timer(String name, String description, Duration duration, String... tags) {
		DistributionSummary timer = getMetric(name, tags, () -> {
			return DistributionSummary.builder(name).description(description).tags(tags)
					.publishPercentileHistogram(true).percentilePrecision(2).distributionStatisticBufferLength(5)
					.distributionStatisticExpiry(duration).publishPercentiles(0.75, 0.95, 0.98, 0.99, 0.999)
					.register(this);
		});
		long start = System.nanoTime();
		return () -> {
			timer.record(System.nanoTime() - start);
		};
	}

	// --- METRIC REGISTRY ---

	@SuppressWarnings("unchecked")
	protected <T> T getMetric(String name, String[] tags, Supplier<T> factory) {

		// Create map key
		StringBuilder keyBuilder = new StringBuilder(128);
		keyBuilder.append(name);
		if (tags != null && tags.length > 1) {
			keyBuilder.append('.');
			for (int i = 0; i < tags.length; i++) {
				keyBuilder.append(tags[i]);
				if (i < tags.length - 1) {
					keyBuilder.append('.');
				}
			}
		}
		String key = keyBuilder.toString();

		// Find in registry
		Object metric = null;
		long stamp = lock.tryOptimisticRead();
		if (stamp != 0) {
			try {
				metric = registry.get(key);
			} catch (Exception modified) {
				stamp = 0;
			}
		}
		if (!lock.validate(stamp) || stamp == 0) {
			stamp = lock.readLock();
			try {
				metric = registry.get(key);
			} finally {
				lock.unlockRead(stamp);
			}
		}
		if (metric == null) {
			stamp = lock.writeLock();
			try {
				if (!registry.containsKey(key)) {
					metric = factory.get();
					registry.put(key, metric);
				}
			} finally {
				lock.unlockWrite(stamp);
			}
		}

		// Return the new metric
		return (T) metric;
	}

	// --- DROPWIZARD REPORTERS ---

	/**
	 * Starts JmxReporter. With JmxReporter, you can expose your metrics as JMX
	 * MBeans. To explore this you can use VisualVM (which ships with most JDKs
	 * as jvisualvm) with the VisualVM-MBeans plugins installed or JConsole
	 * (which ships with most JDKs as jconsole).
	 */
	public void startJmxReporter() {
		reporters().started(this, DropwizardReporters.TYPE_JMX, 0, null, null);
	}

	/**
	 * Starts ConsoleReporter. For simple benchmarks, Metrics comes with
	 * ConsoleReporter, which periodically reports all registered metrics to the
	 * console. The period time of the sampling is ONE MINUTE.
	 */
	public void startConsoleReporter() {
		startConsoleReporter(1, TimeUnit.MINUTES);
	}

	/**
	 * Starts ConsoleReporter. For simple benchmarks, Metrics comes with
	 * ConsoleReporter, which periodically reports all registered metrics to the
	 * console.
	 * 
	 * @param period
	 *            the amount of time between polls
	 * @param periodUnit
	 *            the unit for {@code period}
	 */
	public void startConsoleReporter(long period, TimeUnit periodUnit) {
		reporters().started(this, DropwizardReporters.TYPE_CONSOLE, period, periodUnit, null);
	}

	/**
	 * Starts CsvReporter. For more complex benchmarks, Metrics comes with
	 * CsvReporter, which periodically appends to a set of .csv files in a given
	 * directory. The period time of the sampling is ONE SECOND.
	 * 
	 * @param directory
	 *            directory the directory in which the {@code .csv} files will
	 *            be created
	 */
	public void startCsvReporter(String directory) {
		startCsvReporter(1, TimeUnit.SECONDS, directory);
	}

	/**
	 * Starts CsvReporter. For more complex benchmarks, Metrics comes with
	 * CsvReporter, which periodically appends to a set of .csv files in a given
	 * directory.
	 * 
	 * @param period
	 *            the amount of time between polls
	 * @param periodUnit
	 *            the unit for {@code period}
	 * @param directory
	 *            directory the directory in which the {@code .csv} files will
	 *            be created
	 */
	public void startCsvReporter(long period, TimeUnit periodUnit, String directory) {
		reporters().started(this, DropwizardReporters.TYPE_CSV, period, periodUnit, directory);
	}

	/**
	 * Starts Slf4jReporter. A reporter class for logging metrics values to a
	 * SLF4J Logge} periodically, similar to ConsoleReporter or CsvReporter, but
	 * using the SLF4J framework instead. The period time of the sampling is ONE
	 * MINUTE.
	 */
	public void startSlf4jReporter() {
		startSlf4jReporter(1, TimeUnit.MINUTES);
	}

	/**
	 * Starts Slf4jReporter. A reporter class for logging metrics values to a
	 * SLF4J Logge} periodically, similar to ConsoleReporter or CsvReporter, but
	 * using the SLF4J framework instead.
	 * 
	 * @param period
	 *            the amount of time between polls
	 * @param periodUnit
	 *            the unit for {@code period}
	 */
	public void startSlf4jReporter(long period, TimeUnit periodUnit) {
		startSlf4jReporter(period, periodUnit, null);
	}

	/**
	 * Starts Slf4jReporter. A reporter class for logging metrics values to a
	 * SLF4J Logge} periodically, similar to ConsoleReporter or CsvReporter, but
	 * using the SLF4J framework instead.
	 * 
	 * @param period
	 *            the amount of time between polls
	 * @param periodUnit
	 *            the unit for {@code period}
	 * @param logger
	 *            name of the logger (eg. "moleculer.metrics" or null)
	 */
	public void startSlf4jReporter(long period, TimeUnit periodUnit, String logger) {
		reporters().started(this, DropwizardReporters.TYPE_CSV, period, periodUnit,
				logger == null ? DefaultMetrics.class.getName() : logger);
	}

	protected synchronized DropwizardReporters reporters() {
		if (reporters == null) {
			try {
				reporters = (DropwizardReporters) Class.forName("services.moleculer.metrics.DropwizardReportersImpl")
						.newInstance();
			} catch (Throwable cause) {
				logger.error("Unable to create Dropwizard Reporter!", cause);
			}
		}
		return reporters;
	}

	// --- STOP REGISTRY ---
	
	public void stopped() {
		this.close();
		if (reporters != null) {
			reporters.stopped();
			reporters = null;
		}
	}
	
}