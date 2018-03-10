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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import services.moleculer.service.Name;

/**
 * JMX-based System Monitor. {@link SigarMonitor} is more accurate than this
 * monitor.
 *
 * @see SigarMonitor
 */
@Name("JMX System Monitor")
public class JmxMonitor extends Monitor {

	// --- PROPERTIES ---

	protected OperatingSystemMXBean mxBean;

	// --- CONSTRUCTOR ---

	public JmxMonitor() {
		if (!invalidMonitor.get()) {
			try {
				mxBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
			} catch (Exception cause) {
				logger.error("Unable to reach OperatingSystemMXBean!", cause);
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
		Double value = (Double) mxBean.getSystemLoadAverage();
		return (int) Math.max(value * 100d, 0d);
	}

	/**
	 * Returns the system CPU usage, in percents, between 0 and 100.
	 *
	 * @return total CPU usage of the current OS
	 */
	@Override
	protected long detectPID() throws Exception {
		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		int index = jvmName.indexOf('@');
		if (index < 1) {
			return 0;
		}
		return Long.parseLong(jvmName.substring(0, index));
	}

}