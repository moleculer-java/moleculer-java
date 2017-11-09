/**
 * This software is licensed under MIT license.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.config;

import static services.moleculer.util.CommonUtils.nameOf;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Names;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.monitor.Monitor;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.UIDGenerator;

/**
 * Guice-based Component Registry. Guice is a lightweight dependency injection
 * framework, brought to you by Google (https://github.com/google/guice). It
 * similar to Spring Framework, but smaller and more simple. You can use
 * {@code @Inject} in your code to access classes. This feature same as Spring's
 * {@code @Autowired} feature. Guice has no configuration file (like the
 * "application.xml" in Spring), {@code GuiceComponentRegistry} and
 * {@code StandaloneComponentRegistry} require similar configuration file.<br>
 * <br>
 * ServiceBroker broker = new ServiceBroker("config/moleculer.json");<br>
 * broker.start();<br>
 * <br>
 * ...and in the "moleculer.json":<br>
 * <br>
 * {<br>
 * "nodeID": "node-1",<br>
 * "componentRegistry": {<br>
 * "class": "services.moleculer.config.GuiceComponentRegistry",<br>
 * "packagesToScan": "your.service.package",<br>
 * "stage": "production"<br>
 * }<br>
 * }<br>
 * <br>
 * You access any Service or MoleculerComponent instance from yout code by using
 * the "Inject" annotation, for example:<br>
 * <br>
 * {@code @Inject}<br>
 * public UserDAO userDAO;
 * 
 * @see StandaloneComponentRegistry
 * @see SpringComponentRegistry
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
	 * The stage we're running in ("tool", "development" or "production")
	 */
	private Stage stage = Stage.PRODUCTION;

	// --- OPTIONAL CONFIGURATOR MODULE ---

	/**
	 * Optional Guice configurator
	 */
	private Module module;

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
	 * @param packagesToScan
	 *            package(s) where your Moleculer Services and Components are
	 *            located
	 */
	public GuiceComponentRegistry(String... packagesToScan) {

	}

	/**
	 * Creates a new CDI-based Component Registry.
	 * 
	 * @param module
	 *            Optional Guice configurator
	 * @param stage
	 *            DEVELOPMENT, PRODUCTION or TOOL stage
	 * @param packagesToScan
	 *            package(s) where your Moleculer Services and Components are
	 *            located
	 */
	public GuiceComponentRegistry(Module module, Stage stage, String... packagesToScan) {
		this.module = module;
		this.stage = stage;
		this.packagesToScan = packagesToScan;
	}

	// --- FIND COMPONENTS AND SERVICES ---

	@Override
	protected final void findServices(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		Tree packagesNode = config.get(PACKAGES_TO_SCAN);
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
		String s = config.get(STAGE, "").toUpperCase();
		if (!s.isEmpty()) {
			stage = Stage.valueOf(s);
		}
		String m = config.get(MODULE, "");
		if (!m.isEmpty()) {
			module = (Module) Class.forName(m).newInstance();
		}

		// Check required "packagesToScan" parameter
		if (packagesToScan == null || packagesToScan.length == 0) {
			logger.warn("The \"" + PACKAGES_TO_SCAN + "\" parameter is required for the Dependency Injector!");
			logger.warn("Please specify the proper Java package(s) where your Services are located.");
			return;
		}

		// Create Guice Dependency Injector
		Module mainModule = module == null ? new MoleculerModule(this, config) : module;
		Injector injector = Guice.createInjector(stage, mainModule);

		// Load Moleculer Services and Components (eg. DAO classes) with Guice
		// CDI framework
		for (String packageName : packagesToScan) {
			if (!packageName.isEmpty()) {
				LinkedList<String> classNames = scan(packageName);
				for (String className : classNames) {
					if (className.indexOf('$') > -1) {
						continue;
					}
					className = packageName + '.' + className;
					try {
						Class<?> type = Class.forName(className);
						if (isInternalComponent(type)) {
							continue;
						}
						if (Service.class.isAssignableFrom(type)) {
							Service service = (Service) injector.getInstance(type);
							String name = service.name();
							broker.createService(service, configOf(name, config));
							logger.info("Object \"" + name + "\" registered as Moleculer Service.");
							continue;
						}
						if (MoleculerComponent.class.isAssignableFrom(type)) {
							MoleculerComponent c = (MoleculerComponent) injector.getInstance(type);
							String name = nameOf(c, true);
							componentMap.put(name, new MoleculerComponentContainer(c, configOf(name, config)));
							logger.info("Object " + name + " registered as Moleculer Component.");
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
	public final class MoleculerModule extends AbstractModule {

		private final GuiceComponentRegistry registry;
		private final Tree config;

		private MoleculerModule(GuiceComponentRegistry registry, Tree config) {
			this.registry = registry;
			this.config = config.getRoot();
		}

		@Override
		protected void configure() {

			// You can access any value from the configuration file by using the
			// "Named" annotation, for example:
			//
			// @Inject
			// @Named("nodeID")
			// public String nodeID;
			//
			Properties properties = new Properties();
			try {

				// Convert config to Java Properties format
				byte[] bytes = config.toBinary("properties", true);
				if (logger.isDebugEnabled()) {
					logger.debug("Named parameters:\r\n" + new String(bytes, StandardCharsets.UTF_8));
				}
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

			// You can access any internal component by using the "Inject"
			// annotation, for example:
			//
			// @Inject
			// public Transporter transporter;
			//
			bind(ComponentRegistry.class).toInstance(registry);
			ContextFactory contextFactory = context();
			if (contextFactory != null) {
				bind(ContextFactory.class).toInstance(contextFactory);
			}
			UIDGenerator uidGenerator = uid();
			if (uidGenerator != null) {
				bind(UIDGenerator.class).toInstance(uidGenerator);
			}
			StrategyFactory strategyFactory = strategy();
			if (strategyFactory != null) {
				bind(StrategyFactory.class).toInstance(strategyFactory);
			}
			EventBus eventBus = eventbus();
			if (eventBus != null) {
				bind(EventBus.class).toInstance(eventBus);
			}
			Cacher cacher = cacher();
			if (cacher != null) {
				bind(Cacher.class).toInstance(cacher);
			}
			ServiceRegistry serviceRegistry = registry();
			if (serviceRegistry != null) {
				bind(ServiceRegistry.class).toInstance(serviceRegistry);
			}
			Transporter transporter = transporter();
			if (transporter != null) {
				bind(Transporter.class).toInstance(transporter);
			}
			Monitor monitor = monitor();
			if (monitor != null) {
				bind(Monitor.class).toInstance(monitor);
			}
		}

	}

}