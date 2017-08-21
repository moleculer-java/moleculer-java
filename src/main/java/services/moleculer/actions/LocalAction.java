package services.moleculer.actions;

import services.moleculer.Action;
import services.moleculer.Context;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cacher;

public final class LocalAction extends ActionContainer {

	// --- VARIABLES ---

	final Action action;

	// --- CONSTRUCTOR ---

	LocalAction(ServiceBroker broker, Cacher cacher, String name, Action action) {
		super(broker, cacher, broker.getNodeID(), name);
		this.action = action;
	}

	// --- INVOKE LOCAL ACTION ---

	@Override
	final Object invoke(Context ctx) throws Exception {
		return action.handler(ctx);
	}

}