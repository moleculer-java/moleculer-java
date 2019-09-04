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
package services.moleculer.internal;

import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.getNodeInfos;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.Context;
import services.moleculer.monitor.Monitor;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.transporter.TcpTransporter;
import services.moleculer.transporter.Transporter;

/**
 * The broker contains some internal services to check the health of node or get
 * broker statistics. You can disable it with the "internalServices: false"
 * broker option with the ServiceBroker.builder().
 */
@Name("$node")
public class NodeService extends Service {

	// --- VARIABLES ---

	protected static final long startedAt = System.currentTimeMillis();

	protected String localNodeID;

	// --- COMPONENTS ---

	protected Transporter transporter;
	protected Monitor monitor;

	// --- ACTIONS ---

	/**
	 * Implementation of the "$node.actions" action
	 */
	public Action actions = (ctx) -> {

		// Parse input parameters
		boolean onlyLocal = getParameter(ctx, "onlyLocal", false);
		boolean skipInternal = getParameter(ctx, "skipInternal", false);
		boolean withEndpoints = getParameter(ctx, "withEndpoints", false);

		// Create response structure
		Tree root = new Tree();
		Tree list = root.putList("list");

		// Collect data
		Tree infos = getNodeInfos(broker, transporter);

		HashMap<String, HashSet<String>> actionMap = new HashMap<>();
		HashMap<String, Tree> configs = new HashMap<>();

		for (Tree info : infos) {
			String nodeID = info.getName();
			Tree services = info.get("services");
			if (services == null || services.isNull()) {
				continue;
			}
			for (Tree service : services) {
				Tree actions = service.get("actions");
				if (actions == null || actions.isNull()) {
					continue;
				}
				for (Tree action : actions) {
					String actionName = action.get("name", "unknown");
					HashSet<String> nodeSet = actionMap.get(actionName);
					if (nodeSet == null) {
						nodeSet = new HashSet<String>();
						actionMap.put(actionName, nodeSet);
					}
					nodeSet.add(nodeID);
					configs.putIfAbsent(actionName, action);
				}
			}
		}

		for (String actionName : actionMap.keySet()) {
			if (skipInternal && actionName.startsWith("$")) {

				// Skip internal actions
				continue;
			}
			HashSet<String> nodeSet = actionMap.get(actionName);
			if (nodeSet == null) {
				continue;
			}
			if (onlyLocal && !nodeSet.contains(localNodeID)) {

				// Skip non-local services
				continue;
			}
			Tree map = list.addMap();
			map.put("name", actionName);
			map.put("count", nodeSet.size());
			map.put("hasLocal", nodeSet.contains(localNodeID));

			// Add "available" flag
			boolean online = false;
			for (String nodeID : nodeSet) {
				if (transporter == null) {
					online = true;
					break;
				} else {
					online = transporter.isOnline(nodeID);
					if (online) {
						break;
					}
				}
			}
			map.put("available", online);

			// Add actions
			Tree action = configs.get(actionName);
			if (action != null) {
				map.putObject("action", action);
			}

			// Add endpoints
			if (withEndpoints) {
				Tree endpoints = map.putList("endpoints");
				for (String nodeID : nodeSet) {
					Tree endpoint = endpoints.addMap();
					endpoint.put("nodeID", nodeID);
					if (transporter == null) {
						endpoint.put("state", true);
					} else {
						endpoint.put("state", transporter.isOnline(nodeID));
					}
				}
			}
		}

		return list;
	};

