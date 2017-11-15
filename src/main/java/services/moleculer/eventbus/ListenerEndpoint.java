package services.moleculer.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.strategy.Endpoint;

public abstract class ListenerEndpoint implements MoleculerComponent, Endpoint {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	// --- PROPERTIES ---

	protected String nodeID;
	protected String subscribe;
	protected String group;
	
	// --- CONSTRUCTOR ---

	ListenerEndpoint() {
	}
	
	// --- START ENDPOINT ---

	/**
	 * Initializes enpoint instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current endpoint
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Set name
		subscribe = config.get("subscribe", (String) null);

		// Set nodeID
		nodeID = config.get(NODE_ID, (String) null);

		// Set group
		group = config.get("group", (String) null);
	}
	
	// --- STOP ENDPOINT ---

	@Override
	public void stop() {
	}
	
	// --- SEND EVENT TO ENDPOINT ---

	public abstract void on(Tree payload) throws Exception;
	
	// --- PROPERTY GETTERS ---

	public abstract boolean local();

	public final String subscribe() {
		return subscribe;
	}

	public final String group() {
		return group;
	}
	
	public final String nodeID() {
		return nodeID;
	}
	
	// --- EQUALS / HASHCODE ---
	
	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((subscribe == null) ? 0 : subscribe.hashCode());
		result = prime * result + ((nodeID == null) ? 0 : nodeID.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ListenerEndpoint other = (ListenerEndpoint) obj;
		if (group == null) {
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (subscribe == null) {
			if (other.subscribe != null)
				return false;
		} else if (!subscribe.equals(other.subscribe))
			return false;
		if (nodeID == null) {
			if (other.nodeID != null)
				return false;
		} else if (!nodeID.equals(other.nodeID))
			return false;
		return true;
	}
	
}