package services.moleculer.utils;

import java.util.concurrent.atomic.AtomicLong;

public final class UIDGenerator {

	// --- HOST/NODE PREFIX ---

	private final char[] prefix;

	// --- SEQUENCE ---

	private final AtomicLong counter = new AtomicLong();
	
	// --- CONSTRUCTOR ---
	
	public UIDGenerator(String prefix) {
		this.prefix = (prefix + ':').toCharArray();
	}
		
	// --- NEXT ID ---
	
	public final String next() {
		StringBuilder tmp = new StringBuilder(64);
		tmp.append(prefix);
		tmp.append(System.currentTimeMillis());
		tmp.append(':');
		tmp.append(counter.incrementAndGet());
		return tmp.toString();
	}

}