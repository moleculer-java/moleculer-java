package services.moleculer.actions;

import services.moleculer.ServiceBroker;
import services.moleculer.context.Context;

public final class LocalAction extends ActionContainer {

	// --- PROPERTIES ---

	private final Action action;

	// --- CONSTRUCTOR ---

	LocalAction(ServiceBroker broker, String name, boolean cached, String[] keys, Action action) {
		super(broker, broker.nodeID(), name, cached, keys);
		this.action = action;
	}

	// --- INVOKE LOCAL ACTION ---

	@Override
	final Object invoke(Context ctx) throws Exception {
		return action.handler(ctx);
	}

}