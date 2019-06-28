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
package services.moleculer.serializer;

import org.junit.Test;

import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.transporter.TcpTransporter;

public class SetJsonApiTest extends TestCase {

	@Test
	public void testJsonApi() throws Exception {
		ServiceBroker br = null;
		try {
			ServiceBrokerConfig cfg = new ServiceBrokerConfig();
			cfg.setJsonReaders("builtin");
			cfg.setJsonWriters("builtin");
			cfg.setMonitor(new ConstantMonitor());
			cfg.setTransporter(new TcpTransporter());

			br = new ServiceBroker(cfg);
			br.start();
			Serializer s = br.getConfig().getTransporter().getSerializer();
			assertTrue(s.reader.getClass().toString().toLowerCase().contains("builtin"));
			assertTrue(s.writer.getClass().toString().toLowerCase().contains("builtin"));
			br.stop();
			br = null;

			cfg = new ServiceBrokerConfig();
			cfg.setJsonReaders("jackson");
			cfg.setJsonWriters("jackson");
			cfg.setMonitor(new ConstantMonitor());
			cfg.setTransporter(new TcpTransporter());

			br = new ServiceBroker(cfg);
			br.start();
			s = br.getConfig().getTransporter().getSerializer();
			assertTrue(s.reader.getClass().toString().toLowerCase().contains("jackson"));
			assertTrue(s.writer.getClass().toString().toLowerCase().contains("jackson"));
			br.stop();
			br = null;

		} finally {
			if (br != null) {
				br.stop();
			}
		}
	}

}
