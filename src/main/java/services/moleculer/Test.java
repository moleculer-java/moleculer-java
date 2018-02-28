package services.moleculer;

import io.datatree.Tree;
import services.moleculer.cacher.Cache;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.Context;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.repl.LocalRepl;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.Version;
import services.moleculer.transporter.RedisTransporter;

public class Test {

	public static void main(String[] args) throws Exception {
		System.out.println("START");
		try {
			ServiceBrokerConfig cfg = new ServiceBrokerConfig();
			
			RedisTransporter t = new RedisTransporter();
			t.setDebug(false);
			cfg.setTransporter(t);
			
			cfg.setRepl(new LocalRepl());
			
			ServiceBroker broker = new ServiceBroker(cfg);
			broker.createService(new Service("test") {

				@Name("add")
				@Cache(keys = { "a", "b" }, ttl = 30)
				public Action add = ctx -> {

					//broker.getLogger().info("Call " + ctx.params);
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

			});			
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
			for (int i = 0; i < 2; i++) {
				broker.call("test.add", "a", 3, "b", 5).then(in -> {

					broker.getLogger(Test.class).info("Result: " + in);

				}).catchError(err -> {

					broker.getLogger(Test.class).error("Error: " + err);

				});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("STOP");
	}

}
