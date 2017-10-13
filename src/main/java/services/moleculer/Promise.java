package services.moleculer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import io.datatree.Tree;

/**
 * ES6-like Promise based on the Java8's CompletableFuture API. A Promise is an
 * object that may produce a single value some time in the future: either a
 * resolved value, or a reason that it's not resolved (e.g., a network error
 * occurred). Promise users can attach callbacks to handle the fulfilled value
 * or the reason for rejection.
 */
public class Promise {

	// --- INTERNAL COMPLETABLE FUTURE ---

	/**
	 * An internal CompletableFuture, which does the working logic of this
	 * Promise.
	 */
	protected final CompletableFuture<Tree> future;

	// --- STATIC CONSTRUCTORS ---

	/**
	 * Returns a Promise object that is resolved with {@code null} value.
	 * 
	 * @return new RESOLVED/COMPLETED Promise
	 */
	public static final Promise resolve() {
		return new Promise(CompletableFuture.completedFuture(null));
	}

	/**
	 * Returns a Promise object that is resolved with the given value. Allowed
	 * Object types of the "value" parameter are: Tree, String, int, double,
	 * byte, float, short, long, boolean, byte[], UUID, Date, InetAddress,
	 * BigInteger, BigDecimal, and Java Collections with these types.
	 * 
	 * @param value
	 *            value of the new Promise
	 * 
	 * @return new RESOLVED/COMPLETED Promise
	 */
	public static final Promise resolve(Object value) {
		return new Promise(toCompletableFuture(value));
	}

	/**
	 * Returns a Promise object that is rejected with the given reason.
	 * 
	 * @param error
	 *            error state of the new Promise
	 * 
	 * @return new REJECTED/COMPLETED EXCEPTIONALLY Promise
	 */
	public static final Promise reject(Throwable error) {
		CompletableFuture<Tree> future = new CompletableFuture<>();
		future.completeExceptionally(error);
		return new Promise(future);
	}

	/**
	 * Returns a Promise object that is rejected with an empty Exception.
	 * 
	 * @param error
	 *            error state of the new Promise
	 * 
	 * @return new REJECTED/COMPLETED EXCEPTIONALLY Promise
	 */
	public static final Promise reject() {
		return reject(new Exception());
	}

	// --- PUBLIC CONSTRUCTOR ---

	/**
	 * Creates an empty PENDING/INCOMPLETED Promise.
	 */
	public Promise() {
		future = new CompletableFuture<>();
	}

	/**
	 * Creates a Promise with an asynchronous initializer. Sample code:<br>
	 * <br>
	 * <b>return new Promise((r) -> {</b><br>
	 * Tree value = new Tree();<br>
	 * value.put("a.b.c", 3);<br>
	 * r.resolve(value);<br>
	 * <b>});</b>
	 */
	public Promise(Initializer initializer) {
		future = new CompletableFuture<>();
		initializer.init(new Resolver(future));
	}

	/**
	 * Creates a Promise.
	 * 
	 * @param value
	 *            Promise, CompletableFuture, Tree, String, int, double, byte,
	 *            float, short, long, boolean, byte[], UUID, Date, InetAddress,
	 *            BigInteger, BigDecimal, and Java Collections with these types.
	 */
	public Promise(Object value) {
		future = toCompletableFuture(value);
	}

	// --- PROTECTED CONSTRUCTOR ---

	protected Promise(CompletableFuture<Tree> future) {
		this.future = future;
	}

	// --- WATERFALL FUNCTION ---

	/**
	 * Promises can be used to unnest asynchronous functions and allows one to
	 * chain multiple functions together - increasing readability and making
	 * individual functions, within the chain, more reusable. Sample code:<br>
	 * <br>
	 * return Promise.resolve().<b>then(value -> {</b><br>
	 * <i>// ...do something...</i><br>
	 * return value;<br>
	 * <b>}).then(value -> {</b><br>
	 * <i>// ...do something...</i><br>
	 * return value;<br>
	 * <b>})</b>.Catch(error -> {<br>
	 * <i>// ...error handling...</i><br>
	 * return value;<br>
	 * });
	 * 
	 * @param action
	 *            next action in the invocation chain (allowed return types:
	 *            Promise, CompletableFuture, Tree, String, int, double, byte,
	 *            float, short, long, boolean, byte[], UUID, Date, InetAddress,
	 *            BigInteger, BigDecimal, and Java Collections with these types)
	 * 
	 * @return output Promise
	 */
	public Promise then(Function<Tree, Object> action) {
		return new Promise(future.thenApply(action));
	}

