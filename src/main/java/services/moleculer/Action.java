package services.moleculer;

@FunctionalInterface
public interface Action {

	Object handler(Context ctx) throws Exception;
	
}