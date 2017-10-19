package services.moleculer.config;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.InvocationStrategy;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.transporters.Transporter;
import services.moleculer.uids.UIDGenerator;
import services.moleculer.utils.CommonUtils;

/**
 * Abstract class for Standalone, Spring, and Guice Component Registries.
 *
 * @see StandaloneComponentRegistry
 * @see SpringComponentRegistry
 * @see GuiceComponentRegistry
 */
public abstract class BaseComponentRegistry extends ComponentRegistry {

	// --- THREAD POOLS ---

	private ExecutorService executor;
	private ScheduledExecutorService scheduler;

	private boolean shutdownThreadPools;

	// --- BASE COMPONENTS ---

	private ContextFactory contextFactory;
	private UIDGenerator uidGenerator;
	private InvocationStrategyFactory invocationStrategyFactory;
	private EventBus eventBus;
	private Cacher cacher;
	private ServiceRegistry serviceRegistry;
	private Transporter transporter;

	// --- CUSTOM COMPONENTS ---

	protected Map<String, MoleculerComponentContainer> components;

	// --- START REGISTRY AND COMPONENTS ---

	/**
	 * Initializes registry instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param brokerConfig
	 *            configuration of the current component (created by the
	 *            ServiceBrokerBuilder)
	 * @param customConfig
	 *            optional configuration (loaded from file)
	 */
	@Override
	public final void start(ServiceBroker broker, ServiceBrokerConfig brokerConfig, Tree customConfig)
			throws Exception {

		// Set thread pools
		executor = brokerConfig.getExecutor();
		scheduler = brokerConfig.getScheduler();

		// Should terminate thread pools on stop()?
		shutdownThreadPools = brokerConfig.getShutDownThreadPools();

		// Set internal components
		contextFactory = brokerConfig.getContextFactory();
		uidGenerator = brokerConfig.getUidGenerator();
		invocationStrategyFactory = brokerConfig.getInvocationStrategyFactory();
		serviceRegistry = brokerConfig.getServiceRegistry();
		cacher = brokerConfig.getCacher();
		eventBus = brokerConfig.getEventBus();
		transporter = brokerConfig.getTransporter();
		components = brokerConfig.getComponents();

		// Create components by config file
		for (Tree componentConfig : customConfig) {
			if (!componentConfig.isMap()) {
				continue;
			}

			// Get id property
			String id = componentConfig.get("id", "");
			if (id.isEmpty()) {

				// Get as XML attribute
				id = componentConfig.get("@id", "");
			}
			if (id.isEmpty()) {

				// Use node name as id
				id = componentConfig.getName();
			}
			id = id.trim();

			// Get class name
			String className = componentConfig.get("class", "");
			if (className.isEmpty()) {

				// Get class name as XML attribute
				className = componentConfig.get("@class", "");
			}
			if (className.isEmpty()) {
				continue;
			}

			// Create instance
			Class<?> implClass = Class.forName(className);
			if (!MoleculerComponent.class.isAssignableFrom(implClass)) {
				if (ComponentRegistry.class.isAssignableFrom(implClass)) {
					continue;
				}
				throw new IllegalArgumentException(
						"Class \"" + className + "\" must implement the MoleculerComponent interface!");
			}
			MoleculerComponent component = (MoleculerComponent) implClass.newInstance();

			// Maybe it's an internal compoment
			if (CONTEXT_FACTORY_ID.equals(id) && checkType(ContextFactory.class, implClass)) {
				contextFactory = (ContextFactory) component;
				continue;
			}
			if (UID_GENERATOR_ID.equals(id) && checkType(UIDGenerator.class, implClass)) {
				uidGenerator = (UIDGenerator) component;
				continue;
			}
			if (EVENT_BUS_ID.equals(id) && checkType(EventBus.class, implClass)) {
				eventBus = (EventBus) component;
				continue;
			}
			if (CACHER_ID.equals(id) && checkType(Cacher.class, implClass)) {
				cacher = (Cacher) component;
				continue;
			}
			if (INVOCATION_STRATEGY_FACTORY_ID.equals(id) && checkType(InvocationStrategyFactory.class, implClass)) {
				invocationStrategyFactory = (InvocationStrategyFactory) component;
				continue;
			}
			if (SERVICE_REGISTRY_ID.equals(id) && checkType(ServiceRegistry.class, implClass)) {
				serviceRegistry = (ServiceRegistry) component;
				continue;
			}
			if (TRANSPORTER_ID.equals(id) && checkType(Transporter.class, implClass)) {
				transporter = (Transporter) component;
				continue;
			}

			// Store as custom component
			components.put(id, new MoleculerComponentContainer(component, componentConfig));
		}

		// Find services in Spring Context / Classpath / etc.
		findServices(broker, configOf(COMPONENT_REGISTRY_ID, customConfig));

		// Start internal components
		start(broker, contextFactory, configOf(CONTEXT_FACTORY_ID, customConfig));
		start(broker, uidGenerator, configOf(UID_GENERATOR_ID, customConfig));
		start(broker, eventBus, configOf(EVENT_BUS_ID, customConfig));
		start(broker, cacher, configOf(CACHER_ID, customConfig));
		start(broker, invocationStrategyFactory, configOf(INVOCATION_STRATEGY_FACTORY_ID, customConfig));
		start(broker, serviceRegistry, configOf(SERVICE_REGISTRY_ID, customConfig));
		start(broker, transporter, configOf(TRANSPORTER_ID, customConfig));

		// Start custom components
		for (MoleculerComponentContainer container : components.values()) {
			container.component.start(broker, container.config);
		}
	}

