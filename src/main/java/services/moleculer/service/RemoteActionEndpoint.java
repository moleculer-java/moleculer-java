package services.moleculer.service;

import io.datatree.Tree;
import services.moleculer.context.Context;

public class RemoteActionEndpoint extends ActionEndpoint {

	// --- CONSTRUCTOR ---
	
	public RemoteActionEndpoint(Tree config) {
		this.config = config;
	}
	
	// --- INVOKE ACTION ---

	@Override
	public Object handler(Context ctx) throws Exception {
		return null;
	}
	
}