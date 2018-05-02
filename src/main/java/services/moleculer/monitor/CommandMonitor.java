/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
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
package services.moleculer.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import services.moleculer.service.Name;

/**
 * CPU monitor, which detects the current CPU usage via command line.
 */
@Name("OS Command-based System Monitor")
public class CommandMonitor extends Monitor {

	// --- PROPERTIES ---

	protected String command;

	// --- CONSTUCTORS ---

	public CommandMonitor() {
	}

	public CommandMonitor(String command) {
		this.command = command;
	}

	// --- SYSTEM MONITORING METHODS ---

	/**
	 * Returns the system CPU usage, in percents, between 0 and 100.
	 *
	 * @return total CPU usage of the current OS
	 */
	@Override
	protected int detectTotalCpuPercent() throws Exception {
		Process process = null;
		try {

			// Execute command
			String cmd;
			if (command == null || command.isEmpty()) {
				String os = System.getProperty("os.name").toLowerCase();
				if (os.indexOf("win") >= 0) {

					// Windows command to query the actual CPU usage
					cmd = "wmic cpu get loadpercentage";

				} else {

					// Linux command to query the actual CPU usage
					cmd = "top -b -n2 -p 1 | fgrep \"Cpu(s)\" | tail -1 | awk -F'id,' -v prefix=\"$prefix\" "
							+ "'{ split($1, vs, \",\"); v=vs[length(vs)]; sub(\"%\", \"\", v); printf \"%s%.1f%%\n\", prefix, 100 - v }'";
				}
			} else {
				cmd = command;
			}
			process = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder tmp = new StringBuilder();
			String line = "";
			while ((line = reader.readLine()) != null) {
				tmp.setLength(0);
				char[] chars = line.toCharArray();
				for (char c : chars) {
					if (Character.isDigit(c)) {
						tmp.append(c);
					}
					if (c == ',' || c == '.') {
						tmp.append('.');
					}
				}
				if (tmp.length() > 0) {
					try {
						return (int) Double.parseDouble(tmp.toString());
					} catch (Exception ignored) {
					}
				}
			}
		} finally {
			if (process != null) {
				try {
					process.destroy();
				} catch (Exception ignored) {
				}
			}
		}
		return 0;
	}

	/**
	 * Returns the PID of Java VM.
	 *
	 * @return current Java VM's process ID
	 */
	protected long detectPID() {

		// Use generated, "fake" PID
		return 0;
	}

	// --- GETTERS / SETTERS ---

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

}