package services.moleculer.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.breaker.TestTransporter;
import services.moleculer.context.Context;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.util.FastBuildTree;

public class ServiceTest extends TestCase {

	// --- VARIABLES ---

	protected TestTransporter tr;
	protected DefaultServiceRegistry sr;
	protected ServiceBroker br;

	// --- TEST METHODS ---

	@Test
	public void testCall() throws Exception {

		// Normal internal call
		br.createService("test", new TestService());
		Tree rsp = br.call("test.add", "a", 3, "b", 6).waitFor();
		assertEquals(9, (int) rsp.asInteger());

		// Invalid call
		try {
			br.call("test.invalid", "a", 3, "b", 6).waitFor();
			assertTrue(false);
		} catch (Exception e) {

			// Must throw an error
		}

		// Using filter
		br.use(new TestFilter());
		rsp = br.call("test.add", "a", 3, "b", 6).waitFor();
		assertEquals(123, (int) rsp.asInteger());

		// Incoming call
		putIncomingCall("test.add", new Tree().put("a", 2).put("b", 3));
		assertEquals(1, tr.getMessageCount());
		rsp = tr.getMessages().removeFirst();
		assertEquals("local", rsp.get("sender", ""));
		assertEquals("123", rsp.get("id", ""));
		assertEquals("3", rsp.get("ver", ""));
		assertEquals(true, rsp.get("success", false));
		assertEquals(123, rsp.get("data", -1));
		assertEquals("MOL.RES.node5", rsp.get("channel", ""));
	}

	protected void putIncomingCall(String name, Tree params) throws Exception {
		FastBuildTree msg = new FastBuildTree(6);
		msg.putUnsafe("ver", ServiceBroker.PROTOCOL_VERSION);
		msg.putUnsafe("sender", "node5");
		msg.putUnsafe("action", name);
		msg.putUnsafe("id", "123");
		if (params != null) {
			msg.putUnsafe("params", params);
		}
		tr.received(tr.requestChannel, msg);
	}

	protected static final class TestService extends Service {

		public Action add = ctx -> {
			return ctx.params.get("a", 0) + ctx.params.get("b", 0);
		};
	}

	public class TestFilter extends Middleware {

		public Action install(Action action, Tree config) {
			if (config.get("name", "?").endsWith("add")) {
				return new Action() {

					@Override
					public Object handler(Context ctx) throws Exception {
						Object original = action.handler(ctx);
						Object replaced = 123;
						broker.getLogger().info("Middleware invoked! Replacing " + original + " to " + replaced);
						return replaced;
					}

				};
			}
			return null;
		}

	}

	// --- SET UP ---

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void setUp() throws Exception {
		sr = new DefaultServiceRegistry();
		tr = new TestTransporter();
		ExecutorService ex = new ExecutorService() {

			@Override
			public void execute(Runnable command) {
				command.run();
			}

			@Override
			public <T> Future<T> submit(Runnable task, T result) {
				task.run();
				return CompletableFuture.completedFuture(result);
			}

			@Override
			public Future<?> submit(Runnable task) {
				task.run();
				return CompletableFuture.completedFuture(null);
			}

			@Override
			public <T> Future<T> submit(Callable<T> task) {
				try {
					return CompletableFuture.completedFuture(task.call());
				} catch (Exception e) {
					CompletableFuture future = CompletableFuture.completedFuture(null);
					future.completeExceptionally(e);
					return future;
				}
			}

			@Override
			public List<Runnable> shutdownNow() {
				return Collections.emptyList();
			}

			@Override
			public void shutdown() {
			}

			@Override
			public boolean isTerminated() {
				return false;
			}

			@Override
			public boolean isShutdown() {
				return false;
			}

			@Override
			public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				return null;
			}

			@Override
			public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
					throws InterruptedException, ExecutionException {
				return null;
			}

			@Override
			public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
					throws InterruptedException {
				return null;
			}

			@Override
			public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
				return null;
			}

			@Override
			public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
				return false;
			}

		};
		br = ServiceBroker.builder().monitor(new ConstantMonitor()).registry(sr).transporter(tr).nodeID("local")
				.executor(ex).build();
		br.start();
	}

	// --- TEAR DOWN ---

	@Override
	protected void tearDown() throws Exception {
		if (br != null) {
			br.stop();
		}
	}

}
