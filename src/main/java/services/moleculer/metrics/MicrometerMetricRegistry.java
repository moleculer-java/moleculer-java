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

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.HistogramSupport;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import services.moleculer.service.Name;

/**
 * Micrometer provides a simple facade over the instrumentation clients for the
 * most popular monitoring systems, allowing you to instrument your JVM-based
 * application code without vendor lock-in. Think SLF4J, but for metrics.
 * https://micrometer.io/
 * 
 * @see DropwizardMetricRegistry
 */
@Name("Micrometer Metric Registry")
public class MicrometerMetricRegistry extends MetricRegistry {

	// --- PROPERTIES ---

	protected MeterRegistry micrometerRegistry;

	protected boolean useGlobalRegistry;

	protected boolean closeRegistry;

	protected boolean publishPercentileHistogram = true;

	protected int percentilePrecision = 2;

	protected int distributionStatisticBufferLength = 5;

	protected JvmGcMetrics jvmGcMetrics;

	// --- CONSTRUCTORS ---

	public MicrometerMetricRegistry() {
	}

	public MicrometerMetricRegistry(MeterRegistry registry) {
		this.micrometerRegistry = registry;
	}

	// --- STOP METRIC REGISTRY ---

	@Override
	public void stopped() {
		if (jvmGcMetrics != null) {
			jvmGcMetrics.close();
			jvmGcMetrics = null;
		}
		if (micrometerRegistry != null && closeRegistry) {
			micrometerRegistry.close();
		}
	}

	// --- METRIC FACTORIES ---

	@Override
	public MetricCounter getCounter(String name, String description, String... tagValuePairs) {
		return (MetricCounter) getOrCreate(name, tagValuePairs, () -> {

			// Create Counter
			Counter counter = Counter.builder(name).description(description).tags(tagValuePairs)
					.register(getRegistry());

			// Increment / get value
			return new MetricCounter(name, description, tagValuePairs) {

				@Override
				public final void increment() {
					counter.increment();
				}

				@Override
				public final void increment(long value) {
					counter.increment(value);
				}

				@Override
				public final long getCounter() {
					return (long) counter.count();
				}

			};
		});
	}

	@Override
	public MetricGauge getGauge(String name, String description, String... tagValuePairs) {
		return (MetricGauge) getOrCreate(name, tagValuePairs, () -> {

			// Create Gauge
			AtomicLong store = new AtomicLong();
			Gauge.builder(name, store, Number::doubleValue).description(description).tags(tagValuePairs)
					.register(getRegistry());

			// Set value
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

			// Convert TimeUnit to Duration
			Duration duration;
			switch (windowUnit) {
			case DAYS:
				duration = Duration.ofDays(window);
				break;
			case HOURS:
				duration = Duration.ofHours(window);
				break;
			case MINUTES:
				duration = Duration.ofMinutes(window);
				break;
			case SECONDS:
				duration = Duration.ofSeconds(window);
				break;
			case MILLISECONDS:
				duration = Duration.ofMillis(window);
				break;
			case NANOSECONDS:
				duration = Duration.ofNanos(window);
				break;
			default:
				throw new IllegalArgumentException("Unsupported time unit!");
			}

			// Create histogram
			DistributionSummary histogram = DistributionSummary.builder(name).description(description)
					.tags(tagValuePairs).publishPercentileHistogram(publishPercentileHistogram)
					.percentilePrecision(percentilePrecision)
					.distributionStatisticBufferLength(distributionStatisticBufferLength)
					.distributionStatisticExpiry(duration).publishPercentiles(0.75, 0.95, 0.98, 0.99, 0.999)
					.register(getRegistry());

			// Add / get values
			return new MetricHistogram(name, description, tagValuePairs) {

				@Override
				public final void addValue(long value) {
					histogram.record(value);
				}

				@Override
				public final long getMax() {
					return (long) histogram.max();
				}

				@Override
				public final double getMean() {
					return histogram.mean();
				}

				@Override
				public final double get75thPercentile() {
					return getPercentile(histogram, 0.75);
				}

				@Override
				public final double get95thPercentile() {
					return getPercentile(histogram, 0.95);
				}

				@Override
				public final double get98thPercentile() {
					return getPercentile(histogram, 0.98);
				}

				@Override
				public final double get99thPercentile() {
					return getPercentile(histogram, 0.99);
				}

				@Override
				public final double get999thPercentile() {
					return getPercentile(histogram, 0.999);
				}

			};
		});
	}

