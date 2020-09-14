package services.moleculer.metrics;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

public class DropwizardReportersImpl implements DropwizardReporters {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(DropwizardReportersImpl.class);

	// --- REPORTERS ---

	protected HashMap<Class<? extends Closeable>, Closeable> reporters = new HashMap<>();

	// --- REGISTRIES ---

	protected MeterRegistry micrometerRegistry;
	protected MetricRegistry dropwizardRegistry;

	// --- VARIABLES ---

	protected final AtomicBoolean dropwizardInstalled = new AtomicBoolean();

	// --- CONSTRUCTOR ---

	public DropwizardReportersImpl() {
		dropwizardRegistry = new MetricRegistry();
		DropwizardConfig consoleConfig = new DropwizardConfig() {

			@Override
			public String prefix() {
				return "console";
			}

			@Override
			public String get(String key) {
				return null;
			}

		};
		micrometerRegistry = new DropwizardMeterRegistry(consoleConfig, dropwizardRegistry,
				HierarchicalNameMapper.DEFAULT, Clock.SYSTEM) {

			@Override
			protected Double nullGaugeValue() {
				return null;
			}

		};
	}

	@Override
	public void started(DefaultMetrics metrics, int type, long period, TimeUnit periodUnit, String param) {

		// Add Dropwizard registry
		if (dropwizardInstalled.compareAndSet(false, true)) {
			metrics.add(micrometerRegistry);
		}

		// Add reporter
		switch (type) {
		case TYPE_CONSOLE:
			startReporter(ConsoleReporter.forRegistry(dropwizardRegistry).build(), period, periodUnit);
			break;
		case TYPE_LOGGER:
			startReporter(
					Slf4jReporter.forRegistry(dropwizardRegistry).outputTo(LoggerFactory.getLogger(param)).build(),
					period, periodUnit);
			break;
		case TYPE_JMX:
			startReporter(JmxReporter.forRegistry(dropwizardRegistry).build(), 0, null);
			break;
		case TYPE_CSV:
			startReporter(CsvReporter.forRegistry(dropwizardRegistry).build(new File(param)), period, periodUnit);
			break;
		default:
			logger.error("Invalid reporter type: " + type);
			break;
		}
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

}
