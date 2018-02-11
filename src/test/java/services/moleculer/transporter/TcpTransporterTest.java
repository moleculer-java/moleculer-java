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
package services.moleculer.transporter;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.transporter.tcp.NodeDescriptor;

public class TcpTransporterTest extends TestCase {

	// --- VARIABLES ---
	
	protected TcpTransporter tr;
	protected ServiceBroker br;
	
	// --- GOSSIP REQUEST ---
	
	@Test
	public void testSendGossipRequest() throws Exception {		
		
		// Add "node2"
		tr.nodes.put("node2", getDescriptor(false, "node2"));
		
		Tree req = tr.sendGossipRequest();
		assertEquals(req.get("ver", "?"), ServiceBroker.PROTOCOL_VERSION);
		assertEquals(req.get("sender", "?"), "node1");
		assertEquals(req.get("online.node1[0]", -1), 1);
		assertEquals(req.get("online.node1[1]", -1), 0);
		assertEquals(req.get("online.node1[2]", -1), 0);
		assertEquals(2, req.get("online").size());
		assertEquals(req.get("online.node2[0]", -1), 1);
		assertEquals(req.get("online.node2[1]", -1), 0);
		assertEquals(req.get("online.node2[2]", -1), 0);
		
		// Add "node3"
		tr.nodes.put("node3", getDescriptor(false, "node3"));
		
		req = tr.sendGossipRequest();
		assertEquals(3, req.get("online").size());
		assertEquals(req.get("online.node3[0]", -1), 1);
		assertEquals(req.get("online.node3[1]", -1), 0);
		assertEquals(req.get("online.node3[2]", -1), 0);
		
		// Node3 -> offline
		tr.nodes.get("node3").markAsOffline();
		
		req = tr.sendGossipRequest();
		assertEquals(2, req.get("online").size());
		assertNull(req.get("online.node3"));
		assertEquals(2, req.get("offline.node3", -1));
		assertEquals(1, req.get("offline").size());
	}

	// --- UTILITIES --

	@Override
	protected void setUp() throws Exception {
		tr = new TcpTransporter();
		br = ServiceBroker.builder().transporter(tr).monitor(new ConstantMonitor()).nodeID("node1").build();
		br.start();
	}	

	@Override
	protected void tearDown() throws Exception {
		if (br != null) {
			br.stop();
		}
	}

	protected NodeDescriptor getDescriptor(boolean local, String nodeID) {
		Tree info = new Tree();
		info.put("seq", 1);
		info.put("port", 1);
		return new NodeDescriptor(nodeID, true, local, info);
	}
	
}