package services.moleculer;

import io.datatree.Tree;

public class Context {

	// --- VARIABLES ---

	/**
	 * Context ID
	 */
	public String id;

	/**
	 * Broker instance
	 */
	public final ServiceBroker broker;

	/**
	 * Action definition
	 */
	public final Action action;

	/**
	 * Node ID
	 */
	public String nodeID;

	/**
	 * Parent Context ID
	 */
	public String parentID;

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

	/**
	 * Parameters
	 */
	public Tree params;

	/**
	 * Metadata
	 */
	public final Tree meta;
	
	// --- METRICS PROPERTIES ---
	
	public String requestID;
	
	public long startTime;
	
	public long stopTime;
	
	public double duration;

	public boolean cachedResult;

	// --- CONSTUCTORS ---

	public Context(ServiceBroker broker, Action action) {

		this.id = null;

		this.broker = broker;
		this.action = action;
		this.nodeID = null;
		this.parentID = null;

		this.metrics = false;
		this.level = 1;

		this.timeout = 0;
		this.retryCount = 0;

		this.params = new Tree();
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
	
	public static Context create(ServiceBroker broker, Action action, String nodeID, Tree params, CallingOptions opts) {
		// TODO
		return new Context(broker, action);		
	}
}
