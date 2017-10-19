package services.moleculer.uids;

import java.util.UUID;

import services.moleculer.services.Name;

/**
 * Slower UIDGenerator (but it produces standard UUID's). In production mode
 * preferably use the faster TimeSequenceUIDGenerator.
 * 
 * @see TimeSequenceUIDGenerator
 */
@Name("Standard UUID Generator")
public final class StandardUIDGenerator extends UIDGenerator {

	// --- GENERATE UID ---

	@Override
	public final String nextUID() {
		return UUID.randomUUID().toString();
	}

}