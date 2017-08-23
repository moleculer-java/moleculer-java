package services.moleculer.actions;

import services.moleculer.Action;
import services.moleculer.Context;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cacher;

abstract class ActionContainer implements Action {

	// --- CONSTANTS ---

	protected static final String NULL_VALUE = "null";

	// --- PROPERTIES ---

	protected final ServiceBroker broker;
	protected final String nodeID;
	protected final String name;
	protected final boolean local;
	protected final Cacher cacher;	

	// --- CONSTRUCTOR ---

	ActionContainer(ServiceBroker broker, String nodeID, String name, boolean cached) {
		this.broker = broker;
		this.nodeID = nodeID;
		this.name = name;
		this.cacher = cached ? broker.cacher() : null;
		this.local = broker.nodeID().equals(nodeID);
	}

	// --- INVOKE ACTION USING CACHE ---

	@Override
	public final Object handler(Context ctx) throws Exception {
		if (cacher == null) {
			
			// Invoke without cache
			return invoke(ctx);
		}

		// Generate cache key
		String key = cacher.getCacheKey(name, ctx.params, (String[]) null);
		
		// Find in cache
		Object result = cacher.get(key);
		if (result != null) {
			if (NULL_VALUE.equals(result)) {
				return null;
			}
			return result;
		}
		
		// Call action
		result = invoke(ctx);
		
		// Store result into cache
		if (result == null) {
			cacher.set(key, NULL_VALUE);
		} else {
			cacher.set(key, result);
		}
		
		// Return result
		return result;
	}

	// --- INVOKE ACTION ---

	abstract Object invoke(Context ctx) throws Exception;
	
	// --- EQUALS ---

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof ActionContainer) {
			ActionContainer other = (ActionContainer) obj;
			return name.equals(other.name) && nodeID.equals(other.nodeID);
		}
		return false;
	}

	// --- GETTERS ---

	@Override
	public final String name() {
		return name;
	}
	
	@Override
	public final String nodeID() {
		return nodeID;
	}

	@Override
	public final boolean isLocal() {
		return local;
	}
	
}