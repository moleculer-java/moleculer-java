package services.moleculer.utils;

import services.moleculer.ServiceBroker;

public interface MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	public String name();
	
	// --- START COMPONENT ---
	
	public void init(ServiceBroker broker) throws Exception;
	
	// --- STOP COMPONENT ---
	
	public void close();
	
}