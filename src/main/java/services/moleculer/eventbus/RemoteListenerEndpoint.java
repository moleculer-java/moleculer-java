package services.moleculer.eventbus;

import static services.moleculer.transporter.Transporter.PACKET_EVENT;

import io.datatree.Tree;

public class RemoteListenerEndpoint extends ListenerEndpoint {

	@Override
	public final void on(Tree payload) {
		transporter.publish(PACKET_EVENT, nodeID, payload);
	}
	
}
