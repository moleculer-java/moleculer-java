package services.moleculer.fibers;

import static services.moleculer.config.ServiceBrokerConfig.AGENT_ENABLED;
import static services.moleculer.config.ServiceBrokerConfig.SCHEDULER_FACTORY;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;

public final class FiberEngine {

	// --- COMMON SCHEDULER ---

	private static FiberScheduler nonBlockingScheduler;

	private static ExecutorService blockingExecutor;

	// --- INITALIZE ENGINE ---

	private FiberEngine() {
	}

	static {
		nonBlockingScheduler = SCHEDULER_FACTORY.createNonBlockingScheduler();
		blockingExecutor = SCHEDULER_FACTORY.createBlockingExecutor();
	}

	// --- GET SCHEDULER FOR NON-BLOCKING TASKS ---

	public static final FiberScheduler getNonBlockingScheduler() {
		return nonBlockingScheduler;
	}

	// --- GET EXECUTOR FOR BLOCKING TASKS ---

	public static final ExecutorService getBlockingExecutor() {
		return blockingExecutor;
	}

	// --- SHUTDOWN NON-BLOCKING SCHEDULER AND BLOCKING EXECUTOR ---

	public static List<Runnable> shutdownNow() {
		LinkedList<Runnable> canceledTasks = new LinkedList<Runnable>();
		if (nonBlockingScheduler != null) {
			try {
				Executor executor = nonBlockingScheduler.getExecutor();
				if (executor instanceof ExecutorService) {
					ExecutorService service = (ExecutorService) executor;
					if (!service.isShutdown() && !service.isTerminated()) {
						canceledTasks.addAll(service.shutdownNow());
					}
				}
			} catch (Throwable ignored) {
			}
		}
		if (blockingExecutor != null) {
			try {
				if (!blockingExecutor.isShutdown() && !blockingExecutor.isTerminated()) {
					canceledTasks.addAll(blockingExecutor.shutdownNow());
				}
			} catch (Throwable ignored) {
			}
		}
		return canceledTasks;
	}

	// --- COMMON "RUN NON-BLOCKING" FUNCTION ---

	public static final <ANY> void runNonBlockingCallable(SuspendableCallable<ANY> callable) {

		// Run as Fiber thread
		if (AGENT_ENABLED) {
			new Fiber<ANY>(null, nonBlockingScheduler, Fiber.DEFAULT_STACK_SIZE, callable).start();
			return;
		}

		// Run directly in the blocking executor
		runBlockingRunnable(() -> {
			try {
				callable.run();
			} catch (Exception ingored) {
			}
		});
	}

	// --- COMMON "RUN BLOCKING" FUNCTION ---

	public static final void runBlockingRunnable(Runnable runnable) {
		blockingExecutor.execute(runnable);
	}

	// --- COMMON "INVOKE BLOCKING" FUNCTION ---

	/**
	 * Invokes an I/O blocking method from a lightweight or native thread.
	 * 
	 * @param callable
	 *            Callable to start in a heavyweight thread, and waits for its
	 * 
	 * @return response of the CheckedCallable's "call" method
	 * 
	 * @throws InterruptedException
	 * @throws SuspendExecution
	 * @throws ERR
	 */
	public static final <OUT, ERR extends Exception> OUT invokeBlockingCallable(CheckedCallable<OUT, ERR> callable)
			throws InterruptedException, SuspendExecution, ERR {

		// Suspend this fiber-thread, and executes task in the blocking executor
		if (AGENT_ENABLED && Fiber.isCurrentFiber()) {
			return FiberAsync.runBlocking(blockingExecutor, callable);
		}

		// Invoke blocking code directly (we are in a blocking thread)
		return callable.call();
	}

	// --- CONSUMER WITHOUT PARAMETERS ---

