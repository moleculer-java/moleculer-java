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
	public boolean metrics;

	/**
	 * Level of context
	 */
	public int level;

	/**
	 * Calling timeout
	 */
	public long timeout;

	/**
	 * Count of retries
	 */
	public int retryCount;
	
	// --- METRICS PROPERTIES ---
	
	public String requestID;
	
	public long startTime;
	
	public long stopTime;
	
	public double duration;

	public boolean cachedResult;

	// --- CONSTUCTORS ---

	public Context(ServiceBroker broker, Action action, Tree params) {
		this.broker = broker;
		this.id = "";
		this.action = action;
		this.nodeID = action.getNodeID();
		this.parentID = null;
		this.params = params;
		this.meta = new Tree();
	}

	protected void generateID() {
		//this.id = utils.generateToken();
	}
	
	protected void setParams(Tree params, boolean cloning) {
		//
	}
	
	public Object call(String actionName, Tree params, CallingOptions opts) {
		return null;
	}
	
	public void emit(String eventName, Object payload) {
		//
	}
	
	protected void metricStart(boolean emitEvent) {
		
	}
	
	protected void metricFinish(Throwable error, boolean emitEvent) {
		
	}

}