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

	public RemoteActionContainer(ServiceBroker broker, Tree config) {

		// Set nodeID
		nodeID = Objects.requireNonNull(config.get("nodeID", (String) null));

		// Set name
		name = Objects.requireNonNull(config.get("name", (String) null));

		// Set service registry
		serviceRegistry = Objects.requireNonNull(broker.components().serviceRegistry());

		// Set cache parameters
		cached = config.get("cached", false);
		cacheKeys = config.get("cacheKeys", "").split(",");
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