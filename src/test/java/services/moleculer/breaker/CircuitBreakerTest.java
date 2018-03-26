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
package services.moleculer.breaker;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.DefaultServiceRegistry;

public class CircuitBreakerTest extends TestCase {

	// --- VARIABLES ---

	protected TestTransporter tr;
	protected DefaultServiceRegistry sr;
	protected ServiceBroker br;

	// --- TEST METHODS ---

	protected int currentID = 1;

	public String getCurrentID() {
		String nodeID = "node" + currentID;
		currentID++;
		if (currentID > 9) {
			currentID = 0;
		}
		return nodeID;
	}

	@Test
	public void testRoundRobin() throws Exception {
		
		// Simple round-robin test
		for (int i = 0; i < 20; i++) {
			br.call("test.test", (Tree) null);
			assertTrue(tr.hasMessage(getCurrentID()));
			assertEquals(1, tr.getMessageCount());
			tr.clearMessages();
		}
		
		// Create fault
		br.call("test.test", (Tree) null);
		
		// "ver": "3",
	    // "sender": "local",
	    // "id": "pca:21",
	    // "action": "test.test",
	    // "channel": "MOL.REQ.node1"
		System.out.println(tr.getMessages());
	}

	public String createFaultResponse() throws Exception {

		// "ver": "3",
	    // "sender": "local",
	    // "id": "pca:21",
	    // "action": "test.test",
	    // "channel": "MOL.REQ.node1"
		Tree msg = tr.getMessages().get(0);
		String id = msg.get("id", "");
		String channel = msg.get("channel", "");
		int i = channel.lastIndexOf('.');
		String nodeID = channel.substring(i + 1);
		Tree rsp = new Tree();
		rsp.put("ver", "3");
		rsp.put("sender", nodeID);
		rsp.put("id", id);		
		rsp.put("success", false);
		rsp.put("data", (String) null);
		tr.received(channel, rsp);
		tr.clearMessages();
		return nodeID;
	}
	
	// --- SET UP ---

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void setUp() throws Exception {
		sr = new DefaultServiceRegistry();
		tr = new TestTransporter();
		br = ServiceBroker.builder().monitor(new ConstantMonitor()).registry(sr).transporter(tr).nodeID("local")
				.build();
		br.start();
		for (int i = 0; i < 10; i++) {
			Tree config = new Tree();
			config.put("nodeID", "node" + i);
			Tree actions = config.putMap("actions");
			LinkedHashMap<String, Object> action = new LinkedHashMap<>();
			action.put("name", "test.test");
			((Map) actions.asObject()).put("test.test", action);
			sr.addActions(config);
		}
	}

	// --- TEAR DOWN ---

	@Override
	protected void tearDown() throws Exception {
		if (br != null) {
			br.stop();
		}
	}

}