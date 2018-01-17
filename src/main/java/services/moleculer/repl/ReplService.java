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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.MoleculerComponent;

public class ReplService implements MoleculerComponent, Runnable {

	// --- PROPERTIES ---

	/**
	 * Java package(s) where your Commands are located.
	 */
	private String[] packagesToScan = new String[] { "services.moleculer.repl" };

	// --- COMPONENTS ---

	protected ServiceBroker broker;

	// --- MAP OF THE REGISTERED COMMANDS ---

	protected HashMap<String, Command> commands = new HashMap<>(64);

	// --- CONSTRUCTORS ---

	public ReplService() {
	}

	public ReplService(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	// --- START ---

	protected ExecutorService executor;

	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
		this.broker = broker;

		// Process config
		Tree packagesNode = config.get("packagesToScan");
		if (packagesNode != null) {
			if (packagesNode.isPrimitive()) {

				// List of packages
				String value = packagesNode.asString().trim();
				packagesToScan = value.split(",");
			} else {

				// Array structure of packages
				List<String> packageList = packagesNode.asList(String.class);
				if (!packageList.isEmpty()) {
					packagesToScan = new String[packageList.size()];
					packageList.toArray(packagesToScan);
				}
			}
		}
		
		// Check required "packagesToScan" parameter
		if (packagesToScan == null || packagesToScan.length == 0) {
			//logger.warn("The \"packagesToScan\" parameter is required for the Dependency Injector!");
			//logger.warn("Please specify the proper Java package(s) where your Services are located.");
			return;
		}
		
		// Add commands
		
		// Start standard input reader
		executor = Executors.newSingleThreadExecutor();
		executor.execute(this);
	}

	// --- COMMAND PROCESSOR LOOP ---

	protected String lastCommand = "help";

	@Override
	public void run() {
		try {
			Thread.sleep(3000);
			while (!Thread.currentThread().isInterrupted()) {
				SystemInReader reader = new SystemInReader();
				reader.start();
				reader.join();
				String command = reader.getLine();
				if (command.length() > 0) {
					if ("r".equalsIgnoreCase(command) || "repeat".equalsIgnoreCase(command)) {
						command = lastCommand;
					}
					onCommand(System.out, command);
					lastCommand = command;
				}
			}
		} catch (InterruptedException i) {

			// Interrupt
		} catch (Throwable cause) {

			// Never happens
			cause.printStackTrace();
		}
	}

	// --- COMMAND PROCESSOR ---

	protected void onCommand(PrintStream out, String command) throws Exception {
		try {
			if (command == null) {
				return;
			}
			command = command.trim();
			if (command.length() == 0) {
				return;
			}
			String[] tokens = command.split(" ");
			String cmd = tokens[0].toLowerCase();
			if ("help".equals(cmd) || "h".equals(cmd)) {
				if (tokens.length > 1) {
					String name = tokens[1].toLowerCase();
					Command impl = commands.get(name);
					if (impl == null) {
						out.println("The \"" + name + "\" command is unknown!");
						out.println("Type \"help\" or \"h\" for more information.");
						return;
					}
					out.println("Sample of the usage:");
					out.println(impl.getSample());
					return;
				}
				String[] names = new String[commands.size()];
				commands.keySet().toArray(names);
				Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
				StringTable table = new StringTable("List of commands", "Command", "Description");
				for (String name : names) {
					Command impl = commands.get(name);
					table.addRow(name, impl.getDescription());
				}
				table.printTable(out);
				out.println();
				out.println("  Type \"repeat\" or \"r\"  to repeat the execution of the last command.");
				out.println("  Type \"help <command>\" to display the usage of a specified command.");
				out.println();
				return;
			}
			Command impl = commands.get(cmd);
			if (impl == null) {
				out.println("The \"" + cmd + "\" command is unknown!");
				out.println("Type \"help\" or \"h\" for more information.");
				return;
			}
			String[] args = new String[tokens.length - 1];
			System.arraycopy(tokens, 1, args, 0, args.length);
			if (impl.getNumberOfRequiredParameters() > args.length) {
				out.println("Unable to call \"" + cmd + "\" command!");
				out.println("Too few command parameters (" + args.length + " < " + impl.getNumberOfRequiredParameters()
						+ ")!");
				out.println("Type \"help " + cmd + "\" for more information.");
				return;
			}
			impl.onCommand(broker, out, args);
		} catch (Exception cause) {
			out.println("Command execution failed!");
			cause.printStackTrace(out);
		}
	}

	// --- STOP ---

	@Override
	public void stop() {
		if (executor != null) {
			try {
				executor.shutdownNow();
			} catch (Exception ignored) {

				// Do nothing
			}
			executor = null;
		}
		commands.clear();
	}

	// --- GETTERS AND SETTERS ---

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	public void setPackagesToScan(String[] packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

}