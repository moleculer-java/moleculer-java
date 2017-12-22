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

import services.moleculer.cacher.Cache;
import services.moleculer.cacher.Cacher;
import services.moleculer.cacher.OHCacher;
import services.moleculer.eventbus.Group;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.monitor.Monitor;
import services.moleculer.monitor.SigarMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.Service;
import services.moleculer.transporter.NatsTransporter;
import services.moleculer.transporter.Transporter;

public class Test {

	public static void main(String[] args) throws Exception {

		// Test sigar
		String nativeDir = "./native";
		System.setProperty("java.library.path", nativeDir);

		// Define transporter
		Transporter transporter = new NatsTransporter();
		transporter.setDebug(true);
		// Transporter transporter = null;

		// Define cacher
		Cacher cacher = new OHCacher();

		// CPU monitor
		Monitor monitor = new SigarMonitor();

		// Create broker
		ServiceBroker broker = ServiceBroker.builder().transporter(transporter).cacher(cacher).monitor(monitor)
				.nodeID("server-2").build();

		broker.createService(new Service("math") {

			@Cache(keys = { "a", "b" })
			public Action add = ctx -> {
				int a = ctx.params.get("a", 0);
				int b = ctx.params.get("b", 0);
				return a + b;
			};

			@Subscribe("user.*")
			@Group("group1")
			public Listener listener = payload -> {
				System.out.println("group1,listener1: " + payload.get("a", -1));
			};

		});

		broker.start();
	}

}