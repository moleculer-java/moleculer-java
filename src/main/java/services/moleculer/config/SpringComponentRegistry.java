package services.moleculer.config;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.services.Name;
import services.moleculer.services.Service;

/**
 * Spring-based Component Registry. The Spring Framework provides a
 * comprehensive programming and configuration model for modern Java-based
 * enterprise applications - on any kind of deployment platform
 * (https://projects.spring.io/spring-framework/). Minimalistic Spring config:
 * <br>
 * <br>
 * &lt;beans ...&gt;
 * <ul>
 * &lt;context:component-scan base-package="your.service.package" /&gt;<br>
 * &lt;bean id="componentRegistry"
 * class="services.moleculer.config.SpringComponentRegistry" /&gt;<br>
 * &lt;bean id="brokerConfig"
 * class="services.moleculer.config.ServiceBrokerConfig"&gt;
 * <ul>
 * &lt;property name="nodeID" value="server-2" /&gt;<br>
 * &lt;property name="componentRegistry" ref="componentRegistry"/&gt;
 * </ul>
 * &lt;/bean&gt;<br>
 * &lt;bean id="serviceBroker" class="services.moleculer.ServiceBroker"
 * init-method="start" destroy-method="stop"&gt;
 * <ul>
 * &lt;constructor-arg ref="brokerConfig"/&gt;
 * </ul>
 * &lt;/bean&gt;<br>
 * </ul>
 * &lt;/beans&gt;
 * 
 * @see StandaloneComponentRegistry
 * @see GuiceComponentRegistry
*/
@Name("Spring Component Registry")
public final class SpringComponentRegistry extends BaseComponentRegistry implements ApplicationContextAware {

	// --- PARAMETERS ---

	/**
	 * Path of the optional service configuration file. Sample values:
	 * <ul>
	 * <li>"file:C:/directory/config.json"
	 * <li>"classpath:/directory/config.xml"
	 * <li>"WEB-INF/directory/config.yaml"
	 * </ul>
	 */
	private String configuration;

	// --- FIND COMPONENTS AND SERVICES ---

	/**
	 * Pointer to Spring Application Context
	 */
	private ApplicationContext ctx;

	@Override
	public final void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.ctx = ctx;
	}

	@Override
	protected final void findServices(ServiceBroker broker, Tree config) throws Exception {

		// Load configuration (OPTIONAL service configuration)
		if (configuration != null) {
			Resource res = ctx.getResource(configuration);
			if (res.isReadable()) {
				String format = ServiceBrokerConfig.getFormat(configuration);
				config = ServiceBrokerConfig.loadConfig(res.getInputStream(), format);
				logger.info("Configuration file \"" + configuration + "\" loaded.");
			}
		}

		// Find Moleculer Services in Spring Application Context
		Map<String, MoleculerComponent> componentMap = ctx.getBeansOfType(MoleculerComponent.class);
		for (Map.Entry<String, MoleculerComponent> entry : componentMap.entrySet()) {
			MoleculerComponent component = entry.getValue();
			if (isInternalComponent(component) || component instanceof Service) {
				continue;
			}
			String name = entry.getKey();
			components.put(name, new MoleculerComponentContainer(component, configOf(name, config)));
			logger.info("Spring Bean \"" + name + "\" registered as Moleculer Component.");
		}

		// Find Moleculer Components (eg. DAO classes) in Spring Application Context
		Map<String, Service> serviceMap = ctx.getBeansOfType(Service.class);
		for (Map.Entry<String, Service> entry : serviceMap.entrySet()) {
			Service service = entry.getValue();
			String name = service.name();
			broker.createService(service, configOf(name, config));
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