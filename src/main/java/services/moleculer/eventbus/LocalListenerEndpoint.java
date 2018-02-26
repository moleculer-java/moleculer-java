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
package services.moleculer.eventbus;

import java.util.concurrent.ExecutorService;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

public class LocalListenerEndpoint extends ListenerEndpoint {

	// --- PROPERTIES ---

	/**
	 * Listener instance (it's a field / inner class in Service object)
	 */
	protected Listener listener;

	/**
	 * Invoke all local listeners via Thread pool (true) or directly (false)
	 */
	protected boolean asyncLocalInvocation;

	// --- COMPONENTS ---

	protected ExecutorService executor;

	// --- CONSTRUCTOR ---

	public LocalListenerEndpoint(ServiceBroker broker, String service, String group, String subscribe, Listener listener, boolean asyncLocalInvocation) {
		super(broker.getNodeID(), service, group, subscribe);
		this.listener = listener;
		this.asyncLocalInvocation = asyncLocalInvocation;
		this.executor = broker.getConfig().getExecutor();
	}

	// --- INVOKE LOCAL LISTENER ---

	@Override
	public void on(String name, Tree payload, Groups groups, boolean broadcast) throws Exception {

		// A.) Async invocation
		if (asyncLocalInvocation) {
			executor.execute(() -> {
				try {
					listener.on(payload);
				} catch (Exception cause) {
					logger.warn("Unable to invoke local listener!", cause);
				}
			});
			return;
		}

		// B.) Faster in-process (direct) invocation
		listener.on(payload);
	}

	// --- LOCAL LISTENER? ---
	
	public boolean isLocal() {
		return true;
	}
}