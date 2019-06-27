/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.config;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;

/**
 * Run MoleculerRunner.main(args), when "args" is the main Spring Boot class
 * (the "SpringBootSample").
 */
public class MoleculerRunnerSpringBootTest extends TestCase {

	// --- SPRING CONTEXT ---

	private ConfigurableApplicationContext ctx;

	// --- TEST METHOD ---

	@Test
	public void testRunner() throws Exception {
		ServiceBroker broker = ctx.getBean(ServiceBroker.class);
		Tree rsp = broker.call("structService.action").waitFor(2000);
		assertEquals(1, rsp.get("a", 0));
		assertEquals(2, rsp.get("b", 0));
		assertEquals(3, rsp.get("c[0]", 0));
		assertEquals(4, rsp.get("c[1]", 0));
		assertEquals(5, rsp.get("c[2]", 0));
	}

	// --- START INSTANCE ---

	@Override
	protected void setUp() throws Exception {

		// Main class
		String[] args = { "services.moleculer.config.SpringBootSample" };

		// Run via Spring Boot
		MoleculerRunner.main(args);

		// Get Spring Content
		for (int i = 0; i < 10; i++) {
			ctx = MoleculerRunner.context.get();
			if (ctx != null) {
				break;
			}
			Thread.sleep(200);
		}
		assertNotNull(ctx);
	}

	// --- STOP INSTANCE ---

	@Override
	protected void tearDown() throws Exception {
		if (ctx != null) {
			ctx.stop();
		}
	}

}
