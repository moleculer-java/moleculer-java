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
import services.moleculer.cacher.Cache;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.Context;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Dependencies;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.Version;

public class Sample {

	public static void main(String[] args) throws Exception {
		System.out.println("START");
		try {

			ServiceBrokerConfig cfg = new ServiceBrokerConfig();

			// RedisTransporter t = new RedisTransporter();
			// t.setDebug(false);
			// cfg.setTransporter(t);

			ServiceBroker broker = new ServiceBroker(cfg);

			MathService math = new MathService();
			broker.createService(math);
			broker.start();
			broker.use(new Middleware() {

				@Override
				public Action install(Action action, Tree config) {
					int version = config.get("version", 0);
					if (version > 0) {
						broker.getLogger().info("Middleware installed to " + config.toString(false));
						return new Action() {

							@Override
							public Object handler(Context ctx) throws Exception {
								Object original = action.handler(ctx);
								Object replaced = System.currentTimeMillis();
								broker.getLogger()
										.info("Middleware invoked! Replacing " + original + " to " + replaced);
								return replaced;
							}

						};
					}
					broker.getLogger().info("Middleware not installed to " + config.toString(false));
					return null;
				}

			});
			broker.waitForServices("math").then(ok -> {
				for (int i = 0; i < 2; i++) {
					broker.call("math.add", "a", 3, "b", 5).then(in -> {

						broker.getLogger(Sample.class).info("Result: " + in);

					}).catchError(err -> {

						broker.getLogger(Sample.class).error("Error: " + err);

					});
				}
			});

			Thread.sleep(4000);

			broker.createService(new Service("service2") {

				@SuppressWarnings("unused")
				public Action test2 = ctx -> {

					return ctx.params.get("a", 0) + ctx.params.get("b", 0);

				};

			});

			Thread.sleep(60000);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("STOP");
	}

	@Name("math")
	@Dependencies({ "service2" })
	public static class MathService extends Service {

		@Name("add")
		@Cache(keys = { "a", "b" }, ttl = 30)
		public Action add = ctx -> {

			// broker.getLogger().info("Call " + ctx.params);
			return ctx.params.get("a", 0) + ctx.params.get("b", 0);

		};

		@Name("test")
		@Version("1")
		public Action test = ctx -> {

			return ctx.params.get("a", 0) + ctx.params.get("b", 0);

		};

		@Subscribe("foo.*")
		public Listener listener = payload -> {
			System.out.println("Received: " + payload);
		};

	};

}