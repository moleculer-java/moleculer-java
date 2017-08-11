package services.moleculer;

@FunctionalInterface
public interface Action {

	public Object handler(Context ctx) throws Exception;
	
}