	/**
	 * Void method with 0 input parameters.
	 *
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C0<ERR extends Exception> {
		void invoke() throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <ERR extends Exception> void callBlocking(C0<ERR> consumer)
			throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke();
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <ERR extends Exception> void runBlocking(C0<ERR> consumer) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke();
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITHOUT PARAMETERS ---

	/**
	 * Function with one output value and 0 input parameters.
	 *
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F0<OUT, ERR extends Exception> {
		OUT invoke() throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <OUT, ERR extends Exception> OUT invokeBlocking(F0<OUT, ERR> function)
			throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke();
		});
	}

	// --- CONSUMER WITH 1 PARAMETERS ---

	/**
	 * Void method with 1 input parameter.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C1<IN1, ERR extends Exception> {
		void invoke(IN1 in1) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, ERR extends Exception> void callBlocking(C1<IN1, ERR> consumer, IN1 in1)
			throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, ERR extends Exception> void runBlocking(C1<IN1, ERR> consumer, IN1 in1) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 1 PARAMETERS ---

	/**
	 * Function with one output value and 1 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F1<IN1, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, OUT, ERR extends Exception> OUT invokeBlocking(F1<IN1, OUT, ERR> function, IN1 in1)
			throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1);
		});
	}

	// --- CONSUMER WITH 2 PARAMETERS ---

	/**
	 * Void method with 2 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C2<IN1, IN2, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, ERR extends Exception> void callBlocking(C2<IN1, IN2, ERR> consumer, IN1 in1,
			IN2 in2) throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, ERR extends Exception> void runBlocking(C2<IN1, IN2, ERR> consumer, IN1 in1,
			IN2 in2) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 2 PARAMETERS ---

	/**
	 * Function with one output value and 2 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F2<IN1, IN2, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, OUT, ERR extends Exception> OUT invokeBlocking(F2<IN1, IN2, OUT, ERR> function,
			IN1 in1, IN2 in2) throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2);
		});
	}

	// --- CONSUMER WITH 3 PARAMETERS ---

	/**
	 * Void method with 3 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C3<IN1, IN2, IN3, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, ERR extends Exception> void callBlocking(C3<IN1, IN2, IN3, ERR> consumer,
			IN1 in1, IN2 in2, IN3 in3) throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, ERR extends Exception> void runBlocking(C3<IN1, IN2, IN3, ERR> consumer,
			IN1 in1, IN2 in2, IN3 in3) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 3 PARAMETERS ---

	/**
	 * Function with one output value and 3 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F3<IN1, IN2, IN3, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, OUT, ERR extends Exception> OUT invokeBlocking(
			F3<IN1, IN2, IN3, OUT, ERR> function, IN1 in1, IN2 in2, IN3 in3)
			throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3);
		});
	}

	// --- CONSUMER WITH 4 PARAMETERS ---

	/**
	 * Void method with 4 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C4<IN1, IN2, IN3, IN4, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, ERR extends Exception> void callBlocking(
			C4<IN1, IN2, IN3, IN4, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4)
			throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, ERR extends Exception> void runBlocking(
			C4<IN1, IN2, IN3, IN4, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 4 PARAMETERS ---

	/**
	 * Function with one output value and 4 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F4<IN1, IN2, IN3, IN4, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, OUT, ERR extends Exception> OUT invokeBlocking(
			F4<IN1, IN2, IN3, IN4, OUT, ERR> function, IN1 in1, IN2 in2, IN3 in3, IN4 in4)
			throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4);
		});
	}

	// --- CONSUMER WITH 5 PARAMETERS ---

	/**
	 * Void method with 5 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C5<IN1, IN2, IN3, IN4, IN5, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, ERR extends Exception> void callBlocking(
			C5<IN1, IN2, IN3, IN4, IN5, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5)
			throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, ERR extends Exception> void runBlocking(
			C5<IN1, IN2, IN3, IN4, IN5, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 5 PARAMETERS ---

	/**
	 * Function with one output value and 5 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F5<IN1, IN2, IN3, IN4, IN5, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, OUT, ERR extends Exception> OUT invokeBlocking(
			F5<IN1, IN2, IN3, IN4, IN5, OUT, ERR> function, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5)
			throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5);
		});
	}

	// --- CONSUMER WITH 6 PARAMETERS ---

	/**
	 * Void method with 6 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C6<IN1, IN2, IN3, IN4, IN5, IN6, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, ERR extends Exception> void callBlocking(
			C6<IN1, IN2, IN3, IN4, IN5, IN6, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6)
			throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, ERR extends Exception> void runBlocking(
			C6<IN1, IN2, IN3, IN4, IN5, IN6, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 6 PARAMETERS ---

	/**
	 * Function with one output value and 6 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F6<IN1, IN2, IN3, IN4, IN5, IN6, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, OUT, ERR extends Exception> OUT invokeBlocking(
			F6<IN1, IN2, IN3, IN4, IN5, IN6, OUT, ERR> function, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6)
			throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6);
		});
	}

	// --- CONSUMER WITH 7 PARAMETERS ---

	/**
	 * Void method with 7 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C7<IN1, IN2, IN3, IN4, IN5, IN6, IN7, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, ERR extends Exception> void callBlocking(
			C7<IN1, IN2, IN3, IN4, IN5, IN6, IN7, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6,
			IN7 in7) throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, ERR extends Exception> void runBlocking(
			C7<IN1, IN2, IN3, IN4, IN5, IN6, IN7, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6,
			IN7 in7) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 7 PARAMETERS ---

	/**
	 * Function with one output value and 7 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F7<IN1, IN2, IN3, IN4, IN5, IN6, IN7, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, OUT, ERR extends Exception> OUT invokeBlocking(
			F7<IN1, IN2, IN3, IN4, IN5, IN6, IN7, OUT, ERR> function, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5,
			IN6 in6, IN7 in7) throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7);
		});
	}

	// --- CONSUMER WITH 8 PARAMETERS ---

	/**
	 * Void method with 8 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C8<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, ERR extends Exception> void callBlocking(
			C8<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5,
			IN6 in6, IN7 in7, IN8 in8) throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, ERR extends Exception> void runBlocking(
			C8<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5,
			IN6 in6, IN7 in7, IN8 in8) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 8 PARAMETERS ---

	/**
	 * Function with one output value and 8 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F8<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, OUT, ERR extends Exception> OUT invokeBlocking(
			F8<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, OUT, ERR> function, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5,
			IN6 in6, IN7 in7, IN8 in8) throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7, in8);
		});
	}

	// --- CONSUMER WITH 9 PARAMETERS ---

	/**
	 * Void method with 9 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C9<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, ERR extends Exception> void callBlocking(
			C9<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5,
			IN6 in6, IN7 in7, IN8 in8, IN9 in9) throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, ERR extends Exception> void runBlocking(
			C9<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5,
			IN6 in6, IN7 in7, IN8 in8, IN9 in9) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 9 PARAMETERS ---

	/**
	 * Function with one output value and 9 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F9<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, OUT, ERR extends Exception> OUT invokeBlocking(
			F9<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, OUT, ERR> function, IN1 in1, IN2 in2, IN3 in3, IN4 in4,
			IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9) throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9);
		});
	}

	// --- CONSUMER WITH 10 PARAMETERS ---

	/**
	 * Void method with 10 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C10<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10)
				throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, ERR extends Exception> void callBlocking(
			C10<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4,
			IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10) throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, ERR extends Exception> void runBlocking(
			C10<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, ERR> consumer, IN1 in1, IN2 in2, IN3 in3, IN4 in4,
			IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 10 PARAMETERS ---

	/**
	 * Function with one output value and 10 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F10<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10)
				throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, OUT, ERR extends Exception> OUT invokeBlocking(
			F10<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, OUT, ERR> function, IN1 in1, IN2 in2, IN3 in3,
			IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10)
			throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10);
		});
	}

	// --- CONSUMER WITH 11 PARAMETERS ---

	/**
	 * Void method with 11 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C11<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, ERR extends Exception> void callBlocking(
			C11<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, ERR> consumer, IN1 in1, IN2 in2, IN3 in3,
			IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11)
			throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, ERR extends Exception> void runBlocking(
			C11<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, ERR> consumer, IN1 in1, IN2 in2, IN3 in3,
			IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 11 PARAMETERS ---

	/**
	 * Function with one output value and 11 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F11<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, OUT, ERR extends Exception> OUT invokeBlocking(
			F11<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, OUT, ERR> function, IN1 in1, IN2 in2, IN3 in3,
			IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11)
			throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11);
		});
	}

	// --- CONSUMER WITH 12 PARAMETERS ---

	/**
	 * Void method with 12 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C12<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, ERR extends Exception> void callBlocking(
			C12<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, ERR> consumer, IN1 in1, IN2 in2, IN3 in3,
			IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12)
			throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, ERR extends Exception> void runBlocking(
			C12<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, ERR> consumer, IN1 in1, IN2 in2, IN3 in3,
			IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 12 PARAMETERS ---

	/**
	 * Function with one output value and 12 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F12<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, OUT, ERR extends Exception> OUT invokeBlocking(
			F12<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, OUT, ERR> function, IN1 in1, IN2 in2,
			IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12)
			throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12);
		});
	}

	// --- CONSUMER WITH 13 PARAMETERS ---

	/**
	 * Void method with 13 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <IN13>
	 *            type of the 13. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C13<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12, IN13 in13) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, ERR extends Exception> void callBlocking(
			C13<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, ERR> consumer, IN1 in1, IN2 in2,
			IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12, IN13 in13)
			throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, ERR extends Exception> void runBlocking(
			C13<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, ERR> consumer, IN1 in1, IN2 in2,
			IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12, IN13 in13) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 13 PARAMETERS ---

	/**
	 * Function with one output value and 13 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <IN13>
	 *            type of the 13. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F13<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12, IN13 in13) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, OUT, ERR extends Exception> OUT invokeBlocking(
			F13<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, OUT, ERR> function, IN1 in1,
			IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12,
			IN13 in13) throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13);
		});
	}

	// --- CONSUMER WITH 14 PARAMETERS ---

	/**
	 * Void method with 14 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <IN13>
	 *            type of the 13. input parameter
	 * @param <IN14>
	 *            type of the 14. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C14<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12, IN13 in13, IN14 in14) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @param in14
	 *            value of the 14. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, ERR extends Exception> void callBlocking(
			C14<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, ERR> consumer, IN1 in1,
			IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12,
			IN13 in13, IN14 in14) throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @param in14
	 *            value of the 14. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, ERR extends Exception> void runBlocking(
			C14<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, ERR> consumer, IN1 in1,
			IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12,
			IN13 in13, IN14 in14) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 14 PARAMETERS ---

	/**
	 * Function with one output value and 14 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <IN13>
	 *            type of the 13. input parameter
	 * @param <IN14>
	 *            type of the 14. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F14<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12, IN13 in13, IN14 in14) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @param in14
	 *            value of the 14. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, OUT, ERR extends Exception> OUT invokeBlocking(
			F14<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, OUT, ERR> function, IN1 in1,
			IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12,
			IN13 in13, IN14 in14) throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14);
		});
	}

	// --- CONSUMER WITH 15 PARAMETERS ---

	/**
	 * Void method with 15 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <IN13>
	 *            type of the 13. input parameter
	 * @param <IN14>
	 *            type of the 14. input parameter
	 * @param <IN15>
	 *            type of the 15. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C15<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12, IN13 in13, IN14 in14, IN15 in15) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @param in14
	 *            value of the 14. input parameter
	 * @param in15
	 *            value of the 15. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, ERR extends Exception> void callBlocking(
			C15<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, ERR> consumer, IN1 in1,
			IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12,
			IN13 in13, IN14 in14, IN15 in15) throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14, in15);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @param in14
	 *            value of the 14. input parameter
	 * @param in15
	 *            value of the 15. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, ERR extends Exception> void runBlocking(
			C15<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, ERR> consumer, IN1 in1,
			IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11, IN12 in12,
			IN13 in13, IN14 in14, IN15 in15) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14, in15);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 15 PARAMETERS ---

	/**
	 * Function with one output value and 15 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <IN13>
	 *            type of the 13. input parameter
	 * @param <IN14>
	 *            type of the 14. input parameter
	 * @param <IN15>
	 *            type of the 15. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F15<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12, IN13 in13, IN14 in14, IN15 in15) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @param in14
	 *            value of the 14. input parameter
	 * @param in15
	 *            value of the 15. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, OUT, ERR extends Exception> OUT invokeBlocking(
			F15<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, OUT, ERR> function,
			IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11,
			IN12 in12, IN13 in13, IN14 in14, IN15 in15) throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14, in15);
		});
	}

	// --- CONSUMER WITH 16 PARAMETERS ---

	/**
	 * Void method with 16 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <IN13>
	 *            type of the 13. input parameter
	 * @param <IN14>
	 *            type of the 14. input parameter
	 * @param <IN15>
	 *            type of the 15. input parameter
	 * @param <IN16>
	 *            type of the 16. input parameter
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface C16<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, IN16, ERR extends Exception> {
		void invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12, IN13 in13, IN14 in14, IN15 in15, IN16 in16) throws ERR;
	}

	/**
	 * Invokes a void blocking consumer, and waits for this consumer's end.
	 *
	 * @param consumer
	 *            to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @param in14
	 *            value of the 14. input parameter
	 * @param in15
	 *            value of the 15. input parameter
	 * @param in16
	 *            value of the 16. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, IN16, ERR extends Exception> void callBlocking(
			C16<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, IN16, ERR> consumer,
			IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11,
			IN12 in12, IN13 in13, IN14 in14, IN15 in15, IN16 in16) throws InterruptedException, SuspendExecution, ERR {
		invokeBlockingCallable(() -> {
			consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14, in15, in16);
			return null;
		});
	}

	/**
	 * Starts blocking consumer method in the blocking executor's pool.
	 *
	 * @param consumer
	 *            to start in a separated thread
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @param in14
	 *            value of the 14. input parameter
	 * @param in15
	 *            value of the 15. input parameter
	 * @param in16
	 *            value of the 16. input parameter
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, IN16, ERR extends Exception> void runBlocking(
			C16<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, IN16, ERR> consumer,
			IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11,
			IN12 in12, IN13 in13, IN14 in14, IN15 in15, IN16 in16) {
		blockingExecutor.execute(() -> {
			try {
				consumer.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14, in15, in16);
			} catch (Exception cause) {
				error(cause);
			}
		});
	}

	// --- FUNCTION WITH 16 PARAMETERS ---

	/**
	 * Function with one output value and 16 input parameters.
	 *
	 * @param <IN1>
	 *            type of the 1. input parameter
	 * @param <IN2>
	 *            type of the 2. input parameter
	 * @param <IN3>
	 *            type of the 3. input parameter
	 * @param <IN4>
	 *            type of the 4. input parameter
	 * @param <IN5>
	 *            type of the 5. input parameter
	 * @param <IN6>
	 *            type of the 6. input parameter
	 * @param <IN7>
	 *            type of the 7. input parameter
	 * @param <IN8>
	 *            type of the 8. input parameter
	 * @param <IN9>
	 *            type of the 9. input parameter
	 * @param <IN10>
	 *            type of the 10. input parameter
	 * @param <IN11>
	 *            type of the 11. input parameter
	 * @param <IN12>
	 *            type of the 12. input parameter
	 * @param <IN13>
	 *            type of the 13. input parameter
	 * @param <IN14>
	 *            type of the 14. input parameter
	 * @param <IN15>
	 *            type of the 15. input parameter
	 * @param <IN16>
	 *            type of the 16. input parameter
	 * @param <OUT>
	 *            function's output
	 * @param <ERR>
	 *            an Exception
	 */
	@FunctionalInterface
	public interface F16<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, IN16, OUT, ERR extends Exception> {
		OUT invoke(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10,
				IN11 in11, IN12 in12, IN13 in13, IN14 in14, IN15 in15, IN16 in16) throws ERR;
	}

