package services.moleculer;

import java.util.concurrent.CompletableFuture;

import io.datatree.Tree;
import services.moleculer.cachers.Cache;
import services.moleculer.cachers.MemoryCacher;
import services.moleculer.eventbus.Listener;
import services.moleculer.services.Action;
import services.moleculer.services.Name;
import services.moleculer.services.Service;
import services.moleculer.transporters.RedisTransporter;

public class Test {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {

		ServiceBroker broker = new ServiceBroker("server-2", new RedisTransporter(), new MemoryCacher());

		TestService service = new TestService();

		Service svc = broker.createService(service);

		// broker.start();
		
		Promise p = new Promise((r) -> {
			
			Tree t = new Tree();
			t.put("a", 3);
			r.resolve(t);
			
		});
		p.then((t) -> {
			
			System.out.println(t);
			return t;
			
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
			return new Promise((r) -> {
				
				Tree t = new Tree();
				t.put("a", 3);
				r.resolve(t);
				
			});
		};

		public Action foo = (ctx) -> {
			Promise p = Promise.resolve();
			
			p.then((t) -> {
				return t;
			});
			
			return p;
		};
		
		@Cache({ "a", "b" })
		public Action add = (ctx) -> {
			return ctx.call("v2.test.list", ctx.params(), null).then((t) -> {
				
				t.putObject("list", t);			
				return t;
				
			}).then((t) -> {
				
				// return ctx.call("posts.find", ctx.params(), null).then((posts) -> {
				// return posts.size();
				// });
				return t; 
				
			}).then((t) -> {
				
				return t;
				
			}).then((t) -> {
				
				return t;
				
			}).then((t) -> {
				
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
