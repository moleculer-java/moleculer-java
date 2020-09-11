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

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.context.Context;
import services.moleculer.error.MoleculerError;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;

/**
 * Base superclass of all MetricRegistry implementations. MetricRegistry is an
 * abstract API, the background layer can be the Dropwizard Metrics or the
 * Micrometer Metrics implementation.
 * 
 * @see DropwizardMetricRegistry
 * @see MicrometerMetricRegistry
 */
@Name("Metric Registry")
public abstract class MetricRegistry extends Middleware implements MetricConstants {

	// --- METRICS MAP ---

	protected HashMap<String, Object> registry = new HashMap<>(64);

	// --- ADD MIDDLEWARE TO ACTION ---

	@Override
	public Action install(Action action, Tree config) {
		return new Action() {

			// Service-specific properties
			String actionName = config.get("name", "unknown");
			String localNodeID = getBroker().getNodeID();

			@Override
			public final Object handler(Context ctx) throws Exception {
				return new Promise(resolver -> {

					// Before call
					String callType = localNodeID.equals(ctx.nodeID) ? "local" : "remote";
					String[] tagValuePairs = new String[] { "action", actionName, "type", callType, "caller",
							ctx.nodeID };
					getCounter(MOLECULER_REQUEST_TOTAL, "Number of requests", tagValuePairs).increment();

					MetricCounter requestActive = getCounter(MOLECULER_REQUEST_ACTIVE, "Number of active requests",
							tagValuePairs);
					requestActive.increment();

					getCounter(MOLECULER_REQUEST_LEVELS, "Number of requests by context level", "action", actionName,
							"caller", ctx.nodeID, "level", Integer.toString(ctx.level)).increment();

					MetricTimer requestTime = getTimer(MOLECULER_REQUEST_TIME, "Request times", 5, TimeUnit.SECONDS,
							TimeUnit.NANOSECONDS, tagValuePairs);
					long startTime = System.nanoTime();

					new Promise(action.handler(ctx)).then(tree -> {

						// After call (normal response)
						requestActive.decrement();
						requestTime.addValue(System.nanoTime() - startTime);

						resolver.resolve(tree);
					}).catchError(err -> {

						// After call (error)
						requestActive.decrement();
						requestTime.addValue(System.nanoTime() - startTime);

						String errorName = null;
						int errorCode = 500;
						String errorType = null;
						if (err instanceof MoleculerError) {
							MoleculerError me = (MoleculerError) err;
							errorName = me.getName();
							errorCode = me.getCode();
							errorType = me.getType();
						}
						if (errorName == null || errorName.isEmpty()) {
							errorName = "MoleculerError";
						}
						if (errorType == null || errorType.isEmpty()) {
							errorType = err.getMessage();
							if (errorType == null || errorType.isEmpty()) {
								errorType = "MOLECULER_ERROR";
							}
						}
						getCounter(MOLECULER_REQUEST_ERROR_TOTAL, "Number of request errors", "action", actionName,
								"type", callType, "caller", ctx.nodeID, "errorName", errorName, "errorCode",
								Integer.toString(errorCode), "errorType", errorType).increment();

						resolver.reject(err);
					});
				});
			}

		};
	}

	// --- METRIC GETTERS ---

	public MetricCounter[] getCounters() {
		return getCounters(null);
	}

	public MetricCounter[] getCounters(String prefix) {
		return getMetricArray(prefix, MetricCounter.class);
	}

	public MetricGauge[] getGauges() {
		return getGauges(null);
	}

	public MetricGauge[] getGauges(String prefix) {
		return getMetricArray(prefix, MetricGauge.class);
	}

	public MetricHistogram[] getHistograms() {
		return getHistograms(null);
	}

	public MetricHistogram[] getHistograms(String prefix) {
		return getMetricArray(prefix, MetricHistogram.class);
	}

	public MetricTimer[] getTimers() {
		return getTimers(null);
	}

	public MetricTimer[] getTimers(String prefix) {
		return getMetricArray(prefix, MetricTimer.class);
	}

	@SuppressWarnings("unchecked")
	protected <T> T[] getMetricArray(String prefix, Class<T> type) {
		HashSet<T> set = new HashSet<>();
		String name;
		Object metric;
		for (Map.Entry<String, Object> entry : registry.entrySet()) {
			name = entry.getKey();
			if (prefix != null && !name.startsWith(prefix)) {
				continue;
			}
			metric = entry.getValue();
			if (!type.isAssignableFrom(metric.getClass())) {
				continue;
			}
			set.add((T) metric);
		}
		T[] array = (T[]) Array.newInstance(type, set.size());
		set.toArray(array);
		return array;
	}

	// --- METRIC FACTORIES ---

	public MetricCounter getCounter(String name) {
		return getCounter(name, null);
	}

	public MetricCounter getCounter(String name, String description) {
		return getCounter(name, description, (String[]) null);
	}

	public MetricGauge getGauge(String name) {
		return getGauge(name, null);
	}

	public MetricGauge getGauge(String name, String description) {
		return getGauge(name, description, (String[]) null);
	}

	public MetricHistogram getHistogram(String name) {
		return getHistogram(name, null);
	}

	public MetricHistogram getHistogram(String name, String description) {
		return getHistogram(name, description, 1, TimeUnit.MINUTES);
	}

	public MetricHistogram getHistogram(String name, String description, long window, TimeUnit windowUnit) {
		return getHistogram(name, description, window, windowUnit, (String[]) null);
	}

	public MetricTimer getTimer(String name) {
		return getTimer(name, null);
	}

	public MetricTimer getTimer(String name, String description) {
		return getTimer(name, description, 1, TimeUnit.MINUTES, TimeUnit.NANOSECONDS);
	}

	public MetricTimer getTimer(String name, String description, long window, TimeUnit windowUnit, TimeUnit timeUnit) {
		return getTimer(name, description, window, windowUnit, timeUnit, (String[]) null);
	}

	// --- ABSTRACT METRIC FACTORIES ---

	public abstract MetricCounter getCounter(String name, String description, String... tagValuePairs);

	public abstract MetricGauge getGauge(String name, String description, String... tagValuePairs);

	public abstract MetricHistogram getHistogram(String name, String description, long window, TimeUnit windowUnit,
			String... tagValuePairs);

	public abstract MetricTimer getTimer(String name, String description, long window, TimeUnit windowUnit,
			TimeUnit timeUnit, String... tagValuePairs);

	// --- MAP HANDLER ---

	protected Object getOrCreate(String name, String[] tagValuePairs, Supplier<Object> factory) {

		// Create map key
		String key = name(name, tagValuePairs);

		// Find in registry
		Object metric = registry.get(key);
		if (metric != null) {
			return metric;
		}

		// Create new metric
		metric = factory.get();

		// Store metric
		registry.put(key, metric);

		// Return the new metric
		return metric;
	}

	protected String name(String name, String[] tagValuePairs) {
		if (tagValuePairs == null || tagValuePairs.length == 0) {
			return name;
		}
		StringBuilder nameBuilder = new StringBuilder(128);
		nameBuilder.append(name);
		for (int i = 1; i < tagValuePairs.length; i += 2) {
			nameBuilder.append('.');

			// Append values only
			nameBuilder.append(tagValuePairs[i]);
		}
		return nameBuilder.toString();
	}

}