package services.moleculer.context;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

public interface Context {

	// --- VARIABLE GETTERS ---

	public String id();

	public ServiceBroker broker();

	public Tree params();

	public Tree meta();

	// --- ACTION CALL ---

	public Object call(String actionName, Tree params, CallingOptions opts) throws Exception;

	// --- SUBMIT EVENT ---

	public void emit(String eventName, Object payload);

}