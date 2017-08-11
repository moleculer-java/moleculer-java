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
	 * Action definition
	 */
	public final Action action;

	/**
	 * Node ID
	 */
	public final String nodeID;

	/**
	 * Parent Context ID
	 */
	public final String parentID;

	/**
	 * Need send metrics events
	 */
	public final boolean metrics;

	/**
	 * Level of context
	 */
	public final int level;

	/**
	 * 
	 */
	public final long timeout;

	/**
	 * 
	 */
	public final int retryCount;

	/**
	 * 
	 */
	public final Tree params;

	/**
	 * 
	 */
	public final Tree meta;

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

}
