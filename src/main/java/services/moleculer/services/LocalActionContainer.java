package services.moleculer.services;

import static services.moleculer.utils.CommonUtils.idOf;
import static services.moleculer.utils.CommonUtils.nameOf;

import java.util.Objects;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;

public final class LocalActionContainer implements ActionContainer {

	// --- PROPERTIES ---

	private final String name;
	private final String nodeID;
	private final boolean cached;
	private final String[] cacheKeys;

	// --- LOCAL ACTION ---

	private final Action action;

	// --- COMPONENTS ---

	private ServiceRegistry serviceRegistry;

	// --- CONSTRUCTOR ---

	public LocalActionContainer(ServiceBroker broker, Tree parameters, Action instance) {

		// Set name
		String n = idOf(parameters);
		if (n.isEmpty()) {
			n = nameOf(instance, false);
		}
		name = n;

		// Set nodeID
		nodeID = Objects.requireNonNull(broker.nodeID());

		// Set action
		action = Objects.requireNonNull(instance);

		// Set cache parameters
		cached = parameters.get("cached", false);
		cacheKeys = parameters.get("cacheKeys", "").split(",");
	}

	// --- INVOKE LOCAL SERVICE ---

	@Override
	public final Promise call(Tree params, CallingOptions opts) {
		return serviceRegistry.call(action, params, opts);
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
		return true;
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