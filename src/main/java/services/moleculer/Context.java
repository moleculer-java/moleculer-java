package services.moleculer;

import io.datatree.Tree;

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
	 * Metadata
	 */
	public final Tree meta;
	
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

	public Context(ServiceBroker broker, Action action, Tree params, Tree meta, String requestID) {
		this.broker = broker;
		this.id = action.local() ? null : broker.nextUID();
		this.action = action;
		this.nodeID = action.nodeID();
		this.parentID = null;
		this.params = params;
		this.meta = meta == null ? new Tree() : meta;
		this.requestID = requestID;
		this.level = 1;
		this.metrics = false;
	}

	Context(ServiceBroker broker, Action action, Tree params, Tree meta, Context parent) {
		this.broker = broker;
		this.id = action.local() ? null : broker.nextUID();
		this.action = action;
		this.nodeID = action.nodeID();
		this.parentID = parent.id;
		this.params = params;
		this.requestID = parent.requestID;
		this.level = parent.level + 1;
		this.metrics = parent.metrics;
		
		Tree m;
		if (parent.meta == null) {
			m = meta;
		} else {
			m = parent.meta.clone();
			if (meta != null) {
				m.copyFrom(meta);
			}
		}
		
		this.meta = m;
	}

	// --- ACTION CALL ---
	
	public Object call(String actionName, Tree params, CallingOptions opts) throws Exception {
		Action action = broker.getAction(actionName);
		Context ctx = new Context(broker, action, params, null, this);
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