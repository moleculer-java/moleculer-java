package services.moleculer.internal;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.service.Action;
import services.moleculer.transporter.Transporter;

public abstract class AbstractInternalAction implements Action, MoleculerComponent {

	// --- PARENT BROKER ---
	
	protected ServiceBroker broker;
	
	// --- COMPONENTS ---
	
	protected Transporter transporter;
	
	// --- START ACTION INSTANCE ---

	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
		this.broker = broker;
		this.transporter = broker.components().transporter();
	}

	// --- STOP ACTION INSTANCE ---

	@Override
	public void stop() {
		this.broker = null;
		this.transporter = null;
	}

}