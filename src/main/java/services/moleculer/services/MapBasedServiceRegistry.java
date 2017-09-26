package services.moleculer.services;

import java.util.HashMap;

import services.moleculer.ServiceBroker;
import services.moleculer.logger.Logger;
import services.moleculer.logger.NoOpLoggerFactory;

public final class MapBasedServiceRegistry extends ServiceRegistry {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public final String name() {
		return "Default Service Registry";
	}
	
	// --- INTERNAL COMPONENTS ---

	private final HashMap<String, Service> services = new HashMap<>(256);

	private ServiceBroker broker;
	
	private Logger logger = NoOpLoggerFactory.getInstance();

	// --- INIT SERVICE REGISTRY ---

	@Override
	public final void init(ServiceBroker broker) throws Exception {
		this.broker = broker;
		this.logger = broker.getLogger(MapBasedServiceRegistry.class);
	}

	// --- STOP SERVICE REGISTRY ---

	@Override
	public final void close() {
		for (Service service : services.values()) {
			String info = service.name();
			if (info == null || info.isEmpty()) {
				info = service.getClass().toString();
			}
			try {
				service.close();
				logger.info("Service \"" + info + "\" stopped successfully: " + service);
			} catch (Throwable cause) {
				logger.warn("Unable to stop \"" + info + "\" service!", cause);
			}
		}
		services.clear();
	}

	// --- ADD SERVICE ---
	
	@Override
	public final void add(Service service) throws Exception {
		
		// TODO synchronize service map
		// TODO remove service on error + log
		service.init(broker);
		services.put(service.name, service);
		service.started();
	}

	// --- GET SERVICE ---
	
	@Override
	public final Service get(String name) {
		return services.get(name);
	}

	// --- REMOVE SERVICE ---
	
	@Override
	public final boolean remove(String name) {
		Service service = services.remove(name);
		if (service == null) {
			return false;
		}
		
		// TODO try-catch + log
		service.close();
		return true;
	}

	// --- QUERY SERVICE ---
	
	@Override
	public final boolean contains(String name) {
		return services.containsKey(name);
	}
	
}