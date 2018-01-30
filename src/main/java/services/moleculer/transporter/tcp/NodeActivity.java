package services.moleculer.transporter.tcp;

public final class NodeActivity {

	// --- PROPERTIES ---

	public final long when;
	public final int cpu;
	
	// --- CONSTRUCTORS ---
	
	public NodeActivity(long when, int cpu) {
		this.when = when;
		this.cpu = cpu;
	}

}
