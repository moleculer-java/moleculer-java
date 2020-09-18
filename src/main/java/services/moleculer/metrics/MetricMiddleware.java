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

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.context.Context;
import services.moleculer.error.MoleculerError;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;

public class MetricMiddleware extends Middleware implements MetricConstants {

	protected Metrics metrics;

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		metrics = broker.getConfig().getMetrics();
	}

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
					String[] tags = new String[] { "action", actionName, "type", callType, "caller", ctx.nodeID };
					metrics.increment(MOLECULER_REQUEST_TOTAL, MOLECULER_REQUEST_TOTAL_DESC, tags);

					MetricCounter requestActive = metrics.increment(MOLECULER_REQUEST_ACTIVE,
							MOLECULER_REQUEST_ACTIVE_DESC, tags);

					metrics.increment(MOLECULER_REQUEST_LEVELS, MOLECULER_REQUEST_LEVELS_DESC, "action",
							actionName, "caller", ctx.nodeID, "level", Integer.toString(ctx.level));

					StoppableTimer timer = metrics.timer(MOLECULER_REQUEST_TIME, MOLECULER_REQUEST_TIME_DESC, tags);

					new Promise(action.handler(ctx)).then(tree -> {

						// After call (normal response)
						timer.stop();
						requestActive.decrement();

						resolver.resolve(tree);
					}).catchError(err -> {

						// After call (error)
						timer.stop();
						requestActive.decrement();

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
						metrics.increment(MOLECULER_REQUEST_ERROR_TOTAL, MOLECULER_REQUEST_ERROR_TOTAL_DESC, "action",
								actionName, "type", callType, "caller", ctx.nodeID, "errorName", errorName, "errorCode",
								Integer.toString(errorCode), "errorType", errorType);

						resolver.reject(err);
					});
				});
			}

		};
	}

}
