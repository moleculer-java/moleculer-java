package services.moleculer.eventbus;

@FunctionalInterface
public interface Listener {

	void on(Object payload) throws Exception;
	
}