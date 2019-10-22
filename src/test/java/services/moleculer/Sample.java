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

import io.datatree.Tree;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.service.Action;
import services.moleculer.service.Service;

public class Sample {

	public static void main(String[] args) throws Exception {
		try {

			// Create Service Broker config
			ServiceBrokerConfig cfg = new ServiceBrokerConfig();

			// Unique nodeID
			cfg.setNodeID("node1");

			// NatsTransporter t = new NatsTransporter("localhost");
			// t.setVerbose(true);
			// t.setDebug(true);
			// t.setNoEcho(true);
			// cfg.setTransporter(t);

			// Create Service Broker by config
			ServiceBroker broker = new ServiceBroker(cfg);

			// Install "MyService" Service
			broker.createService(new MyService());

			// Start Service Broker
			broker.start();

			// Invoke service (blocking style)
			Tree response = broker.call("myService.first").waitFor(5000);
			
			// Print response
			System.out.println("RESPONSE: " + response);

			// Stop Service Broker
			broker.stop();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class MyService extends Service {

		// First action (which calls the second action)
		Action first = ctx -> {

			// Create input JSON structure
			Tree params = new Tree();
			params.put("a", 2);
			params.put("b", "text");
			params.put("c.d", 3);

			// Invoke local action via EventBus
			return ctx.call("myService.second", params).then(in -> {

				// The result will be 10
				return in.asLong() * 2;
			});
		};
		
		// Sacond action
		Action second = ctx -> {
			return ctx.params.get("a", 0) + ctx.params.get("c.d", 0);
		};

	}

}