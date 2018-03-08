package services.moleculer.service;

import io.datatree.Tree;

public class LocalActionEndpoint extends ActionEndpoint {

	// --- CONSTRUCTOR ---
		
	public LocalActionEndpoint(String nodeID, Tree config, Action action) {
		super(nodeID, config);
		this.current = action;
	}
	
}