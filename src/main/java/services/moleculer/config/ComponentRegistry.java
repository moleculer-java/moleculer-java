package services.moleculer.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.InvocationStrategyFactory;
import services.moleculer.transporters.Transporter;
import services.moleculer.uids.UIDGenerator;
import services.moleculer.utils.CommonUtils;

public final class ComponentRegistry implements MoleculerComponent {

	// --- INTERNAL COMPONENT IDS ---

	public static final String CONTEXT_FACTORY_ID = "contextFactory";
	public static final String UID_GENERATOR_ID = "uidGenerator";
	public static final String EVENT_BUS_ID = "eventBus";
	public static final String CACHER_ID = "cacher";
	public static final String SERVICE_REGISTRY_ID = "serviceRegistry";
	public static final String TRANSPORTER_ID = "transporter";

	// --- LOGGER ---

	private static final Logger logger = LoggerFactory.getLogger(ComponentRegistry.class);

	// --- INTERNAL COMPONENTS ---

	private ServiceBroker broker;
	private Executor executor;
	private ScheduledExecutorService scheduler;
	private ContextFactory contextFactory;
	private UIDGenerator uidGenerator;
	private InvocationStrategyFactory invocationStrategyFactory;
	private ServiceRegistry serviceRegistry;
	private Cacher cacher;
	private EventBus eventBus;
	private Transporter transporter;

	// --- CUSTOM COMPONENTS ---

	private Map<String, MoleculerComponentConfig> components;

	// --- CONSTRUCTOR ---

	public ComponentRegistry(ServiceBrokerConfig config) {

		// Set internal components
		executor = config.getExecutor();
		scheduler = config.getScheduler();
		contextFactory = config.getContextFactory();
		uidGenerator = config.getUIDGenerator();
		invocationStrategyFactory = config.getInvocationStrategyFactory();
		serviceRegistry = config.getServiceRegistry();
		cacher = config.getCacher();
		eventBus = config.getEventBus();
		transporter = config.getTransporter();
		components = config.getComponents();
	}

	// --- START ALL COMPONENTS ---

	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {
		this.broker = broker;

		// Create components
		for (Tree componentConfig : config) {

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
			String clazz = componentConfig.get("class", "");
			if (clazz.isEmpty()) {

				// Get class name as XML attribute
				clazz = componentConfig.get("@class", "");
			}
			if (clazz.isEmpty()) {
				continue;
			}

			// Create instance
			Class<?> realClazz = Class.forName(clazz);
			if (!MoleculerComponent.class.isAssignableFrom(realClazz)) {
				throw new IllegalArgumentException(
						"Class \"" + clazz + "\" must implement the MoleculerComponent interface!");
			}
			MoleculerComponent component = (MoleculerComponent) realClazz.newInstance();

			// Maybe it's an internal compoment
			if (CONTEXT_FACTORY_ID.equals(id) && checkType(ContextFactory.class, realClazz)) {
				contextFactory = (ContextFactory) component;
				continue;
			}
			if (UID_GENERATOR_ID.equals(id) && checkType(UIDGenerator.class, realClazz)) {
				uidGenerator = (UIDGenerator) component;
				continue;
			}
			if (EVENT_BUS_ID.equals(id) && checkType(EventBus.class, realClazz)) {
				eventBus = (EventBus) component;
				continue;
			}
			if (CACHER_ID.equals(id) && checkType(Cacher.class, realClazz)) {
				cacher = (Cacher) component;
				continue;
			}
			if (SERVICE_REGISTRY_ID.equals(id) && checkType(ServiceRegistry.class, realClazz)) {
				serviceRegistry = (ServiceRegistry) component;
				continue;
			}
			if (TRANSPORTER_ID.equals(id) && checkType(Transporter.class, realClazz)) {
				transporter = (Transporter) component;
				continue;
			}

			// Store as custom component
			components.put(id, new MoleculerComponentConfig(component, componentConfig));
		}

		// Start internal components
		start(contextFactory, configOf(CONTEXT_FACTORY_ID, config));
		start(uidGenerator, configOf(UID_GENERATOR_ID, config));
		start(eventBus, configOf(EVENT_BUS_ID, config));
		start(cacher, configOf(CACHER_ID, config));
		start(serviceRegistry, configOf(SERVICE_REGISTRY_ID, config));
		start(transporter, configOf(TRANSPORTER_ID, config));

		// Start custom components
		for (MoleculerComponentConfig container : components.values()) {
			container.component().start(broker, container.config());
		}
	}

