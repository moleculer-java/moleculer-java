package services.moleculer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.context.CallingOptions;

@Name("Service Registry")
public abstract class ServiceRegistry implements MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- INIT SERVICE REGISTRY ---

	/**
	 * Initializes ServiceRegistry instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public void stop() {
	}

	// --- CALL LOCAL SERVICE ---

	public abstract Promise call(Action action, Tree params, CallingOptions opts);

	// --- SEND REQUEST TO REMOTE SERVICE ---

	public abstract Promise send(String name, Tree params, CallingOptions opts);

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	public abstract void receive(Tree message);

	// --- ADD LOCAL SERVICE ---

	public abstract void addService(Service service, Tree config) throws Exception;

	// --- REMOVE LOCAL SERVICE ---

	public abstract void removeService(Service service);

	// --- ADD REMOTE ACTION ---

	public abstract void addAction(Tree parameters) throws Exception;

	// --- GET LOCAL SERVICE ---

	public abstract Service getService(String name);

	// --- GET LOCAL OR REMOTE ACTION CONTAINER ---

	public abstract ActionContainer getAction(String nodeID, String name);

}