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
package services.moleculer.internal;

import static services.moleculer.util.CommonUtils.getNodeInfos;

import java.util.Arrays;
import java.util.HashMap;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.transporter.TcpTransporter;
import services.moleculer.transporter.Transporter;

/**
 * The broker contains some internal services to check the health of node or get
 * broker statistics. You can disable it with the internalServices: false broker
 * option within the constructor.
 */
@Name("$node")
public class NodeService extends Service {

	// --- PARENT BROKER ---
	
	protected ServiceBroker broker;
	
	// --- VARIABLES ---
	
	protected String localNodeID;
	
	// --- COMPONENTS ---
	
	protected Transporter transporter;
	
	// --- START SERVICE ---

	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
		this.broker = broker;
		this.transporter = broker.components().transporter();
		this.localNodeID = broker.nodeID();
	}

	// --- ACTIONS ---

	/**
	 * Implementation of the "$node.actions" action
	 */
	public Action actions = (ctx) -> {
		Tree root = new Tree();
		Tree list = root.putList("list");
		
		// Collect data
		Tree infos = getNodeInfos(broker, transporter);
		
		return list;
	};
	
	/**
	 * Implementation of the "$node.events" action
	 */
	public Action events = (ctx) -> {
		Tree root = new Tree();
		Tree list = root.putList("list");
		
		// Collect data
		Tree infos = getNodeInfos(broker, transporter);
		
		return list;
	};
	
	/**
	 * Implementation of the "$node.health" action
	 */
	public Action health = (ctx) -> {
		return null;
	};
	
	/**
	 * Implementation of the "$node.list" action
	 */
	public Action list = (ctx) -> {
		Tree root = new Tree();
		Tree list = root.putList("list");
		
		// Collect data
		Tree infos = getNodeInfos(broker, transporter);
		for (Tree info: infos) {
			Tree map = list.addMap();
			String nodeID = info.getName();
			map.put("id", nodeID);
			if (nodeID.equals(localNodeID)) {
				map.put("available", true);
				map.put("lastHeartbeatTime", System.currentTimeMillis());
				map.put("cpu", broker.components().monitor().getTotalCpuPercent());
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
		Tree root = new Tree();
		Tree list = root.putList("list");
		
		// Collect data
		Tree infos = getNodeInfos(broker, transporter);
		
		HashMap<String, HashMap<String, Tree>> serviceMap = new HashMap<>();
		for (Tree info : infos) {
			String nodeID = info.getName();
			Tree services = info.get("services");
			if (services == null || services.isNull()) {
				continue;
			}
			for (Tree service : services) {
				String serviceName = service.get("name", "unknown");
				HashMap<String, Tree> configs = serviceMap.get(serviceName);
				if (configs == null) {
					configs = new HashMap<String, Tree>();
					serviceMap.put(serviceName, configs);
				}
				configs.put(nodeID, service);
			}
		}
		
		// Sort names
		String[] serviceNames = new String[serviceMap.size()];
		serviceMap.keySet().toArray(serviceNames);
		Arrays.sort(serviceNames, String.CASE_INSENSITIVE_ORDER);
		
		for (String serviceName : serviceNames) {
			HashMap<String, Tree> configs = serviceMap.get(serviceName);
			if (configs == null) {
				continue;
			}
			
			
		}
		return list;
	};
	
}