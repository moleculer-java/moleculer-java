package services.moleculer.uids;

import services.moleculer.ServiceBroker;

public abstract class UIDGenerator {

	public void init(ServiceBroker broker) {
	}

	public void close() {
	}
	
	public abstract String nextUID();
	
}