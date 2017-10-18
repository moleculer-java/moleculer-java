package services.moleculer.config;

import java.util.LinkedList;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.services.Name;
import services.moleculer.services.Service;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.utils.CommonUtils;

/**
 * Standalone Component Registry. It's the simplest way to start a Moleculer
 * Service Broker without any CDI framework (eg. Spring or Guice). Sample code,
 * to create a new Service Broker:<br>
 * <br>
 * ServiceBroker broker = ServiceBroker.builder()<br>
 * &nbsp;&nbsp;&nbsp;.componentRegistry(new
 * StandaloneComponentRegistry("my.service.package"))<br>
 * &nbsp;&nbsp;&nbsp;.build();<br>
 * broker.start();
 */
@Name("Standalone Component Registry")
public final class StandaloneComponentRegistry extends BaseComponentRegistry {

	// --- PACKAGES TO SCAN ---

	/**
	 * Java package(s) where your Moleculer Services and Components are located.
	 * This is an optional parameter, you can add Services and Components
	 * directly to the {@code ServiceBroker}.
	 */
	private String[] packagesToScan;

	// --- CONSTRUCTORS ---

	/**
	 * Creates a Component Registry without "packagesToScan" parameter. You can
	 * add Services and Components later directly to the {@code ServiceBroker}.
	 */
	public StandaloneComponentRegistry() {
	}

	/**
	 * Creates a new Component Registry.
	 * 
	 * @param packagesToScan
	 *            package(s) where your Moleculer Services and Components are
	 *            located
	 */
	public StandaloneComponentRegistry(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	// --- FIND COMPONENTS AND SERVICES ---

	@Override
	protected final void findServices(ServiceBroker broker, Tree config) throws Exception {
		if (packagesToScan == null || packagesToScan.length == 0) {
			return;
		}
		ServiceRegistry serviceRegistry = broker.components().serviceRegistry();
		for (String packageName : packagesToScan) {
			if (!packageName.isEmpty()) {
				LinkedList<String> classNames = scan(packageName);
				for (String className : classNames) {
					className = packageName + '.' + className;
					try {
						Object component = Class.forName(className).newInstance();
						if (isInternalComponent(component)) {
							continue;
						}

						// Find Moleculer Services
						if (component instanceof Service) {
							Service service = (Service) component;
							String name = service.name();
							serviceRegistry.addService(service, configOf(name, config));
							logger.info("Class \"" + name + "\" registered as Moleculer Service.");
							continue;
						}

						// Find Moleculer Components (eg. DAO classes)
						if (component instanceof MoleculerComponent) {
							MoleculerComponent c = (MoleculerComponent) component;
							String name = CommonUtils.nameOf(c);
							components.put(name, new MoleculerComponentContainer(c, configOf(name, config)));
							logger.info("Class \"" + name + "\" registered as Moleculer Component.");
						}
					} catch (Throwable cause) {
						logger.warn("Unable to load class \"" + className + "\"!", cause);
					}
				}
			}
		}
	}

}