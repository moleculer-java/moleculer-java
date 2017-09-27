package services.moleculer.context;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

public class ThreadBasedContext implements Context {

	@Override
	public String id() {
		return null;
	}

	@Override
	public ServiceBroker broker() {
		return null;
	}

	@Override
	public Tree params() {
		return null;
	}

	@Override
	public Tree meta() {
		return null;
	}

	@Override
	public Object call(String actionName, Tree params, CallingOptions opts) throws Exception {
		return null;
	}

	@Override
	public void emit(String eventName, Object payload) {
	}

}