	/**
	 * Implementation of the "$node.events" action
	 */
	public Action events = (ctx) -> {

		// Parse input parameters
		boolean onlyLocal = getParameter(ctx, "onlyLocal", false);
		boolean skipInternal = getParameter(ctx, "skipInternal", false);
		boolean withEndpoints = getParameter(ctx, "withEndpoints", false);

		// Create response structure
		Tree root = new Tree();
		Tree list = root.putList("list");

		// Collect data
		Tree infos = getNodeInfos(broker, transporter);

		HashMap<String, HashSet<String>> eventMap = new HashMap<>();
		HashMap<String, Tree> configs = new HashMap<>();
		for (Tree info : infos) {
			String nodeID = info.getName();
			Tree services = info.get("services");
			if (services == null || services.isNull()) {
				continue;
			}
			for (Tree service : services) {
				Tree events = service.get("events");
				if (events == null || events.isNull()) {
					continue;
				}
				for (Tree event : events) {
					String eventName = event.get("name", "unknown");
					HashSet<String> nodeSet = eventMap.get(eventName);
					if (nodeSet == null) {
						nodeSet = new HashSet<String>();
						eventMap.put(eventName, nodeSet);
					}
					nodeSet.add(nodeID);
					configs.putIfAbsent(eventName, event);
				}
			}
		}

		for (String eventName : eventMap.keySet()) {
			if (skipInternal && eventName.startsWith("$")) {

				// Skip internal events
				continue;
			}
			HashSet<String> nodeSet = eventMap.get(eventName);
			if (nodeSet == null) {
				continue;
			}
			if (onlyLocal && !nodeSet.contains(localNodeID)) {

				// Skip non-local services
				continue;
			}
			Tree map = list.addMap();
			Tree event = configs.get(eventName);
			map.put("name", eventName);
			if (event != null) {
				map.putObject("group", event.get("group"));
			}
			map.put("count", nodeSet.size());
			map.put("hasLocal", nodeSet.contains(localNodeID));
			boolean online = false;
			for (String nodeID : nodeSet) {
				if (transporter == null) {
					online = true;
				} else {
					online = transporter.isOnline(nodeID);
				}
				if (online) {
					break;
				}
			}
			map.put("available", online);
			if (event != null) {
				map.putObject("event", event);
			}

			// Add endpoints
			if (withEndpoints) {
				Tree endpoints = map.putList("endpoints");
				for (String nodeID : nodeSet) {
					Tree endpoint = endpoints.addMap();
					endpoint.put("nodeID", nodeID);
					if (transporter == null) {
						endpoint.put("state", true);
					} else {
						endpoint.put("state", transporter.isOnline(nodeID));
					}
				}
			}
		}
		return list;
	};

	/**
	 * Implementation of the "$node.health" action
	 */
	public Action health = (ctx) -> {

		// Create response structure
		Tree root = new Tree();

		// Get Runtime
		Runtime r = Runtime.getRuntime();

		// CPU block
		Tree cpu = root.putMap("cpu");
		int cpuUsage = monitor.getTotalCpuPercent();

		cpu.put("cores", r.availableProcessors());
		cpu.put("utilization", cpuUsage);

		// OS block
		Tree os = root.putMap("os");
		os.put("type", System.getProperty("os.name", "unknown"));
		os.put("release", System.getProperty("os.version", "unknown"));
		os.put("hostname", getHostName());
		os.put("arch", System.getProperty("os.arch", "unknown"));

		// User block
		Tree user = os.putMap("user");
		user.put("username", System.getProperty("user.name", "unknown"));
		user.put("homedir", System.getProperty("user.home", "unknown"));
		user.put("shell", System.getProperty("user.script", "unknown"));

		// Process block
		Tree process = root.putMap("process");
		process.put("pid", monitor.getPID());

		// Memory block
		Tree memory = process.putMap("memory");
		long total = r.totalMemory();
		long free = r.freeMemory();
		long used = total - free;
		memory.put("heapTotal", total);
		memory.put("heapUsed", used);
		process.put("uptime", System.currentTimeMillis() - startedAt);

		// Client block
		Tree descriptor = broker.getConfig().getServiceRegistry().getDescriptor();
		root.putObject("client", descriptor.get("client"));

		// Net block
		Tree net = root.putMap("net");
		net.putObject("ip", descriptor.get("ipList"));

		// Time block
		Tree time = root.putMap("time");
		long now = System.currentTimeMillis();
		time.put("now", now);
		time.put("iso", Instant.ofEpochMilli(now).toString());

		SimpleDateFormat utc = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
		utc.setTimeZone(TimeZone.getTimeZone("GMT"));
		time.put("utc", utc.format(new Date(now)));

		return root;
	};

