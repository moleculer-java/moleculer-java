package services.moleculer.services;

import services.moleculer.ServiceBroker;
import services.moleculer.utils.MoleculerComponent;

public abstract class ServiceRegistry implements MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public String name() {
		return "Service Registry";
	}
	
	// --- INIT SERVICE REGISTRY ---
	
	@Override
	public void init(ServiceBroker broker) throws Exception {
	}

	// --- STOP SERVICE REGISTRY ---
	
	@Override
	public void close() {
	}

	// --- ADD SERVICE ---
	
	public abstract void add(Service service) throws Exception;

	// --- GET SERVICE ---
	
	public abstract Service get(String name);
	
	// --- REMOVE SERVICE ---
	
	public abstract boolean remove(String name);

	// --- QUERY SERVICE ---
	
	public abstract boolean contains(String name);
	
}
