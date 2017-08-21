package services.moleculer.actions;

import services.moleculer.Action;
import services.moleculer.Context;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cacher;
import services.moleculer.transporters.Transporter;

public final class RemoteAction extends ActionContainer implements Action {

	// --- VARIABLES ---
	
	private final Transporter transporter;
	
	// --- CONSTRUCTOR ---

	RemoteAction(ServiceBroker broker, Cacher cacher, String nodeID, String name) {
		super(broker, cacher, nodeID, name);
		this.transporter = broker.getTransporter();
	}
	
	// --- INVOKE REMOTE ACTION ---

	@Override
	final Object invoke(Context ctx) throws Exception {
		// return transporter.publish(packet);
		return null;
	}
	
}