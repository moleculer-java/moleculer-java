package services.moleculer.service;

import static services.moleculer.util.CommonUtils.nameOf;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.moleculer.ServiceBroker;

public abstract class Service {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- PROPERTIES ---
	
	protected final String name;
	protected ServiceBroker broker;

	// --- CONSTRUCTORS ---
	
	public Service() {
		this.name = nameOf(this, false);
	}

	public Service(String name) {
		this.name = Objects.requireNonNull(name);
	}

	// --- INSTANCE STARTED AND PUBLISHED ---

	public void started(ServiceBroker broker) throws Exception {
		this.broker = broker;
	}

	// --- INSTANCE STOPPED ---

	public void stopped() {
	}

	// --- PROPERTY GETTERS ---

	public ServiceBroker getBroker() {
		return broker;
	}

	public Logger getLogger() {
		return logger;
	}

	public String getName() {
		return name;
	}

}