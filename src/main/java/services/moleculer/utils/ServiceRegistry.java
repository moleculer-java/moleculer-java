package services.moleculer.utils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import services.moleculer.Cache;
import services.moleculer.Service;
import services.moleculer.Version;

public class ServiceRegistry {

	/**
	 * Registered services
	 */
	public final HashMap<String, ServiceContainer[]> services;

	/**
	 * Reader lock of the Service Registry
	 */
	private final Lock readerLock;

	/**
	 * Writer lock of the Service Registry
	 */
	private final Lock writerLock;

	// --- CONSTRUCTOR ---

	public ServiceRegistry() {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
		services = new HashMap<>(2048);
	}

	// --- REGISTER LOCAL SERVICE ----

	public void registerLocalService(String prefix, String action, Service service) {
		Annotation[] annotations = service.getClass().getAnnotations();

		// Annotation values
		boolean cached = false;
		String version = "v1";

		for (Annotation annotation : annotations) {
			if (annotation instanceof Cache) {
				cached = ((Cache) annotation).value();
				continue;
			}
			if (annotation instanceof Version) {
				version = ((Version) annotation).value();

				// TODO Add "v" prefix to numeric version Strings
				continue;
			}
		}

		// Create container
		ServiceContainer container = new ServiceContainer(service, cached);

		// Generate full name of the service
		String name = version + '.' + prefix + '.' + action;

		// Lock getter and setter threads
		writerLock.lock();
		try {

			ServiceContainer[] containers = services.get(name);
			if (containers == null) {
				containers = new ServiceContainer[1];
				containers[0] = container;
				services.put(name, containers);
			} else {
				for (int i = 0; i < containers.length; i++) {
					if (containers[i].equals(container)) {
						
						// Already registered
						return;
					}
				}
				
				// Add to array
				containers = Arrays.copyOf(containers, containers.length + 1);
				containers[containers.length - 1] = container;
				services.put(name, containers);
			}
			
		} finally {
			writerLock.unlock();
		}
	}

}