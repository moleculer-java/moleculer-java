package services.moleculer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Service {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- INSTANCE CREATED ---
	
	public void created() throws Exception {
	}

	// --- INSTANCE STARTED AND PUBLISHED ---
	
	public void started() throws Exception {
	}

	// --- INSTANCE STOPPED ---
	
	public void stopped() {
	}
	
}