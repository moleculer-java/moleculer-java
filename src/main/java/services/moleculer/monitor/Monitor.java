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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.moleculer.service.Name;

/**
 * Base superclass of all System Monitor implementations.
 *
 * @see SigarMonitor
 * @see JmxMonitor
 */
@Name("Monitor")
public abstract class Monitor {

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- PROPERTIES ---

	/**
	 * CPU cache timeout in MILLISECONDS
	 */
	protected long cacheTimeout = 1000;

	// --- JVM'S SHARED CACHES ---

	/**
	 * Cached process ID
	 */
	protected static AtomicLong cachedPID = new AtomicLong();

	/**
	 * Cached CPU usage
	 */
	protected static int cachedCPU;

	/**
	 * Timestamp of CPU detection
	 */
	protected static long cpuDetectedAt;

	// --- PUBLIC SYSTEM MONITORING METHODS ---

	protected static final AtomicBoolean invalidMonitor = new AtomicBoolean();

	/**
	 * Returns the cached system CPU usage, in percents, between 0 and 100.
	 *
	 * @return total CPU usage of the current OS
	 */
	public int getTotalCpuPercent() {
		if (invalidMonitor.get()) {
			return 0;
		}
		long now = System.currentTimeMillis();
		int cpu;
		synchronized (Monitor.class) {
			if (now - cpuDetectedAt > cacheTimeout) {
				try {
					cachedCPU = detectTotalCpuPercent();
				} catch (Throwable cause) {
					logger.info("Unable to detect CPU usage!", cause);
					invalidMonitor.set(true);
				}
				cpuDetectedAt = now;
			}
			cpu = cachedCPU;
		}
		return cpu;
	}

	/**
	 * Returns the cached PID of Java VM.
	 *
	 * @return current Java VM's process ID
	 */
	public long getPID() {
		long currentPID = cachedPID.get();
		if (currentPID != 0) {
			return currentPID;
		}
		try {
			currentPID = detectPID();
		} catch (Throwable cause) {
			logger.info("Unable to detect process ID!", cause);
		}
		if (currentPID == 0) {
			currentPID = System.nanoTime();
			if (!cachedPID.compareAndSet(0, currentPID)) {
				currentPID = cachedPID.get();
			}
		} else {
			cachedPID.set(currentPID);
		}
		return currentPID;
	}

	// --- ABSTRACT SYSTEM MONITORING METHODS ---

	/**
	 * Returns the system CPU usage, in percents, between 0 and 100.
	 *
	 * @return total CPU usage of the current OS
	 * 
	 * @throws Exception
	 *             any I/O or missing DLL exception
	 */
	protected abstract int detectTotalCpuPercent() throws Exception;

	/**
	 * Returns the system CPU usage, in percents, between 0 and 100.
	 *
	 * @return total CPU usage of the current OS
	 * 
	 * @throws Exception
	 *             any I/O or missing DLL exception
	 */
	protected abstract long detectPID() throws Exception;

}