package services.moleculer;

import io.datatree.Tree;
import services.moleculer.cachers.MemoryCacher;
import services.moleculer.utils.UIDGenerator;

public class Test {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {

		ServiceBroker broker = new ServiceBroker(null, new MemoryCacher(), null, null);

		Service svc = broker.createService(new Service(broker, "test", null) {

			// --- CREATED ---

			@Override
			public void created() {

				// Created
				this.logger.debug("Service created!");

			}

			// --- ACTIONS ---

			@Cache(false)
			@Version("v1")
			public Action list = (ctx) -> {
				return this.processData(ctx.params.get("a", -1));
			};

			@Cache(true)
			@Version("v2")
			public Action add = (ctx) -> {
				return ctx.call("v1.test.list", ctx.params, null);
				// return 2;
			};

			// --- EVENT LISTENERS ---

			// Context, Tree, or Object????
			public Listener test = (input) -> {

			};

			// --- METHODS ---

			int processData(int a) {
				this.logger.info("Process data invoked: " + a);
				return a * 2;
			}

		});

		broker.start();

		// ---------

		Tree t = new Tree().put("a", 5);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			Object result = broker.call("v2.test.add", t, null);
			// System.out.println("RESULT: " + result);
		}
		System.out.println(System.currentTimeMillis() - start);

		// ------------------

		broker.on("user.create", (payload) -> {
			System.out.println("RECEIVED in 'user.create': " + payload);
		});
		broker.on("user.created", (payload) -> {
			System.out.println("RECEIVED in 'user.created': " + payload);
		});
		broker.on("user.*", (payload) -> {
			System.out.println("RECEIVED in 'user.*': " + payload);
		});
		broker.on("post.*", (payload) -> {
			System.out.println("RECEIVED in 'post.*': " + payload);
		});
		broker.on("*", (payload) -> {
			System.out.println("RECEIVED in '*': " + payload);
		});
		broker.on("**", (payload) -> {
			System.out.println("RECEIVED in '**': " + payload);
		});
		broker.emit("user.created", "Hello EventBus1!");

		// ------------------

		UIDGenerator gen = new UIDGenerator("host1");
		start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			String s = gen.next();
			System.out.println(s);
		}
		System.out.println(System.currentTimeMillis() - start);
	}

}
