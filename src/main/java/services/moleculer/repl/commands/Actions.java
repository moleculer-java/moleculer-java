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
 * List of actions.
 */
@Name("actions")
public class Actions extends Nodes {

	public Actions() {
		options.clear();
		option("local, -l", "only local actions");
		option("skipinternal, -i", "skip internal actions");
		option("details, -d", "print endpoints");
		option("all, -a", "list all (offline) actions");
	}

	@Override
	public String getDescription() {
		return "List of actions";
	}

	@Override
	public String getUsage() {
		return "actions [options]";
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

		HashMap<String, HashMap<String, Tree>> actionMap = new HashMap<>();
		for (Tree info : infos) {
			Tree services = info.get("services");
			if (services == null || services.isNull()) {
				continue;
			}
			for (Tree service : services) {
				Tree actions = service.get("actions");
				if (actions == null || actions.isNull()) {
					continue;
				}
				String nodeID = info.get("sender", "unknown");
				for (Tree action : actions) {
					String actionName = action.get("name", "unknown");
					HashMap<String, Tree> configs = actionMap.get(actionName);
					if (configs == null) {
						configs = new HashMap<String, Tree>();
						actionMap.put(actionName, configs);
					}
					configs.put(nodeID, action);
				}
			}
		}

		// Sort names
		String[] actionNames = new String[actionMap.size()];
		actionMap.keySet().toArray(actionNames);
		Arrays.sort(actionNames, String.CASE_INSENSITIVE_ORDER);

		// Create table
		TextTable table = new TextTable("Action", "Nodes", "State", "Cached", "Params");
		for (String actionName : actionNames) {
			if (skipinternal && actionName.startsWith("$")) {

				// Skip internal actions
				continue;
			}

			// Create row
			ArrayList<String> row = new ArrayList<>(5);
			HashMap<String, Tree> configs = actionMap.get(actionName);
			if (configs == null) {
				continue;
			}

			// Add "Action" cell
			row.add(actionName);

			// Add "Nodes" cell
			String nodes = Integer.toString(configs.size());
			if (configs.containsKey(localNodeID)) {
				nodes = "(*) " + nodes;
			} else if (local) {

				// Skip non-local actions
				continue;
			}
			row.add(nodes);

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

			// Add "Cached" cell
			boolean cache = false;
			for (Tree config : configs.values()) {
				if (config.get("cache", false)) {
					cache = true;
					break;
				}
			}
			row.add(cache ? "Yes" : "No");

			// Add "Params" cell
			HashSet<String> paramSet = new HashSet<>();
			for (Tree config : configs.values()) {
				Tree paramsBlock = config.get("params");
				if (paramsBlock == null || paramsBlock.isNull()) {
					continue;
				}
				for (Tree param : paramsBlock) {
					paramSet.add(param.getName());
				}
			}
			String[] paramArray = new String[paramSet.size()];
			paramSet.toArray(paramArray);
			Arrays.sort(paramArray, String.CASE_INSENSITIVE_ORDER);
			StringBuilder paramList = new StringBuilder(64);
			for (String param : paramArray) {
				if (paramList.length() > 0) {
					paramList.append(", ");
				}
				paramList.append(param);
			}
			row.add(paramList.toString());

			// Add row
			table.addRow(row);

			if (details) {
				String[] nodeIDArray = new String[configs.size()];
				configs.keySet().toArray(nodeIDArray);
				Arrays.sort(nodeIDArray, String.CASE_INSENSITIVE_ORDER);
				for (String nodeID : nodeIDArray) {
					if (localNodeID.equals(nodeID)) {
						table.addRow("", "<local>", "OK", "", "");
					} else {
						online = transporter == null ? true : transporter.isOnline(nodeID);
						table.addRow("", nodeID, online ? "OK" : "FAILED", "", "");
					}
				}
			}
		}
		out.println(table);
	}

}