	/**
	 * Invokes a blocking function, and returns its result.
	 *
	 * @param function
	 *            the function to invoke
	 * @param in1
	 *            value of the 1. input parameter
	 * @param in2
	 *            value of the 2. input parameter
	 * @param in3
	 *            value of the 3. input parameter
	 * @param in4
	 *            value of the 4. input parameter
	 * @param in5
	 *            value of the 5. input parameter
	 * @param in6
	 *            value of the 6. input parameter
	 * @param in7
	 *            value of the 7. input parameter
	 * @param in8
	 *            value of the 8. input parameter
	 * @param in9
	 *            value of the 9. input parameter
	 * @param in10
	 *            value of the 10. input parameter
	 * @param in11
	 *            value of the 11. input parameter
	 * @param in12
	 *            value of the 12. input parameter
	 * @param in13
	 *            value of the 13. input parameter
	 * @param in14
	 *            value of the 14. input parameter
	 * @param in15
	 *            value of the 15. input parameter
	 * @param in16
	 *            value of the 16. input parameter
	 * @out function's output
	 * @throws InterruptedException
	 *             task interrrupted
	 * @throws SuspendExecution
	 *             marker for java agent
	 * @throws ERR
	 *             function's fault response
	 */
	public static final <IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, IN16, OUT, ERR extends Exception> OUT invokeBlocking(
			F16<IN1, IN2, IN3, IN4, IN5, IN6, IN7, IN8, IN9, IN10, IN11, IN12, IN13, IN14, IN15, IN16, OUT, ERR> function,
			IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5, IN6 in6, IN7 in7, IN8 in8, IN9 in9, IN10 in10, IN11 in11,
			IN12 in12, IN13 in13, IN14 in14, IN15 in15, IN16 in16) throws InterruptedException, SuspendExecution, ERR {
		return invokeBlockingCallable(() -> {
			return function.invoke(in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14, in15,
					in16);
		});
	}