	// --- SERVICE AND COMPONENT FINDER FOR SPRING / GUICE / STANDALONE ---

	protected abstract void findServices(ServiceBroker broker, Tree customConfig) throws Exception;

	// --- CHECK OBJECT TYPE ---

	private static final HashSet<Class<? extends MoleculerComponent>> internalTypes = new HashSet<>();

	static {
		internalTypes.add(ContextFactory.class);
		internalTypes.add(UIDGenerator.class);
		internalTypes.add(EventBus.class);
		internalTypes.add(Cacher.class);
		internalTypes.add(InvocationStrategyFactory.class);
		internalTypes.add(InvocationStrategy.class);
		internalTypes.add(ServiceRegistry.class);
		internalTypes.add(Transporter.class);
	}

	protected static final boolean isInternalComponent(Object component) {
		return isInternalComponent(component.getClass());
	}

	protected static final boolean isInternalComponent(Class<?> component) {
		for (Class<? extends MoleculerComponent> type : internalTypes) {
			if (type.isAssignableFrom(component)) {
				return true;
			}
		}
		return false;
	}

	// --- PACKAGE SCANNER ---

	protected static final LinkedList<String> scan(String packageName) throws Exception {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		LinkedList<String> names = new LinkedList<>();
		packageName = packageName.replace('.', '/');
		URL packageURL = classLoader.getResource(packageName);
		if (packageURL == null) {
			return names;
		}
		if (packageURL.getProtocol().equals("jar")) {

			String jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
			jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));

