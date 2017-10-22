package services.moleculer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;

public abstract class AbstractContainer implements ActionContainer, MoleculerComponent {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- PROPERTIES ---

	protected String nodeID;
	protected String name;
	protected boolean cached;
	protected String[] cacheKeys;

	// --- COMPONENTS ---

	protected ServiceBroker broker;

	// --- CONSTRUCTOR ---
	
	AbstractContainer() {
	}
	
	// --- INIT CONTAINER ---

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

		// Set name
		name = config.get("name", (String) null);

		// Set nodeID
		nodeID = config.get("nodeID", (String) null);

		// Set cache parameters
		cached = config.get("cached", false);
		cacheKeys = config.get("cacheKeys", "").split(",");

		// Set components
		this.broker = broker;
	}

	// --- STOP CONTAINER ---

	@Override
	public void stop() {
	}

	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Calls an action (local or remote)
	 */
	@Override
	public final Promise call(Object... params) {
		Tree tree = null;
		Context parent = null;
		CallingOptions opts = null;
		if (params != null) {
			if (params.length == 1) {
				if (params[0] instanceof Tree) {
					tree = (Tree) params[0];
				} else {
					tree = new Tree().setObject(params[0]);
				}
			} else {
				tree = new Tree();
				String prev = null;
				Object value;
				for (int i = 0; i < params.length; i++) {
					value = params[i];
					if (prev == null) {
						if (!(value instanceof String)) {
							if (value instanceof CallingOptions) {
								opts = (CallingOptions) value;
								continue;
							}
							if (value instanceof Context) {
								parent = (Context) value;
								continue;
							}
							i++;
							throw new IllegalArgumentException("Parameter #" + i + " (\"" + value
									+ "\") must be String, Context, or CallingOptions!");
						}
						prev = (String) value;
						continue;
					}
					tree.putObject(prev, value);
				}
			}
		}
		return call(tree, opts, parent);
	}

	public abstract Promise call(Tree params, CallingOptions opts, Context parent);

	// --- PROPERTY GETTERS ---

	@Override
	public final String name() {
		return name;
	}

	@Override
	public final String nodeID() {
		return nodeID;
	}

	@Override
	public final boolean cached() {
		return cached;
	}

	@Override
	public final String[] cacheKeys() {
		return cacheKeys;
	}

}