package services.moleculer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;
import services.moleculer.utils.MoleculerComponent;

@Name("Service Registry")
public abstract class ServiceRegistry implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- INIT SERVICE REGISTRY ---

	@Override
	public void init(ServiceBroker broker) throws Exception {
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public void close() {
	}

	// --- CALL LOCAL SERVICE ---

	public abstract Promise call(Action action, Tree params, CallingOptions opts);

	// --- SEND REQUEST TO REMOTE SERVICE ---

	public abstract Promise send(String name, Tree params, CallingOptions opts);

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	public abstract void receive(Tree message);

	// --- ADD LOCAL SERVICE(S) ---

	public abstract void addService(Service... services) throws Exception;

	// --- REMOVE LOCAL SERVICE(S) ---

	public abstract void removeService(Service... services);

	// --- ADD REMOTE ACTION ---

	public abstract void addAction(Tree parameters) throws Exception;

	// --- GET LOCAL SERVICE ---

	public abstract Service getService(String name);

	// --- GET LOCAL OR REMOTE ACTION CONTAINER ---

	public abstract ActionContainer getAction(String nodeID, String name);

}