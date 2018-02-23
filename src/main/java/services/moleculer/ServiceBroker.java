package services.moleculer;

import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.service.Service;

public class ServiceBroker {

	// --- CONSTRUCTORS ---
	
	public ServiceBroker(ServiceBrokerConfig config) {
		
	}
	
	// --- START BROKER INSTANCE ---

	/**
	 * Start broker. If has transporter, transporter.connect will be called.
	 */
	public void start() throws Exception {
	}

	// --- STOP BROKER INSTANCE ---

	/**
	 * Stop broker. If the Broker has a Transporter, transporter.disconnect will
	 * be called.
	 */
	public void stop() {
	}

	// --- CREATE SERVICE ---
	
	public void createService(String name, Service service) {
		System.out.println("CREATE " + name + " " + service);
	}
	
}