package services.moleculer.config;

import io.datatree.Tree;

public final class MoleculerComponentConfig {
	
	// --- PROPERTIES ---
	
	private final MoleculerComponent component;
	private final Tree config;

	// --- CONSTRUCTOR ---
	
	public MoleculerComponentConfig(MoleculerComponent component, Tree config) {
		this.component = component;
		this.config = config;
	}
	
	// --- GETTERS ---

	public final MoleculerComponent component() {
		return component;
	}
	
	public final Tree config() {
		return config;
	}
	
}
