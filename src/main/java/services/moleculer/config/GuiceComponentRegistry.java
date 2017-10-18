package services.moleculer.config;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Names;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.services.Name;
import services.moleculer.services.Service;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.utils.CommonUtils;

/**
 * Guice-based Component Registry. Guice is a lightweight dependency injection
 * framework, brought to you by Google (https://github.com/google/guice). It
 * similar to Spring Framework, but smaller and more simple. You can use
 * {@code @Inject} in your code to access classes. This feature same as Spring's
 * {@code @Autowired} feature. Guice has no configuration file (like the
 * "application.xml" in Spring), {@code GuiceComponentRegistry} and
 * {@code StandaloneComponentRegistry} require similar configuration file.
 */
@Name("Guice Component Registry")
public final class GuiceComponentRegistry extends BaseComponentRegistry {

	// --- PACKAGES TO SCAN (REQUIRED) ---

	/**
	 * Java package(s) where your Moleculer Services and Components are located.
	 * CDI framework won't work without this parameter!
	 */
	private String[] packagesToScan;

	// --- DEVELOPMENT / PRODUCTION / TOOL STAGE ---

	/**
	 * The stage we're running in (TOOL, DEVELOPMENT or PRODUCTION)
	 */
	private Stage stage = Stage.PRODUCTION;

	// --- CONSTRUCTORS ---

	/**
	 * Creates a Dependency Injector without "packagesToScan" parameter. This
	 * must be specified in the configuration file.
	 */
	public GuiceComponentRegistry() {
	}

	/**
	 * Creates a new CDI-based Component Registry.
	 * 
	 * @param stage
	 *            DEVELOPMENT, PRODUCTION or TOOL stage
	 * @param packagesToScan
	 *            package(s) where your Moleculer Services and Components are
	 *            located
	 */
	public GuiceComponentRegistry(Stage stage, String... packagesToScan) {
		this.stage = stage;
		this.packagesToScan = packagesToScan;
	}

	// --- FIND COMPONENTS AND SERVICES ---

	@Override
	protected final void findServices(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		Tree packagesNode = config.get("packagesToScan");
		if (packagesNode != null) {
			if (packagesNode.isPrimitive()) {

				// List of packages
				String value = packagesNode.asString().trim();
				packagesToScan = value.split(",");
			} else {

				// Array structure of packages
				List<String> packageList = packagesNode.asList(String.class);
				if (!packageList.isEmpty()) {
					packagesToScan = new String[packageList.size()];
					packageList.toArray(packagesToScan);
				}
			}
		}
		String s = config.get("stage", "").toUpperCase();
		if (!s.isEmpty()) {
			stage = Stage.valueOf(s);
		}

		// Check required "packagesToScan" parameter
		if (packagesToScan == null || packagesToScan.length == 0) {
			logger.warn("The \"packagesToScan\" parameter is required for the Dependency Injector!");
			logger.warn("Please specify the proper Java package(s) where your Services are located.");
			return;
		}

		// Create Guice Dependency Injector
		MoleculerModule module = new MoleculerModule(config);
		Injector injector = Guice.createInjector(stage, module);

		// Load Moleculer Services and Components (eg. DAO classes) with Guice CDI framework
		ServiceRegistry serviceRegistry = broker.components().serviceRegistry();
		for (String packageName : packagesToScan) {
			if (!packageName.isEmpty()) {
				LinkedList<String> classNames = scan(packageName);
				for (String className : classNames) {
					className = packageName + '.' + className;
					try {
						Class<?> type = Class.forName(className);
						if (isInternalComponent(type)) {
							continue;
						}
						if (Service.class.isAssignableFrom(type)) {
							Service service = injector.getInstance(type);
							String name = service.name();
							serviceRegistry.addService(service, configOf(name, config));
							logger.info("Class \"" + name + "\" registered as Moleculer Service.");
							continue;
						}
						if (MoleculerComponent.class.isAssignableFrom(type)) {
							MoleculerComponent c = injector.getInstance(type);
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

	/**
	 * Utility to convert Moleculer configuration to Guice configuration. This
	 * makes constant binding to {@code @Named(key)} for each property from
	 * Moleculer configuration file.
	 */
	public static final class MoleculerModule extends AbstractModule {

		private final Tree config;

		private MoleculerModule(Tree config) {
			this.config = config;
		}

		@Override
		protected void configure() {
			Properties properties = new Properties();
			try {

				// Convert config to Java Properties format
				byte[] bytes = config.toBinary("properties", true);
				ByteArrayInputStream in = new ByteArrayInputStream(bytes);
				properties.load(in);
			} catch (Exception unsupportedFormat) {

				// Copy the basic entries
				for (Tree child : config) {
					if (child.isPrimitive()) {
						properties.setProperty(child.getName(), child.asString());
					}
				}
			}
			Names.bindProperties(binder(), properties);
		}

	}

}