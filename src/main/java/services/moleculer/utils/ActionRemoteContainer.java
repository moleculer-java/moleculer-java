package services.moleculer.utils;

import io.datatree.Tree;
import services.moleculer.CallingOptions;
import services.moleculer.transporters.Transporter;

final class ActionRemoteContainer extends ActionContainer {

	// --- VARIABLES ---
	
	private final String nodeID;
	private final Transporter transporter;
	
	// --- CONSTRUCTOR ---

	ActionRemoteContainer(boolean cached, String nodeID, Transporter transporter) {
		super(cached);
		this.nodeID = nodeID;
		this.transporter = transporter;
	}
	
	// --- INVOKE ACTION ---
	
	@Override
	final Object call(Tree params, CallingOptions opts) throws Exception {
		return null;
	}

	// --- LOCAL / REMOTE ---
	
	@Override
	final boolean isLocal() {
		return false;
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
		ActionRemoteContainer other = (ActionRemoteContainer) obj;
		if (nodeID == null) {
			if (other.nodeID != null) {
				return false;
			}
		} else if (!nodeID.equals(other.nodeID)) {
			return false;
		}
		if (cached != other.cached) {
			return false;
		}
		return true;
	}
	
}