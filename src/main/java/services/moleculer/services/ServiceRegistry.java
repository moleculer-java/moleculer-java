package services.moleculer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.utils.MoleculerComponent;

@Name("Service Registry")
public abstract class ServiceRegistry implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- CONSTUCTOR ---

	public ServiceRegistry() {
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

	// --- REMOVE LOCAL SERVICE(S) ---
	
	public abstract void removeService(Service... services);

	// --- ADD REMOTE ACTION ---
	
	public abstract void addAction(String nodeID, String name, Tree parameters) throws Exception;

	// --- GET LOCAL SERVICE ---
	
	public abstract Service getService(String name);
		
	// --- GET ACTION ---
	
	public abstract ActionContainer getAction(String nodeID, String name);
	
}
