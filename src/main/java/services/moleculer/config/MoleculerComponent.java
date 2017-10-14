package services.moleculer.config;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

public interface MoleculerComponent {

	// --- START COMPONENT ---

	public void start(ServiceBroker broker, Tree config) throws Exception;

	// --- STOP COMPONENT ---

	public void stop();

}