	/**
	 * Implementation of the "$node.list" action
	 */
	public Action list = (ctx) -> {

		// Create response structure
		Tree root = new Tree();
		Tree list = root.putList("list");

		// Collect data
		Tree infos = getNodeInfos(broker, transporter);
		for (Tree info : infos) {
			Tree map = list.addMap();
			String nodeID = info.getName();
			map.put("id", nodeID);
			if (nodeID.equals(localNodeID)) {
				map.put("available", true);
				map.put("lastHeartbeatTime", System.currentTimeMillis());
				map.put("cpu", monitor.getTotalCpuPercent());
				if (transporter != null && transporter instanceof TcpTransporter) {
					TcpTransporter tt = (TcpTransporter) transporter;
					map.put("port", tt.getCurrentPort());
				} else {
					map.put("port", 0);
				}
			} else {
				map.put("available", transporter.isOnline(nodeID));
				map.put("lastHeartbeatTime", transporter.getLastHeartbeatTime(nodeID));
				map.put("cpu", transporter.getCpuUsage(nodeID));
				map.put("port", info.get("port", 0));
			}
			map.copyFrom(info, (child) -> {
				String name = child.getName();
				return "hostname".equals(name) || "ipList".equals(name) || "client".equals(name);
			});
		}
		return list;
	};

	/**
	 * Implementation of the "$node.services" action
	 */
	public Action services = (ctx) -> {

		// Parse input parameters
		boolean onlyLocal = getParameter(ctx, "onlyLocal", false);
		boolean skipInternal = getParameter(ctx, "skipInternal", false);
		boolean withActions = getParameter(ctx, "withActions", false);

		// Create response structure
		Tree root = new Tree();
		Tree list = root.putList("list");

		// Collect data
		Tree infos = getNodeInfos(broker, transporter);

		HashMap<String, HashSet<String>> serviceMap = new HashMap<>();
		HashMap<String, Tree> configs = new HashMap<>();
		for (Tree info : infos) {
			String nodeID = info.getName();
			Tree services = info.get("services");
			if (services == null || services.isNull()) {
				continue;
			}
			for (Tree service : services) {
				String serviceName = service.get("name", "unknown");
				HashSet<String> nodeSet = serviceMap.get(serviceName);
				if (nodeSet == null) {
					nodeSet = new HashSet<String>();
					serviceMap.put(serviceName, nodeSet);
				}
				nodeSet.add(nodeID);
				configs.putIfAbsent(serviceName, service);
			}
		}

		for (String serviceName : serviceMap.keySet()) {
			if (skipInternal && serviceName.startsWith("$")) {

				// Skip internal events
				continue;
			}
			HashSet<String> nodeSet = serviceMap.get(serviceName);
			if (nodeSet == null) {
				continue;
			}
			if (onlyLocal && !nodeSet.contains(localNodeID)) {

				// Skip non-local services
				continue;
			}

			Tree map = list.addMap();
			map.put("name", serviceName);
			map.putObject("nodes", nodeSet);

			Tree service = configs.get(serviceName);
			if (service != null) {
				map.putObject("settings", service.get("settings"));
				map.putObject("metadata", service.get("metadata"));
				if (withActions) {
					map.putObject("actions", service.get("actions"));
				}
			}
		}
		return list;
	};

	// --- START SERVICE ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Set components
		ServiceBrokerConfig cfg = broker.getConfig();
		this.transporter = cfg.getTransporter();
		this.monitor = cfg.getMonitor();

		// Set local nodeID
		this.localNodeID = broker.getNodeID();
	}
	
	// --- UTILS ---

	protected boolean getParameter(Context ctx, String name, boolean defaultValue) {
		if (ctx == null || ctx.params == null) {
			return defaultValue;
		}
		return ctx.params.get(name, defaultValue);
	}

}