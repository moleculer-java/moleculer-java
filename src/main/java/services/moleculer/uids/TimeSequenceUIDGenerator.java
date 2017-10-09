package services.moleculer.uids;

import java.util.concurrent.atomic.AtomicLong;

import services.moleculer.ServiceBroker;
import services.moleculer.services.Name;

/**
 * Fast UIDGenerator, based on nodeID, timestamp and an atomic sequence number.
 */
@Name("Time-Sequence UID Generator")
public final class TimeSequenceUIDGenerator extends UIDGenerator {
	
	// --- HOST/NODE PREFIX ---

	private char[] prefix;

	// --- SEQUENCE ---

	private final AtomicLong counter = new AtomicLong();

	// --- START GENERATOR ---

	@Override
	public final void init(ServiceBroker broker) {
		prefix = (broker.nodeID() + ':').toCharArray();
	}

	// --- GENERATE UID ---

	@Override
	public final String nextUID() {
		StringBuilder tmp = new StringBuilder(64);
		tmp.append(prefix);
		tmp.append(System.currentTimeMillis());
		tmp.append(':');
		tmp.append(counter.incrementAndGet());
		return tmp.toString();
	}
}
