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
import services.moleculer.eventbus.Groups;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.monitor.Monitor;
import services.moleculer.monitor.SigarMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.Service;
import services.moleculer.transporter.Transporter;

public class Test {

	public static void main(String[] args) throws Exception {

		// Test sigar
		String nativeDir = "./native";
		System.setProperty("java.library.path", nativeDir);

		// Define transporter
		// Transporter transporter = new RedisTransporter();
		// transporter.setDebug(true);
		Transporter transporter = null;
		
		// Define cacher
		Cacher cacher = new OHCacher();

		// CPU monitor
		Monitor monitor = new SigarMonitor();

		// Create broker
		ServiceBroker broker = ServiceBroker.builder().transporter(transporter).cacher(cacher).monitor(monitor)
				.nodeID("server-2").build();

		// --- GROUP1 ---
		
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
		broker.createService(new Service("test2") {
			
			@Subscribe("user.*")
			@Group("group1")
			public Listener listener = payload -> {
				System.out.println("group1,listener2: " + payload.get("a", -1));
			};
		});				
		broker.createService(new Service("test3") {
			
			@Subscribe("user.*")
			@Group("group1")
			public Listener listener = payload -> {
				System.out.println("group1,listener3: " + payload.get("a", -1));
			};
		});						

		// --- GROUP2 ---

		broker.createService(new Service("test4") {
			
			@Subscribe("user.*")
			@Group("group2")
			public Listener listener = payload -> {
				System.out.println("group2,listener1: " + payload.get("a", -1));
			};
		});						
		broker.createService(new Service("test5") {
			
			@Subscribe("user.*")
			@Group("group2")
			public Listener listener = payload -> {
				System.out.println("group2,listener2: " + payload.get("a", -1));
			};
		});						
		broker.createService(new Service("test6") {
			
			@Subscribe("user.*")
			@Group("group2")
			public Listener listener = payload -> {
				System.out.println("group2,listener3: " + payload.get("a", -1));
			};
		});						

		// --- START ---

		broker.start();

		// --- EMIT / BROADCAST ---

		System.out.println("EMIT TO GROUP1");
		for (int i = 0; i < 3; i++) {
			broker.emit("user.foo", "a", i, Groups.of("group1"));
			System.out.println("--------------------");
		}

		System.out.println();
		System.out.println("EMIT TO GROUP2");
		for (int i = 0; i < 3; i++) {
			broker.emit("user.foo", "a", i, Groups.of("group2"));
			System.out.println("--------------------");
		}

		System.out.println();
		System.out.println("EMIT TO 2 GROUPS");
		for (int i = 0; i < 3; i++) {
			broker.emit("user.foo", "a", i, Groups.of("group1", "group2"));
			System.out.println("--------------------");
		}

		System.out.println();
		System.out.println("EMIT TO ALL LISTENERS");
		for (int i = 0; i < 3; i++) {
			broker.emit("user.foo", "a", i);
			System.out.println("--------------------");
		}

		System.out.println();
		System.out.println("BROADCAST TO GROUP1");
		for (int i = 0; i < 3; i++) {
			broker.broadcast("user.foo", "a", i, Groups.of("group1"));
			System.out.println("--------------------");
		}

		System.out.println();
		System.out.println("BROADCAST TO GROUP2");
		for (int i = 0; i < 3; i++) {
			broker.broadcast("user.foo", "a", i, Groups.of("group2"));
			System.out.println("--------------------");
		}
		
		System.out.println();
		System.out.println("BROADCAST TO 2 GROUPS");
		for (int i = 0; i < 3; i++) {
			broker.broadcast("user.foo", "a", i, Groups.of("group1", "group2"));
			System.out.println("--------------------");
		}
		
		System.out.println();
		System.out.println("BROADCAST TO ALL LISTENERS");
		for (int i = 0; i < 3; i++) {
			broker.broadcast("user.foo", "a", i);
			System.out.println("--------------------");
		}				
	}

}