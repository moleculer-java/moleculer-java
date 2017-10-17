package services.moleculer.config;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.Service;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.InvocationStrategy;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.transporters.Transporter;
import services.moleculer.uids.UIDGenerator;

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
		Map<String, MoleculerComponent> componentMap = ctx.getBeansOfType(MoleculerComponent.class);
		for (Map.Entry<String, MoleculerComponent> entry : componentMap.entrySet()) {
			MoleculerComponent component = entry.getValue();
			if (component instanceof Service || component instanceof ContextFactory || component instanceof UIDGenerator
					|| component instanceof EventBus || component instanceof Cacher
					|| component instanceof InvocationStrategyFactory || component instanceof InvocationStrategy
					|| component instanceof ServiceRegistry || component instanceof Transporter) {
				continue;
			}
			String name = entry.getKey();
			components.put(name, new MoleculerComponentContainer(component, configOf(name, config)));
			logger.info("Spring Bean \"" + name + "\" registered as Moleculer Component.");
		}

		// Find Moleculer Services in Spring Context then register them in the
		// ServiceBroker
		ServiceRegistry serviceRegistry = broker.components().serviceRegistry();
		Map<String, Service> serviceMap = ctx.getBeansOfType(Service.class);
		for (Map.Entry<String, Service> entry : serviceMap.entrySet()) {
			Service service = entry.getValue();
			String name = service.name();
			serviceRegistry.addService(service, configOf(name, config));
			logger.info("Spring Bean \"" + name + "\" registered as Moleculer Service.");
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