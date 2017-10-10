package services.moleculer.services;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.context.CallingOptions;

public interface ActionContainer {

	// --- CALL ACTION ---

	public Promise call(Tree params, CallingOptions opts);

	// --- PROPERTY GETTERS ---

	public String name();

	public String nodeID();

	public boolean local();

	public boolean cached();

	public String[] cacheKeys();

}