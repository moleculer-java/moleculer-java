package services.moleculer;

public class ServiceBroker {

	public <T extends Service> T createService(T service) {
			
		return service;
	}
	
}
