package services.moleculer;

import io.datatree.Tree;
import services.moleculer.cachers.Cache;
import services.moleculer.eventbus.Listener;
import services.moleculer.services.Action;
import services.moleculer.services.Name;
import services.moleculer.services.Service;

public class Test {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {

		// ServiceBroker broker = new ServiceBroker("server-2", new
		// RedisTransporter(), new MemoryCacher());
		// TestService service = new TestService();
		// Service svc = broker.createService(service);

		// broker.start();

		Promise.resolve(100).then(a -> {
			System.out.println("#1. a=" + a);
			return a.asInteger() * 2;
		}).then(b -> {
			System.out.println("#2. b=" + b);
			int c = b.asInteger() + 100;
			return c;
		}).then(c -> {
			System.out.println("#3. c=" + c);
			return Promise.resolve().then((n) -> {
				System.out.println("#3.1. c=" + c);
				return 400;
			}).then(d -> {
				System.out.println("#3.2. d=" + d);
				return Promise.resolve(500);
			}).then(e -> {
				System.out.println("#3.3. e=" + e);
				return Promise.reject();
			}).Catch((err) -> {
				System.out.println("#3.4. Catch error");
				return 600;
			}).then(x -> {
				System.out.println("#3.5. x=" + x);
				return new Promise(r -> {
					r.resolve(700);
				});
			});
		}).then(f -> {
			System.out.println("#4. d=" + f + ", throw error");
			throw new Error("Throw error!");
		}).then(g -> {
			System.out.println("#5. g=" + g);
			return null;
		}).Catch(err -> {
			System.out.println("Catched error:" + err.getMessage());
			return 1000;
		}).then(h -> {
			System.out.println("#6. h=" + h);
			return null;
		});

		System.out.println("---------------------------");
		
		new Promise(r -> {
			System.out.println("#1");
			new Thread(() -> {
				try {
					Thread.sleep(1000);
					System.out.println("#2");
					r.resolve("a");
				} catch (Exception error) {
					r.reject(error);
				}
			}).start();
		}).then(a -> {
			System.out.println("#3 " + a.asString());
			return null;
		});

		// ---------

		// Tree t = new Tree().put("a", 5).put("b", 3);
		// long start = System.currentTimeMillis();
		// for (int i = 0; i < 3; i++) {
		// Object result = broker.call("v2.test.add", t, null);
		// System.out.println("RESULT: " + result);
		// }
		// System.out.println(System.currentTimeMillis() - start);

		// ------------------

		/*
		 * broker.on("user.create", (payload) -> { System.out.println(
		 * "RECEIVED in 'user.create': " + payload); });
		 * broker.on("user.created", (payload) -> { System.out.println(
		 * "RECEIVED in 'user.created': " + payload); }); broker.on("user.*",
		 * (payload) -> { System.out.println("RECEIVED in 'user.*': " +
		 * payload); }); broker.on("post.*", (payload) -> { System.out.println(
		 * "RECEIVED in 'post.*': " + payload); }); broker.on("*", (payload) ->
		 * { System.out.println("RECEIVED in '*': " + payload); });
		 * broker.on("**", (payload) -> { System.out.println(
		 * "RECEIVED in '**': " + payload); });
		 */
	}

	@Name("v2.test")
	public static class TestService extends Service {

		// --- CREATED ---

		@Override
		public void created() {

			// Created
			logger.debug("Service created!");

		}

		// --- ACTIONS ---

		public Action list = (ctx) -> {
			return new Promise(r -> {

				Tree t = new Tree();
				t.put("a", 3);
				r.resolve(t);

			});
		};

		public Action foo = (ctx) -> {
			Promise p = Promise.resolve();

			p.then(t -> {
				return t;
			});

			return p;
		};

		@Cache({ "a", "b" })
		public Action add = (ctx) -> {
			return ctx.call("v2.test.list", ctx.params(), null).then(t -> {

				t.putObject("list", t);
				return t;

			}).then(t -> {

				// return ctx.call("posts.find", ctx.params(),
				// null).then((posts) -> {
				// return posts.size();
				// });
				return t;

			}).then(t -> {

				return t;

			}).then(t -> {

				return t;

			}).then(t -> {

				return t;

			}).Catch((error) -> {

				return null;

			});
		};

		// --- EVENT LISTENERS ---

		// Context, Tree, or Object????
		public Listener test = (input) -> {
		};

		// --- METHODS ---

		int processData(int a, int b) {
			// this.logger.info("Process data invoked: " + a + ", " + b);
			return a + b;
		}

	}

}
