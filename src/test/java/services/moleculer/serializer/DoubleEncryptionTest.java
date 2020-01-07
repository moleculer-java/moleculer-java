package services.moleculer.serializer;

public class DoubleEncryptionTest extends SerializerTest {

	@Override
	protected Serializer createSerializer() {
		JsonSerializer jsonSerializer = new JsonSerializer();
		
		// First cipher
		BlockCipherSerializer first = new BlockCipherSerializer(jsonSerializer, "password1", "ARCFOUR");
		
		// Second cipher
		BlockCipherSerializer second = new BlockCipherSerializer(first, "password2", "Blowfish");
		
		// Init (by MessageBroker)
		try {
			first.started(null);			
			second.started(null);			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		assertEquals("json", second.getFormat());		
		return second;
	}

}