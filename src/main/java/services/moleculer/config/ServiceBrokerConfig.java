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

import static services.moleculer.util.CommonUtils.getHostName;

import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import services.moleculer.cacher.Cacher;
import services.moleculer.cacher.MemoryCacher;
import services.moleculer.eventbus.DefaultEventbus;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.metrics.DefaultMetrics;
import services.moleculer.metrics.Metrics;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.monitor.Monitor;
import services.moleculer.service.DefaultServiceInvoker;
import services.moleculer.service.DefaultServiceRegistry;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.strategy.RoundRobinStrategyFactory;
import services.moleculer.strategy.StrategyFactory;
import services.moleculer.transporter.NullTransporter;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.IncrementalUidGenerator;
import services.moleculer.uid.UidGenerator;

public class ServiceBrokerConfig {

	// --- THREAD POOLS ---

	protected ExecutorService executor;
	protected ScheduledExecutorService scheduler;

	protected boolean shutDownThreadPools = true;

	// --- PROPERTIES ---

	/**
	 * Namespace of Broker.
	 */
	protected String namespace = "";

	/**
	 * Unique NodeID of Broker.
	 */
	protected String nodeID;

	/**
	 * Install internal ($node) services?
	 */
	protected boolean internalServices = true;

	// --- JSON API SERIALIZER / DESERIALIZER ---

	/**
	 * Name (or comma-separated list) of the JSON deserializer API ("jackson",
	 * "boon", "builtin", "gson", "fastjson", "genson", etc., null = autodetect
	 * / fastest)
	 */
	protected String jsonReaders;

	/**
	 * Name (or comma-separated list) of the JSON serializer API ("jackson",
	 * "boon", "builtin", "gson", "fast", "genson", "flex", "nano", etc., null =
	 * autodetect / fastest)
	 */
	protected String jsonWriters;

	// --- INSTANCE COUNTER ---

	/**
	 * Service Broker instance counter (0...N)
	 */
	protected AtomicLong instanceCounter = new AtomicLong();

	// --- INTERNAL COMPONENTS ---

	protected UidGenerator uidGenerator = new IncrementalUidGenerator();
	protected StrategyFactory strategyFactory = new RoundRobinStrategyFactory();
	protected Eventbus eventbus = new DefaultEventbus();
	protected ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
	protected ServiceInvoker serviceInvoker = new DefaultServiceInvoker();
	protected Metrics metrics = new DefaultMetrics();

	protected Cacher cacher;
	protected Transporter transporter;
	protected Monitor monitor;

	protected boolean metricsEnabled;

	// --- SET DEFAULT CPU MONITOR ---

	private static Monitor defaultMonitor;

	static {
		try {

			// Try to load native library (for Windows and Linux)
			String[] libs = { "sigar-x86-winnt", "sigar-amd64-winnt", "libsigar-x86-linux", "libsigar-amd64-linux" };
			ClassLoader loader = ServiceBrokerConfig.class.getClassLoader();
			String pkg = "services.moleculer.monitor.";
			for (String lib : libs) {
				try {
					System.loadLibrary(lib);

					// Found!
					Class.forName("org.hyperic.sigar.Sigar");
					defaultMonitor = (Monitor) loader.loadClass(pkg + "SigarMonitor").newInstance();
					break;
				} catch (Throwable notFound) {

					// Not found
				}
			}

			// Try to get "SystemCpuLoad" JMX attribute
			if (defaultMonitor == null) {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
				AttributeList list = mbs.getAttributes(name, new String[] { "SystemCpuLoad" });
				if (list != null && !list.isEmpty()) {
					defaultMonitor = (Monitor) loader.loadClass(pkg + "JmxMonitor").newInstance();
				}
			}
		} catch (Throwable ignored) {
		} finally {

			// Fallback to constant-based "monitor"
			if (defaultMonitor == null) {
				defaultMonitor = new ConstantMonitor();
			}
		}
	}