	private final void start(MoleculerComponent component, Tree config) throws Exception {
		if (component != null) {
			String name = CommonUtils.nameOf(component);
			try {
				component.start(broker, config);
				logger.info(name + " started.");
			} catch (Exception cause) {
				logger.error("Unable to start " + name + "!", cause);
				throw cause;
			}
		}
	}

	private static final Tree configOf(String id, Tree config) {
		for (Tree child : config) {
			if (id.equals(idOf(child))) {
				return child;
			}
		}
		return null;
	}

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

	private static final boolean checkType(Class<?> required, Class<?> type) {
		if (!required.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Class \"" + type + "\" must be and instance of \"" + required + "\"!");
		}
		return true;
	}

	// --- STOP ALL COMPONENTS ---

	@Override
	public final void stop() {

		// Stop custom components
		for (MoleculerComponentConfig container : components.values()) {
			stop(container.component());
		}
		components.clear();

		// Stop internal components
		stop(transporter);
		stop(serviceRegistry);
		stop(cacher);
		stop(eventBus);
		stop(uidGenerator);
		stop(contextFactory);
	}

	private final void stop(MoleculerComponent component) {
		if (component != null) {
			String name = CommonUtils.nameOf(component);
			try {
				component.stop();
				logger.info(name + " stopped.");
			} catch (Throwable cause) {
				logger.error("Unable to stop " + name + "!", cause);
			}
		}
	}

	// --- GET COMPONENTS ---

	public final Executor executor() {
		return executor;
	}

	public final ScheduledExecutorService scheduler() {
		return scheduler;
	}

	public final ContextFactory contextFactory() {
		return contextFactory;
	}

	public final UIDGenerator uidGenerator() {
		return uidGenerator;
	}

	public final InvocationStrategyFactory invocationStrategyFactory() {
		return invocationStrategyFactory;
	}

	public final ServiceRegistry serviceRegistry() {
		return serviceRegistry;
	}

	public final Cacher cacher() {
		return cacher;
	}

	public final EventBus eventBus() {
		return eventBus;
	}

	public final Transporter transporter() {
		return transporter;
	}

	// --- GET CUSTOM COMPONENT ---

	public final List<String> getComponentNames() {
		LinkedList<String> names = new LinkedList<String>();
		if (contextFactory != null) {
			names.add(CONTEXT_FACTORY_ID);
		}
		if (uidGenerator != null) {
			names.add(UID_GENERATOR_ID);
		}
		if (eventBus != null) {
			names.add(EVENT_BUS_ID);
		}
		if (cacher != null) {
			names.add(CACHER_ID);
		}
		if (serviceRegistry != null) {
			names.add(SERVICE_REGISTRY_ID);
		}
		if (transporter != null) {
			names.add(TRANSPORTER_ID);
		}
		names.addAll(components.keySet());
		return names;
	}

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
		case SERVICE_REGISTRY_ID:
			return serviceRegistry;
		case TRANSPORTER_ID:
			return transporter;
		default:
			MoleculerComponentConfig container = components.get(id);
			if (container == null) {
				return null;
			}
			return container.component();
		}
	}

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
		case SERVICE_REGISTRY_ID:
			return serviceRegistry != null;
		case TRANSPORTER_ID:
			return transporter != null;
		default:
			return components.containsKey(id);
		}
	}

}