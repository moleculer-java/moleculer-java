/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
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

import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cacher;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.monitor.Monitor;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.UidGenerator;

/**
 * Builder-style ServiceBroker factory. Sample of usage:<br>
 * 
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder().cacher(new RedisCacher()).transporter(new RedisTransporter()).build();
 * </pre>
 */
public class ServiceBrokerBuilder {

	// --- CONFIGURATION ---

	protected ServiceBrokerConfig config = new ServiceBrokerConfig();

	// --- BUILD METHOD ---

	public ServiceBroker build() {
		return new ServiceBroker(config);
	}

	// --- INTERNAL COMPONENTS AND PROPERTIES ---

	public ServiceBrokerBuilder namespace(String namespace) {
		config.setNamespace(namespace);
		return this;
	}

	public ServiceBrokerBuilder nodeID(String nodeID) {
		config.setNodeID(nodeID);
		return this;
	}

	public ServiceBrokerBuilder internalServices(boolean internalServices) {
		config.setInternalServices(internalServices);
		return this;
	}

	public ServiceBrokerBuilder scheduler(ScheduledExecutorService scheduler) {
		config.setScheduler(scheduler);
		return this;
	}

	public ServiceBrokerBuilder executor(ExecutorService executor) {
		config.setExecutor(executor);
		return this;
	}

	public ServiceBrokerBuilder registry(ServiceRegistry serviceRegistry) {
		config.setServiceRegistry(serviceRegistry);
		return this;
	}

	public ServiceBrokerBuilder eventbus(Eventbus eventBus) {
		config.setEventbus(eventBus);
		return this;
	}

	public ServiceBrokerBuilder uid(UidGenerator uidGenerator) {
		config.setUidGenerator(uidGenerator);
		return this;
	}

	public ServiceBrokerBuilder strategy(StrategyFactory strategyFactory) {
		config.setStrategyFactory(strategyFactory);
		return this;
	}

	public ServiceBrokerBuilder transporter(Transporter transporter) {
		config.setTransporter(transporter);
		return this;
	}

	public ServiceBrokerBuilder cacher(Cacher cacher) {
		config.setCacher(cacher);
		return this;
	}

	public ServiceBrokerBuilder monitor(Monitor monitor) {
		config.setMonitor(monitor);
		return this;
	}

	public ServiceBrokerBuilder invoker(ServiceInvoker invoker) {
		config.setServiceInvoker(invoker);
		return this;
	}

	public ServiceBrokerBuilder readers(String jsonReader) {
		config.setJsonReaders(jsonReader);
		return this;
	}

	public ServiceBrokerBuilder writers(String jsonWriter) {
		config.setJsonWriters(jsonWriter);
		return this;
	}

}