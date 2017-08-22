package services.moleculer;

@FunctionalInterface
public interface Action {
	
	Object handler(Context ctx) throws Exception;

	public default String getName() {
		return null;
	}
	
	public default String getNodeID() {
		return null;
	}
	
}