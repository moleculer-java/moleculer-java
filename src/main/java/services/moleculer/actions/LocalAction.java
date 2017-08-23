package services.moleculer.actions;

import services.moleculer.Action;
import services.moleculer.Context;
import services.moleculer.ServiceBroker;

public final class LocalAction extends ActionContainer {

	// --- PROPERTIES ---

	private final Action action;

	// --- CONSTRUCTOR ---

	LocalAction(ServiceBroker broker, String name, boolean cached, Action action) {
		super(broker, broker.nodeID(), name, cached);
		this.action = action;
	}

	// --- INVOKE LOCAL ACTION ---

	@Override
	final Object invoke(Context ctx) throws Exception {
		return action.handler(ctx);
	}

}