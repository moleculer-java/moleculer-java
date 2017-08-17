package services.moleculer;

@FunctionalInterface
public interface Listener {

	void on(Object payload) throws Exception;
	
}