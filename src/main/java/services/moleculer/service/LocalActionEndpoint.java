package services.moleculer.service;

import services.moleculer.context.Context;

public class LocalActionEndpoint extends ActionEndpoint {

	// --- PROPERTIES ---

	/**
	 * Action instance (it's a field / inner class in Service object)
	 */
	protected final Action action;

	// --- CONSTRUCTOR ---
		
	public LocalActionEndpoint(Action action) {
		this.action = action;
		
		// Process annotations
	}

	// --- INVOKE ACTION ---

	@Override
	public Object handler(Context ctx) throws Exception {
		return action.handler(ctx);
	}

}
