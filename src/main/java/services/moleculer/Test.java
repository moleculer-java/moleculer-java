package services.moleculer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;

import services.moleculer.cachers.MemoryCacher;

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

			@Cache(true)
			public Action list = (ctx) -> {
				return this.processData();
			};

			@Cache(false)
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

		HashMap<String, Action> map = new HashMap<>();

		Field[] fields = svc.getClass().getFields();
		for (Field field : fields) {
			if (Action.class.isAssignableFrom(field.getType())) {

				// "list"
				String name = field.getName();

				// Action instance
				Action action = (Action) field.get(svc);

				Annotation[] as = field.getAnnotations();
				for (Annotation a : as) {
					boolean cache = ((Cache) a).value();
				}

				map.put(name, action);

			}
		}

		Action action = map.get("list");

		Context ctx = null;
		Object result = action.handler(ctx);

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
		
		// -------------------
		
		MemoryCacher c = new MemoryCacher();
	    
		c.set("a.1", "a.1!");
		c.set("a.a.a", "a.a.a!");
		c.set("a.xxx", "a.xxx!");
		
		System.out.println(c.get("a.1"));
		System.out.println(c.get("a.a.a"));
		System.out.println(c.get("a.xxx"));
		
		c.clean("a.*");

		System.out.println(c.get("a.1"));
		System.out.println(c.get("a.a.a"));
		System.out.println(c.get("a.xxx"));
	}

}
