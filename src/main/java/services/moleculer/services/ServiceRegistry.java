package services.moleculer.services;

import io.datatree.Tree;
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

	// --- ADD LOCAL SERVICE(S) ---
	
	public abstract void addService(Service... services) throws Exception;

	// --- ADD REMOTE ACTION ---
	
	public abstract void addAction(String nodeID, String name, Tree parameters) throws Exception;

	// --- GET SERVICE ---
	
	public abstract Service getService(String name);
	
	// --- REMOVE SERVICE ---
	
	public abstract boolean removeService(String name);
	
	// --- GET THE NUMBER OF SERVICES ---
	
	public abstract int countServices();

	// --- GET ACTION ---
	
	public abstract Action getAction(String nodeID, String name);
	
}
