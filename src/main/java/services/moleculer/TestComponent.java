package services.moleculer;

import org.springframework.stereotype.Component;

import io.datatree.Tree;
import services.moleculer.config.MoleculerComponent;

@Component
public class TestComponent implements MoleculerComponent {

	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
	}

	@Override
	public void stop() {
	}

}
