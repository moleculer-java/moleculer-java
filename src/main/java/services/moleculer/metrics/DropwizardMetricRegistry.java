/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2020 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.metrics;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Timer;

import services.moleculer.service.Name;

/**
 * (Dropwizard) Metrics provides a powerful toolkit of ways to measure the
 * behavior of critical components in your production environment. With modules
 * for common libraries like Jetty, Logback, Log4j, Apache HttpClient, Ehcache,
 * JDBI, Jersey and reporting backends like Graphite, Metrics provides you with
 * full-stack visibility. https://metrics.dropwizard.io
 * 
 * <pre>
 * // Create Service Broker configuration
 * ServiceBrokerConfig cfg = new ServiceBrokerConfig();
 * 
 * // Create and enable Dropwizard Metric Registry
 * DropwizardMetricRegistry metrics = new DropwizardMetricRegistry();
 * cfg.setMetrics(metrics);
 * cfg.setMetricsEnabled(true);
 * 
 * // Start JMX Reporter (optional)
 * metrics.startJmxReporter();
 * 
 * // Create Service Broker with metrics
 * ServiceBroker broker = new ServiceBroker(cfg);
 * </pre>
 * 
 * @see MicrometerMetricRegistry
 */
@Name("Dropwizard Metric Registry")
public class DropwizardMetricRegistry extends MetricRegistry {

	// --- PROPERTIES ---

	protected String registryName = "moleculer";

	protected boolean useSharedRegistry;

	protected com.codahale.metrics.MetricRegistry dropwizardRegistry;

	// --- REPORTERS ---

	protected HashMap<Class<? extends Closeable>, Closeable> reporters = new HashMap<>();

	// --- CONSTRUCTORS ---

	public DropwizardMetricRegistry() {		
	}
	
	public DropwizardMetricRegistry(String sharedRegistryName) {
		setRegistryName(sharedRegistryName);
		setUseSharedRegistry(true);
	}

	public DropwizardMetricRegistry(com.codahale.metrics.MetricRegistry registry) {
		this.dropwizardRegistry = registry;
	}

	// --- METRIC FACTORIES ---

	@Override
	public MetricCounter getCounter(String name, String description, String... tagValuePairs) {
		return (MetricCounter) getOrCreate(name, tagValuePairs, () -> {

			// Create new counter
			Counter counter = getRegistry().counter(name(name, tagValuePairs));

			// Increment / decrement / get value
			return new MetricCounter(name, description, tagValuePairs) {

				@Override
				public final void increment() {
					counter.inc();
				}

				@Override
				public final void increment(long value) {
					counter.inc(value);
				}

				@Override
				public final void decrement() {
					counter.dec();
				}

				@Override
				public final void decrement(long value) {
					counter.dec(value);
				}

				@Override
				public final long getCounter() {
					return counter.getCount();
				}
				
			};
			
		});
	}

	@Override
	public MetricGauge getGauge(String name, String description, String... tagValuePairs) {
		return (MetricGauge) getOrCreate(name, tagValuePairs, () -> {

			// Create new gauge
			AtomicLong store = new AtomicLong();
			Gauge<Long> gauge = new Gauge<Long>() {

				@Override
				public Long getValue() {
					return store.get();
				}

			};
			getRegistry().register(name(name, tagValuePairs), gauge);

			// Set / get value
			return new MetricGauge(name, description, tagValuePairs) {

				@Override
				public final void setValue(long value) {
					store.set(value);
				}

				@Override
				public final long getValue() {
					return store.get();
				}

			};
		});
	}

	@Override
	public MetricHistogram getHistogram(String name, String description, long window, TimeUnit windowUnit,
			String... tagValuePairs) {
		return (MetricHistogram) getOrCreate(name, tagValuePairs, () -> {

			// Create new histogram
			SlidingTimeWindowReservoir reservoir = new SlidingTimeWindowReservoir(window, windowUnit);
			Histogram histogram = new Histogram(reservoir);
			getRegistry().register(name(name, tagValuePairs), histogram);

			// Add / get values
			return new MetricHistogram(name, description, tagValuePairs) {

				@Override
				public final void addValue(long value) {
					histogram.update(value);
				}

				@Override
				public final long getMax() {
					return histogram.getSnapshot().getMax();
				}

				@Override
				public final double getMean() {
					return histogram.getSnapshot().getMean();
				}

				@Override
				public final double get75thPercentile() {
					return histogram.getSnapshot().get75thPercentile();
				}

				@Override
				public final double get95thPercentile() {
					return histogram.getSnapshot().get95thPercentile();
				}

				@Override
				public final double get98thPercentile() {
					return histogram.getSnapshot().get98thPercentile();
				}

				@Override
				public final double get99thPercentile() {
					return histogram.getSnapshot().get99thPercentile();
				}

				@Override
				public final double get999thPercentile() {
					return histogram.getSnapshot().get999thPercentile();
				}

			};
		});
	}

