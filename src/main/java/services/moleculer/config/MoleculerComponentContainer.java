package services.moleculer.config;

import io.datatree.Tree;

final class MoleculerComponentContainer {
	
	// --- PROPERTIES ---
	
	final MoleculerComponent component;
	final Tree config;

	// --- CONSTRUCTOR ---
	
	MoleculerComponentContainer(MoleculerComponent component, Tree config) {
		this.component = component;
		this.config = config;
	}
	
}
