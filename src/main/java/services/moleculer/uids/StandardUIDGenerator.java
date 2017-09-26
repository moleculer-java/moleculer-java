package services.moleculer.uids;

import java.util.UUID;

/**
 * Slower UIDGenerator (but it produces standard UUID's). In production mode
 * preferably use the faster TimeSequenceUIDGenerator.
 */
public final class StandardUIDGenerator extends UIDGenerator {

	// --- NAME OF THE MOLECULER COMPONENT ---
	
	@Override
	public final String name() {
		return "Standard UUID Generator";
	}
	
	// --- GENERATE UID ---
	
	@Override
	public final String nextUID() {
		return UUID.randomUUID().toString();
	}

}