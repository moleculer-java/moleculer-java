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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Properties;

import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.repl.TextTable;
import services.moleculer.service.Name;

/**
 * Lists Java System Properties.
 */
@Name("props")
public class Props extends Command {

	public Props() {
		option("full", "show full-length keys");
	}

	@Override
	public String getDescription() {
		return "List of Java properties";
	}

	@Override
	public String getUsage() {
		return "props [options]";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 0;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintStream out, String[] parameters) throws Exception {
		TextTable table = new TextTable("Key", "Value");

		// Print long keys
		boolean full = false;
		if (parameters.length > 0) {
			full = "--full".equals(parameters[0]);
		}
		
		// Query and formatting
		Properties properties = System.getProperties();
		LinkedList<Object> list = new LinkedList<Object>();
		Enumeration<Object> keys = properties.keys();
		while (keys.hasMoreElements()) {
			list.addLast(keys.nextElement());
		}
		String[] names = new String[list.size()];
		list.toArray(names);
		Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
		String key, value;
		for (int n = 0; n < names.length; n++) {
			key = names[n];
			if (key.equals("line.separator")) {
				String sep = properties.getProperty(key);
				value = "";
				for (int i = 0; i < sep.length(); i++) {
					value = value + (int) sep.charAt(i) + " ";
				}
			} else {
				value = properties.getProperty(key);
				if (value == null || value.length() == 0) {
					value = "[undefined]";
				}
			}
			if (!full && key.length() > 23) {
				key = key.substring(0, 10) + "..." + key.substring(key.length() - 10, key.length());
			}
			table.addRow(true, key, value);
		}
		out.println(table);
	}
	
}