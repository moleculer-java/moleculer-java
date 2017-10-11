package services.moleculer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.datatree.Tree;

public class Promise {

	// --- INTERNAL COMPLETABLE FUTURE ---

	protected final CompletableFuture<Tree> future;

	// --- PARENT ---

	protected Promise parent;

	// --- STATIC CONSTRUCTORS ---

	public static final Promise resolve() {
		return new Promise((Tree) null, (Throwable) null);
	}

	public static final Promise resolve(Tree value) {
		if (value == null) {
			value = new Tree().setObject(null);
		}
		return new Promise(value, null);
	}

	public static final Promise reject(Throwable error) {
		return new Promise(null, error);
	}

	// --- PUBLIC CONSTRUCTOR ---

	public Promise(Initializer initializer) {
		this((Tree) null, (Throwable) null);
		if (initializer != null) {
			initializer.init(this.resolver);
		}
	}

	@FunctionalInterface
	public interface Initializer {

		public void init(Resolver resolver);

	}

	protected final Resolver resolver = new Resolver();

	public class Resolver {

		public void resolve(Tree value) {
			future.complete(value);
		}

		public void reject(Throwable error) {
			future.completeExceptionally(error);
		}

	}

	// --- PROTECTED CONSTRUCTORS ---

	protected Promise(Tree value, Throwable error) {
		if (error != null) {
			future = new CompletableFuture<>();
			future.completeExceptionally(error);
		} else if (value == null) {
			future = new CompletableFuture<>();
		} else {
			future = CompletableFuture.completedFuture(value);
		}
	}

	protected Promise(CompletableFuture<Tree> future, Promise parent) {
		this.future = future;
		this.parent = parent;
	}

	// --- GET COMPLETABLE FUTURE ---

	public CompletableFuture<Tree> toFuture() {
		return future;
	}

	// --- WATERFALL FUNCTION ---

	public Promise then(Function<Tree, Tree> action) {
		return new Promise(future.thenApply(action), this);
	}
	
	// --- ERROR HANDLER METHODS ---

	public Promise Catch(Function<Throwable, Tree> action) {
		return new Promise(addCatch(action), null);
	}

	protected CompletableFuture<Tree> addCatch(Function<Throwable, Tree> action) {
		CompletableFuture<Tree> f = future.exceptionally((error) -> {
			return action.apply(error);
		});
		if (parent != null) {
			parent.addCatch(action);
		}
		return f;
	}

	// --- ALL / RACE ---

	public static Promise all(Promise... promises) {

		@SuppressWarnings("unchecked")
		CompletableFuture<Tree>[] futures = new CompletableFuture[promises.length];
		for (int i = 0; i < promises.length; i++) {
			futures[i] = promises[i].future;
		}
		CompletableFuture<Void> all = CompletableFuture.allOf(futures);
		return new Promise((r) -> {
			all.whenComplete((Void, error) -> {
				if (error != null) {
					r.reject(error);
					return;
				}
				try {
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

	public static Promise race(Promise... promises) {

		@SuppressWarnings("unchecked")
		CompletableFuture<Tree>[] futures = new CompletableFuture[promises.length];
		for (int i = 0; i < promises.length; i++) {
			futures[i] = promises[i].future;
		}
		CompletableFuture<Object> any = CompletableFuture.anyOf(futures);
		return new Promise((r) -> {
			any.whenComplete((object, error) -> {
				if (error != null) {
					r.reject(error);
					return;
				}
				r.resolve((Tree) object);
			});
		});
	}
}