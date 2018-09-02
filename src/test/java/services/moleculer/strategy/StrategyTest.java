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

import java.util.HashSet;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.breaker.TestTransporter;
import services.moleculer.context.Context;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.DefaultServiceRegistry;
import services.moleculer.service.LocalActionEndpoint;

public abstract class StrategyTest extends TestCase {

	// --- PROPERTIES ---

	protected ServiceBroker br;

	// --- STRATEGY FACTORY ---

	protected abstract Strategy<LocalActionEndpoint> createStrategy(boolean preferLocal) throws Exception;

	// --- TEST METHODS ---

	@Test
	public void testStrategy() throws Exception {
		simpleTest(true);
		simpleTest(false);

		Strategy<LocalActionEndpoint> s = createStrategy(true);
		for (int i = 1; i <= 6; i++) {
			s.addEndpoint(createEndpoint(br, i < 4 ? "node1" : "node2", "e" + i));
		}
		assertEquals(6, s.getAllEndpoints().size());
		HashSet<LocalActionEndpoint> set = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			set.add(s.getEndpoint(null));
		}
		assertEquals(3, set.size());
		set.clear();
		for (int i = 0; i < 100; i++) {
			LocalActionEndpoint ep = s.getEndpoint("node1");
			assertEquals("node1", ep.getNodeID());
			set.add(ep);
		}
		assertEquals(3, set.size());
		set.clear();
		for (int i = 0; i < 100; i++) {
			LocalActionEndpoint ep = s.getEndpoint("node2");
			assertEquals("node2", ep.getNodeID());
			set.add(ep);
		}
		assertEquals(3, set.size());
		s.remove("node1");
		for (int i = 0; i < 10; i++) {
			assertNull(s.getEndpoint("node1"));
		}
		for (int i = 0; i < 10; i++) {
			assertNotNull(s.getEndpoint("node2"));
		}
		assertEquals(3, s.getAllEndpoints().size());
		s.remove("node2");
		for (int i = 0; i < 10; i++) {
			assertNull(s.getEndpoint("node2"));
		}
		assertEquals(0, s.getAllEndpoints().size());
	}

	protected void simpleTest(boolean preferLocal) throws Exception {
		Strategy<LocalActionEndpoint> s = createStrategy(preferLocal);
		for (int i = 1; i <= 5; i++) {
			s.addEndpoint(createEndpoint(br, "node1", "e" + i));
		}
		assertEquals(5, s.getAllEndpoints().size());
		HashSet<LocalActionEndpoint> set1 = new HashSet<>();
		HashSet<LocalActionEndpoint> set2 = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			LocalActionEndpoint ep = s.getEndpoint(null);
			if (i > 0 && i % 10 == 0) {
				assertTrue(set2.size() > 1);
				set2.clear();
			}
			set2.add(ep);
			set1.add(ep);
		}
		assertEquals(5, set1.size());
	}

	protected LocalActionEndpoint createEndpoint(ServiceBroker broker, String nodeID, String name) {
		Tree cfg = new Tree();
		cfg.put("name", name);
		DefaultServiceRegistry registry = (DefaultServiceRegistry) broker.getConfig().getServiceRegistry();
		LocalActionEndpoint e = new LocalActionEndpoint(registry, broker.getConfig().getExecutor(), nodeID,
				cfg, new Action() {

					@Override
					public Object handler(Context ctx) throws Exception {
						return null;
					}

				});
		return e;
	}

	// --- START BROKER ---

	@Override
	protected void setUp() throws Exception {
		TestTransporter tr = new TestTransporter();
		br = ServiceBroker.builder().nodeID("node1").transporter(tr).monitor(new ConstantMonitor()).build();
		br.start();
	}

	// --- STOP BROKER ---

	@Override
	protected void tearDown() throws Exception {
		if (br != null) {
			br.stop();
			br = null;
		}
	}

}