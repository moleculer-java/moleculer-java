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
package services.moleculer.service;

import org.springframework.stereotype.Component;

import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Dependencies;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.Version;

@Component
@Name("math")
@Version("2")
@Dependencies({"service1", "service2"})
public class TestService extends Service {

	// --- SERVICE STARTED AND PUBLISHED ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		System.out.println("STARTED");
	}

	// --- PUBLISHED ACTIONS ---

	@Name("add")
	@Cache(keys = { "a", "b" }, ttl = 30)
	public Action add = ctx -> {

		return ctx.params.get("a", 0) + ctx.params.get("b", 0);

	};

	@Name("math.test")
	public Action test = ctx -> {
		return ctx.params.get("a", 0) + ctx.params.get("b", 0);
	};

	// --- EVENT LISTENERS ---

	@Subscribe("math.*")
	public Listener evt = payload -> {
		System.out.println("RECEIVED: " + payload);
	};

	// --- STOP SERVICE INSTANCE ---

	@Override
	public void stopped() {
		System.out.println("STOPPED");
	}

}