package services.moleculer.utils;

import services.moleculer.ServiceBroker;

public interface MoleculerComponent {

	// --- START COMPONENT ---

	public void init(ServiceBroker broker) throws Exception;

	// --- STOP COMPONENT ---

	public void close();

}