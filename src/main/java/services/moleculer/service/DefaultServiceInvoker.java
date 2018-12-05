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
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.CallOptions;
import services.moleculer.context.CallOptions.Options;
import services.moleculer.context.Context;
import services.moleculer.context.ContextFactory;
import services.moleculer.error.MoleculerError;
import services.moleculer.stream.PacketStream;

/**
 * Default service invoker with retry logic.
 */
@Name("Default Service Invoker")
public class DefaultServiceInvoker extends ServiceInvoker {

	// --- PROPERTIES ---

	/**
	 * Write exceptions into the log file
	 */
	protected boolean writeErrorsToLog = true;

	// --- COMPONENTS ---

	protected ServiceRegistry serviceRegistry;
	protected ContextFactory contextFactory;

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
		this.contextFactory = cfg.getContextFactory();
	}

	// --- CALL SERVICE ---

	@Override
	public Promise call(String name, Tree params, Options opts, PacketStream stream, Context parent) {
		String targetID;
		int remaining;
		if (opts == null) {
			targetID = null;
			remaining = 0;
		} else {
			targetID = opts.nodeID;
			remaining = opts.retryCount;
		}
		return call(name, params, opts, stream, parent, targetID, remaining);
	}

	protected Promise call(String name, Tree params, Options opts, PacketStream stream, Context parent, String targetID,
			int remaining) {
		try {
			Action action = serviceRegistry.getAction(name, targetID);
			Context ctx = contextFactory.create(name, params, opts, stream, parent);
			if (remaining < 1 && !writeErrorsToLog) {
				return Promise.resolve(action.handler(ctx));
			}
			return Promise.resolve(action.handler(ctx)).catchError(cause -> {
				return retry(cause, name, params, opts, stream, parent, targetID, remaining);
			});
		} catch (Throwable cause) {
			return retry(cause, name, params, opts, stream, parent, targetID, remaining);
		}
	}

	// --- RETRY CALL ---

	protected Promise retry(Throwable cause, String name, Tree params, CallOptions.Options opts, PacketStream stream,
			Context parent, String targetID, int remaining) {

		// Write error to log file
		if (writeErrorsToLog) {
			logger.error("Unexpected error occurred while invoking \"" + name + "\" action!", cause);
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
		return call(name, params, opts, stream, parent, targetID, newRemaining);
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

}