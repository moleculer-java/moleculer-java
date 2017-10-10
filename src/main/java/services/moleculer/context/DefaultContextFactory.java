package services.moleculer.context;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.services.Name;
import services.moleculer.uids.UIDGenerator;

@Name("Default Context Factory")
public final class DefaultContextFactory extends ContextFactory {

	// --- VARIABLES ---

	private String nodeID;

	// --- INTERNAL COMPONENTS ---

	private ServiceBroker broker;
	private UIDGenerator uidGenerator;

	// --- START CONTEXT FACTORY ---

	/**
	 * Initializes Default Context Factory instance.
	 * 
	 * @param broker
	 */
	@Override
	public void init(ServiceBroker broker) throws Exception {
		this.nodeID = broker.nodeID();
		this.broker = broker;
		this.uidGenerator = broker.components().uidGenerator();
	}

	// --- CREATE CONTEXT ---

	public final Context create(Tree params, CallingOptions opts) {

		// Target node
		String targetID = opts == null ? null : opts.nodeID();

		// Generate context ID (for remote services only)
		String id;
		if (targetID != null && !targetID.equals(nodeID)) {
			id = uidGenerator.nextUID();
		} else {
			id = null;
		}

		// Create or join meta
		Tree meta = null;
		if (opts != null) {
			Tree optsMeta = opts.meta();
			Context parentContext = opts.parentContext();
			Tree parentMeta = null;
			if (parentContext != null) {
				parentMeta = parentContext.meta();
			}
			if (optsMeta == null) {
				if (parentMeta != null) {
					meta = parentMeta;
				}
			} else {
				if (parentMeta != null) {
					optsMeta.copyFrom(parentMeta);
				}
			}
		}

		// Create context
		return new Context(broker, id, params, meta);
	}

}