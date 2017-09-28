package services.moleculer.fibers;

import java.util.concurrent.ExecutorService;

import co.paralleluniverse.fibers.FiberScheduler;

public interface SchedulerFactory {

	// --- FACTORY METHODS ---

	public FiberScheduler createNonBlockingScheduler();

	public ExecutorService createBlockingExecutor();

}
