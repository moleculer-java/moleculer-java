/**
 * This software is licensed under MIT license.<br>
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
package services.moleculer;

import io.datatree.Tree;
import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.Group;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.DefaultServiceRegistry;
import services.moleculer.service.Service;
import services.moleculer.transporter.MqttTransporter;

public class Test {

	public static void main(String[] args) throws Exception {		
		
		// Test sigar
		String nativeDir = "./native";
		System.setProperty("java.library.path", nativeDir);
		// System.setProperty("hawtdispatch.threads", "1");
		
		// Define a service
		MqttTransporter transporter = new MqttTransporter();
		transporter.setDebug(false);
		
		ServiceBroker broker = ServiceBroker.builder().registry(new DefaultServiceRegistry(false))
				.transporter(transporter).nodeID("server-2").build();
		
		broker.createService(new Service("math") {

			@Cache(keys={"a", "b"})
			public Action add = (ctx) -> {
				int a = ctx.params().get("a", 0);
				int b = ctx.params().get("b", 0);
				
				/*return ctx.call("math2.mult", "a", a + b, "b", 2).then(in -> {
					System.out.println("Res: " + in);
					return new Promise(r -> {
						broker.components().scheduler().schedule(() -> {
							
							r.resolve("Result: " + in.asString());
							
						}, 3, TimeUnit.SECONDS);
					});
				});	*/
				
				return a + b;
			};

			@Subscribe("user.*")
			public Listener evt1 = payload -> {
				System.out.println("Event1: " + payload.get("a", -1));
			};

			@Subscribe("user.*")
			@Group("special")
			public Listener evt2 = payload -> {
				System.out.println("Event2: " + payload.get("a", -1));
			};
			
		});
		broker.start();
		
		Thread.sleep(20000);

		// Emit local event
		Tree payload = new Tree();
		payload.put("a", 5);
		broker.broadcastLocal("user.foo", payload, null);
		broker.broadcastLocal("user.foo", payload, new String[] { "math" });
		broker.broadcastLocal("user.foo", payload, new String[] { "special" });
		broker.emit("test.foo", payload, null);
		broker.broadcast("user.foo", payload, null);
		
		/*broker.call("math2.mult", "a", 5, "b", 2).then(in -> {
			System.out.println("Result: " + in.asString());
			//broker.emit("test.foo", payload, null);
		});*/
	}

}