	// --- CONSTRUCTORS ---

	public ServiceBrokerConfig() {
		this(null, new MemoryCacher(), null);
	}

	public ServiceBrokerConfig(String nodeID, Cacher cacher, Transporter transporter) {

		// Create default thread pools
		int threads = Math.max(4, Runtime.getRuntime().availableProcessors());
		executor = Executors.newWorkStealingPool(threads);
		scheduler = Executors.newScheduledThreadPool(threads);

		// Set the default System Monitor
		monitor = defaultMonitor;

		// Set the default (generated) NodeID
		if (nodeID == null || nodeID.isEmpty()) {
			this.nodeID = getHostName() + '-' + monitor.getPID();
			long index = instanceCounter.incrementAndGet();
			if (index > 1) {
				this.nodeID += '-' + Long.toString(index);
			}
		} else {
			this.nodeID = nodeID;
		}

		// Set cacher
		setCacher(cacher);

		// Set transporter
		setTransporter(transporter);
	}

	// --- GETTERS AND SETTERS ---

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = Objects.requireNonNull(executor);
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public void setScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = Objects.requireNonNull(scheduler);
	}

	public boolean isShutDownThreadPools() {
		return shutDownThreadPools;
	}

	public void setShutDownThreadPools(boolean shutDownThreadPools) {
		this.shutDownThreadPools = shutDownThreadPools;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getNodeID() {
		return nodeID;
	}

	public void setNodeID(String nodeID) {
		this.nodeID = Objects.requireNonNull(nodeID);
	}

	public boolean isInternalServices() {
		return internalServices;
	}

	public void setInternalServices(boolean internalServices) {
		this.internalServices = internalServices;
	}

	public String getJsonReaders() {
		return jsonReaders;
	}

	public void setJsonReaders(String jsonReader) {
		this.jsonReaders = jsonReader;
	}

	public String getJsonWriters() {
		return jsonWriters;
	}

	public void setJsonWriters(String jsonWriter) {
		this.jsonWriters = jsonWriter;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = Objects.requireNonNull(serviceRegistry);
	}

	public StrategyFactory getStrategyFactory() {
		return strategyFactory;
	}

	public void setStrategyFactory(StrategyFactory strategyFactory) {
		this.strategyFactory = Objects.requireNonNull(strategyFactory);
	}

	public UidGenerator getUidGenerator() {
		return uidGenerator;
	}

	public void setUidGenerator(UidGenerator uidGenerator) {
		this.uidGenerator = Objects.requireNonNull(uidGenerator);
	}

	public Eventbus getEventbus() {
		return eventbus;
	}

	public void setEventbus(Eventbus eventbus) {
		this.eventbus = Objects.requireNonNull(eventbus);
	}

	public Cacher getCacher() {
		return cacher;
	}

	public void setCacher(Cacher cacher) {
		this.cacher = cacher;
	}

	public Monitor getMonitor() {
		return monitor;
	}

	public void setMonitor(Monitor monitor) {
		this.monitor = Objects.requireNonNull(monitor);
	}

	public Transporter getTransporter() {
		return transporter;
	}

	public void setTransporter(Transporter transporter) {
		if (transporter == null || transporter instanceof NullTransporter) {
			this.transporter = null;
		} else {
			this.transporter = transporter;
		}
	}

	public ServiceInvoker getServiceInvoker() {
		return serviceInvoker;
	}

	public void setServiceInvoker(ServiceInvoker serviceInvoker) {
		this.serviceInvoker = Objects.requireNonNull(serviceInvoker);
	}

	public Metrics getMetrics() {
		return metrics;
	}

	public void setMetrics(Metrics metrics) {
		this.metrics = metrics;
	}

	public boolean isMetricsEnabled() {
		return metricsEnabled;
	}

	public void setMetricsEnabled(boolean metricsEnabled) {
		this.metricsEnabled = metricsEnabled;
	}

}