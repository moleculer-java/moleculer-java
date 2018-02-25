package services.moleculer.service;

import java.util.HashSet;

import io.datatree.Tree;
import services.moleculer.context.Context;

public abstract class ActionEndpoint extends Endpoint implements Action {

	// --- CONFIGURATION OF ACTION ---
	
	protected final Tree config;
	
	// --- ACTION WITH MIDDLEWARES ---
	
	protected Action current;
	
	// --- CONSTRUCTOR ---
	
	public ActionEndpoint(String nodeID, Tree config) {
		super(nodeID);
		this.config = config;
	}
	
	// --- INVOKE ACTION ---
	
	@Override
	public Object handler(Context ctx) throws Exception {
		return current.handler(ctx);
	}
	
	// --- APPLY MIDDLEWARE ---
	
	protected HashSet<Middleware> checkedMiddlewares = new HashSet<>(32);
	
	public boolean use(Middleware middleware) {
		if (checkedMiddlewares.add(middleware)) {
			Action action = middleware.install(current, config);
			if (action != null) {
				current = action;
				return true;
			}
		}
		return false;
	}
	
	// --- PROPERTY GETTERS ---
	
	public Tree getConfig() {
		return config;
	}

	public Action getAction() {
		return current;
	}	
	
}