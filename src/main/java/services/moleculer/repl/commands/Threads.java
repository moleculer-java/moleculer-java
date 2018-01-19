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

import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;

/**
 * Lists hierarchy of threads.
 */
@Name("threads")
public class Threads extends Command {

	// --- VARIABLES ---

	private String newLine = System.getProperty("line.separator", "\r\n");
	
	// --- METHODS ---
	
	@Override
	public String getDescription() {
		return "List of threads";
	}

	@Override
	public String getUsage() {
		return "threads";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 0;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintStream out, String[] parameters) throws Exception {
		ThreadGroup mainGroup = Thread.currentThread().getThreadGroup();
		while (mainGroup.getParent() != null) {
			mainGroup = mainGroup.getParent();
		}
		StringBuilder writer = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			try {
				writer.setLength(0);
				writer.append("Thread hierarchy:");
				writer.append(newLine);
				writer.append(newLine);			
				printThreadGroup(writer, mainGroup, 2);
				break;
			} catch (Exception e) {
			}
			try {
				Thread.sleep(100);
			} catch (Throwable t) {
				throw new ThreadDeath();
			}
		}
		out.print(writer.toString());
	}
	
	protected void printThreadGroup(StringBuilder tmp, ThreadGroup group, int tabs) throws Exception {
		printChars(tmp, ' ', tabs);
		tmp.append("Group: ");
		tmp.append(group.getName());
		if (group.isDaemon()) {
			tmp.append(" (Daemon)");
		}
		tmp.append(newLine);
		tabs++;
		int max = group.activeCount() + 50;
		Thread subThreads[] = new Thread[max];
		max = group.enumerate(subThreads, false);
		for (int n = 0; n < max; n++) {
			Thread thread = subThreads[n];
			String name = thread.getName();
			printChars(tmp, ' ', tabs);
			tmp.append(Integer.toString(thread.getPriority()));
			tmp.append(' ');
			tmp.append(name);
			if (name.startsWith("Thread")) {
				tmp.append(" <");
				tmp.append(thread.getClass().getName());
				tmp.append('>');
			}
			if (thread.isDaemon()) {
				tmp.append(" (Daemon)");
			}
			if (!thread.isAlive()) {
				tmp.append(" (Dead)");
			}
			if (thread.isInterrupted()) {
				tmp.append(" (Interrupted)");
			}
			tmp.append(newLine);
		}
		max = group.activeGroupCount() + 50;
		ThreadGroup subGroups[] = new ThreadGroup[max];
		max = group.enumerate(subGroups, false);
		tabs += 2;
		for (int idx = 0; idx < max; idx++) {
			ThreadGroup tg = subGroups[idx];
			printThreadGroup(tmp, tg, tabs);
		}
	}
	
}