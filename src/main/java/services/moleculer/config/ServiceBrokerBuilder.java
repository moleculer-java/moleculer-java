package services.moleculer.config;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.cachers.Cacher;
import services.moleculer.context.ContextFactory;
import services.moleculer.eventbus.EventBus;
import services.moleculer.services.ServiceRegistry;
import services.moleculer.strategies.StrategyFactory;
import services.moleculer.transporters.Transporter;
import services.moleculer.uids.UIDGenerator;

/**
 * Builder-style ServiceBroker factory. Sample of usage:<br>
 * <br>
 * ServiceBroker broker = ServiceBroker.builder().cacher(cacher).build();
 */
public class ServiceBrokerBuilder {

	// --- CONFIGURATION ---

	private final ServiceBrokerConfig config;

	// --- CONSTRUCTOR ---

	public ServiceBrokerBuilder(ServiceBrokerConfig config) {
		this.config = config;
	}

	// --- BUILD METHOD ---

	public final ServiceBroker build() {
		return new ServiceBroker(config);
	}

	// --- INTERNAL COMPONENTS AND PROPERTIES ---

	public final ServiceBrokerBuilder scheduler(ScheduledExecutorService scheduler) {
		config.setScheduler(scheduler);
		return this;
	}

	public final ServiceBrokerBuilder executor(ExecutorService executor) {
		config.setExecutor(executor);
		return this;
	}

	public final ServiceBrokerBuilder contextFactory(ContextFactory contextFactory) {
		config.setContextFactory(contextFactory);
		return this;
	}

	public final ServiceBrokerBuilder serviceRegistry(ServiceRegistry serviceRegistry) {
		config.setServiceRegistry(serviceRegistry);
		return this;
	}

	public final ServiceBrokerBuilder eventBus(EventBus eventBus) {
		config.setEventBus(eventBus);
		return this;
	}

	public final ServiceBrokerBuilder uidGenerator(UIDGenerator uidGenerator) {
		config.setUidGenerator(uidGenerator);
		return this;
	}

	public final ServiceBrokerBuilder strategyFactory(StrategyFactory strategyFactory) {
		config.setStrategyFactory(strategyFactory);
		return this;
	}

	public final ServiceBrokerBuilder namespace(String namespace) {
		config.setNamespace(namespace);
		return this;
	}

	public final ServiceBrokerBuilder nodeID(String nodeID) {
		config.setNodeID(nodeID);
		return this;
	}

	public final ServiceBrokerBuilder transporter(Transporter transporter) {
		config.setTransporter(transporter);
		return this;
	}

	public final ServiceBrokerBuilder cacher(Cacher cacher) {
		config.setCacher(cacher);
		return this;
	}

	public final ServiceBrokerBuilder componentRegistry(ComponentRegistry componentRegistry) {
		config.setComponentRegistry(componentRegistry);
		return this;		
	}
	
	// --- ADD CUSTOM COMPONENT ---
	
	public final ServiceBrokerBuilder addComponent(String id, MoleculerComponent component, Tree configuration) {
		Objects.requireNonNull(component);
		id = Objects.requireNonNull(id).trim();
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Empty id is not allowed!");
		}
		if (configuration == null) {
			configuration = new Tree();
		}
		config.getComponents().put(id, new MoleculerComponentContainer(component, configuration));
		return this;
	}

}