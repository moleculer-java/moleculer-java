package services.moleculer.service;

import java.util.HashSet;

import io.datatree.Tree;
import services.moleculer.context.Context;

public abstract class ActionEndpoint extends Endpoint implements Action {

	// --- CONFIGURATION OF ACTION ---

	protected final Tree config;

	protected final int hashCode;

	// --- ACTION WITH MIDDLEWARES ---

	protected Action current;

	// --- CONSTRUCTOR ---

	public ActionEndpoint(String nodeID, Tree config) {
		super(nodeID);
		this.config = config;
		this.hashCode = config.hashCode();
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

	// --- COLLECTION HELPERS ---

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ActionEndpoint other = (ActionEndpoint) obj;
		if (!nodeID.equals(other.nodeID)) {
			return false;
		}
		return config.equals(other.config);
	}

	// --- PROPERTY GETTERS ---

	public Tree getConfig() {
		return config;
	}

	public Action getAction() {
		return current;
	}
	
}