	// --- CALLBACK HANDLING ---

	@FunctionalInterface
	public interface Callback<OUT> {

		@Suspendable
		void invoke(OUT out, Throwable err);
	}

	@FunctionalInterface
	public interface CallbackInvoker<OUT> {
		void invoke(Callback<OUT> callback) throws SuspendExecution, InterruptedException;
	}

	public static final <OUT> OUT invokeCallback(CallbackInvoker<OUT> invoker)
			throws SuspendExecution, InterruptedException, Throwable {
		return invokeCallback(invoker, 0, null);
	}

	public static final <OUT> OUT invokeCallback(CallbackInvoker<OUT> invoker, long timeout, TimeUnit unit)
			throws SuspendExecution, InterruptedException, Throwable {

		// We are in a lightweight thread
		if (AGENT_ENABLED && Fiber.isCurrentFiber()) {
			return new FiberAsync<OUT, Throwable>() {

				private static final long serialVersionUID = 1L;

				@Override
				protected final void requestAsync() {
					try {
						invoker.invoke((out, err) -> {
							if (err == null) {
								asyncCompleted(out);
							} else {
								asyncFailed(err);
							}
						});
					} catch (Throwable cause) {
						asyncFailed(cause);
					}
				}

			}.run(timeout, unit);
		}

		// We are in a heavyweight thread
		CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<OUT> output = new AtomicReference<OUT>();
		final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
		invoker.invoke((out, err) -> {
			if (err == null) {
				output.set(out);
			} else {
				cause.set(err);
			}
			latch.countDown();
		});
		if (unit == null) {
			latch.await();
		} else {
			latch.await(timeout, unit);
		}
		Throwable err = cause.get();
		if (err != null) {
			throw err;
		}
		return output.get();
	}

	// --- LOGGER ---

	private static final void error(Exception cause) {
		cause.printStackTrace();
	}

}