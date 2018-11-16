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
package services.moleculer.internal;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

public class NodeServiceTest extends TestCase {

	// --- VARIABLES ---

	protected ServiceBroker br;

	// --- TEST METHODS ---

	@Test
	public void testInternalService() throws Exception {
		Tree rsp, params;

		// Actions
		params = new Tree();
		params.put("onlyLocal", false);
		params.put("skipInternal", false);
		params.put("withEndpoints", true);
		rsp = br.call("$node.actions", params).waitFor();

		assertEquals(5, rsp.size());
		assertEquals(6, rsp.get("[0]").size());
		assertEquals(rsp.get("[0].name", "?"), "$node.events");
		assertEquals(rsp.get("[0].count", "?"), "1");
		assertEquals(rsp.get("[0].hasLocal", "?"), "true");
		assertEquals(rsp.get("[0].available", "?"), "true");
		assertEquals(1, rsp.get("[0].action").size());
		assertEquals(rsp.get("[0].action.name", "?"), "$node.events");
		assertEquals(1, rsp.get("[0].endpoints").size());
		assertEquals(2, rsp.get("[0].endpoints[0]").size());
		assertEquals(rsp.get("[0].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[0].endpoints[0].state", "?"), "true");
		assertEquals(6, rsp.get("[1]").size());
		assertEquals(rsp.get("[1].name", "?"), "$node.services");
		assertEquals(rsp.get("[1].count", "?"), "1");
		assertEquals(rsp.get("[1].hasLocal", "?"), "true");
		assertEquals(rsp.get("[1].available", "?"), "true");
		assertEquals(1, rsp.get("[1].action").size());
		assertEquals(rsp.get("[1].action.name", "?"), "$node.services");
		assertEquals(1, rsp.get("[1].endpoints").size());
		assertEquals(2, rsp.get("[1].endpoints[0]").size());
		assertEquals(rsp.get("[1].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[1].endpoints[0].state", "?"), "true");
		assertEquals(6, rsp.get("[2]").size());
		assertEquals(rsp.get("[2].name", "?"), "$node.actions");
		assertEquals(rsp.get("[2].count", "?"), "1");
		assertEquals(rsp.get("[2].hasLocal", "?"), "true");
		assertEquals(rsp.get("[2].available", "?"), "true");
		assertEquals(1, rsp.get("[2].action").size());
		assertEquals(rsp.get("[2].action.name", "?"), "$node.actions");
		assertEquals(1, rsp.get("[2].endpoints").size());
		assertEquals(2, rsp.get("[2].endpoints[0]").size());
		assertEquals(rsp.get("[2].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[2].endpoints[0].state", "?"), "true");
		assertEquals(6, rsp.get("[3]").size());
		assertEquals(rsp.get("[3].name", "?"), "$node.list");
		assertEquals(rsp.get("[3].count", "?"), "1");
		assertEquals(rsp.get("[3].hasLocal", "?"), "true");
		assertEquals(rsp.get("[3].available", "?"), "true");
		assertEquals(1, rsp.get("[3].action").size());
		assertEquals(rsp.get("[3].action.name", "?"), "$node.list");
		assertEquals(1, rsp.get("[3].endpoints").size());
		assertEquals(2, rsp.get("[3].endpoints[0]").size());
		assertEquals(rsp.get("[3].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[3].endpoints[0].state", "?"), "true");
		assertEquals(6, rsp.get("[4]").size());
		assertEquals(rsp.get("[4].name", "?"), "$node.health");
		assertEquals(rsp.get("[4].count", "?"), "1");
		assertEquals(rsp.get("[4].hasLocal", "?"), "true");
		assertEquals(rsp.get("[4].available", "?"), "true");
		assertEquals(1, rsp.get("[4].action").size());
		assertEquals(rsp.get("[4].action.name", "?"), "$node.health");
		assertEquals(1, rsp.get("[4].endpoints").size());
		assertEquals(2, rsp.get("[4].endpoints[0]").size());
		assertEquals(rsp.get("[4].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[4].endpoints[0].state", "?"), "true");

		params = new Tree();
		params.put("onlyLocal", true);
		params.put("skipInternal", false);
		params.put("withEndpoints", false);
		rsp = br.call("$node.actions", params).waitFor();

		assertEquals(5, rsp.size());
		assertEquals(5, rsp.get("[0]").size());
		assertEquals(rsp.get("[0].name", "?"), "$node.events");
		assertEquals(rsp.get("[0].count", "?"), "1");
		assertEquals(rsp.get("[0].hasLocal", "?"), "true");
		assertEquals(rsp.get("[0].available", "?"), "true");
		assertEquals(1, rsp.get("[0].action").size());
		assertEquals(rsp.get("[0].action.name", "?"), "$node.events");
		assertEquals(5, rsp.get("[1]").size());
		assertEquals(rsp.get("[1].name", "?"), "$node.services");
		assertEquals(rsp.get("[1].count", "?"), "1");
		assertEquals(rsp.get("[1].hasLocal", "?"), "true");
		assertEquals(rsp.get("[1].available", "?"), "true");
		assertEquals(1, rsp.get("[1].action").size());
		assertEquals(rsp.get("[1].action.name", "?"), "$node.services");
		assertEquals(5, rsp.get("[2]").size());
		assertEquals(rsp.get("[2].name", "?"), "$node.actions");
		assertEquals(rsp.get("[2].count", "?"), "1");
		assertEquals(rsp.get("[2].hasLocal", "?"), "true");
		assertEquals(rsp.get("[2].available", "?"), "true");
		assertEquals(1, rsp.get("[2].action").size());
		assertEquals(rsp.get("[2].action.name", "?"), "$node.actions");
		assertEquals(5, rsp.get("[3]").size());
		assertEquals(rsp.get("[3].name", "?"), "$node.list");
		assertEquals(rsp.get("[3].count", "?"), "1");
		assertEquals(rsp.get("[3].hasLocal", "?"), "true");
		assertEquals(rsp.get("[3].available", "?"), "true");
		assertEquals(1, rsp.get("[3].action").size());
		assertEquals(rsp.get("[3].action.name", "?"), "$node.list");
		assertEquals(5, rsp.get("[4]").size());
		assertEquals(rsp.get("[4].name", "?"), "$node.health");
		assertEquals(rsp.get("[4].count", "?"), "1");
		assertEquals(rsp.get("[4].hasLocal", "?"), "true");
		assertEquals(rsp.get("[4].available", "?"), "true");
		assertEquals(1, rsp.get("[4].action").size());
		assertEquals(rsp.get("[4].action.name", "?"), "$node.health");

		// Events
		params = new Tree();
		params.put("onlyLocal", false);
		params.put("skipInternal", false);
		params.put("withEndpoints", true);
		rsp = br.call("$node.events", params).waitFor();

		assertTrue(rsp.isEmpty());

		// Health
		rsp = br.call("$node.health").waitFor();

		assertEquals(6, rsp.size());
		assertEquals(2, rsp.get("cpu").size());

		assertTrue(rsp.get("cpu.cores", 0L) > 0);
		assertTrue(rsp.get("cpu.utilization", -1L) >= -1L);

		assertEquals(5, rsp.get("os").size());

		assertEquals(System.getProperty("os.name", "unknown"), rsp.get("os.type", ""));
		assertEquals(System.getProperty("os.version", "unknown"), rsp.get("os.release", ""));

		assertTrue(rsp.get("os.hostname", "").length() > 0);

		assertEquals(System.getProperty("os.arch", "unknown"), rsp.get("os.arch", ""));

		assertEquals(3, rsp.get("os.user").size());

		assertEquals(System.getProperty("user.name", "unknown"), rsp.get("os.user.username", ""));
		assertEquals(System.getProperty("user.home", "unknown"), rsp.get("os.user.homedir", ""));
		assertEquals(System.getProperty("user.script", "unknown"), rsp.get("os.user.shell", ""));

		assertEquals(3, rsp.get("process").size());
		assertEquals(2, rsp.get("process.memory").size());

		assertTrue(rsp.get("process.pid", 0L) > 0);
		assertTrue(rsp.get("process.memory.heapTotal", 0L) > 0);
		assertTrue(rsp.get("process.memory.heapUsed", 0L) > 0);
		assertTrue(rsp.get("process.uptime", 0L) > 0);

		assertTrue(rsp.get("client.type", "").length() > 0);
		assertTrue(rsp.get("client.version", "").length() > 0);
		assertTrue(rsp.get("client.langVersion", "").length() > 0);
		assertTrue(rsp.get("net.ip[0]", "").length() > 0);
		assertTrue(rsp.get("time.now", 0L) > 0);
		assertTrue(rsp.get("time.iso", "").length() > 0);
		assertTrue(rsp.get("time.utc", "").length() > 0);

		// List
		params = new Tree();
		params.put("onlyLocal", false);
		params.put("skipInternal", false);
		params.put("withActions", true);
		rsp = br.call("$node.list", params).waitFor();

		assertEquals(1, rsp.size());
		assertEquals(8, rsp.get("[0]").size());
		assertEquals("local", rsp.get("[0].id", ""));

		assertTrue(rsp.get("[0].available", 0L) > 0);
		assertTrue(rsp.get("[0].lastHeartbeatTime", 0L) > 0);
		assertTrue(rsp.get("[0].cpu", -1L) >= 0);
		assertTrue(rsp.get("[0].port", -1L) >= 0);
		assertTrue(rsp.get("[0].hostname", "").length() > 0);
		assertTrue(rsp.get("[0].ipList").size() > 0);
		assertTrue(rsp.get("[0].ipList[0]", "").length() > 0);

		assertEquals(3, rsp.get("[0].client").size());
		assertTrue(rsp.get("[0].client.type", "").length() > 0);
		assertTrue(rsp.get("[0].client.version", "").length() > 0);
		assertTrue(rsp.get("[0].client.langVersion", "").length() > 0);

		// Deploy test service
		br.createService(new TestService());

		// Actions
		params = new Tree();
		params.put("onlyLocal", false);
		params.put("skipInternal", false);
		params.put("withEndpoints", true);
		rsp = br.call("$node.actions", params).waitFor();

		assertEquals(6, rsp.size());
		assertEquals(6, rsp.get("[0]").size());
		assertEquals(rsp.get("[0].name", "?"), "$node.events");
		assertEquals(rsp.get("[0].count", "?"), "1");
		assertEquals(rsp.get("[0].hasLocal", "?"), "true");
		assertEquals(rsp.get("[0].available", "?"), "true");
		assertEquals(1, rsp.get("[0].action").size());
		assertEquals(rsp.get("[0].action.name", "?"), "$node.events");
		assertEquals(1, rsp.get("[0].endpoints").size());
		assertEquals(2, rsp.get("[0].endpoints[0]").size());
		assertEquals(rsp.get("[0].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[0].endpoints[0].state", "?"), "true");
		assertEquals(6, rsp.get("[1]").size());
		assertEquals(rsp.get("[1].name", "?"), "$node.services");
		assertEquals(rsp.get("[1].count", "?"), "1");
		assertEquals(rsp.get("[1].hasLocal", "?"), "true");
		assertEquals(rsp.get("[1].available", "?"), "true");
		assertEquals(1, rsp.get("[1].action").size());
		assertEquals(rsp.get("[1].action.name", "?"), "$node.services");
		assertEquals(1, rsp.get("[1].endpoints").size());
		assertEquals(2, rsp.get("[1].endpoints[0]").size());
		assertEquals(rsp.get("[1].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[1].endpoints[0].state", "?"), "true");
		assertEquals(6, rsp.get("[2]").size());
		assertEquals(rsp.get("[2].name", "?"), "$node.actions");
		assertEquals(rsp.get("[2].count", "?"), "1");
		assertEquals(rsp.get("[2].hasLocal", "?"), "true");
		assertEquals(rsp.get("[2].available", "?"), "true");
		assertEquals(1, rsp.get("[2].action").size());
		assertEquals(rsp.get("[2].action.name", "?"), "$node.actions");
		assertEquals(1, rsp.get("[2].endpoints").size());
		assertEquals(2, rsp.get("[2].endpoints[0]").size());
		assertEquals(rsp.get("[2].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[2].endpoints[0].state", "?"), "true");
		assertEquals(6, rsp.get("[3]").size());
		assertEquals(rsp.get("[3].name", "?"), "$node.list");
		assertEquals(rsp.get("[3].count", "?"), "1");
		assertEquals(rsp.get("[3].hasLocal", "?"), "true");
		assertEquals(rsp.get("[3].available", "?"), "true");
		assertEquals(1, rsp.get("[3].action").size());
		assertEquals(rsp.get("[3].action.name", "?"), "$node.list");
		assertEquals(1, rsp.get("[3].endpoints").size());
		assertEquals(2, rsp.get("[3].endpoints[0]").size());
		assertEquals(rsp.get("[3].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[3].endpoints[0].state", "?"), "true");
		assertEquals(6, rsp.get("[4]").size());
		assertEquals(rsp.get("[4].name", "?"), "$node.health");
		assertEquals(rsp.get("[4].count", "?"), "1");
		assertEquals(rsp.get("[4].hasLocal", "?"), "true");
		assertEquals(rsp.get("[4].available", "?"), "true");
		assertEquals(1, rsp.get("[4].action").size());
		assertEquals(rsp.get("[4].action.name", "?"), "$node.health");
		assertEquals(1, rsp.get("[4].endpoints").size());
		assertEquals(2, rsp.get("[4].endpoints[0]").size());
		assertEquals(rsp.get("[4].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[4].endpoints[0].state", "?"), "true");
		assertEquals(6, rsp.get("[5]").size());
		assertEquals(rsp.get("[5].name", "?"), "test.foo");
		assertEquals(rsp.get("[5].count", "?"), "1");
		assertEquals(rsp.get("[5].hasLocal", "?"), "true");
		assertEquals(rsp.get("[5].available", "?"), "true");
		assertEquals(1, rsp.get("[5].action").size());
		assertEquals(rsp.get("[5].action.name", "?"), "test.foo");
		assertEquals(1, rsp.get("[5].endpoints").size());
		assertEquals(2, rsp.get("[5].endpoints[0]").size());
		assertEquals(rsp.get("[5].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[5].endpoints[0].state", "?"), "true");

		params = new Tree();
		params.put("onlyLocal", false);
		params.put("skipInternal", true);
		params.put("withEndpoints", false);
		rsp = br.call("$node.actions", params).waitFor();

		assertEquals(1, rsp.size());
		assertEquals(5, rsp.get("[0]").size());
		assertEquals(rsp.get("[0].name", "?"), "test.foo");
		assertEquals(rsp.get("[0].count", "?"), "1");
		assertEquals(rsp.get("[0].hasLocal", "?"), "true");
		assertEquals(rsp.get("[0].available", "?"), "true");
		assertEquals(1, rsp.get("[0].action").size());
		assertEquals(rsp.get("[0].action.name", "?"), "test.foo");

		params = new Tree();
		params.put("onlyLocal", false);
		params.put("skipInternal", true);
		params.put("withEndpoints", true);
		rsp = br.call("$node.actions", params).waitFor();

		assertEquals(1, rsp.size());
		assertEquals(6, rsp.get("[0]").size());
		assertEquals(rsp.get("[0].name", "?"), "test.foo");
		assertEquals(rsp.get("[0].count", "?"), "1");
		assertEquals(rsp.get("[0].hasLocal", "?"), "true");
		assertEquals(rsp.get("[0].available", "?"), "true");
		assertEquals(1, rsp.get("[0].action").size());
		assertEquals(rsp.get("[0].action.name", "?"), "test.foo");
		assertEquals(1, rsp.get("[0].endpoints").size());
		assertEquals(2, rsp.get("[0].endpoints[0]").size());
		assertEquals(rsp.get("[0].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[0].endpoints[0].state", "?"), "true");

		// Events
		params = new Tree();
		params.put("onlyLocal", false);
		params.put("skipInternal", false);
		params.put("withEndpoints", true);
		rsp = br.call("$node.events", params).waitFor();

		assertEquals(1, rsp.size());
		assertEquals(7, rsp.get("[0]").size());
		assertEquals(rsp.get("[0].name", "?"), "xyz.*");
		assertEquals(rsp.get("[0].group", "?"), "test");
		assertEquals(rsp.get("[0].count", "?"), "1");
		assertEquals(rsp.get("[0].hasLocal", "?"), "true");
		assertEquals(rsp.get("[0].available", "?"), "true");
		assertEquals(2, rsp.get("[0].event").size());
		assertEquals(rsp.get("[0].event.name", "?"), "xyz.*");
		assertEquals(rsp.get("[0].event.group", "?"), "test");
		assertEquals(1, rsp.get("[0].endpoints").size());
		assertEquals(2, rsp.get("[0].endpoints[0]").size());
		assertEquals(rsp.get("[0].endpoints[0].nodeID", "?"), "local");
		assertEquals(rsp.get("[0].endpoints[0].state", "?"), "true");

		params = new Tree();
		params.put("onlyLocal", true);
		params.put("skipInternal", false);
		params.put("withEndpoints", false);
		rsp = br.call("$node.events", params).waitFor();

		assertEquals(1, rsp.size());
		assertEquals(6, rsp.get("[0]").size());
		assertEquals(rsp.get("[0].name", "?"), "xyz.*");
		assertEquals(rsp.get("[0].group", "?"), "test");
		assertEquals(rsp.get("[0].count", "?"), "1");
		assertEquals(rsp.get("[0].hasLocal", "?"), "true");
		assertEquals(rsp.get("[0].available", "?"), "true");
		assertEquals(2, rsp.get("[0].event").size());
		assertEquals(rsp.get("[0].event.name", "?"), "xyz.*");
		assertEquals(rsp.get("[0].event.group", "?"), "test");
	}

