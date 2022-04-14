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
package services.moleculer.service;

import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

import io.datatree.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.Context;
import services.moleculer.error.MaxCallLevelError;
import services.moleculer.error.MoleculerClientError;
import services.moleculer.error.MoleculerError;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.metrics.MetricConstants;
import services.moleculer.metrics.Metrics;
import services.moleculer.uid.UidGenerator;

/**
 * Default service invoker with retry logic.
 */
@Name("Default Service Invoker")
public class DefaultServiceInvoker extends ServiceInvoker implements MetricConstants {

	// --- PROPERTIES ---

	/**
	 * Write exceptions into the log file
	 */
	protected boolean writeErrorsToLog = true;

	/**
	 * Max call level (for nested calls)
	 */
	protected int maxCallLevel = 100;

	// --- COMPONENTS ---

	protected ServiceRegistry serviceRegistry;
	protected Eventbus eventbus;
	protected UidGenerator uidGenerator;
	protected Metrics metrics;

	// --- RETRY LOGIC (BY ERROR) ---

	protected Predicate<Throwable> retryLogic = cause -> {
		if (cause == null) {
			return false;
		}
		Throwable test;
		if (cause instanceof CompletionException) {
			test = cause.getCause();
		} else {
			test = cause;
		}
		if (!(test instanceof MoleculerError)) {
			return false;
		}
		return ((MoleculerError) test).isRetryable();
	};

	// --- START INVOKER ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Set components
		ServiceBrokerConfig cfg = broker.getConfig();
		this.serviceRegistry = cfg.getServiceRegistry();
		this.eventbus = cfg.getEventbus();
		this.uidGenerator = cfg.getUidGenerator();
		if (cfg.isMetricsEnabled()) {
			metrics = cfg.getMetrics();
		}
	}

	// --- CALL SERVICE ---

	@Override
	public Promise call(Context ctx) {

		// Verify call level
		if (maxCallLevel > 0 && ctx.level >= maxCallLevel) {
			throw new MaxCallLevelError(broker.getNodeID(), maxCallLevel);
		}

		// Calling...
		String targetID;
		int remaining;
		if (ctx.opts == null) {
			targetID = null;
			remaining = 0;
		} else {
			targetID = ctx.opts.nodeID;
			remaining = ctx.opts.retryCount;
		}
		return call(ctx, targetID, remaining);
	}

	protected Promise call(Context ctx, String targetID, int remaining) {
		try {
			Action action = serviceRegistry.getAction(ctx.name, targetID);
			if (remaining < 1 && !writeErrorsToLog) {
				return Promise.resolve(action.handler(ctx));
			}
			return Promise.resolve(action.handler(ctx)).catchError(cause -> {
				return retry(ctx, targetID, remaining, cause);
			});
		} catch (Throwable cause) {
			return retry(ctx, targetID, remaining, cause);
		}
	}

	// --- RETRY CALL ---

	protected Promise retry(Context ctx, String targetID, int remaining, Throwable cause) {

		// Write error to log file
		if (writeErrorsToLog && cause != null) {
			boolean clientError = false;
			Throwable err = cause;
			while (err != null) {
				if (err instanceof MoleculerClientError) {
					clientError = true;
					break;
				}
				err = err.getCause();
			}
			if (clientError) {
				logger.warn("Unexpected client error occurred while invoking \"" + ctx.name + "\" action!", err);
			} else {
				logger.error("Unexpected error occurred while invoking \"" + ctx.name + "\" action!", cause);				
			}
		}

		// Check error type and error counter
		if (remaining < 1 || !retryLogic.test(cause)) {
			return Promise.reject(cause);
		}

		// Call again...
		int newRemaining = remaining - 1;
		if (writeErrorsToLog) {
			logger.warn("Retrying request (" + newRemaining + " attempts left)...");
		}

		// Metrics
		if (metrics != null) {
			metrics.increment(MOLECULER_REQUEST_RETRY_ATTEMPTS_TOTAL, MOLECULER_REQUEST_RETRY_ATTEMPTS_TOTAL_DESC, "action", ctx.name); 
		}
		
		// Create new Context (with new id)
		return call(new Context(this, eventbus, uidGenerator, uidGenerator.nextUID(), ctx.name, ctx.params, ctx.level,
				ctx.parentID, ctx.requestID, ctx.stream, ctx.opts, ctx.nodeID), targetID, newRemaining);
	}

	// --- GETTERS / SETTERS ---

	public boolean isWriteErrorsToLog() {
		return writeErrorsToLog;
	}

	public void setWriteErrorsToLog(boolean writeErrorsToLog) {
		this.writeErrorsToLog = writeErrorsToLog;
	}

	public Predicate<Throwable> getRetryLogic() {
		return retryLogic;
	}

	public void setRetryLogic(Predicate<Throwable> retryLogic) {
		this.retryLogic = Objects.requireNonNull(retryLogic);
	}

	public int getMaxCallLevel() {
		return maxCallLevel;
	}

	public void setMaxCallLevel(int maxCallLevel) {
		this.maxCallLevel = maxCallLevel;
	}

}