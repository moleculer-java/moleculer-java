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

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

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
 * Builder-style ServiceBroker factory. Sample of usage:<br>
 * <br>
 * ServiceBroker broker = ServiceBroker.builder().cacher(cacher).build();
 */
public final class ServiceBrokerBuilder {

	// --- CONFIGURATION ---

	private final ServiceBrokerSettings settings;

	// --- CONSTRUCTOR ---

	public ServiceBrokerBuilder(ServiceBrokerSettings settings) {
		this.settings = settings;
	}

	// --- BUILD METHOD ---

	public final ServiceBroker build() {
		return new ServiceBroker(settings);
	}

	// --- INTERNAL COMPONENTS AND PROPERTIES ---

	public final ServiceBrokerBuilder namespace(String namespace) {
		settings.setNamespace(namespace);
		return this;
	}

	public final ServiceBrokerBuilder nodeID(String nodeID) {
		settings.setNodeID(nodeID);
		return this;
	}

	public final ServiceBrokerBuilder internalServices(boolean internalServices) {
		settings.setInternalServices(internalServices);
		return this;
	}
	
	public final ServiceBrokerBuilder scheduler(ScheduledExecutorService scheduler) {
		settings.setScheduler(scheduler);
		return this;
	}

	public final ServiceBrokerBuilder executor(ExecutorService executor) {
		settings.setExecutor(executor);
		return this;
	}

	public final ServiceBrokerBuilder context(ContextFactory contextFactory) {
		settings.setContext(contextFactory);
		return this;
	}

	public final ServiceBrokerBuilder registry(ServiceRegistry serviceRegistry) {
		settings.setRegistry(serviceRegistry);
		return this;
	}

	public final ServiceBrokerBuilder eventbus(EventBus eventBus) {
		settings.setEventbus(eventBus);
		return this;
	}

	public final ServiceBrokerBuilder uid(UIDGenerator uidGenerator) {
		settings.setUid(uidGenerator);
		return this;
	}

	public final ServiceBrokerBuilder strategy(StrategyFactory strategyFactory) {
		settings.setStrategy(strategyFactory);
		return this;
	}

	public final ServiceBrokerBuilder transporter(Transporter transporter) {
		settings.setTransporter(transporter);
		return this;
	}

	public final ServiceBrokerBuilder cacher(Cacher cacher) {
		settings.setCacher(cacher);
		return this;
	}

	public final ServiceBrokerBuilder components(ComponentRegistry componentRegistry) {
		settings.setComponents(componentRegistry);
		return this;
	}

	public final ServiceBrokerBuilder monitor(Monitor monitor) {
		settings.setMonitor(Objects.requireNonNull(monitor));
		return this;
	}

	public final ServiceBrokerBuilder repl(Repl repl) {
		settings.setRepl(Objects.requireNonNull(repl));
		return this;
	}

	// --- ADD CUSTOM COMPONENT ---

	public final ServiceBrokerBuilder add(MoleculerComponent component) {
		return add(component, new Tree());
	}

	public final ServiceBrokerBuilder add(MoleculerComponent component, Tree config) {
		return add(nameOf(component, false), component, config);
	}

	public final ServiceBrokerBuilder add(String id, MoleculerComponent component, Tree config) {
		Objects.requireNonNull(component);
		id = Objects.requireNonNull(id).trim();
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Empty id is not allowed!");
		}
		if (config == null) {
			config = new Tree();
		}
		settings.getComponentMap().put(id, new MoleculerComponentContainer(component, config));
		return this;
	}

}