package services.moleculer.utils;

import io.datatree.Tree;
import services.moleculer.CallingOptions;

abstract class ActionContainer {

	// --- PROPERTIES ---

	protected final boolean cached;

	// --- CONSTRUCTOR ---

	ActionContainer(boolean cached) {
		this.cached = cached;
	}

	// --- INVOKE ACTION ---
	
	abstract Object call(Tree params, CallingOptions opts) throws Exception;
	
	// --- LOCAL / REMOTE ---
	
	abstract boolean isLocal();
	
}