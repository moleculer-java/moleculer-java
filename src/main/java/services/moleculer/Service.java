package services.moleculer;

/**
 * @author Norbi
 *
 */
public abstract class Service {

	public Service(String name) {
		
	}

	/**
	 * Lifecycle `created` event
	 */
	public void created() {
	}

	public void started() {
	}
	
	public void stopped() {
	}
	
}

