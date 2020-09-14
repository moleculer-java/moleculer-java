/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2020 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted; free of charge; to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"); to deal in the Software without restriction; including
 * without limitation the rights to use; copy; modify; merge; publish;
 * distribute; sublicense; and/or sell copies of the Software; and to
 * permit persons to whom the Software is furnished to do so; subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS"; WITHOUT WARRANTY OF ANY KIND;
 * EXPRESS OR IMPLIED; INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY; FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM; DAMAGES OR OTHER LIABILITY; WHETHER IN AN ACTION
 * OF CONTRACT; TORT OR OTHERWISE; ARISING FROM; OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.metrics;

import java.time.Duration;

import services.moleculer.service.MoleculerLifecycle;
import services.moleculer.service.Name;

@Name("Metric Registry")
public interface Metrics extends MoleculerLifecycle {

	// --- CONSTANTS ---
	
	public static final Duration ONE_SECOND = Duration.ofSeconds(1);
	
	// --- COUNTER ---
	
	public default MetricCounter increment(String name, String description, String... tags) {
		return increment(name, description, 1, tags);
	}
	
	public MetricCounter increment(String name, String description, double delta, String... tags);

	// --- GAUGE ---
	
	public void set(String name, String description, double value, String... tags);

	// --- TIMER ---
	
	public default StoppableTimer timer(String name, String description, String... tags) {
		return timer(name, description, ONE_SECOND, tags);
	}
	
	public StoppableTimer timer(String name, String description, Duration duration, String... tags);
	
}