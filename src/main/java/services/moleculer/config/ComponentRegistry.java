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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.monitor.Monitor;
import services.moleculer.repl.Repl;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.UIDGenerator;

/**
 * ComponentRegistry is a newChannels for loaded Moleculer Components. A Moleculer
 * Component can be a required internal component (eg. Cacher), or a custom
 * user-defined object (eg. DAO object to access a database). ComponentRegistry
 * has similar functionality to Spring's ApplicationContext; stores "beans"
 * (MoleculerComponents), and by using the method "get(id)" you can retrieve
 * instances of your component.
 *
 * @see StandaloneComponentRegistry
 * @see SpringComponentRegistry
 * @see GuiceComponentRegistry
 */
public abstract class ComponentRegistry {

	// --- PROTECTED / INTERNAL COMPONENT IDS ---

	/**
	 * ID of ComponentRegistry (Spring, Guice, Standalone) in the configuration
	 * file. Sample configuration entry in JSON format:<br>
	 * <br>
	 * "components": {<br>
	 * "type": "standalone",<br>
	 * "opts": {<br>
	 * "packagesToScan": "your.service.packages"<br>
	 * }<br>
	 * }<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components();
	 */
	public static final String COMPONENTS_ID = "components";

	/**
	 * ID of ContextFactory definition in the configuration file. Sample
	 * configuration <br>
	 * <br>
	 * "context": {<br>
	 * "type": "default",<br>
	 * "opts": {}<br>
	 * }<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components().context();
	 */
	public static final String CONTEXT_ID = "context";

	/**
	 * ID of UIDGenerator definition in the configuration file. Sample
	 * configuration <br>
	 * "uid": {<br>
	 * "type": "incremental",<br>
	 * "opts": {}<br>
	 * }<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components().uid();
	 */
	public static final String UID_ID = "uid";

	/**
	 * ID of StrategyFactory definition in the configuration file. Sample
	 * configuration <br>
	 * <br>
	 * "strategy": {<br>
	 * "type": "round-robin",<br>
	 * "opts": {<br>
	 * "preferLocal":true }<br>
	 * }<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components().strategy();
	 */
	public static final String STRATEGY_ID = "strategy";

	/**
	 * ID of EventBus definition in the configuration file. Sample configuration
	 * entry in JSON format:<br>
	 * <br>
	 * "eventbus": {<br>
	 * "type": "default",<br>
	 * "opts": {}<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components().eventbus();
	 */
	public static final String EVENTBUS_ID = "eventbus";

	/**
	 * ID of Cacher definition in the configuration file. Sample configuration
	 * entry in JSON format:<br>
	 * <br>
	 * "cacher": {<br>
	 * "type": "memory",<br>
	 * "opts": {<br>
	 * "capacity": 2048<br>
	 * }<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components().cacher();
	 */
	public static final String CACHER_ID = "cacher";

	/**
	 * ID of ServiceRegistry definition in the configuration file. Sample
	 * configuration entry in JSON format:<br>
	 * <br>
	 * "newChannels": {<br>
	 * "type": "default",<br>
	 * "opts": {}<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components().registry();
	 */
	public static final String REGISTRY_ID = "newChannels";

	/**
	 * ID of Transporter definition in the configuration file. Sample
	 * configuration entry in JSON format:<br>
	 * <br>
	 * "transporter": {<br>
	 * "type": "redis",<br>
	 * "opts": {<br>
	 * "url":"redis://host:port"<br>
	 * }<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components().transporter();
	 */
	public static final String TRANSPORTER_ID = "transporter";

	/**
	 * ID of System Monitor definition in the configuration file. Sample
	 * configuration entry in JSON format:<br>
	 * <br>
	 * "monitor": {<br>
	 * "type": "sigar"<br>
	 * }<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components().monitor();
	 */
	public static final String MONITOR_ID = "monitor";

	/**
	 * ID of ExecutorService definition in the configuration file.
	 */
	public static final String EXECUTOR_ID = "executor";

	/**
	 * ID of SchedulerService definition in the configuration file.
	 */
	public static final String SCHEDULER_ID = "scheduler";

