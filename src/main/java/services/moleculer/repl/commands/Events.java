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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.repl.TextTable;
import services.moleculer.service.Name;
import services.moleculer.transporter.Transporter;

/**
 * List of event listeners.
 */
@Name("events")
public class Events extends Nodes {

	public Events() {
		options.clear();
		option("local, -l", "only local event listeners");
		option("skipinternal, -i", "skip internal event listeners");
		option("details, -d", "print endpoints");
		option("all, -a", "list all (offline) event listeners");
	}

	@Override
	public String getDescription() {
		return "List of event listeners";
	}

	@Override
	public String getUsage() {
		return "events [options]";
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
		Transporter transporter = broker.components().transporter();
		Tree infos = getNodeInfos(broker, transporter);
		String localNodeID = broker.nodeID();

		HashMap<String, HashSet<String>> eventMap = new HashMap<>();
		for (Tree info : infos) {
			Tree services = info.get("services");
			if (services == null || services.isNull()) {
				continue;
			}
			for (Tree service : services) {
				Tree events = service.get("events");
				if (events == null || events.isNull()) {
					continue;
				}
				String nodeID = info.get("sender", "unknown");
				for (Tree event : events) {
					String eventName = event.get("name", "unknown");
					HashSet<String> nodeSet = eventMap.get(eventName);
					if (nodeSet == null) {
						nodeSet = new HashSet<String>();
						eventMap.put(eventName, nodeSet);
					}
					nodeSet.add(nodeID);
				}
			}
		}

		// Sort names
		String[] eventNames = new String[eventMap.size()];
		eventMap.keySet().toArray(eventNames);
		Arrays.sort(eventNames, String.CASE_INSENSITIVE_ORDER);

		// Create table
		TextTable table = new TextTable("Event", "Nodes");
		for (String eventName : eventNames) {
			if (skipinternal && eventName.startsWith("$")) {

				// Skip internal events
				continue;
			}

			// Create row
			ArrayList<String> row = new ArrayList<>(2);
			HashSet<String> nodeSet = eventMap.get(eventName);
			if (nodeSet == null) {
				continue;
			}
			if (!all) {
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
				if (!online) {
					
					// Not live
					continue;
				}
			}

			// Add "Event" cell
			row.add(eventName);

			// Add "Nodes" cell
			String nodes = Integer.toString(nodeSet.size());
			if (nodeSet.contains(localNodeID)) {
				nodes = "(*) " + nodes;
			} else if (local) {

				// Skip non-local actions
				continue;
			}
			row.add(nodes);

			// Add row
			table.addRow(row);

			if (details) {
				String[] nodeIDArray = new String[nodeSet.size()];
				nodeSet.toArray(nodeIDArray);
				Arrays.sort(nodeIDArray, String.CASE_INSENSITIVE_ORDER);
				for (String nodeID : nodeIDArray) {
					if (localNodeID.equals(nodeID)) {
						table.addRow("", "<local>");
					} else {
						table.addRow("", nodeID);
					}
				}
			}
		}
		out.println(table);
	}

}