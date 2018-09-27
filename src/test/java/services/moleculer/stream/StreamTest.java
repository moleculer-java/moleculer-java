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
package services.moleculer.stream;

import org.junit.Test;

import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.transporter.Transporter;

public abstract class StreamTest extends TestCase {

	// --- VARIABLES ---

	protected Transporter tr1;
	protected ServiceBroker br1;

	protected Transporter tr2;
	protected ServiceBroker br2;

	// --- ABSTRACT METHODS ---

	public abstract Transporter createTransporter();

	// --- COMMON TESTS ---

	@Test
	public void testStreams() throws Exception {
	}
	
	// --- SAMPLES ---
	
	@Name("receiver1")
	protected static final class Receiver1Service extends Service {

		public Action receive = ctx -> {
			return null;
		};

	}
	
	// --- UTILITIES ---

	@Override
	protected void setUp() throws Exception {

		// Create transporters
		tr1 = createTransporter();
		tr2 = createTransporter();

		// Enable debug messages
		tr1.setDebug(true);
		tr2.setDebug(true);

		// Create brokers
		br1 = ServiceBroker.builder().transporter(tr1).monitor(new ConstantMonitor()).nodeID("node1").build();
		br2 = ServiceBroker.builder().transporter(tr2).monitor(new ConstantMonitor()).nodeID("node2").build();

		// Start brokers
		br1.start();
		br2.start();
	}

	@Override
	protected void tearDown() throws Exception {
		if (br1 != null) {
			br1.stop();
		}
		if (br2 != null) {
			br2.stop();
		}
	}
	
}