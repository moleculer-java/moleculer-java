/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.eventbus;

import java.util.concurrent.TimeUnit;

import services.moleculer.context.Context;
import services.moleculer.error.MoleculerError;
import services.moleculer.metrics.MetricConstants;
import services.moleculer.metrics.MetricCounter;
import services.moleculer.metrics.MetricRegistry;
import services.moleculer.metrics.MetricTimer;

public class LocalListenerEndpoint extends ListenerEndpoint implements MetricConstants {

	// --- PROPERTIES ---

	/**
	 * Listener instance (it's a field / inner class in Service object)
	 */
	protected Listener listener;

	/**
	 * Metrics registry (or null)
	 */
	protected MetricRegistry metrics;

	// --- CONSTRUCTOR ---

	public LocalListenerEndpoint(String nodeID, String service, String group, String subscribe, Listener listener,
			boolean privateAccess, MetricRegistry metrics) {
		super(nodeID, service, group, subscribe, privateAccess);
		this.listener = listener;
		this.metrics = metrics;
	}

	// --- INVOKE LOCAL LISTENER ---

	@Override
	public void on(Context ctx, Groups groups, boolean broadcast) throws Exception {
		if (metrics == null) {

			// Call without metrics
			listener.on(ctx);

		} else {

			// Get metrics
			String[] tagValuePairs = new String[] { "service", serviceName, "event", subscribe, "group",
					group == null ? "null" : group, "caller", ctx.nodeID };
			MetricCounter receivedActive = metrics.getCounter(MOLECULER_EVENT_RECEIVED_ACTIVE,
					"Number of active event executions", tagValuePairs);
			MetricTimer receivedTime = metrics.getTimer(MOLECULER_EVENT_RECEIVED_TIME, "Execution time of events", 5,
					TimeUnit.SECONDS, TimeUnit.NANOSECONDS, tagValuePairs);
			long startTime = System.nanoTime();

			// Call with metrics
			try {

				// Before call
				metrics.getCounter(MOLECULER_EVENT_RECEIVED_TOTAL, "Number of received events", tagValuePairs)
						.increment();
				receivedActive.increment();

				// Call listener
				listener.on(ctx);

			} catch (Exception cause) {

				// After call (error)
				String errorName = null;
				int errorCode = 500;
				String errorType = null;
				if (cause instanceof MoleculerError) {
					MoleculerError me = (MoleculerError) cause;
					errorName = me.getName();
					errorCode = me.getCode();
					errorType = me.getType();
				}
				if (errorName == null || errorName.isEmpty()) {
					errorName = "MoleculerError";
				}
				if (errorType == null || errorType.isEmpty()) {
					errorType = cause.getMessage();
					if (errorType == null || errorType.isEmpty()) {
						errorType = "MOLECULER_ERROR";
					}
				}
				metrics.getCounter(MOLECULER_EVENT_RECEIVED_ERROR_TOTAL, "Number of event execution errors", "service",
						serviceName, "event", subscribe, "group", group == null ? "null" : group, "caller", ctx.nodeID,
						"errorName", errorName, "errorCode", Integer.toString(errorCode), "errorType", errorType)
						.increment();

				// Rethrow error
				throw cause;

			} finally {

				// After call (error and normal response)
				receivedActive.decrement();
				receivedTime.addValue(System.nanoTime() - startTime);
			}
		}
	}

	// --- IS IT A LOCAL EVENT LISTENER? ---

	public boolean isLocal() {
		return true;
	}

}