	/**
	 * Promises can be used to unnest asynchronous functions and allows one to
	 * chain multiple functions together - increasing readability and making
	 * individual functions, within the chain, more reusable. Sample code:<br>
	 * <br>
	 * return Promise.resolve().<b>then(() -> {</b><br>
	 * <i>// ...do something...</i><br>
	 * return null;<br>
	 * <b>}).then(() -> {</b><br>
	 * <i>// ...do something...</i><br>
	 * return value;<br>
	 * <b>})</b>.Catch(error -> {<br>
	 * <i>// ...error handling...</i><br>
	 * return value;<br>
	 * });
	 * 
	 * @param action
	 *            next action in the invocation chain (allowed return types:
	 *            Promise, CompletableFuture, Tree, String, int, double, byte,
	 *            float, short, long, boolean, byte[], UUID, Date, InetAddress,
	 *            BigInteger, BigDecimal, and Java Collections with these types)
	 * 
	 * @return output Promise
	 */
	public Promise then(Supplier<Object> action) {
		return then(ignored -> action.get());
	}

	// --- ERROR HANDLER METHODS ---

	/**
	 * The Catch() method returns a Promise and deals with rejected cases only.
	 * Sample:<br>
	 * <br>
	 * Promise.resolve().then(() -> {<br>
	 * <i>// do something</i><br>
	 * return 123;<br>
	 * <b>}).Catch(error -> {</b><br>
	 * <i>// catch error</i><br>
	 * return 456;<br>
	 * })
	 * 
	 * @param action
	 *            error handler of the previous "next" handlers
	 * 
	 * @return output Promise (allowed return types: Tree, String, int, double,
	 *         byte, float, short, long, boolean, byte[], UUID, Date,
	 *         InetAddress, BigInteger, BigDecimal, and Java Collections with
	 *         these types)
	 */
	public Promise Catch(Function<Throwable, Object> action) {
		return new Promise(future.handle((data, error) -> {
			if (error != null) {
				return action.apply(error);
			}
			return data;
		}));
	}

	/**
	 * The Catch() method returns a Promise and deals with rejected cases only.
	 * Sample:<br>
	 * <br>
	 * Promise.resolve().then(() -> {<br>
	 * <i>// do something</i><br>
	 * return 123;<br>
	 * <b>}).Catch(() -> {</b><br>
	 * <i>// catch unknown error</i><br>
	 * return 456;<br>
	 * })
	 * 
	 * @param action
	 *            error handler of the previous "next" handlers
	 * 
	 * @return output Promise (allowed return types: Promise, CompletableFuture,
	 *         Tree, String, int, double, byte, float, short, long, boolean,
	 *         byte[], UUID, Date, InetAddress, BigInteger, BigDecimal, and Java
	 *         Collections with these types)
	 */
	public Promise Catch(Supplier<Object> action) {
		return Catch(ignored -> action.get());
	}

	// --- COMPLETE UNRESOLVED / INCOMPLETED PROMISE ---

	/**
	 * If not already completed, sets the value to the given value. Sample code:
	 * <br>
	 * <br>
	 * Promise p = new Promise();<br>
	 * // Listener:<br>
	 * p.next(value -> {<br>
	 * System.out.println("Received: " + value);<br>
	 * return value;<br>
	 * });<br>
	 * // Invoke chain:<br>
	 * Tree t = new Tree().put("a", "b");<br>
	 * p.complete(t);
	 * 
	 * @param value
	 *            the result value (allowed types: Tree, String, int, double,
	 *            byte, float, short, long, boolean, byte[], UUID, Date,
	 *            InetAddress, BigInteger, BigDecimal, and Java Collections with
	 *            these types)
	 * 
	 * @return {@code true} if this invocation caused this Promise to transition
	 *         to a completed state, else {@code false}
	 */
	public boolean complete(Object value) {
		return future.complete(toTree(value));
	}

	/**
	 * If not already completed, sets the exception state to the given
	 * exception. Sample code:<br>
	 * <br>
	 * Promise p = new Promise();<br>
	 * // Listener:<br>
	 * p.Catch((error) -> {<br>
	 * System.out.println("Received: " + error);<br>
	 * return null;<br>
	 * });<br>
	 * // Invoke chain:<br>
	 * p.complete(new Exception("Foo!"));
	 *
	 * @param error
	 *            the exception
	 * 
	 * @return {@code true} if this invocation caused this Promise to transition
	 *         to a completed state, else {@code false}
	 */
	public boolean complete(Throwable error) {
		return future.completeExceptionally(error);
	}

	// --- STATUS ---

	/**
	 * Returns {@code true} if this Promise completed exceptionally, in any way.
	 *
	 * @return {@code true} if this Promise completed exceptionally
	 */
	public boolean isRejected() {
		return future.isCompletedExceptionally();
	}

