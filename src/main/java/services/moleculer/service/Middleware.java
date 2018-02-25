package services.moleculer.service;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;

public abstract class Middleware implements MoleculerComponent {

	// --- ADD MIDDLEWARE TO ACTION ---
	
	public abstract Action install(Action action, Tree config);

	// --- START MIDDLEWARE ---
	
	@Override
	public void start(ServiceBroker broker) throws Exception {
	}

	// --- STOP MIDDLEWARE ---
	
	@Override
	public void stop() {
	}
		
}