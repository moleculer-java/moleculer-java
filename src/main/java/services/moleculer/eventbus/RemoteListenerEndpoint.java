package services.moleculer.eventbus;

import static services.moleculer.transporter.Transporter.PACKET_EVENT;

import java.util.LinkedHashMap;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.transporter.Transporter;
import services.moleculer.util.CheckedTree;

public class RemoteListenerEndpoint extends ListenerEndpoint {

	// --- COMPONENTS ---

	protected Transporter transporter;

	// --- START ENDPOINT ---

	/**
	 * Initializes Container instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Set base properties
		super.start(broker, config);

		// Set components
		transporter = broker.components().transporter();
	}

	// --- INVOKE REMOTE LISTENER ---

	@Override
	public void on(String name, Tree payload, Groups groups, boolean emit) throws Exception {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("ver", ServiceBroker.MOLECULER_VERSION);
		map.put("sender", nodeID);
		map.put("event", name);
		if (emit) {
			map.put("groups", groups == null ? null : groups.groups());
		} else if (groups != null) {
			logger.warn("Moleculer V2 doesn't support grouped broadcast (message: " + payload.toString(false) + ")!");
		}
		if (payload != null) {
			map.put("data", payload.asObject());
		}
		transporter.publish(PACKET_EVENT, nodeID, new CheckedTree(map));
	}

	@Override
	public boolean local() {
		return false;
	}

}