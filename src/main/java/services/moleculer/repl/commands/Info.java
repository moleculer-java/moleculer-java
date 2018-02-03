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

import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.nameOf;

import java.io.PrintStream;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import io.datatree.dom.TreeWriterRegistry;
import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.repl.TextTable;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.transporter.Transporter;

/**
 * Informations (node ID, IP address, etc.) about the ServiceBroker instance.
 */
@Name("info")
public class Info extends Command {

	// --- NUMBER FORMATTER ---

	protected DecimalFormat formatter = new DecimalFormat("#.##");

	// --- METHODS ---

	@Override
	public String getDescription() {
		return "Information about the broker";
	}

	@Override
	public String getUsage() {
		return "info";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 0;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintStream out, String[] parameters) throws Exception {

		// Get Runtime
		Runtime r = Runtime.getRuntime();

		// General informations
		printHeader(out, "General information");
		TextTable table = new TextTable(false, "Name", "Value");
		table.addRow("CPU", System.getProperty("os.arch", "unknown") + ", cores: " + r.availableProcessors());
		int cpuUsage = broker.components().monitor().getTotalCpuPercent();
		table.addRow("CPU usage", cpuUsage + "%");
		long total = r.totalMemory();
		long free = r.freeMemory();
		long used = total - free;
		int usedLen = (int) (20 * used / total);
		StringBuilder tmp = new StringBuilder();
		tmp.append('[');
		printChars(tmp, '#', usedLen);
		printChars(tmp, '-', 20 - usedLen);
		tmp.append("] ");
		synchronized (formatter) {
			tmp.append(formatter.format((double) (free / (double) 1024) / (double) 1024));
		}
		tmp.append(" MB free");
		table.addRow("Heap", tmp.toString());
		table.addRow("OS",
				System.getProperty("os.name", "unknown") + " (V" + System.getProperty("os.version", "?") + ')');
		try {
			table.addRow("IP", InetAddress.getLocalHost().getHostAddress());
		} catch (Exception ignored) {
		}
		table.addRow("Hostname", getHostName());
		table.addRow("Software version", ServiceBroker.SOFTWARE_VERSION);
		table.addRow("Protocol version", ServiceBroker.PROTOCOL_VERSION);
		table.addRow("Java VM version", System.getProperty("java.version", "unknown") + " from "
				+ System.getProperty("java.vm.vendor", "unknown vendor"));
		table.addRow("Java VM type", System.getProperty("java.vm.name", "unknown"));
		table.addRow("Date and time", SimpleDateFormat
				.getDateTimeInstance(SimpleDateFormat.FULL, SimpleDateFormat.MEDIUM, Locale.US).format(new Date()));
		TimeZone zone = TimeZone.getDefault();
		int rawOffset = zone.getRawOffset();
		String offset;
		if (rawOffset == 0) {
			offset = "+00:00";
		} else {
			long hours = TimeUnit.MILLISECONDS.toHours(rawOffset);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(rawOffset);
			minutes = Math.abs(minutes - TimeUnit.HOURS.toMinutes(hours));
			offset = String.format("%+03d:%02d", hours, Math.abs(minutes));
		}
		table.addRow("Time zone", zone.getDisplayName() + " (GMT" + offset + ')');
		out.println(table);

		// Broker properties
		Transporter t = broker.components().transporter();
		printHeader(out, "Properties of broker");
		table = new TextTable(false, "Name", "Value");
		table.addRow("Node ID", broker.nodeID());

		Tree info = broker.components().registry().generateDescriptor();
		Tree services = info.get("services");
		if (services != null && !services.isNull()) {
			int actionCounter = 0;
			int eventCounter = 0;
			for (Tree service : services) {
				Tree actions = service.get("actions");
				if (actions != null) {
					actionCounter += actions.size();
				}
				Tree events = service.get("events");
				if (events != null) {
					eventCounter += events.size();
				}
			}
			table.addRow("Services", Integer.toString(services.size()));
			table.addRow("Actions", Integer.toString(actionCounter));
			table.addRow("Events", Integer.toString(eventCounter));
		} else {
			table.addRow("Services", "0");
			table.addRow("Actions", "0");
			table.addRow("Events", "0");
		}

		table.addRow("", "");
		addType(table, "Invocation strategy", broker.components().strategy());
		addType(table, "Cacher", broker.components().cacher());
		if (t == null) {
			table.addRow("Nodes", "1");
		} else {
			table.addRow("Nodes", Integer.toString(t.getAllNodeIDs().size()));
		}
		table.addRow("", "");
		addType(table, "Context factory", broker.components().context());
		addType(table, "Event bus", broker.components().eventbus());
		addType(table, "System monitor", broker.components().monitor());
		addType(table, "Service registry", broker.components().registry());
		addType(table, "REPL console", broker.components().repl());
		addType(table, "UID generator", broker.components().uid());
		table.addRow("", "");

		addType(table, "Task executor", broker.components().executor());
		addType(table, "Task scheduler", broker.components().scheduler());
		out.println(table);

		// Transporter properties
		printHeader(out, "Properties of transporter");
		table = new TextTable(false, "Name", "Value");
		if (t != null) {
			Serializer s = t.getSerializer();
			addType(table, "Serializer", s);
			try {
				if (s != null && "JSON".equalsIgnoreCase(s.format())) {
					String readers = getAPIs(TreeReaderRegistry.getReader("json"), TreeReaderRegistry.getReadersByFormat("json"));
					String writers = getAPIs(TreeWriterRegistry.getWriter("json"), TreeWriterRegistry.getWritersByFormat("json"));
					if (readers.equals(writers)) {
						table.addRow("JSON implementations", readers);			
					} else {
						table.addRow("JSON readers", readers);
						table.addRow("JSON writers", writers);
					}
				}
			} catch (Exception ignored) {
			}
			addType(table, "Transporter", t);
		} else {
			table.addRow("Transporter", "<none>");
		}
		out.println(table);
	}

	protected String getAPIs(Object defaultAPI, Set<String> values) {
		String api = nameOf(defaultAPI, false);
		if (api.startsWith("json")) {
			api = api.substring(4);
		}
		StringBuilder list = new StringBuilder(32);
		for (String name : values) {
			int i = name.lastIndexOf(".Json");
			if (i > -1) {
				if (list.length() > 0) {
					list.append(", ");
				}
				name = name.substring(i + 5);
				list.append(name);
				if (name.equals(api)) {
					list.append(" (*)");
				}
			}
		}
		return list.toString();
	}
	
	protected void addType(TextTable table, String title, Object component) {
		if (component == null) {
			table.addRow(title, "<none>");
		} else {
			table.addRow(title, nameOf(component, false));
		}
	}

	protected void printHeader(PrintStream out, String header) {
		header = "  " + header;
		int len = header.length() + 2;
		StringBuilder line = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			line.append('=');
		}
		out.println(line);
		out.println(header);
		out.println(line);
		out.println();
	}

}