package services.moleculer.context;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.actions.Action;

public class Context {

	// --- VARIABLES ---

	/**
	 * Context ID
	 */
	public final String id;

	/**
	 * Broker instance
	 */
	public final ServiceBroker broker;

	/**
	 * Node ID
	 */
	public final String nodeID;

	/**
	 * Action
	 */
	public final Action action;

	/**
	 * Parent Context ID
	 */
	public final String parentID;

	/**
	 * Parameters
	 */
	public final Tree params;

	/**
	 * Need send metrics events
	 */
	public final boolean metrics;

	/**
	 * Level of context
	 */
	public final int level;

	/**
	 * Calling timeout
	 */
	public long timeout;

	/**
	 * Count of retries
	 */
	public int retryCount;
	
	// --- METRICS PROPERTIES ---
	
	public final String requestID;
	
	// public long startTime;
	
	// public long stopTime;
	
	// public double duration;

	// public boolean cachedResult;

	// --- CONSTUCTORS ---

	public Context(ServiceBroker broker, Action action, Tree params, String requestID) {
		this.broker = broker;
		this.id = action.local() ? null : broker.nextUID();
		this.action = action;
		this.nodeID = action.nodeID();
		this.parentID = null;
		this.params = params;
		this.requestID = requestID;
		this.level = 1;
		this.metrics = false;
	}

	Context(ServiceBroker broker, Action action, Tree params, Context parent) {
		this.broker = broker;
		this.id = action.local() ? null : broker.nextUID();
		this.action = action;
		this.nodeID = action.nodeID();
		this.parentID = parent.id;
		this.params = params;
		this.requestID = parent.requestID;
		this.level = parent.level + 1;
		this.metrics = parent.metrics;
		if (params != null && parent.params != null) {
			Tree parentMeta = parent.params.getMeta(false);
			if (parentMeta != null) {
				params.getMeta().copyFrom(parentMeta);
			}
		}
	}

	// --- ACTION CALL ---
	
	public Object call(String actionName, Tree params, CallingOptions opts) throws Exception {
		Action action = broker.getAction(actionName);
		Context ctx = new Context(broker, action, params, this);
		return action.handler(ctx);
	}
	
	// --- SUBMIT EVENT ---
	
	public void emit(String eventName, Object payload) {
		broker.emit(eventName, payload);
	}
	
	// --- METRICS ---
	
	protected void metricStart(boolean emitEvent) {
		
	}
	
	protected void metricFinish(Throwable error, boolean emitEvent) {
		
	}

}