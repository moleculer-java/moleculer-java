package services.moleculer.config;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
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
	 * Optional directory of service confgiurations. Sample values:
	 * <ul>
	 * <li>"file:C:/directory"
	 * <li>"classpath:"
	 * <li>"classpath:/directory"
	 * <li>"WEB-INF/directory"
	 * </ul>
	 */
	private String configDirectory;

	/**
	 * Format of service confgiuration (eg. "json", "yaml", "toml", "xml",
	 * etc.). Default format is "json".
	 */
	private String configFormat = "json";

	// --- START REGISTRY AND COMPONENTS ---

	private ApplicationContext ctx;

	@Override
	public final void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.ctx = ctx;
	}

	protected final void findServices(ServiceBroker broker) {
		
		// Find Moleculer Services in Spring Context then register them in the
		// ServiceBroker
		ServiceRegistry serviceRegistry = broker.components().serviceRegistry();
		Map<String, Service> services = ctx.getBeansOfType(Service.class);
		for (Map.Entry<String, Service> entries : services.entrySet()) {
			String name = entries.getKey();
			try {
				Service service = entries.getValue();
				name = service.name();
				Tree config = null;
				if (configDirectory != null) {
					String format = configFormat == null ? "json" : configFormat.toLowerCase();
					String configPath = configDirectory + '/' + name + '.' + format;
					Resource res = ctx.getResource(configPath);
					if (res.isReadable()) {
						InputStream is = null;
						try {
							is = res.getInputStream();
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							byte[] buffer = new byte[1024];
							while (true) {
								int r = is.read(buffer);
								if (r == -1)
									break;
								out.write(buffer, 0, r);
							}
							byte[] bytes = out.toByteArray();
							config = new Tree(bytes, format);
							logger.debug(
									"Configuration file \"" + configPath + "\" loaded for Service \"" + name + "\".");
						} catch (Exception ioError) {
							throw new BeanInitializationException(
									"Unable to load configuration file from \"" + configPath + "\"!", ioError);
						} finally {
							if (is != null) {
								is.close();
							}
						}
					} else {
						logger.debug("Configuration file \"" + configPath + "\" not found.");
					}
				}
				serviceRegistry.addService(service, config == null ? new Tree() : config);
			} catch (Exception cause) {
				throw new BeanInitializationException("Unable to register \"" + name + "\" Moleculer Service!", cause);
			}
			logger.debug("Spring Bean \"" + name + "\" registered as Moleculer Service.");
		}		
	}
	
	// --- GETTERS AND SETTERS ---

	public final String getConfigDirectory() {
		return configDirectory;
	}

	public final void setConfigDirectory(String configDirectory) {
		this.configDirectory = configDirectory;
	}

	public final String getConfigFormat() {
		return configFormat;
	}

	public final void setConfigFormat(String configFormat) {
		this.configFormat = configFormat;
	}

}