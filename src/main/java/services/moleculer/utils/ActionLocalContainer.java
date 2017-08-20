package services.moleculer.utils;

import io.datatree.Tree;
import services.moleculer.Action;
import services.moleculer.CallingOptions;

final class ActionLocalContainer extends ActionContainer {

	// --- VARIABLES ---

	private final Action action;

	// --- CONSTRUCTOR ---

	ActionLocalContainer(boolean cached, Action action) {
		super(cached);
		this.action = action;
	}

	// --- INVOKE ACTION ---

	@Override
	final Object call(Tree params, CallingOptions opts) throws Exception {
		return action.handler(null);
	}

	// --- LOCAL / REMOTE ---

	@Override
	final boolean isLocal() {
		return true;
	}

	// --- EQUALS ---

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
		ActionLocalContainer other = (ActionLocalContainer) obj;
		if (action == null) {
			if (other.action != null) {
				return false;
			}
		} else if (!action.equals(other.action)) {
			return false;
		}
		if (cached != other.cached) {
			return false;
		}
		return true;
	}

}