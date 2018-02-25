package services.moleculer.context;

import io.datatree.Tree;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.service.ServiceRegistry;

public class Context {

	// --- PROPERTIES ---

	public final String id;
	public final String name;
	public final Tree params;
	public final CallingOptions.Options opts;

	// --- COMPONENTS ---

	protected final ServiceRegistry serviceRegistry;
	protected final Eventbus eventbus;

	// --- CONSTRUCTOR ---

	public Context(ServiceRegistry serviceRegistry, Eventbus eventbus, String id, String name, Tree params,
			CallingOptions.Options opts) {
		this.serviceRegistry = serviceRegistry;
		this.eventbus = eventbus;
		this.id = id;
		this.name = name;
		this.params = params;
		this.opts = opts;
	} 
		
}