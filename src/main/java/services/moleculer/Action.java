package services.moleculer;

@FunctionalInterface
public interface Action {

	Object handler(Context ctx) throws Exception;
	
	/*
	public default Object call(Object... params) {
		System.out.println("sd");
		return null;
	}
	*/
	
}