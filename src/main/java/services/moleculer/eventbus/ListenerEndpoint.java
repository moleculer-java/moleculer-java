package services.moleculer.eventbus;

import java.util.Objects;

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
	protected String service;
	protected String group;
	protected String subscribe;

	private int hashCode = 1;

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

		// Set nodeID
		nodeID = config.get("nodeID", (String) null);

		// Set service name
		service = config.get("service", (String) null);

		// Set group
		group = config.get("group", service);

		// Set name
		subscribe = config.get("subscribe", (String) null);

		// Generate hash code
		final int prime = 31;
		hashCode = prime * hashCode + ((group == null) ? 0 : group.hashCode());
		hashCode = prime * hashCode + ((nodeID == null) ? 0 : nodeID.hashCode());
		hashCode = prime * hashCode + ((service == null) ? 0 : service.hashCode());
		hashCode = prime * hashCode + ((subscribe == null) ? 0 : subscribe.hashCode());

		// Verify values
		Objects.requireNonNull(nodeID);
		Objects.requireNonNull(service);
		Objects.requireNonNull(group);
		Objects.requireNonNull(subscribe);
	}

	// --- STOP ENDPOINT ---

	@Override
	public void stop() {
	}

	// --- SEND EVENT TO ENDPOINT ---

	public abstract void on(String name, Tree payload, Groups groups) throws Exception;

	// --- PROPERTY GETTERS ---

	public abstract boolean local();

	public final String nodeID() {
		return nodeID;
	}

	public final String service() {
		return service;
	}

	public final String group() {
		return group;
	}

	public final String subscribe() {
		return subscribe;
	}

	// --- EQUALS / HASHCODE ---

	@Override
	public final int hashCode() {
		return hashCode;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ListenerEndpoint other = (ListenerEndpoint) obj;
		if (other.hashCode != hashCode) {
			return false;
		}
		if (group == null) {
			if (other.group != null) {
				return false;
			}
		} else if (!group.equals(other.group)) {
			return false;
		}
		if (nodeID == null) {
			if (other.nodeID != null) {
				return false;
			}
		} else if (!nodeID.equals(other.nodeID)) {
			return false;
		}
		if (service == null) {
			if (other.service != null) {
				return false;
			}
		} else if (!service.equals(other.service)) {
			return false;
		}
		if (subscribe == null) {
			if (other.subscribe != null) {
				return false;
			}
		} else if (!subscribe.equals(other.subscribe)) {
			return false;
		}
		return true;
	}

}