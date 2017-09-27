package services.moleculer.services;

import services.moleculer.ServiceBroker;
import services.moleculer.logger.Logger;
import services.moleculer.logger.NoOpLoggerFactory;
import services.moleculer.utils.MoleculerComponent;

public abstract class Service implements MoleculerComponent {

	// --- NAME OF THE MOLECULER COMPONENT ---

	@Override
	public final String name() {
		return name;
	}

	// --- PROPERTIES ---

	protected final String name;

	protected ServiceBroker broker;

	protected Logger logger = NoOpLoggerFactory.getInstance();

	// --- CONSTRUCTOR ---

	public Service() {
		this(null);
	}

	public Service(String name) {
		if (name == null || name.isEmpty()) {
			Name n = getClass().getAnnotation(Name.class);
			if (n != null) {
				name = n.value();
			}
			if (name != null) {
				name = name.trim();
			}
			if (name == null || name.isEmpty()) {
				name = getClass().getName();
				int i = Math.max(name.lastIndexOf('.'), name.lastIndexOf('$'));
				if (i > -1) {
					name = name.substring(i + 1);
				}
				name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
			}
		}
		this.name = name;
	}

	// --- START SERVICE ---

	/**
	 * Initializes service instance.
	 * 
	 * @param broker
	 */
	@Override
	public final void init(ServiceBroker broker) throws Exception {
		this.broker = broker;
		this.logger = broker.getLogger(name);
		created();
	}

	public void created() throws Exception {
	}

	// --- SERVICE INITED ---

	public void started() throws Exception {
	}

	// --- STOP SERVICE ---

	/**
	 * Closes logger.
	 */
	@Override
	public void close() {
	}

}