package services.moleculer.utils;

import java.util.HashMap;

import services.moleculer.Service;

public class ServiceRegistry {

	public final HashMap<String, Service[]> services;
	
	// --- CONSTRUCTOR ---
	
	public ServiceRegistry() {
		services = new HashMap<>(2048);
	}
	
}
