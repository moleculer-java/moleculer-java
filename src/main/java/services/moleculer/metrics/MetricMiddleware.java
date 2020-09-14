package services.moleculer.metrics;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.context.Context;
import services.moleculer.error.MoleculerError;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;

public class MetricMiddleware extends Middleware implements MetricsConstants {

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
					metrics.increment(MOLECULER_REQUEST_TOTAL, "Number of requests", tags);

					MetricCounter requestActive = metrics.increment(MOLECULER_REQUEST_ACTIVE,
							"Number of active requests", tags);

					metrics.increment(MOLECULER_REQUEST_LEVELS, "Number of requests by context level", "action",
							actionName, "caller", ctx.nodeID, "level", Integer.toString(ctx.level));

					StoppableTimer timer = metrics.timer(MOLECULER_REQUEST_TIME, "Request times", tags);

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
						metrics.increment(MOLECULER_REQUEST_ERROR_TOTAL, "Number of request errors", "action",
								actionName, "type", callType, "caller", ctx.nodeID, "errorName", errorName, "errorCode",
								Integer.toString(errorCode), "errorType", errorType);

						resolver.reject(err);
					});
				});
			}

		};
	}

}
