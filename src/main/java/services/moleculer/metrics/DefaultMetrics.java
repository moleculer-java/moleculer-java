/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2020 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted; free of charge; to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"); to deal in the Software without restriction; including
 * without limitation the rights to use; copy; modify; merge; publish;
 * distribute; sublicense; and/or sell copies of the Software; and to
 * permit persons to whom the Software is furnished to do so; subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS"; WITHOUT WARRANTY OF ANY KIND;
 * EXPRESS OR IMPLIED; INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY; FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM; DAMAGES OR OTHER LIABILITY; WHETHER IN AN ACTION
 * OF CONTRACT; TORT OR OTHERWISE; ARISING FROM; OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.metrics;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.util.CommonUtils;

@Name("Default Metric Registry")
public class DefaultMetrics extends CompositeMeterRegistry implements Metrics {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(DefaultMetrics.class);

	// --- INTERNAL VARIABLES ---

	protected final HashMap<String, Object> registry = new HashMap<>(128);

	protected final HashMap<Class<? extends MeterBinder>, MeterBinder> binders = new HashMap<>();

	protected final StampedLock lock = new StampedLock();

	protected DropwizardReporters reporters;

	// --- START METRICS ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
	}

	// --- METRIC REGISTRY FUNCTIONS ---

	@Override
	public MetricCounter increment(String name, String description, double delta, String... tags) {
		Counter counter = getMetric(name, tags, () -> {
			return Counter.builder(name).description(description).tags(tags).register(this);
		});
		if (delta != 0) {
			counter.increment(delta);
		}
		return value -> {
			counter.increment(value);
		};
	}

	@Override
	public MetricGauge set(String name, String description, double value, String... tags) {
		AtomicReference<Double> store = getMetric(name, tags, () -> {
			AtomicReference<Double> ref = new AtomicReference<>(0d);
			Gauge.builder(name, ref, d -> d.get()).description(description).tags(tags).register(this);
			return ref;
		});
		store.set(value);
		return newValue -> {
			store.set(newValue);
		};
	}

	@Override
	public StoppableTimer timer(String name, String description, Duration duration, String... tags) {
		Timer timer = Timer.builder(name).description(description).tags(tags).publishPercentileHistogram(true)
				.percentilePrecision(2).distributionStatisticBufferLength(5).distributionStatisticExpiry(duration)
				.publishPercentiles(0.75, 0.95, 0.98, 0.99, 0.999).register(this);
		long start = System.nanoTime();
		AtomicBoolean submitted = new AtomicBoolean();
		return () -> {
			if (submitted.compareAndSet(false, true)) {
				timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
			}
		};
	}

	// --- METRIC REGISTRY ---

	@SuppressWarnings("unchecked")
	protected <T> T getMetric(String name, String[] tags, Supplier<T> factory) {

		// Create map key
		String key;
		if (tags == null || tags.length == 0) {
			key = name;
		} else {
			StringBuilder keyBuilder = new StringBuilder(128);
			keyBuilder.append(name);
			keyBuilder.append('.');
			for (int i = 0; i < tags.length; i++) {
				keyBuilder.append(tags[i]);
				if (i < tags.length - 1) {
					keyBuilder.append('.');
				}
			}
			key = keyBuilder.toString();
		}

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
	 * @param loggerName
	 *            name of the logger (eg. "moleculer.metrics" or null)
	 */
	public void startSlf4jReporter(long period, TimeUnit periodUnit, String loggerName) {
		reporters().started(this, DropwizardReporters.TYPE_CSV, period, periodUnit,
				loggerName == null ? DefaultMetrics.class.getName() : loggerName);
	}

	protected synchronized DropwizardReporters reporters() {
		if (reporters == null) {
			try {
				reporters = (DropwizardReporters) Class.forName("services.moleculer.metrics.DropwizardReportersImpl")
						.newInstance();
			} catch (Throwable cause) {
				logger.error("Unable to create Dropwizard Reporter!", cause);
				CommonUtils.suggestDependency("com.codahale.metrics", "metrics-core", "3.0.2");
			}
		}
		return reporters;
	}

	// --- JVM AND SYSTEM METRICS ---

	public void addClassLoaderMetrics() {
		addMetrics(new ClassLoaderMetrics());
	}

	public void addJvmMemoryMetrics() {
		addMetrics(new JvmMemoryMetrics());
	}

	public void addJvmGcMetrics() {
		addMetrics(new JvmGcMetrics());
	}

	public void addProcessorMetrics() {
		addMetrics(new ProcessorMetrics());
	}

	public void addJvmThreadMetrics() {
		addMetrics(new JvmThreadMetrics());
	}

	public void addExecutorServiceMetrics(ExecutorService executor, String executorServiceName, String... tags) {
		addMetrics(new ExecutorServiceMetrics(executor, executorServiceName, Tags.of(tags)));
	}

	public void addMetrics(MeterBinder binder) {
		if (!binders.containsKey(binder.getClass())) {
			binders.put(binder.getClass(), binder);
			binder.bindTo(this);
		}
	}

	// --- STOP REGISTRY ---

	@Override
	public void stopped() {
		try {
			this.close();
		} catch (Exception ignored) {
		}
		if (reporters != null) {
			try {
				reporters.stopped();
			} catch (Exception ignored) {
			}
			reporters = null;
		}
		if (!binders.isEmpty()) {
			for (MeterBinder binder : binders.values()) {
				if (binder instanceof AutoCloseable) {
					AutoCloseable closeable = (AutoCloseable) binder;
					try {
						closeable.close();
					} catch (Exception ignored) {
					}
				}
			}
			binders.clear();
		}
	}

}