	/**
	 * Returns {@code true} if this Promise completed normally, in any way.
	 *
	 * @return {@code true} if this Promise completed normally
	 */
	public boolean isResolved() {
		return future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled();
	}

	/**
	 * Returns {@code true} if this Promise completed in any fashion: normally,
	 * exceptionally, or via cancellation.
	 *
	 * @return {@code true} if completed
	 */
	public boolean isDone() {
		return future.isDone();
	}

	// --- GET THE INTERNAL COMPLETABLE FUTURE ---

	/**
	 * Returns the internal CompletableFuture.
	 * 
	 * @return internal CompletableFuture
	 */
	public CompletableFuture<Tree> toCompletableFuture() {
		return future;
	}

	// --- PARALLEL ALL / ALLOF FUNCTION ---

	/**
	 * Returns a new Promise that is completed when all of the given Promise
	 * complete. If any of the given Promise complete exceptionally, then the
	 * returned Promise also does so, with a Promise holding this exception as
	 * its cause.
	 * 
	 * @param promises
	 *            array of Promises
	 * 
	 * @return a new Promise that is completed when all of the given Promises
	 *         complete
	 */
	public static final Promise all(Promise... promises) {

		@SuppressWarnings("unchecked")
		CompletableFuture<Tree>[] futures = new CompletableFuture[promises.length];
		for (int i = 0; i < promises.length; i++) {
			futures[i] = promises[i].future;
		}
		CompletableFuture<Void> all = CompletableFuture.allOf(futures);
		return new Promise((r) -> {
			all.whenComplete((Void, error) -> {
				try {
					if (error != null) {
						r.reject(error);
						return;
					}
					Tree array = new Tree().putList("array");
					for (int i = 0; i < futures.length; i++) {
						array.addObject(futures[i].get());
					}
					r.resolve(array);
				} catch (Throwable cause) {
					r.reject(cause);
				}
			});
		});
	}

	// --- PARALLEL RACE / ANYOF FUNCTION ---

	/**
	 * Returns a new Promise that is completed when any of the given Promises
	 * complete, with the same result. Otherwise, if it completed exceptionally,
	 * the returned Promise also does so, with a CompletionException holding
	 * this exception as its cause.
	 * 
	 * @param promises
	 *            array of Promises
	 * 
	 * @return a new Promise that is completed with the result or exception of
	 *         any of the given Promises when one completes
	 */
	public static final Promise race(Promise... promises) {

		@SuppressWarnings("unchecked")
		CompletableFuture<Tree>[] futures = new CompletableFuture[promises.length];
		for (int i = 0; i < promises.length; i++) {
			futures[i] = promises[i].future;
		}
		CompletableFuture<Object> any = CompletableFuture.anyOf(futures);
		return new Promise((r) -> {
			any.whenComplete((object, error) -> {
				try {
					if (error != null) {
						r.reject(error);
						return;
					}
					r.resolve((Tree) object);
				} catch (Throwable cause) {
					r.reject(cause);
				}
			});
		});
	}

	// --- CONVERTERS ---

	protected static final CompletableFuture<Tree> toCompletableFuture(Object object) {
		if (object != null) {
			if (object instanceof Promise) {
				return ((Promise) object).future;
			}
			if (object instanceof CompletableFuture) {
				return ((CompletableFuture<?>) object).thenCompose(Promise::toCompletableFuture);
			}
			if (object instanceof Throwable) {
				CompletableFuture<Tree> future = new CompletableFuture<>();
				future.completeExceptionally((Throwable) object);
				return future;
			}
		}
		return CompletableFuture.completedFuture(toTree(object));
	}

	protected static final Tree toTree(Object object) {
		if (object == null) {
			return new Tree().setObject(null);
		}
		if (object instanceof Tree) {
			return (Tree) object;
		}
		return new Tree().setObject(object);
	}

	// --- SUBCLASSES AND INTERFACES ---

	@FunctionalInterface
	public static interface Initializer {

		void init(Resolver resolver);

	}

	public static final class Resolver {

		private final CompletableFuture<Tree> future;

		private Resolver(CompletableFuture<Tree> future) {
			this.future = future;
		}

		/**
		 * Resolve the value of the current Promise with the given value.
		 * Allowed Object types of the "value" parameter are: Tree, String, int,
		 * double, byte, float, short, long, boolean, byte[], UUID, Date,
		 * InetAddress, BigInteger, BigDecimal, and Java Collections with these
		 * types.
		 * 
		 * @param value
		 *            value of the current Promise
		 */
		public final void resolve(Object value) {
			future.complete(toTree(value));
		}

		public final void reject(Throwable error) {
			future.completeExceptionally(error);
		}

	}

}