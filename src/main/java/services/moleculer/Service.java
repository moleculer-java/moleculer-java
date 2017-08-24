package services.moleculer;

public abstract class Service {
	
	protected ServiceBroker broker;
	
	protected Logger logger;
	
	public String name;
	public String version;
	
	public void init(ServiceBroker broker, String name, String version) throws Exception {
		created();
	}

	public void created() throws Exception {
	}

	public void started() throws Exception {
	}
	
	public void stopped() throws Exception {
	}
	
}

