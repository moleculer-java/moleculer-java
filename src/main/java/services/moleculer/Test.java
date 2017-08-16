package services.moleculer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisStringAsyncCommands;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.EventLoopGroupProvider;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import rx.Observable;

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
		DefaultClientResources.Builder builder = DefaultClientResources.builder();

		EventLoopGroup group = new NioEventLoopGroup(1, Executors.newFixedThreadPool(1));
		builder.eventExecutorGroup(group);

		builder.eventLoopGroupProvider(new EventLoopGroupProvider() {

			@Override
			public int threadPoolSize() {
				return 1;
			}

			@Override
			public Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
				return null;
			}

			@Override
			public Future<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout,
					TimeUnit unit) {
				return null;
			}

			@SuppressWarnings("unchecked")
			@Override
			public <T extends EventLoopGroup> T allocate(Class<T> type) {
				return (T) group;
			}
		});

		builder.eventBus(new EventBus() {

			@Override
			public void publish(Event event) {
				System.out.println("event: " + event);
			}

			@Override
			public Observable<Event> get() {
				return null;
			}

		});
		
		// ----------------

		ClientResources res = builder.build();
		RedisClient client = RedisClient.create(res, "redis://localhost");
		StatefulRedisConnection<String, String> connection = client.connect();
		RedisStringAsyncCommands<String, String> async = connection.async();
		RedisFuture<String> get = async.get("key1");
		System.out.println(get.get());

		// ----------------

		StatefulRedisPubSubConnection<String, String> cps = client.connectPubSub();
		RedisPubSubAsyncCommands<String, String> messager = cps.async();
		messager.subscribe("pub1");
		cps.addListener(new RedisPubSubListener<String, String>() {

			@Override
			public void message(String channel, String message) {
				System.out.println("msg1: " + message);
			}

			@Override
			public void message(String pattern, String channel, String message) {
				System.out.println("msg2: " + message);
			}

			@Override
			public void subscribed(String channel, long count) {
			}

			@Override
			public void psubscribed(String pattern, long count) {
			}

			@Override
			public void unsubscribed(String channel, long count) {
			}

			@Override
			public void punsubscribed(String pattern, long count) {
			}

		});

		messager.publish("pub1", "val1");
		System.out.println("waiting...");
		
		Thread.sleep(100000);
		connection.close();
	}

}