	@Override
	public MetricTimer getTimer(String name, String description, long window, TimeUnit windowUnit, TimeUnit timeUnit,
			String... tagValuePairs) {
		return (MetricTimer) getOrCreate(name, tagValuePairs, () -> {

			// Create new timer
			SlidingTimeWindowReservoir reservoir = new SlidingTimeWindowReservoir(window, windowUnit);
			Timer timer = new Timer(reservoir);
			getRegistry().register(name(name, tagValuePairs), timer);

			// Add / get values
			return new MetricTimer(name, description, tagValuePairs) {

				@Override
				public final void addValue(long value) {
					timer.update(value, timeUnit);
				}

				@Override
				public final long getMax() {
					return timer.getSnapshot().getMax();
				}

				@Override
				public final double getMean() {
					return timer.getSnapshot().getMean();
				}

				@Override
				public final double get75thPercentile() {
					return timer.getSnapshot().get75thPercentile();
				}

				@Override
				public final double get95thPercentile() {
					return timer.getSnapshot().get95thPercentile();
				}

				@Override
				public final double get98thPercentile() {
					return timer.getSnapshot().get98thPercentile();
				}

				@Override
				public final double get99thPercentile() {
					return timer.getSnapshot().get99thPercentile();
				}

				@Override
				public final double get999thPercentile() {
					return timer.getSnapshot().get999thPercentile();
				}

			};
		});
	}

	// --- BUILT-IN REPORTERS ---

	/**
	 * Starts JmxReporter. With JmxReporter, you can expose your metrics as JMX
	 * MBeans. To explore this you can use VisualVM (which ships with most JDKs
	 * as jvisualvm) with the VisualVM-MBeans plugins installed or JConsole
	 * (which ships with most JDKs as jconsole).
	 */
	public void startJmxReporter() {
		startReporter(JmxReporter.forRegistry(getRegistry()).build(), 0, null);
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
	 * @param unit
	 *            the unit for {@code period}
	 */
	public void startConsoleReporter(long period, TimeUnit unit) {
		startReporter(ConsoleReporter.forRegistry(getRegistry()).build(), period, unit);
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
	public void startCsvReporter(File directory) {
		startReporter(CsvReporter.forRegistry(getRegistry()).build(directory), 1, TimeUnit.SECONDS);
	}

	/**
	 * Starts CsvReporter. For more complex benchmarks, Metrics comes with
	 * CsvReporter, which periodically appends to a set of .csv files in a given
	 * directory.
	 * 
	 * @param period
	 *            the amount of time between polls
	 * @param unit
	 *            the unit for {@code period}
	 * @param directory
	 *            directory the directory in which the {@code .csv} files will
	 *            be created
	 */
	public void startCsvReporter(long period, TimeUnit unit, File directory) {
		startReporter(CsvReporter.forRegistry(getRegistry()).build(directory), period, unit);
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
	 * @param unit
	 *            the unit for {@code period}
	 */
	public void startSlf4jReporter(long period, TimeUnit unit) {
		startSlf4jReporter(period, unit, null);
	}

	/**
	 * Starts Slf4jReporter. A reporter class for logging metrics values to a
	 * SLF4J Logge} periodically, similar to ConsoleReporter or CsvReporter, but
	 * using the SLF4J framework instead.
	 * 
	 * @param period
	 *            the amount of time between polls
	 * @param unit
	 *            the unit for {@code period}
	 * @param logger
	 *            name of the logger (eg. "moleculer.metrics" or null)
	 */
	public void startSlf4jReporter(long period, TimeUnit unit, String logger) {
		startReporter(Slf4jReporter.forRegistry(getRegistry())
				.outputTo(logger == null ? this.logger : broker.getLogger(logger)).build(), period, unit);
	}

	protected void startReporter(Closeable reporter, long period, TimeUnit unit) {
		Class<? extends Closeable> key = reporter.getClass();
		closeReporter(reporters.put(key, reporter));
		if (reporter instanceof ScheduledReporter) {
			ScheduledReporter task = (ScheduledReporter) reporter;
			task.start(period, unit);
		} else {
			try {
				reporter.getClass().getMethod("start").invoke(reporter);
			} catch (Exception cause) {
				reporters.remove(key);
				logger.error("Unable to start reporter!", cause);
			}
		}
	}

	protected void closeReporter(Closeable reporter) {
		if (reporter != null) {
			try {
				reporter.close();
			} catch (IOException ignored) {
			}
		}
	}

	// --- STOP ALL REPORTERS ---

	@Override
	public void stopped() {
		for (Closeable reporter : reporters.values()) {
			closeReporter(reporter);
		}
		reporters.clear();
	}

	// --- GETTERS / SETTERS ---

	public String getRegistryName() {
		return registryName;
	}

	public void setRegistryName(String registryName) {
		this.registryName = Objects.requireNonNull(registryName);
	}

	public boolean isUseSharedRegistry() {
		return useSharedRegistry;
	}

	public void setUseSharedRegistry(boolean useSharedRegistry) {
		this.useSharedRegistry = useSharedRegistry;
	}

	public com.codahale.metrics.MetricRegistry getRegistry() {
		if (dropwizardRegistry == null) {
			synchronized (this) {
				if (dropwizardRegistry == null) {
					if (useSharedRegistry) {
						dropwizardRegistry = SharedMetricRegistries.getOrCreate(registryName);
					} else {
						dropwizardRegistry = new com.codahale.metrics.MetricRegistry();
					}
				}
			}
		}
		return dropwizardRegistry;
	}

	public void setRegistry(com.codahale.metrics.MetricRegistry registry) {
		this.dropwizardRegistry = registry;
	}

}