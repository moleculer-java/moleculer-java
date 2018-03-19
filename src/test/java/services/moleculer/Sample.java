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
import services.moleculer.context.CallOptions;
import services.moleculer.context.Context;
import services.moleculer.context.DefaultContextFactory;
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
					if (config.get("name", "?").equals("v1.math.test")) {
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
					return null;
				}

			});
			broker.waitForServices("v1.math").then(ok -> {
				for (int i = 0; i < 2; i++) {
					broker.call("v1.math.add", "a", 3, "b", 5).then(in -> {

						broker.getLogger(Sample.class).info("Result: " + in);

					}).catchError(err -> {

						broker.getLogger(Sample.class).error("Error: " + err);

					});
				}

				System.out.println("FIRST CALL ->3");
				broker.call("service2.test", new Tree(), CallOptions.retryCount(3)).catchError(cause -> {
					cause.printStackTrace();
				});
			});

			((DefaultContextFactory) broker.getConfig().getContextFactory()).setMaxCallLevel(3);

			Thread.sleep(1000);
			broker.createService(new Service2Service());

			Thread.sleep(1000);
			broker.createService(new Service3Service());

			Thread.sleep(60000);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("STOP");
	}

	@Name("math")
	@Dependencies({ "service2", "service3" })
	@Version("1")
	public static class MathService extends Service {

		@Name("add")
		@Cache(keys = { "a", "b" }, ttl = 30)
		public Action add = ctx -> {

			// broker.getLogger().info("Call " + ctx.params);
			return ctx.params.get("a", 0) + ctx.params.get("b", 0);

		};

		@Name("test")
		public Action test = ctx -> {

			return ctx.params.get("a", 0) + ctx.params.get("b", 0);

		};

		@Subscribe("foo.*")
		public Listener listener = payload -> {
			System.out.println("Received: " + payload);
		};

	};

	@Name("service2")
	@Dependencies({ "service3" })
	public static class Service2Service extends Service {

		@Name("test")
		public Action test = ctx -> {
			System.out.println("CALL 2->3");
			return ctx.call("service3.test", ctx.params);
		};

	};

	@Name("service3")
	// @Dependencies({ "service2" })
	public static class Service3Service extends Service {

		@Name("test")
		public Action test = ctx -> {
			// System.out.println("CALL 3->2");
			// return ctx.call("service2.test", ctx.params);
			throw new Exception("X"); 
		};

	};

}