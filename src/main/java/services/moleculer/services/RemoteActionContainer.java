package services.moleculer.services;

import java.util.Objects;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;

public final class RemoteActionContainer implements ActionContainer {

	// --- PROPERTIES ---

	private final String nodeID;
	private final String name;
	private final boolean cached;
	private final String[] cacheKeys;

	// --- COMPONENTS ---

	private final ServiceRegistry serviceRegistry;

	// --- CONSTRUCTOR ---

	public RemoteActionContainer(ServiceBroker broker, Tree parameters) {

		// Set nodeID
		nodeID = parameters.get("nodeID", (String) null);
		Objects.nonNull(nodeID);

		// Set name
		name = parameters.get("name", (String) null);
		Objects.nonNull(name);

		// Set service registry
		serviceRegistry = broker.components().serviceRegistry();
		Objects.nonNull(serviceRegistry);

		// Set cache parameters
		cached = parameters.get("cached", false);
		cacheKeys = parameters.get("cacheKeys", "").split(",");
	}

	// --- INVOKE REMOTE SERVICE ---

	@Override
	public final Promise call(Tree params, CallingOptions opts) {
		return serviceRegistry.send(name, params, opts);
	}

	// --- PROPERTY GETTERS ---

	@Override
	public final String name() {
		return name;
	}

	@Override
	public final String nodeID() {
		return nodeID;
	}

	@Override
	public final boolean local() {
		return false;
	}

	@Override
	public final boolean cached() {
		return cached;
	}

	@Override
	public final String[] cacheKeys() {
		return cacheKeys;
	}

}