package services.moleculer.strategies;

import services.moleculer.actions.Action;

public interface InvocationStrategy {

	// --- ADD ACCTION ---
	
	public void add(Action action);

	// --- REMOVE ACTION ---
	
	public void remove(Action action);

	// --- HAS ACTIONS ---
	
	public boolean isEmpty();

	// --- GET ACTION AT REMOTE NODE ---
	
	public Action get(String nodeID);
	
	// --- CALL LOCAL OR REMOTE INSTANCE ---
	
	public Action get(boolean preferLocal);
	
}