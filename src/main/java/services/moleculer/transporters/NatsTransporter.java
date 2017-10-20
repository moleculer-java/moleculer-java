package services.moleculer.transporters;

import io.datatree.Tree;
import services.moleculer.services.Name;

/**
 * Not implemented (yet)
 */
@Name("NATS Transporter")
public final class NatsTransporter extends Transporter {

	@Override
	public final void publish(String channel, Tree message) {
	}

	@Override
	public final void subscribe(String channel) {
	}

}