			JarFile jar = null;
			try {
				jar = new JarFile(jarFileName);
				Enumeration<JarEntry> jarEntries = jar.entries();
				while (jarEntries.hasMoreElements()) {
					String entryName = jarEntries.nextElement().getName();
					if (entryName.startsWith(packageName) && entryName.endsWith(".class")) {
						entryName = entryName.substring(packageName.length() + 1, entryName.lastIndexOf('.'));
						names.add(entryName);
					}
				}
			} finally {
				if (jar != null) {
					jar.close();
				}
			}

		} else {

			URI uri = new URI(packageURL.toString());
			File folder = new File(uri.getPath());
			File[] files = folder.listFiles();
			String entryName;
			for (File actual : files) {
				entryName = actual.getName();
				if (entryName.endsWith(".class")) {
					entryName = entryName.substring(0, entryName.lastIndexOf('.'));
					names.add(entryName);
				}
			}

		}
		return names;
	}

	// --- START MOLECULER COMPONENT ---

	private final void start(ServiceBroker broker, MoleculerComponent component, Tree config) throws Exception {
		if (component != null) {
			String name = CommonUtils.nameOf(component);
			try {
				component.start(broker, config);
				if (name.indexOf(' ') == -1) {
					logger.info("Component \"" + name + "\" started.");
				} else {
					logger.info(name + " started.");
				}
			} catch (Exception cause) {
				logger.error("Unable to start " + name + "!", cause);
				throw cause;
			}
		}
	}

	// --- FIND CONFIG OF A MOLECULER COMPONENT ---

	protected static final Tree configOf(String id, Tree config) {
		for (Tree child : config) {
			if (id.equals(idOf(child))) {
				return child;
			}
		}
		return new Tree();
	}

	// --- GET ID OF A MOLECULER COMPONENT IN CONFIG ---

	private static final String idOf(Tree tree) {

		// Get id property
		String id = tree.get("id", "");
		if (id.isEmpty()) {

			// Get as XML attribute
			id = tree.get("@id", "");
		}
		if (id.isEmpty()) {

			// Use node name as id
			id = tree.getName();
		}
		return id.trim();
	}

	// --- CHECK TYPE OF CLASS ---

	private static final boolean checkType(Class<?> required, Class<?> type) {
		if (!required.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Class \"" + type + "\" must be and instance of \"" + required + "\"!");
		}
		return true;
	}

	// --- STOP REGISTRY AND COMPONENTS ---

	@Override
	public final void stop() {

		// Stop custom components
		for (MoleculerComponentContainer container : components.values()) {
			stop(container.component);
		}
		components.clear();

		// Stop internal components
		stop(transporter);
		stop(serviceRegistry);
		stop(invocationStrategyFactory);
		stop(cacher);
		stop(eventBus);
		stop(uidGenerator);
		stop(contextFactory);

		// Stop thread pools
		if (shutdownThreadPools) {
			try {
				executor.shutdownNow();
			} catch (Throwable cause) {
				logger.error("Unable to stop executor!", cause);
			}
			if (executor != scheduler) {
				try {
					scheduler.shutdownNow();
				} catch (Throwable cause) {
					logger.error("Unable to stop scheduler!", cause);
				}
			}
		}
	}

	private final void stop(MoleculerComponent component) {
		if (component != null) {
			String name = CommonUtils.nameOf(component);
			try {
				component.stop();
				if (name.indexOf(' ') == -1) {
					logger.info("Component \"" + name + "\" stopped.");
				} else {
					logger.info(name + " stopped.");
				}
			} catch (Throwable cause) {
				logger.error("Unable to stop " + name + "!", cause);
			}
		}
	}

	// --- GET THREAD POOLS ---

	@Override
	public final ExecutorService executor() {
		return executor;
	}

	@Override
	public final ScheduledExecutorService scheduler() {
		return scheduler;
	}

	// --- GET BASE COMPONENTS ---

	@Override
	public final ContextFactory contextFactory() {
		return contextFactory;
	}

	@Override
	public final UIDGenerator uidGenerator() {
		return uidGenerator;
	}

	@Override
	public final InvocationStrategyFactory invocationStrategyFactory() {
		return invocationStrategyFactory;
	}

	@Override
	public final ServiceRegistry serviceRegistry() {
		return serviceRegistry;
	}

	@Override
	public final Cacher cacher() {
		return cacher;
	}

	@Override
	public final EventBus eventBus() {
		return eventBus;
	}

	@Override
	public final Transporter transporter() {
		return transporter;
	}

	// --- GET IDS OF CUSTOM COMPONENTS ---

	private final AtomicReference<String[]> cachedNames = new AtomicReference<>();

	@Override
	public final String[] getComponentNames() {
		String[] array = cachedNames.get();
		if (array == null) {
			HashSet<String> set = new HashSet<>();
			if (contextFactory != null) {
				set.add(CONTEXT_FACTORY_ID);
			}
			if (uidGenerator != null) {
				set.add(UID_GENERATOR_ID);
			}
			if (eventBus != null) {
				set.add(EVENT_BUS_ID);
			}
			if (cacher != null) {
				set.add(CACHER_ID);
			}
			if (invocationStrategyFactory != null) {
				set.add(INVOCATION_STRATEGY_FACTORY_ID);
			}
			if (serviceRegistry != null) {
				set.add(SERVICE_REGISTRY_ID);
			}
			if (transporter != null) {
				set.add(TRANSPORTER_ID);
			}
			set.addAll(components.keySet());
			array = new String[set.size()];
			set.toArray(array);
			Arrays.sort(array, String.CASE_INSENSITIVE_ORDER);
			cachedNames.compareAndSet(null, array);
		}
		String[] copy = new String[array.length];
		System.arraycopy(array, 0, copy, 0, array.length);
		return copy;
	}

	// --- GET COMPONENT BY ID ---

	@Override
	public final MoleculerComponent getComponent(String id) {
		switch (id) {
		case CONTEXT_FACTORY_ID:
			return contextFactory;
		case UID_GENERATOR_ID:
			return uidGenerator;
		case EVENT_BUS_ID:
			return eventBus;
		case CACHER_ID:
			return cacher;
		case INVOCATION_STRATEGY_FACTORY_ID:
			return invocationStrategyFactory;
		case SERVICE_REGISTRY_ID:
			return serviceRegistry;
		case TRANSPORTER_ID:
			return transporter;
		default:
			MoleculerComponentContainer container = components.get(id);
			if (container == null) {
				return null;
			}
			return container.component;
		}
	}

	// --- CHECK COMPONENT ID ---

	@Override
	public final boolean hasComponent(String id) {
		switch (id) {
		case CONTEXT_FACTORY_ID:
			return contextFactory != null;
		case UID_GENERATOR_ID:
			return uidGenerator != null;
		case EVENT_BUS_ID:
			return eventBus != null;
		case CACHER_ID:
			return cacher != null;
		case INVOCATION_STRATEGY_FACTORY_ID:
			return invocationStrategyFactory != null;
		case SERVICE_REGISTRY_ID:
			return serviceRegistry != null;
		case TRANSPORTER_ID:
			return transporter != null;
		default:
			return components.containsKey(id);
		}
	}

}
