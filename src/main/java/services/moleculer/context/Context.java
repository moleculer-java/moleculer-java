package services.moleculer.context;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;

public final class Context {

	// --- PROPERTIES ---

	private final long created = System.currentTimeMillis();
	private final ServiceBroker broker;
	private final String id;
	private final Tree params;
	private final Tree meta;

	// --- CONSTRUCTOR ---

	public Context(ServiceBroker broker, String id, Tree params, Tree meta) {
		this.broker = broker;
		this.id = id;
		this.params = params;
		this.meta = meta;
	}

	// --- VARIABLE GETTERS ---

	public final long created() {
		return created;
	}

	public final String id() {
		return id;
	}

	public final Tree params() {
		return params;
	}

	public final Tree meta() {
		return meta;
	}

	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Call an action (local or remote)
	 * 
	 * @param actionName
	 * @param params
	 * @param opts
	 * 
	 * @return
	 */
	public final Promise call(String actionName, Tree params, CallingOptions opts) throws Exception {
		return broker.call(actionName, params, opts);
	}

	// --- EMIT EVENTS VIA EVENT BUS ---

	/**
	 * Emit an event (grouped & balanced global event)
	 * 
	 * @param name
	 * @param payload
	 * @param groups
	 */
	public final void emit(String name, Object payload, String... groups) {
		broker.emit(name, payload);
	}

	/**
	 * Emit an event for all local & remote services
	 * 
	 * @param name
	 * @param payload
	 */
	public final void broadcast(String name, Object payload) {
		broker.emit(name, payload);
	}

	/**
	 * Emit an event for all local services
	 * 
	 * @param name
	 * @param payload
	 */
	public final void broadcastLocal(String name, Object payload) {
		broker.emit(name, payload);
	}

}