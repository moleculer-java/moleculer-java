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

import org.hyperic.sigar.Sigar;

import services.moleculer.service.Name;

/**
 * Sigar API-based System Monitor. You need to copy Sigar natives (DLLs, etc.)
 * into the directory which is defined by the "java.library.path" System
 * Property. This monitor is more accurate than the {@link JMXMonitor}.<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/org.fusesource/sigar<br>
 * compile (group: 'org.fusesource', name: 'sigar', version: '1.6.4')<br>
 * <br>
 * + Sigar DLLs (eg. "sigar-amd64-winnt.dll" and "sigar-x86-winnt.dll")
 */
@Name("Sigar System Monitor")
public class SigarMonitor extends Monitor {

	// --- SIGAR INSTANCE ---

	protected static Sigar sigar = new Sigar();

	// --- CONSTRUCTOR ---

	public SigarMonitor() {
		if (!invalidMonitor.get()) {
			try {
				sigar = new Sigar();
			} catch (Exception cause) {
				logger.error("Unable to reach Sigar API!", cause);
				invalidMonitor.set(true);
			}
		}
	}

	// --- SYSTEM MONITORING METHODS ---

	/**
	 * Returns the system CPU usage, in percents, between 0 and 100.
	 *
	 * @return total CPU usage of the current OS
	 */
	@Override
	protected int detectTotalCpuPercent() throws Exception {
		return (int) Math.max(sigar.getCpuPerc().getCombined() * 100d, 0d);
	}

	/**
	 * Returns the PID of Java VM.
	 *
	 * @return current Java VM's process ID
	 */
	protected long detectPID() throws Exception {
		return sigar.getPid();
	}

}