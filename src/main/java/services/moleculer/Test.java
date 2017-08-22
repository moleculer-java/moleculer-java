package services.moleculer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import io.datatree.Tree;

public class Test {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {

		ServiceBroker broker = new ServiceBroker();
		
		Service svc = broker.createService(new Service(broker, "test", null) {

			// --- CREATED ---

			@Override
			public void created() {

				// Created
				this.logger.debug("Service created!");
				processData();
			}

			// --- ACTIONS ---

			@Cache()
			@Version("v1")
			public Action list = (ctx) -> {
				return this.processData();
			};

			@Cache(false)
			@Version("v2")
			public Action add = (ctx) -> {
				
				return null;
			};

			// --- EVENT LISTENERS ---
			
			// Context, Tree, or Object????
			public Listener test = (input) -> {
				
			};
			
			// --- METHODS ---

			int processData() {
				this.logger.info("Process data invoked!");
				return 1;
			}

		});
		
		broker.start();

		// ---------
		
		Object result = broker.call("v1.test.list", new Tree().put("a", 5), null);
		System.out.println("RESULT: " + result);

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

	}

}