	@Override
	public MetricTimer getTimer(String name, String description, long window, TimeUnit windowUnit, TimeUnit timeUnit,
			String... tagValuePairs) {
		return (MetricTimer) getOrCreate(name, tagValuePairs, () -> {

			// Convert TimeUnit to Duration
			Duration duration;
			switch (windowUnit) {
			case DAYS:
				duration = Duration.ofDays(window);
				break;
			case HOURS:
				duration = Duration.ofHours(window);
				break;
			case MINUTES:
				duration = Duration.ofMinutes(window);
				break;
			case SECONDS:
				duration = Duration.ofSeconds(window);
				break;
			case MILLISECONDS:
				duration = Duration.ofMillis(window);
				break;
			case NANOSECONDS:
				duration = Duration.ofNanos(window);
				break;
			default:
				throw new IllegalArgumentException("Unsupported time unit!");
			}

			// Create histogram
			Timer timer = Timer.builder(name).description(description).tags(tagValuePairs)
					.publishPercentileHistogram(publishPercentileHistogram).percentilePrecision(percentilePrecision)
					.distributionStatisticBufferLength(distributionStatisticBufferLength)
					.distributionStatisticExpiry(duration).publishPercentiles(0.75, 0.95, 0.98, 0.99, 0.999)
					.register(getRegistry());

			// Add / get values
			return new MetricTimer(name, description, tagValuePairs) {

				@Override
				public final void addValue(long value) {
					timer.record(value, timeUnit);
				}

				@Override
				public final long getMax() {
					return (long) timer.max(timeUnit);
				}

				@Override
				public final double getMean() {
					return timer.mean(timeUnit);
				}

				@Override
				public final double get75thPercentile() {
					return getPercentile(timer, 0.75);
				}

				@Override
				public final double get95thPercentile() {
					return getPercentile(timer, 0.95);
				}

				@Override
				public final double get98thPercentile() {
					return getPercentile(timer, 0.98);
				}

				@Override
				public final double get99thPercentile() {
					return getPercentile(timer, 0.99);
				}

				@Override
				public final double get999thPercentile() {
					return getPercentile(timer, 0.999);
				}

			};
		});
	}

	// --- UTILS ---

	protected double getPercentile(HistogramSupport histogram, double value) {
		HistogramSnapshot snapshot = histogram.takeSnapshot();
		if (snapshot != null) {
			ValueAtPercentile[] percentiles = snapshot.percentileValues();
			if (percentiles != null) {
				for (ValueAtPercentile percentile : percentiles) {
					if (value == percentile.percentile()) {
						return percentile.value();
					}
				}
			}
		}
		return 0;
	}

	// --- BUILT-IN DATA COLLECTORS ---

	public void startClassLoaderMetrics() {
		new ClassLoaderMetrics().bindTo(getRegistry());
	}

	public void startJvmMemoryMetrics() {
		new JvmMemoryMetrics().bindTo(getRegistry());
	}

	public void startProcessorMetrics() {
		new ProcessorMetrics().bindTo(getRegistry());
	}

	public void startJvmThreadMetrics() {
		new JvmThreadMetrics().bindTo(getRegistry());
	}

	public void startJvmGcMetrics() {
		jvmGcMetrics = new JvmGcMetrics();
		jvmGcMetrics.bindTo(getRegistry());
	}

	// --- GETTERS / SETTERS ---

	public boolean isUseGlobalRegistry() {
		return useGlobalRegistry;
	}

	public void setUseGlobalRegistry(boolean useGlobalRegistry) {
		this.useGlobalRegistry = useGlobalRegistry;
	}

	public MeterRegistry getRegistry() {
		if (micrometerRegistry == null) {
			synchronized (this) {
				if (micrometerRegistry == null) {
					if (micrometerRegistry == null) {
						if (useGlobalRegistry) {
							micrometerRegistry = Metrics.globalRegistry;
							closeRegistry = false;
						} else {
							micrometerRegistry = new SimpleMeterRegistry();
							closeRegistry = true;
						}
					} else {
						closeRegistry = false;
					}
				}
				logger.info("Metrics registry type is " + micrometerRegistry.toString() + ".");
			}
		}
		return micrometerRegistry;
	}

	public void setRegistry(MeterRegistry registry) {
		this.micrometerRegistry = registry;
	}

	public boolean isPublishPercentileHistogram() {
		return publishPercentileHistogram;
	}

	public void setPublishPercentileHistogram(boolean publishPercentileHistogram) {
		this.publishPercentileHistogram = publishPercentileHistogram;
	}

	public int getPercentilePrecision() {
		return percentilePrecision;
	}

	public void setPercentilePrecision(int percentilePrecision) {
		this.percentilePrecision = percentilePrecision;
	}

	public int getDistributionStatisticBufferLength() {
		return distributionStatisticBufferLength;
	}

	public void setDistributionStatisticBufferLength(int distributionStatisticBufferLength) {
		this.distributionStatisticBufferLength = distributionStatisticBufferLength;
	}

}