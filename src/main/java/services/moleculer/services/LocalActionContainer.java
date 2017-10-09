package services.moleculer.services;

import java.util.concurrent.CompletableFuture;

import services.moleculer.context.Context;

public class LocalActionContainer implements ActionContainer {

	public LocalActionContainer(Action action) {
		
	}
	
	@Override
	public CompletableFuture<Object> handler(Context ctx) throws Exception {
		return null;
	}

	@Override
	public String name() {
		return null;
	}

	@Override
	public String nodeID() {
		return null;
	}

	@Override
	public boolean local() {
		return false;
	}

}
