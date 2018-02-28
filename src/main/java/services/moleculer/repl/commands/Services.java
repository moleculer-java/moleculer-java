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
package services.moleculer.repl.commands;

import static services.moleculer.util.CommonUtils.getNodeInfos;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.repl.TextTable;
import services.moleculer.service.Name;
import services.moleculer.transporter.Transporter;

/**
 * List of services.
 */
@Name("services")
public class Services extends Command {

	public Services() {
		options.clear();
		option("local, -l", "only local services");
		option("skipinternal, -i", "skip internal services");
		option("details, -d", "print endpoints");
		option("all, -a", "list all (offline) services");
	}

	@Override
	public String getDescription() {
		return "List of services";
	}

	@Override
	public String getUsage() {
		return "services [options]";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 0;
	}
	
	@Override
	public void onCommand(ServiceBroker broker, PrintStream out, String[] parameters) throws Exception {

		// Parse parameters
		List<String> params = Arrays.asList(parameters);
		boolean local = params.contains("--local") || params.contains("-l");
		boolean skipinternal = params.contains("--skipinternal") || params.contains("-i");
		boolean details = params.contains("--details") || params.contains("-d");
		boolean all = params.contains("--all") || params.contains("-a");

		// Collect data
		Transporter transporter = broker.getConfig().getTransporter();
		Tree infos = getNodeInfos(broker, transporter);
		String localNodeID = broker.getNodeID();

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

		// Create table
		TextTable table = new TextTable("Service", "Version", "State", "Actions", "Events", "Nodes");
		for (String serviceName : serviceNames) {
			if (skipinternal && serviceName.startsWith("$")) {

				// Skip internal services
				continue;
			}

			// Create row
			ArrayList<String> row = new ArrayList<>(6);
			HashMap<String, Tree> configs = serviceMap.get(serviceName);
			if (configs == null) {
				continue;
			}

			// Add "Service" cell
			row.add(serviceName);

			// Add "Version" cell
			row.add("-");

			// Add "State" cell
			boolean online = false;
			for (Tree config : configs.values()) {
				String nodeID = config.get("nodeID", localNodeID);
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
			if (!all && !online) {
				continue;
			}
			row.add(online ? "OK" : "FAILED");

			// Add "Actions" cell
			HashSet<String> actionNames = new HashSet<>();
			for (Tree config : configs.values()) {
				Tree actions = config.get("actions");
				if (actions != null) {
					for (Tree action : actions) {
						actionNames.add(action.get("name", "unknown"));
					}
				}
			}
			row.add(Integer.toString(actionNames.size()));

			// Add "Events" cell
			HashSet<String> eventNames = new HashSet<>();
			for (Tree config : configs.values()) {
				Tree events = config.get("events");
				if (events != null) {
					for (Tree action : events) {
						eventNames.add(action.get("name", "unknown"));
					}
				}
			}
			row.add(Integer.toString(eventNames.size()));

			// Add "Nodes" cell
			String nodes = Integer.toString(configs.size());
			if (configs.containsKey(localNodeID)) {
				nodes = "(*) " + nodes;
			} else if (local) {

				// Skip non-local actions
				continue;
			}
			row.add(nodes);

			// Add row
			table.addRow(row);

			if (details) {
				String[] nodeIDArray = new String[configs.size()];
				configs.keySet().toArray(nodeIDArray);
				Arrays.sort(nodeIDArray, String.CASE_INSENSITIVE_ORDER);
				for (String nodeID : nodeIDArray) {
					if (localNodeID.equals(nodeID)) {
						table.addRow("", "", "OK", "", "", "<local>");
					} else {
						online = transporter == null ? true : transporter.isOnline(nodeID);
						table.addRow("", "", online ? "OK" : "FAILED", "", "", nodeID);
					}
				}
			}
		}
		out.println(table);
	}

}