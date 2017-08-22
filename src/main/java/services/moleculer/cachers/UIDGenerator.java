package services.moleculer.cachers;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

public final class UIDGenerator {

	// --- STATIC HARDWARE ADDRESS ---

	private static final char[] address;

	static {
		char[] prefix = null;
		try {
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface nint = en.nextElement();
				if (!nint.isLoopback()) {
					byte[] mac = nint.getHardwareAddress();
					if (mac != null && mac.length == 6) {
						StringBuilder tmp = new StringBuilder(32);
						for (byte b : mac) {
							if (tmp.length() > 0) {
								tmp.append(':');
							}
							tmp.append(String.format("%02x", b));
						}
						tmp.append(':');
						prefix = tmp.toString().toCharArray();
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		address = prefix;
	}

	// --- SEQUENCE ---

	private static final AtomicLong counter = new AtomicLong();

	public static final String generate() {
		return Long.toString(counter.incrementAndGet());
	}
	
	public static final String generate2() {
		StringBuilder tmp = new StringBuilder(64);

		if (address != null) {
			tmp.append(address);
		}
		tmp.append(System.currentTimeMillis());
		tmp.append(':');
		tmp.append(counter.incrementAndGet());

		return tmp.toString();
	}

}