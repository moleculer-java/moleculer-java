package services.moleculer.config;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.services.Service;
import services.moleculer.services.ServiceRegistry;

public final class SpringComponentRegistry extends StandaloneComponentRegistry implements ApplicationContextAware {

	// --- PARAMETERS ---

	/**
	 * Path of the optional service configuration file. Sample values:
	 * <ul>
	 * <li>"file:C:/directory/config.json"
	 * <li>"classpath:/directory/config.json"
	 * <li>"WEB-INF/directory/config.json"
	 * </ul>
	 */
	private String configuration;

	// --- START REGISTRY AND COMPONENTS ---

	private ApplicationContext ctx;

	@Override
	public final void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.ctx = ctx;
	}

	protected final void findServices(ServiceBroker broker) throws Exception {

		// Load configuration
		Tree config = new Tree();
		if (configuration != null) {
			Resource res = ctx.getResource(configuration);
			if (res.isReadable()) {
				String format = ServiceBrokerConfig.getFormat(configuration);
				config = ServiceBrokerConfig.loadConfig(res.getInputStream(), format);
				logger.info("Configuration file \"" + configuration + "\" loaded.");
			}
		}

		// Find components
		Map<String, MoleculerComponent> components = ctx.getBeansOfType(MoleculerComponent.class);
		for (Map.Entry<String, MoleculerComponent> entry : components.entrySet()) {
			MoleculerComponent component = entry.getValue();
			if (component instanceof Service) {
				continue;
			}
			String name = entry.getKey();
			if (CONTEXT_FACTORY_ID.equals(name) || UID_GENERATOR_ID.equals(name) || EVENT_BUS_ID.equals(name)
					|| CACHER_ID.equals(name) || INVOCATION_STRATEGY_FACTORY_ID.equals(name)
					|| SERVICE_REGISTRY_ID.equals(name) || TRANSPORTER_ID.equals(name)) {
				continue;
			}
			components.put(name, component);
			logger.debug("Spring Bean \"" + name + "\" registered as Moleculer Component.");
		}

		// Find Moleculer Services in Spring Context then register them in the
		// ServiceBroker
		ServiceRegistry serviceRegistry = broker.components().serviceRegistry();
		Map<String, Service> services = ctx.getBeansOfType(Service.class);
		for (Map.Entry<String, Service> entry : services.entrySet()) {
			Service service = entry.getValue();
			String name = service.name();
			serviceRegistry.addService(service, configOf(name, config));
			logger.debug("Spring Bean \"" + name + "\" registered as Moleculer Service.");
		}
	}

	// --- GETTERS AND SETTERS ---

	public final String getConfiguration() {
		return configuration;
	}

	public final void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

}