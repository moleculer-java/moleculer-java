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
			startReporter(ConsoleReporter.forRegistry(dropwizardRegistry).convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS).build(), period, periodUnit);
			logger.info(
					"Console Reporter started (period: " + period + " " + periodUnit.toString().toLowerCase() + ").");
			break;
		case TYPE_SLF4J:
			startReporter(
					Slf4jReporter.forRegistry(dropwizardRegistry).convertRatesTo(TimeUnit.SECONDS)
							.convertDurationsTo(TimeUnit.MILLISECONDS).outputTo(LoggerFactory.getLogger(param)).build(),
					period, periodUnit);
			logger.info("Slf4j Reporter started (period: " + period + " " + periodUnit.toString().toLowerCase() + ").");
			break;
		case TYPE_JMX:
			startReporter(JmxReporter.forRegistry(dropwizardRegistry).convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS).build(), 0, null);
			logger.info("JMX Reporter started.");
			break;
		case TYPE_CSV:
			startReporter(CsvReporter.forRegistry(dropwizardRegistry).convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS).build(new File(param)), period, periodUnit);
			logger.info("CSV Reporter started (directory: " + param + ", period: " + period + " "
					+ periodUnit.toString().toLowerCase() + ").");
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
