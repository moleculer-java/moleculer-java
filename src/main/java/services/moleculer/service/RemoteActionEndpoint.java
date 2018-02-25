package services.moleculer.service;

import io.datatree.Tree;
import services.moleculer.context.Context;

public class RemoteActionEndpoint extends ActionEndpoint {

	// --- CONSTRUCTOR ---
	
	public RemoteActionEndpoint(String nodeID, Tree config) {
		super(nodeID, config);
		this.current = new RemoteAction(this);
	}
	
	// --- REMOTE ACTION ---
	
	protected static class RemoteAction implements Action {

		protected final RemoteActionEndpoint endpoint;
		
		protected RemoteAction(RemoteActionEndpoint endpoint) {
			this.endpoint = endpoint;
		}
		
		@Override
		public Object handler(Context ctx) throws Exception {

			// Return promise
			return null;
		}
		
	}
	
}