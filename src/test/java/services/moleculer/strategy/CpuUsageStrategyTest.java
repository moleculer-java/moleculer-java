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
package services.moleculer.strategy;

import org.junit.Test;

import services.moleculer.ServiceBroker;
import services.moleculer.breaker.TestTransporter;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.LocalActionEndpoint;

public class CpuUsageStrategyTest extends StrategyTest {

	@Override
	public Strategy<LocalActionEndpoint> createStrategy(boolean preferLocal) throws Exception {
		CpuUsageStrategyFactory f = new CpuUsageStrategyFactory(preferLocal);
		f.started(br);
		return f.create();
	}

	// --- TEST METHODS ---

	@Test
	public void testSelection() throws Exception {
		TestTransporter tr = new TestTransporter();
		ConstantMonitor cm = new ConstantMonitor();
		ServiceBroker broker = ServiceBroker.builder().nodeID("node1").transporter(tr).monitor(cm).build();
		broker.start();

		CpuUsageStrategyFactory f = new CpuUsageStrategyFactory(false);
		f.started(broker);
		Strategy<LocalActionEndpoint> s = f.create();
		
		for (int i = 1; i <= 6; i++) {
			s.addEndpoint(createEndpoint(broker, "node" + i, "e" + i));
			tr.setCpuUsage("node" + i, i * 10);
		}

		assertEquals(6, s.getAllEndpoints().size());
		double sum = 0;
		for (int i = 0; i < 200; i++) {
			LocalActionEndpoint e = s.getEndpoint(null);
			sum += Integer.parseInt(e.getNodeID().substring(4));
		}
		double average = sum / 200d;
		assertTrue(average < 2.3);

		broker.stop();
	}

}