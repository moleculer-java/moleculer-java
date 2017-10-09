package services.moleculer.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;

import services.moleculer.services.Name;

public class CommonUtils {

	public static final int countThreads(ExecutorService executorService) {
		int count = 1;
		if (executorService instanceof ThreadPoolExecutor) {
			ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
			count = tpe.getCorePoolSize();
		} else if (executorService instanceof ForkJoinPool) {
			ForkJoinPool fjp = (ForkJoinPool) executorService;
			count = fjp.getParallelism();
		}
		if (count < 1) {
			return ForkJoinPool.getCommonPoolParallelism();
		}
		return count;
	}

	public static final String nameOf(Object object) {
		String name = null;
		if (object != null) {
			Class<?> c = object.getClass();
			Name n = c.getAnnotation(Name.class);
			if (n != null) {
				name = n.value();
			}
			if (name != null) {
				name = name.trim();
			}
			if (name == null || name.isEmpty()) {
				name = c.getName();
				int i = Math.max(name.lastIndexOf('.'), name.lastIndexOf('$'));
				if (i > -1) {
					name = name.substring(i + 1);
				}
				name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
			}
		}
		if (name == null || name.isEmpty()) {
			name = "unknown";
		}
		return name;
	}

}