	@Name("test")
	protected static final class TestService extends Service {

		public Action foo = ctx -> {
			return new Tree().put("a", 1).put("b", 2);
		};

		@Subscribe("xyz.*")
		public Listener evt = payload -> {
		};

	}

	public void dumpTypes(Tree t) {
		if (t == null) {
			return;
		}
		if (t.isStructure()) {
			String p = t.getPath();
			p = p.replace("list[", "[");
			System.out.println("assertEquals(" + t.size() + ", rsp.get(\"" + p + "\").size());");
			for (Tree c : t) {
				dumpTypes(c);
			}
		} else if (t.isPrimitive()) {
			String v = t.getType().getName();
			String p = t.getPath();
			p = p.replace("list[", "[");
			if (v.contains("String")) {
				System.out.println("assertTrue(rsp.get(\"" + p + "\", \"\").length() > 0);");
			} else {
				System.out.println("assertTrue(rsp.get(\"" + p + "\", 0L) > 0);");
			}
		}
	}

	public void dumpValues(Tree t) {
		if (t == null) {
			return;
		}
		if (t.isStructure()) {
			String p = t.getPath();
			p = p.replace("list[", "[");
			System.out.println("assertEquals(" + t.size() + ", rsp.get(\"" + p + "\").size());");
			for (Tree c : t) {
				dumpValues(c);
			}
		} else if (t.isPrimitive()) {
			String v = t.asString();
			String p = t.getPath();
			p = p.replace("list[", "[");
			System.out.println("assertEquals(rsp.get(\"" + p + "\", \"?\"), \"" + v + "\");");
		}
	}

	// --- SET UP ---

	@Override
	protected void setUp() throws Exception {
		br = ServiceBroker.builder().monitor(new ConstantMonitor()).nodeID("local").build();
		br.start();
	}

	// --- TEAR DOWN ---

	@Override
	protected void tearDown() throws Exception {
		if (br != null) {
			br.stop();
		}
	}

}