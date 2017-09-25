package services.moleculer;

import services.moleculer.logger.Logger;

public abstract class Service {
	
	// --- PROPERTIES ---

	protected Logger logger;
	protected ServiceBroker broker;
	protected String name;
	
	// --- CONSTRUCTOR ---

	public Service() {
		Name n = getClass().getAnnotation(Name.class);
		String name = null;
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
		this.name = name;
	}
	
	public Service(String name) {
		this.name = name;
	}
	
	// --- INIT SERVICE ---
	
	public void init(ServiceBroker broker, String name) throws Exception {
		this.broker = broker;
		this.name = name;
		this.logger = broker.getLogger(name);
	}

	public void created() throws Exception {
	}

	public void started() throws Exception {
	}
	
	public void stopped() throws Exception {
	}
		
}

