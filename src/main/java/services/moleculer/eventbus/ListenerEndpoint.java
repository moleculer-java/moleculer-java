package services.moleculer.eventbus;

import io.datatree.Tree;

public abstract class ListenerEndpoint {

	// --- SEND EVENT TO ENDPOINT ---

	public abstract void on(Tree payload);
	
	// --- PROPERTY GETTERS ---

	public abstract boolean local();

	public String name() {
		return null;
	}

	public String group() {
		return null;
	}
	
	public String nodeID() {
		return null;
	}
	
}
