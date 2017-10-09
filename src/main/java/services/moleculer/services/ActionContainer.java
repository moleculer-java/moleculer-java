package services.moleculer.services;

import java.util.concurrent.CompletableFuture;

import services.moleculer.context.Context;

public interface ActionContainer {

	CompletableFuture<Object> handler(Context ctx) throws Exception;
	
	// --- GETTERS ---
	
	public String name();
	
	public String nodeID();
	
	public boolean local();
	
}