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
package services.moleculer.repl;

import java.io.PrintStream;
import java.util.LinkedList;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

/**
 * Interface of all interactive console command implementations.
 */
public abstract class Command {

	// --- CONSTRUCTOR ---

	public Command() {
		option("help", "output usage information");
	}

	// --- BASIC METHODS ---

	public abstract String getDescription();

	public abstract String getUsage();

	public abstract int getNumberOfRequiredParameters();

	public abstract void onCommand(ServiceBroker broker, PrintStream out, String[] parameters) throws Exception;

	// --- OPTION HANDLING ---

	protected LinkedList<String[]> options = new LinkedList<>();

	protected void option(String option, String description) {
		options.add(new String[] { option, description });
	}

	// --- CONCATENATE ARGUMENTS ---

	protected Tree getPayload(String[] parameters) throws Exception {
		return getPayload(1, parameters);
	}

	protected Tree getPayload(int from, String[] parameters) throws Exception {
		if (parameters.length > from) {
			if (parameters[from].startsWith("'") || parameters[from].startsWith("{")
					|| parameters[from].startsWith("[")) {

				// JSON format
				StringBuilder tmp = new StringBuilder();
				for (int i = from; i < parameters.length; i++) {
					if (tmp.length() != 0) {
						tmp.append(' ');
					}
					tmp.append(parameters[i]);
				}
				String json = tmp.toString();
				if (json.startsWith("'")) {
					json = json.substring(1);
				}
				if (json.endsWith("'")) {
					json = json.substring(0, json.length() - 1);
				}
				return new Tree(json.trim());
			}
			Tree payload = new Tree();
			String name = null;
			for (int i = from; i < parameters.length; i++) {
				String p = parameters[i];
				if (name == null) {
					if (p.startsWith("--")) {
						p = p.substring(2);
					}
					name = p;
				} else {
					if ("true".equals(p)) {
						payload.put(name, true);
					} else if ("false".equals(p)) {
						payload.put(name, false);
					} else {
						try {
							if (p.contains(".")) {
								payload.put(name, Double.parseDouble(p));
							} else {
								payload.put(name, Integer.parseInt(p));
							}
						} catch (Exception notNumeric) {
							payload.put(name, p);
						}
					}
					name = null;
				}
			}
			return payload;
		}
		return new Tree();
	}

	// --- FORMATTERS ---
	
	protected void printChars(StringBuilder out, char c, int repeats) {
		for (int i = 0; i < repeats; i++) {
			out.append(c);
		}
	}
		
}