package services.moleculer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.datatree.Tree;
import services.moleculer.config.MoleculerComponent;
import services.moleculer.transporters.Transporter;

@Component
public class TestComponent implements MoleculerComponent {

	@Autowired
	public Transporter transporter;
	
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
	}

	@Override
	public void stop() {
	}

}
