package services.moleculer.service;

import io.datatree.Tree;

public abstract class Middleware extends Service {

	// --- CONSTRUCTORS ---
	
	public Middleware() {
	}

	public Middleware(String name) {
		super(name);
	}
	
	// --- ADD MIDDLEWARE TO ACTION ---
	
	public abstract Action install(Action action, Tree config);

}