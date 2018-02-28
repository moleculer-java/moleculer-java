/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
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
package services.moleculer.repl;

import static services.moleculer.util.CommonUtils.nameOf;

import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * Base superclass of all REPL (interactive console) implementations.
 * 
 * @see LocalRepl
 * @see RemoteRepl
 */
@Name("REPL Console")
public abstract class Repl extends Service {

	// --- START CONSOLE INSTANCE ---

	/**
	 * Is console enabled?
	 */
	private boolean enabled = true;

	/**
	 * Is console running?
	 */
	private volatile boolean running;

	/**
	 * Initializes console instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Start (if enabled)
		startOrStopReading();
	}

	protected synchronized void startOrStopReading() {
		if (running) {
			if (!enabled) {
				stopNow();
			}
		} else {
			if (enabled) {
				try {
					startReading();
					running = true;
				} catch (Throwable cause) {
					logger.error("Unable to start console!", cause);
				}
			}
		}
	}

	protected synchronized void stopNow() {
		try {
			stopReading();
			logger.info(nameOf(this, true) + " stopped.");
		} catch (Throwable cause) {
			logger.error("Unable to stop console!", cause);
		}
		running = false;
	}
	
	// --- STOP CONSOLE INSTANCE ---

	/**
	 * Closes console.
	 */
	@Override
	public void stopped() {
		stopNow();
	}

	// --- START READING INPUT ---

	protected abstract void startReading();

	// --- STOP READING INPUT ---

	protected abstract void stopReading();

	// --- GETTERS / SETTERS ---

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		startOrStopReading();
	}

	public boolean isEnabled() {
		return enabled;
	}
	
}