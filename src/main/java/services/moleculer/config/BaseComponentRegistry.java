/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.monitor.Monitor;
import services.moleculer.repl.Repl;
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
	private Repl repl;

	// --- CUSTOM COMPONENTS ---

	protected Map<String, MoleculerComponentContainer> componentMap;

	// --- START REGISTRY AND COMPONENTS ---

	/**
	 * Initializes newChannels instance.
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
		repl = settings.getRepl();

		// Create components by config file
		for (Tree subConfig : config) {

			// Default value
			if (subConfig.isNull()) {
				continue;
			}

			// Get name property
			String id = nameOf(subConfig);
			if (!COMPONENTS_ID.equals(id)) {

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
				if (REPL_ID.equals(id) && checkType(Repl.class, implClass)) {
					repl = (Repl) component;
					continue;
				}

				// Store as custom component
				componentMap.put(id, new MoleculerComponentContainer(component, subConfig));
			}
		}

		// Find services in Spring Context / Classpath / etc.
		Tree registryConfig = configOf(COMPONENTS_ID, config);
		Tree opts = registryConfig.get("opts");
		if (opts == null) {
			opts = registryConfig.putMap("opts");
		}
		findServices(broker, opts);

		// Get namespace
		String ns = settings.getNamespace();

		// Start internal components
		start(broker, context, configOf(CONTEXT_ID, config), ns);
		start(broker, uid, configOf(UID_ID, config), ns);
		start(broker, eventbus, configOf(EVENTBUS_ID, config), ns);
		start(broker, cacher, configOf(CACHER_ID, config), ns);
		start(broker, strategy, configOf(STRATEGY_ID, config), ns);
		start(broker, registry, configOf(REGISTRY_ID, config), ns);
		start(broker, monitor, configOf(MONITOR_ID, config), ns);
		start(broker, transporter, configOf(TRANSPORTER_ID, config), ns);
		start(broker, repl, configOf(REPL_ID, config), ns);

		// Start custom components
		for (MoleculerComponentContainer container : componentMap.values()) {
			start(broker, container.component, container.config, ns);
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

	// --- START MOLECULER COMPONENT ---

	private final void start(ServiceBroker broker, MoleculerComponent component, Tree config, String namespace)
			throws Exception {
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
				if (namespace != null && !namespace.isEmpty()) {
					String ns = opts.get("namespace", "");
					if (ns == null || ns.isEmpty()) {
						opts.put("namespace", namespace);
					}
				}
				component.start(broker, opts);
				if (!(component instanceof Repl)) {
					if (name.indexOf(' ') == -1) {
						logger.info("Component " + name + " started.");
					} else {
						logger.info(name + " started.");
					}
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
			Tree cfg = null;
			if (test.startsWith("redis")) {
				cfg = newConfig("services.moleculer.transporter.RedisTransporter");
			} else if (test.startsWith("nats")) {
				cfg = newConfig("services.moleculer.transporter.NatsTransporter");
			} else if (test.startsWith("mqtt")) {
				cfg = newConfig("services.moleculer.transporter.MqttTransporter");
			} else if (test.startsWith("jms")) {
				cfg = newConfig("services.moleculer.transporter.JmsTransporter");
			} else if (test.startsWith("amqp")) {
				cfg = newConfig("services.moleculer.transporter.AmqpTransporter");
			} else if (test.startsWith("google")) {
				cfg = newConfig("services.moleculer.transporter.GoogleTransporter");
			} else if (test.startsWith("kafka")) {
				cfg = newConfig("services.moleculer.transporter.KafkaTransporter");
			} else if (test.startsWith("tcp")) {
				cfg = newConfig("services.moleculer.transporter.TcpTransporter");
			}
			if (cfg != null && test.contains("://")) {
				cfg.put("url", value);
			}
			return cfg;
		} else if (CACHER_ID.equals(id)) {
			Tree cfg = null;
			if (test.startsWith("redis")) {
				cfg = newConfig("services.moleculer.cacher.RedisCacher");
			} else if (test.equals("memory")) {
				cfg = newConfig("services.moleculer.cacher.MemoryCacher");
			} else if (test.equals("offheap")) {
				cfg = newConfig("services.moleculer.cacher.OHCacher");
			}
			if (cfg != null && test.contains("://")) {
				cfg.put("url", value);
			}
			return cfg;
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
			if (test.equals("cpu")) {
				return newConfig("services.moleculer.strategy.CpuUsageStrategyFactory");
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
			if (test.equals("command")) {
				return newConfig("services.moleculer.monitor.CommandMonitor");
			}
			if (test.equals("constant")) {
				return newConfig("services.moleculer.monitor.ConstantMonitor");
			}
		} else if (REPL_ID.equals(id)) {
			if (test.equals("local")) {
				return newConfig("services.moleculer.repl.LocalRepl");
			}
			if (test.equals("remote")) {
				return newConfig("services.moleculer.repl.RemoteRepl");
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
				if (!(component instanceof Repl)) {
					if (name.indexOf(' ') == -1) {
						logger.info("Component " + name + " stopped.");
					} else {
						logger.info(name + " stopped.");
					}
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

	@Override
	public final Repl repl() {
		return repl;
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
			if (repl != null) {
				set.add(REPL_ID);
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
		case REPL_ID:
			return repl;
		default:
			MoleculerComponentContainer container = componentMap.get(id);
			if (container == null) {
				return null;
			}
			return container.component;
		}
	}

}