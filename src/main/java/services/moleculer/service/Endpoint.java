package services.moleculer.service;

public abstract class Endpoint {

	// --- PROPERTIES ---
	
	protected final String nodeID;
	
	// --- CONSTRUCTOR ---
	
	public Endpoint(String nodeID) {
		this.nodeID = nodeID;
	}
	
	// --- PROPERTY GETTERS ---
	
	public String getNodeID() {
		return nodeID;
	}
	
}
