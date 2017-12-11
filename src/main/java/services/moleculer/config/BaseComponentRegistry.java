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
import static services.moleculer.util.CommonUtils.typeOf;

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
import services.moleculer.cacher.Cacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.monitor.Monitor;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.Strategy;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.UIDGenerator;

/**
 * Abstract class for Standalone, Spring, and Guice Component Registries.
 * ComponentRegistry has similar functionality to Spring's ApplicationContext;
 * stores "beans" (MoleculerComponents), and by using the method "get(id)" you
 * can retrieve instances of your component.
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

	private ContextFactory context;
	private UIDGenerator uid;
	private StrategyFactory strategy;
	private EventBus eventbus;
	private Cacher cacher;
	private ServiceRegistry registry;
	private Transporter transporter;
	private Monitor monitor;

	// --- CUSTOM COMPONENTS ---

	protected Map<String, MoleculerComponentContainer> componentMap;

	// --- START REGISTRY AND COMPONENTS ---

	/**
	 * Initializes registry instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param settings
	 *            settings of the broker (created by the ServiceBrokerBuilder)
	 * @param config
	 *            optional configuration (loaded from file)
	 */
	@Override
	public final void start(ServiceBroker broker, ServiceBrokerSettings settings, Tree config) throws Exception {

		// Set thread pools
		executor = settings.getExecutor();
		scheduler = settings.getScheduler();

		// Should terminate thread pools on stop()?
		shutdownThreadPools = settings.getShutDownThreadPools();

		// Set internal components
		context = settings.getContext();
		uid = settings.getUid();
		strategy = settings.getStrategy();
		registry = settings.getRegistry();
		cacher = settings.getCacher();
		eventbus = settings.getEventbus();
		transporter = settings.getTransporter();
		monitor = settings.getMonitor();
		componentMap = settings.getComponentMap();

		// Create components by config file
		for (Tree subConfig : config) {

			// Default value
			if (subConfig.isNull()) {
				continue;
			}

			// Get name property
			String id = nameOf(subConfig);

			// Rewrite short config to standard config
			if (subConfig.isPrimitive()) {
				String value = subConfig.asString();
				Tree replace = replaceType(id, value);
				if (replace != null) {
					subConfig = replace;
				}
			}

			// Ignore
			if (!subConfig.isMap()) {
				continue;
			}

			// Get class name / type
			String type = typeOf(subConfig);

			// Rewrite short type
			Tree replace = replaceType(id, type);
			if (replace != null) {
				type = replace.get("type", type);
			}

			// Unknown entry
			if (type == null || type.isEmpty()) {
				continue;
			}

			// Create instance
			Class<?> implClass = Class.forName(type);
			if (!MoleculerComponent.class.isAssignableFrom(implClass)) {
				if (ComponentRegistry.class.isAssignableFrom(implClass)) {
					continue;
				}
				throw new IllegalArgumentException(
						"Class \"" + type + "\" must implement the MoleculerComponent interface!");
			}
			MoleculerComponent component = (MoleculerComponent) implClass.newInstance();

			// Maybe it's an internal compoment
			if (CONTEXT_ID.equals(id) && checkType(ContextFactory.class, implClass)) {
				context = (ContextFactory) component;
				continue;
			}
			if (UID_ID.equals(id) && checkType(UIDGenerator.class, implClass)) {
				uid = (UIDGenerator) component;
				continue;
			}
			if (EVENTBUS_ID.equals(id) && checkType(EventBus.class, implClass)) {
				eventbus = (EventBus) component;
				continue;
			}
			if (CACHER_ID.equals(id) && checkType(Cacher.class, implClass)) {
				cacher = (Cacher) component;
				continue;
			}
			if (STRATEGY_ID.equals(id) && checkType(StrategyFactory.class, implClass)) {
				strategy = (StrategyFactory) component;
				continue;
			}
			if (REGISTRY_ID.equals(id) && checkType(ServiceRegistry.class, implClass)) {
				registry = (ServiceRegistry) component;
				continue;
			}
			if (TRANSPORTER_ID.equals(id) && checkType(Transporter.class, implClass)) {
				transporter = (Transporter) component;
				continue;
			}
			if (MONITOR_ID.equals(id) && checkType(Monitor.class, implClass)) {
				monitor = (Monitor) component;
				continue;
			}

			// Store as custom component
			componentMap.put(id, new MoleculerComponentContainer(component, subConfig));
		}

		// Find services in Spring Context / Classpath / etc.
		findServices(broker, configOf(COMPONENTS_ID, config));

		// Start internal components
		start(broker, context, configOf(CONTEXT_ID, config));
		start(broker, uid, configOf(UID_ID, config));
		start(broker, eventbus, configOf(EVENTBUS_ID, config));
		start(broker, cacher, configOf(CACHER_ID, config));
		start(broker, strategy, configOf(STRATEGY_ID, config));
		start(broker, registry, configOf(REGISTRY_ID, config));
		start(broker, monitor, configOf(MONITOR_ID, config));
		start(broker, transporter, configOf(TRANSPORTER_ID, config));

		// Start custom components
		for (MoleculerComponentContainer container : componentMap.values()) {
			start(broker, container.component, container.config);
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
		internalTypes.add(StrategyFactory.class);
		internalTypes.add(Strategy.class);
		internalTypes.add(ServiceRegistry.class);
		internalTypes.add(Transporter.class);
		internalTypes.add(Monitor.class);
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
			String name = nameOf(component, true);
			try {
				Tree opts = config.get("opts");
				if (opts == null) {
					if (config.isMap()) {
						opts = config.putMap("opts");
					} else {
						opts = new Tree();
					}
				}
				component.start(broker, opts);
				if (name.indexOf(' ') == -1) {
					logger.info("Component " + name + " started.");
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
			if (id.equals(nameOf(child))) {
				return child;
			}
		}
		return new Tree();
	}

	// --- CHECK TYPE OF CLASS ---

	private static final boolean checkType(Class<?> required, Class<?> type) {
		if (!required.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Class \"" + type + "\" must be and instance of \"" + required + "\"!");
		}
		return true;
	}

	// --- CREATE COMPONENT CONFIG ---

	private static final Tree replaceType(String id, String value) {
		String test = value.toLowerCase();
		if (TRANSPORTER_ID.equals(id)) {
			if (test.startsWith("redis")) {
				Tree cfg = newConfig("services.moleculer.transporter.RedisTransporter");
				if (test.contains("://")) {
					cfg.put("url", value);
				}
				return cfg;
			}
			if (test.startsWith("nats")) {
				Tree cfg = newConfig("services.moleculer.transporter.NatsTransporter");
				if (test.contains("://")) {
					cfg.put("url", value);
				}
				return cfg;
			}
			if (test.startsWith("mqtt")) {
				Tree cfg = newConfig("services.moleculer.transporter.MqttTransporter");
				if (test.contains("://")) {
					cfg.put("url", value);
				}
				return cfg;
			}
			if (test.startsWith("jms")) {
				Tree cfg = newConfig("services.moleculer.transporter.JmsTransporter");
				if (test.contains("://")) {
					cfg.put("url", value);
				}
				return cfg;
			}
			if (test.startsWith("amqp")) {
				Tree cfg = newConfig("services.moleculer.transporter.AmqpTransporter");
				if (test.contains("://")) {
					cfg.put("url", value);
				}
				return cfg;
			}
			if (test.startsWith("google")) {
				Tree cfg = newConfig("services.moleculer.transporter.GoogleCloudTransporter");
				if (test.contains("://")) {
					cfg.put("url", value);
				}
				return cfg;
			}
			if (test.startsWith("ws")) {
				Tree cfg = newConfig("services.moleculer.transporter.SocketClusterTransporter");
				if (test.contains("://")) {
					cfg.put("url", value);
				}
				return cfg;
			}
		} else if (CACHER_ID.equals(id)) {
			if (test.startsWith("redis")) {
				Tree cfg = newConfig("services.moleculer.cacher.RedisCacher");
				if (value.contains("://")) {
					cfg.put("url", value);
				}
				return cfg;
			}
			if (test.equals("memory") || test.equals("on-heap")) {
				return newConfig("services.moleculer.cacher.MemoryCacher");
			}
			if (test.equals("offheap") || test.equals("off-heap")) {
				return newConfig("services.moleculer.cacher.OHCacher");
			}
		} else if (CONTEXT_ID.equals(id)) {
			if (test.equals("default")) {
				return newConfig("services.moleculer.context.DefaultContextFactory");
			}
		} else if (UID_ID.equals(id)) {
			if (test.equals("incremental")) {
				return newConfig("services.moleculer.uid.IncrementalUIDGenerator");
			}
			if (test.equals("uuid")) {
				return newConfig("services.moleculer.uid.StandardUUIDGenerator");
			}
		} else if (STRATEGY_ID.equals(id)) {
			if (test.startsWith("round")) {
				return newConfig("services.moleculer.strategy.RoundRobinStrategyFactory");
			}
			if (test.equals("random")) {
				return newConfig("services.moleculer.strategy.XORShiftRandomStrategyFactory");
			}
		} else if (REGISTRY_ID.equals(id)) {
			if (test.equals("default")) {
				return newConfig("services.moleculer.service.DefaultServiceRegistry");
			}
		} else if (EVENTBUS_ID.equals(id)) {
			if (test.equals("default")) {
				return newConfig("services.moleculer.eventbus.DefaultEventBus");
			}
		} else if (MONITOR_ID.equals(id)) {
			if (test.equals("sigar")) {
				return newConfig("services.moleculer.monitor.SigarMonitor");
			}
			if (test.equals("jmx")) {
				return newConfig("services.moleculer.monitor.JMXMonitor");
			}
			if (test.equals("constant")) {
				return newConfig("services.moleculer.monitor.ConstantMonitor");
			}
		}
		return null;
	}

	private static final Tree newConfig(String type) {
		return new Tree().put("type", type);
	}

	// --- STOP REGISTRY AND COMPONENTS ---

	@Override
	public final void stop() {

		// Stop custom components
		for (MoleculerComponentContainer container : componentMap.values()) {
			stop(container.component);
		}
		componentMap.clear();

		// Stop internal components
		stop(transporter);
		stop(monitor);
		stop(registry);
		stop(strategy);
		stop(cacher);
		stop(eventbus);
		stop(uid);
		stop(context);

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
			String name = nameOf(component, true);
			try {
				component.stop();
				if (name.indexOf(' ') == -1) {
					logger.info("Component " + name + " stopped.");
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
	public final ContextFactory context() {
		return context;
	}

	@Override
	public final UIDGenerator uid() {
		return uid;
	}

	@Override
	public final StrategyFactory strategy() {
		return strategy;
	}

	@Override
	public final ServiceRegistry registry() {
		return registry;
	}

	@Override
	public final Cacher cacher() {
		return cacher;
	}

	@Override
	public final EventBus eventbus() {
		return eventbus;
	}

	@Override
	public final Transporter transporter() {
		return transporter;
	}

	@Override
	public final Monitor monitor() {
		return monitor;
	}

	// --- GET IDS OF CUSTOM COMPONENTS ---

	private final AtomicReference<String[]> cachedNames = new AtomicReference<>();

	@Override
	public final String[] names() {
		String[] array = cachedNames.get();
		if (array == null) {
			HashSet<String> set = new HashSet<>();
			if (context != null) {
				set.add(CONTEXT_ID);
			}
			if (uid != null) {
				set.add(UID_ID);
			}
			if (eventbus != null) {
				set.add(EVENTBUS_ID);
			}
			if (cacher != null) {
				set.add(CACHER_ID);
			}
			if (strategy != null) {
				set.add(STRATEGY_ID);
			}
			if (registry != null) {
				set.add(REGISTRY_ID);
			}
			if (transporter != null) {
				set.add(TRANSPORTER_ID);
			}
			if (monitor != null) {
				set.add(MONITOR_ID);
			}
			set.addAll(componentMap.keySet());
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
	public final MoleculerComponent get(String id) {
		switch (id) {
		case CONTEXT_ID:
			return context;
		case UID_ID:
			return uid;
		case EVENTBUS_ID:
			return eventbus;
		case CACHER_ID:
			return cacher;
		case STRATEGY_ID:
			return strategy;
		case REGISTRY_ID:
			return registry;
		case TRANSPORTER_ID:
			return transporter;
		case MONITOR_ID:
			return monitor;
		default:
			MoleculerComponentContainer container = componentMap.get(id);
			if (container == null) {
				return null;
			}
			return container.component;
		}
	}

}