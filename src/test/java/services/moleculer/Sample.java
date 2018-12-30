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
package services.moleculer;

import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.error.MoleculerError;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.transporter.NatsTransporter;

public class Sample {

	public static void main(String[] args) throws Exception {
		try {

			for (int i = 0; i < 1; i++) {

				// Create Service Broker config
				ServiceBrokerConfig cfg = new ServiceBrokerConfig();

				// Unique nodeID
				cfg.setNodeID("node" + (i + 10));

				NatsTransporter t = new NatsTransporter();
				t.setVerbose(true);
				t.setDebug(true);
				t.setNoEcho(true);
				cfg.setTransporter(t);

				// Create Service Broker (by config)
				ServiceBroker broker = new ServiceBroker(cfg);

				broker.createService(new ErrorService(i < 1));

				// Start Service Broker
				broker.start();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class ErrorService extends Service {

		private boolean faulty;

		public ErrorService(boolean faulty) {
			this.faulty = faulty;
		}

		@Name("test")
		public Action error = ctx -> {
			if (faulty) {
				Thread.sleep(200);
				throw new MoleculerError("Timed error!", broker.getNodeID(), true);
			}
			return ctx.params;
		};

	}

}