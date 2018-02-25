package services.moleculer.config;

import services.moleculer.ServiceBroker;

/**
 * Base Interface of all Moleculer Components (Services, Cachers, Transporters,
 * etc.) defining methods for start/stop lifecycle control.
 */
public interface MoleculerComponent {

	// --- START COMPONENT ---

	/**
	 * Start this component.
	 * 
	 * @param broker
	 *            parent ServiceBroker instance
	 * 
	 * @throws Exception
	 *             any I/O or configuration exception
	 */
	public void start(ServiceBroker broker) throws Exception;

	// --- STOP COMPONENT ---

	/**
	 * Stop this component (the component is fully stopped upon return of this
	 * method). Should not throw an exception.
	 */
	public void stop();

}
