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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.repl.TextTable;
import services.moleculer.service.Name;
import services.moleculer.transporter.Transporter;

/**
 * List of nodes.
 */
@Name("nodes")
public class Nodes extends Command {

	public Nodes() {
		option("details, -d", "detailed list");
		option("all, -a", "list all (offline) nodes");
	}

	@Override
	public String getDescription() {
		return "List of nodes";
	}

	@Override
	public String getUsage() {
		return "nodes [options]";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 0;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintStream out, String[] parameters) throws Exception {
		
		// Parse parameters
		List<String> params = Arrays.asList(parameters);
		boolean all = params.contains("--all") || params.contains("-a");
		boolean details = params.contains("--details") || params.contains("-d");

		// Collect data
		Transporter transporter = broker.components().transporter();
		Tree infos = getNodeInfos(broker, transporter);
		String localNodeID = broker.nodeID();

		// Create table
		TextTable table = new TextTable("Node ID", "Services", "Version", "Client", "IP", "State", "CPU");		
		for (Tree info : infos) {
			ArrayList<String> row = new ArrayList<>(7);

			// Add "Node ID" cell
			String nodeID = info.getName();
			if (localNodeID.equals(nodeID)) {
				row.add(nodeID + " (*)");
			} else {
				row.add(nodeID);
			}

			// Add "Services" cell
			Tree services = info.get("services");
			row.add(services == null ? "0" : Integer.toString(services.size()));

			// Add "Version" cell
			row.add(info.get("client.version", "unknown"));

			// Add "Client" cell
			row.add(info.get("client.type", "unknown"));

			// Add "IP" cell
			Tree ipList = info.get("ipList");
			int ipCount = ipList == null ? 0 : ipList.size();
			String firstIP = ipCount < 1 ? "unknown" : ipList.get(0).asString();
			if (ipCount > 1) {
				firstIP += " (+" + (ipCount - 1) + ')';
			}
			row.add(firstIP);

			// Add "State" cell (ONLINE / OFFLINE)
			boolean online;
			if (transporter == null) {
				online = true;
			} else {
				online = transporter.isOnline(nodeID);
			}
			if (!all && !online) {
				continue;
			}
			row.add(online ? "ONLINE" : "OFFLINE");

			// Add "CPU" cell
			if (transporter == null) {
				row.add(broker.components().monitor().getTotalCpuPercent() + "%");
			} else {
				Map<String, Transporter.NodeActivity> activities = transporter.getNodeActivities();
				Transporter.NodeActivity activity = activities.get(nodeID);
				if (activity == null) {
					if (localNodeID.equals(nodeID)) {
						row.add(broker.components().monitor().getTotalCpuPercent() + "%");
					} else {
						row.add("0%");
					}
				} else {
					row.add(activity.cpu + "%");
				}
			}

			// Add row
			table.addRow(row);
			
			// Service details
			if (details && services != null) {
				for (Tree service: services) {
					table.addRow("", service.get("name", "unknown"), "-", "", "", "", "");
				}
			}
		}
		out.println(table);
	}

	protected Tree getNodeInfos(ServiceBroker broker, Transporter transporter) {
		Tree infos = new Tree();
		if (transporter != null) {
			Set<String> nodeIDset = transporter.getAllNodeIDs();
			String[] nodeIDarray = new String[nodeIDset.size()];
			nodeIDset.toArray(nodeIDarray);
			Arrays.sort(nodeIDarray, String.CASE_INSENSITIVE_ORDER);
			for (String nodeID : nodeIDarray) {
				Tree info = transporter.getNodeInfo(nodeID);
				if (info == null) {
					continue;
				}
				infos.putObject(info.get("sender", "unknown"), info);
			}
		}
		if (infos.isEmpty()) {
			infos.putObject(broker.nodeID(), broker.components().registry().generateDescriptor());
		}
		return infos;
	}
	
}