	/**
	 * ID of REPL (interactive console) definition in the configuration file.
	 * Sample configuration entry in JSON format:<br>
	 * <br>
	 * "repl": {<br>
	 * "type": "simple"<br>
	 * "enabled": true<br>
	 * }<br>
	 * <br>
	 * You can access this component via {@code ServiceBroker}'s
	 * {@code components()} method:<br>
	 * borker.components().repl();
	 */
	public static final String REPL_ID = "repl";

	// --- LOGGER ---

	/**
	 * Logger of the ComponentRegistry
	 */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- START REGISTRY AND COMPONENTS ---

	/**
	 * Initializes newChannels instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param settings
	 *            settings of the current component (created by the
	 *            ServiceBrokerBuilder)
	 * @param config
	 *            optional configuration (loaded from file)
	 */
	public void start(ServiceBroker broker, ServiceBrokerSettings settings, Tree config) throws Exception {
	}

	// --- STOP REGISTRY AND COMPONENTS ---

	/**
	 * Closes this component newChannels.
	 */
	public void stop() {
	}

	// --- GET THREAD POOLS ---

	/**
	 * Returns the ServiceBroker's common ExecutorService.
	 * 
	 * @return ServiceBroker's ExecutorService
	 */
	public abstract ExecutorService executor();

	/**
	 * Returns the ServiceBroker's common ScheduledExecutorService.
	 * 
	 * @return ServiceBroker's ScheduledExecutorService
	 */
	public abstract ScheduledExecutorService scheduler();

	// --- GET BASE COMPONENTS ---

	/**
	 * Returns the ServiceBroker's ContextFactory (which is defined in the
	 * configuration file's "context" block).
	 * 
	 * @return ServiceBroker's ContextFactory
	 */
	public abstract ContextFactory context();

	/**
	 * Returns the ServiceBroker's UIDGenerator (which is defined in the
	 * configuration file's "uid" block).
	 * 
	 * @return ServiceBroker's UIDGenerator
	 */
	public abstract UIDGenerator uid();

	/**
	 * Returns the ServiceBroker's StrategyFactory (which is defined in the
	 * configuration file's "strategy" block).
	 * 
	 * @return ServiceBroker's StrategyFactory
	 */
	public abstract StrategyFactory strategy();

	/**
	 * Returns the ServiceBroker's ServiceRegistry (which is defined in the
	 * configuration file's "newChannels" block).
	 * 
	 * @return ServiceBroker's ServiceRegistry
	 */
	public abstract ServiceRegistry registry();

	/**
	 * Returns the ServiceBroker's Cacher (which is defined in the configuration
	 * file's "cacher" block).
	 * 
	 * @return ServiceBroker's Cacher
	 */
	public abstract Cacher cacher();

	/**
	 * Returns the ServiceBroker's EventBus (which is defined in the
	 * configuration file's "eventbus" block).
	 * 
	 * @return ServiceBroker's EventBus
	 */
	public abstract EventBus eventbus();

	/**
	 * Returns the ServiceBroker's Transporter (which is defined in the
	 * configuration file's "transporter" block).
	 * 
	 * @return ServiceBroker's Transporter
	 */
	public abstract Transporter transporter();

	/**
	 * Returns the ServiceBroker's Monitor (which is defined in the
	 * configuration file's "monitor" block).
	 * 
	 * @return ServiceBroker's System Monitor
	 */
	public abstract Monitor monitor();

	/**
	 * Returns the ServiceBroker's REPL (interactive console - which is defined
	 * in the configuration file's "repl" block).
	 * 
	 * @return ServiceBroker's REPL implementation
	 */
	public abstract Repl repl();

	// --- GET IDS OF ALL COMPONENTS ---

	/**
	 * Return the names of all registered Moleculer Components.
	 * 
	 * @return array of names
	 */
	public abstract String[] names();

	// --- GET COMPONENT BY ID ---

	/**
	 * Returns the specified Moleculer Component.
	 * 
	 * @param id
	 *            the ID of the Component
	 * @return the Component instance or {@code null}
	 */
	public abstract MoleculerComponent get(String id);

}