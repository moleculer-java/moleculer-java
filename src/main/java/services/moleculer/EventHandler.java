package services.moleculer;

@FunctionalInterface
public interface EventHandler {

	void on(Object payload) throws Exception;
	
}