package services.moleculer.context;

import services.moleculer.ServiceBroker;
import services.moleculer.uids.UIDGenerator;

public final class ThreadBasedContextPool extends ContextPool {

	// --- NAME OF THE MOLECULER COMPONENT ---

	@Override
	public final String name() {
		return "Thread-based Context Pool";
	}

	// --- INTERNAL COMPONENTS ---

	private ServiceBroker broker;
	private UIDGenerator uidGenerator;

	// --- THREAD-BASED CONTEXT POOL ---

	private final ThreadLocal<ThreadBasedContext> pool = new ThreadLocal<>();

	// --- START CONTEXT FACTORY ---

	/**
	 * Initializes Default Context Factory instance.
	 * 
	 * @param broker
	 */
	@Override
	public void init(ServiceBroker broker) throws Exception {
		this.broker = broker;
		this.uidGenerator = broker.components().uidGenerator();
	}

	// --- GET CONTEXT FROM POOL ---

	public final Context borrow() {
		ThreadBasedContext ctx = pool.get();
		if (ctx == null) {
			ctx = new ThreadBasedContext();
		} else {
			pool.remove();
		}

		return ctx;
	}

	// --- PUSHBACK CONTEXT INTO THE POOL ---

	public final void release(Context ctx) {
		pool.set((ThreadBasedContext) ctx);
	}

}