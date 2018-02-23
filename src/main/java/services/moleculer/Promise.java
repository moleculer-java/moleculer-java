/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.datatree.Tree;
import services.moleculer.util.CheckedTree;

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
	 * An internal CompletableFuture, what does the working logic of this
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
	 * <b>})</b>.catchError(error -> {<br>
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
	public Promise then(CheckedFunction<Tree> action) {
		return new Promise(future.handle((data, error) -> {
			if (error != null) {
				return error;
			}
			try {
				return action.apply(data);				
			} catch (Throwable cause) {
				return cause;
			}
		}));
	}

	/**
	 * Promises can be used to unnest asynchronous functions and allows one to
	 * chain multiple functions together - increasing readability and making
	 * individual functions, within the chain, more reusable. Sample code:<br>
	 * <br>
	 * return Promise.resolve().<b>then((value) -> {</b><br>
	 * <i>// ...do something without any return value...</i><br>
	 * <b>});</b>
	 * 
	 * @param action
	 *            next action in the invocation chain
	 * 
	 * @return output Promise
	 */
	public Promise then(CheckedConsumer<Tree> action) {
		return new Promise(future.handle((data, error) -> {
			if (error != null) {
				return error;
			}
			try {
				action.accept(data);				
			} catch (Throwable cause) {
				return cause;
			}
			return data;
		}));
	}

	// --- ERROR HANDLER METHODS ---

	/**
	 * The catchError() method returns a Promise and deals with rejected cases only.
	 * Sample:<br>
	 * <br>
	 * Promise.resolve().then(() -> {<br>
	 * <i>// do something</i><br>
	 * return 123;<br>
	 * <b>}).catchError(error -> {</b><br>
	 * <i>// Catch error</i><br>
	 * return 456;<br>
	 * });
	 * 
	 * @param action
	 *            error handler of the previous "next" handlers
	 * 
	 * @return output Promise (allowed return types: Tree, String, int, double,
	 *         byte, float, short, long, boolean, byte[], UUID, Date,
	 *         InetAddress, BigInteger, BigDecimal, and Java Collections with
	 *         these types)
	 */
	public Promise catchError(CheckedFunction<Throwable> action) {
		return new Promise(future.handle((data, error) -> {
			if (error != null) {
				try {
					return action.apply(error);				
				} catch (Throwable cause) {
					return cause;
				}
			}
			return data;
		}));
	}

	/**
	 * The catchError() method returns a Promise and deals with rejected cases only.
	 * Sample:<br>
	 * <br>
	 * Promise.resolve().then(() -> {<br>
	 * <i>// do something</i><br>
	 * return 123;<br>
	 * <b>}).catchError(error -> {</b><br>
	 * <i>// ...do something without any return value...</i><br>
	 * });
	 * 
	 * @param action
	 *            error handler of the previous "next" handlers
	 * 
	 * @return output Promise
	 */
	public Promise catchError(CheckedConsumer<Throwable> action) {
		return new Promise(future.handle((data, error) -> {
			if (error != null) {
				try {
					action.accept(error);				
				} catch (Throwable cause) {
					return cause;
				}
			}
			return data;
		}));
	}

	// --- COMPLETE UNRESOLVED / INCOMPLETED PROMISE ---

	/**
	 * If not already completed, sets the value to {@code null}. Sample code:
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
	 * p.complete();
	 * 
	 * @return {@code true} if this invocation caused this Promise to transition
	 *         to a completed state, else {@code false}
	 */
	public boolean complete() {
		return future.complete(new CheckedTree(null));
	}

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
	 * p.catchError((error) -> {<br>
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

	public static final Promise all(Collection<Promise> promises) {
		if (promises == null || promises.isEmpty()) {
			return Promise.resolve();
		}
		Promise[] array = new Promise[promises.size()];
		promises.toArray(array);
		return all(array);
	}

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
		if (promises == null || promises.length == 0) {
			return Promise.resolve();
		}

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

	public static final Promise race(Collection<Promise> promises) {
		if (promises == null || promises.isEmpty()) {
			return Promise.resolve();
		}
		Promise[] array = new Promise[promises.size()];
		promises.toArray(array);
		return race(array);
	}

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
		if (promises == null || promises.length == 0) {
			return Promise.resolve();
		}

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
		if (object == null) {
			return CompletableFuture.completedFuture(null);
		}
		if (object instanceof CompletableFuture) {
			return ((CompletableFuture<?>) object).thenCompose(Promise::toCompletableFuture);
		}
		if (object instanceof Promise) {
			return ((Promise) object).future;
		}
		if (object instanceof Throwable) {
			CompletableFuture<Tree> future = new CompletableFuture<>();
			future.completeExceptionally((Throwable) object);
			return future;
		}
		if (object instanceof CompletionStage) {
			CompletableFuture<Tree> future = new CompletableFuture<>();
			((CompletionStage<?>) object).handle((value, error) -> {
				if (error == null) {
					future.complete(toTree(value));
				} else {
					future.completeExceptionally(error);
				}
				return null;
			});
			return future;
		}
		return CompletableFuture.completedFuture(toTree(object));
	}

	@SuppressWarnings("unchecked")
	protected static final Tree toTree(Object object) {
		if (object == null) {
			return new CheckedTree(null);
		}
		if (object instanceof Tree) {
			return (Tree) object;
		}
		if (object instanceof Map) {
			return new Tree((Map<String, Object>) object);
		}
		if (object instanceof Collection) {			
			return new Tree((Collection<Object>) object);
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

		public final void resolve() {
			future.complete(new CheckedTree(null));
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

	@FunctionalInterface
	public static interface CheckedConsumer<IN> {
		
	    /**
	     * Performs this operation on the given argument.
	     *
	     * @param t the input argument
	     */
	    void accept(IN in) throws Exception;
		
	}

	@FunctionalInterface
	public static interface CheckedFunction<IN> {
		
	    /**
	     * Applies this function to the given argument.
	     *
	     * @param t the function argument
	     * @return the function result
	     */
		Object apply(IN in) throws Throwable;
		
	}
		
}