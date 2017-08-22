package services.moleculer;

public abstract class Service {
	
	private ServiceBroker broker;
	
	public String name;
	
	public String version;
	
	protected Logger logger;

	public Service(ServiceBroker broker, String name, String version) throws Exception {
		this.broker = broker;
		this.name = name;
		this.version = version;
		
		this.logger = this.broker.getLogger("service", this.name, this.version);
		
		//if (broker == null)
		//	throw new ServiceSchemaError("Must set a ServiceBroker instance!");
		
		//if (!this.name)
		//	throw new ServiceSchemaError("Service name can't be empty!");
		
		// Call `created` handler
		this.created();
	}

	public void created() throws Exception {
	}

	public void started() throws Exception {
	}
	
	public void stopped() throws Exception {
	}
	
}

