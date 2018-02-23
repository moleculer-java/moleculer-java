package services.moleculer.service;

import io.datatree.Tree;

public abstract class ActionEndpoint implements Action, Endpoint {

	// --- CONFIG ---
	
	protected Tree config;
	
	// --- PROPERTY GETTERS ---
	
	public Tree getConfig() {
		return config == null ? new Tree() : config.clone();
	}

}