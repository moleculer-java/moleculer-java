package services.moleculer.serializer;

public class XorCipherSerializerTest extends SerializerTest {

	@Override
	protected Serializer createSerializer() {
		try {
			XorCipherSerializer serializer = new XorCipherSerializer(null, "0123456789");
			byte[] keyBytes = new byte[1024 * 1024];
			for (int i = 0; i < keyBytes.length; i++) {
				keyBytes[i] = (byte) i;
			}
			serializer.setKeyBytes(keyBytes);
			serializer.started(null);			
			return serializer;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}