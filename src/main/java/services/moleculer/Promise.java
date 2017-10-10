package services.moleculer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Fast, lightweight 'Promise' object. Convertable to CompletableFuture by using
 * the "toCompletableFuture" method.
 */
public class Promise {

	protected final AtomicReference<ResolvedState> resolvedState = new AtomicReference<>();
	protected final AtomicReference<RejectedState> rejectedState = new AtomicReference<>();

	protected static final class ResolvedState {

		protected ResolvedState(Object value, Consumer<Object> consumer) {
			this.value = value;
			this.consumer = consumer;
		}

		protected final Object value;
		protected final Consumer<Object> consumer;
	}

	protected static final class RejectedState {

		protected RejectedState(Throwable error, Consumer<Throwable> consumer) {
			this.error = error;
			this.consumer = consumer;
		}

		protected final Throwable error;
		protected final Consumer<Throwable> consumer;
	}

	public Promise() {
	}

	public Promise(Object value) {
		resolvedState.set(new ResolvedState(value, null));
	}

	public Promise(Throwable error) {
		rejectedState.set(new RejectedState(error, null));
	}

	public void resolve(Object value) {
		final ResolvedState state = resolvedState.getAndSet(new ResolvedState(value, null));
		if (state != null && state.consumer != null) {
			state.consumer.accept(value);
		}
	}

	public void reject(Throwable error) {
		final RejectedState state = rejectedState.getAndSet(new RejectedState(error, null));
		if (state != null && state.consumer != null) {
			state.consumer.accept(error);
		}
	}

	public void then(Consumer<Object> consumer) {
		final ResolvedState state = resolvedState.getAndSet(new ResolvedState(null, consumer));
		if (state != null && state.value != null) {
			consumer.accept(state.value);
		}
	}

	public void thenCatch(Consumer<Throwable> consumer) {
		final RejectedState state = rejectedState.getAndSet(new RejectedState(null, consumer));
		if (state != null && state.error != null) {
			consumer.accept(state.error);
		}
	}

	public CompletableFuture<Object> toCompletableFuture() {
		CompletableFuture<Object> future = new CompletableFuture<>();
		future.whenComplete((value, error) -> {
			if (error == null) {
				resolve(value);
			} else {
				reject(error);
			}
		});
		return future;
	}

}