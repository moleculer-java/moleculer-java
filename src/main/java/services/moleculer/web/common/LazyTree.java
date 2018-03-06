package services.moleculer.web.common;

import java.util.Map;
import java.util.function.Consumer;

import io.datatree.Tree;

public class LazyTree extends Tree {

	// --- SERIAL VERSION ID ---

	private static final long serialVersionUID = -9176218153752352961L;

	// --- CONSTRUCTOR ---

	public LazyTree(Consumer<Map<String, Object>> converter) {
		super(new LazyMap<>(converter), null);
	}

}