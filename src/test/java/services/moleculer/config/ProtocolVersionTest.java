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
package services.moleculer.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import services.moleculer.ServiceBroker;

/**
 * Verifies the configurable Moleculer wire-protocol version ("ver" field). The
 * default is "5" (Moleculer JS 0.15); it can be overridden via the builder, the
 * config setter, or the "moleculer.protocol.version" System Property.
 */
public class ProtocolVersionTest {

	@Test
	public void testDefaultProtocolVersionIsFive() {
		// Out of the box moleculer-java now speaks protocol "5" (Moleculer JS
		// 0.15), so Java <-> Node clusters interoperate without extra config.
		assertEquals("5", ServiceBrokerConfig.DEFAULT_PROTOCOL_VERSION);

		ServiceBrokerConfig config = new ServiceBrokerConfig();
		assertEquals("5", config.getProtocolVersion());

		ServiceBroker broker = new ServiceBroker();
		assertEquals("5", broker.getProtocolVersion());
	}

	@Test
	public void testBuilderOverride() {
		// Legacy Moleculer JS 0.14 nodes expect "4" -> opt back in cleanly.
		ServiceBroker broker = ServiceBroker.builder().protocolVersion("4").build();
		assertEquals("4", broker.getProtocolVersion());
	}

	@Test
	public void testConfigSetterOverride() {
		ServiceBrokerConfig config = new ServiceBrokerConfig();
		config.setProtocolVersion("4");
		assertEquals("4", config.getProtocolVersion());
		assertEquals("4", new ServiceBroker(config).getProtocolVersion());
	}

	@Test
	public void testSystemPropertyOverride() {
		// The field is seeded from the System Property at construction time.
		String previous = System.getProperty("moleculer.protocol.version");
		try {
			System.setProperty("moleculer.protocol.version", "4");
			assertEquals("4", new ServiceBrokerConfig().getProtocolVersion());
		} finally {
			if (previous == null) {
				System.clearProperty("moleculer.protocol.version");
			} else {
				System.setProperty("moleculer.protocol.version", previous);
			}
		}
	}
}
