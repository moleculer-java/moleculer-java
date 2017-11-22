package services.moleculer.eventbus;

import java.util.concurrent.ExecutorService;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

public final class LocalListenerEndpoint extends ListenerEndpoint {

	// --- PROPERTIES ---

	/**
	 * Listener instance (it's a field / inner class in Service object)
	 */
	private Listener listener;

	/**
	 * Invoke all local listeners via Thread pool (true) or directly (false)
	 */
	private boolean asyncLocalInvocation;

	// --- COMPONENTS ---

	private ExecutorService executor;

	// --- CONSTRUCTOR ---

	public LocalListenerEndpoint(Listener listener, boolean asyncLocalInvocation) {
		this.listener = listener;
		this.asyncLocalInvocation = asyncLocalInvocation;
	}

	// --- START CONTAINER ---

	/**
	 * Initializes Container instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {
		
		// Set base properties
		super.start(broker, config);

		// Process config
		asyncLocalInvocation = config.get(ASYNC_LOCAL_INVOCATION, asyncLocalInvocation);
	
		// Set components
		if (asyncLocalInvocation) {
			executor = broker.components().executor();
		}
	}

	// --- INVOKE LOCAL LISTENER ---

	@Override
	public final void on(String name, Tree payload, String[] groups) throws Exception {

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

	@Override
	public final boolean local() {
		return true;
	}

}