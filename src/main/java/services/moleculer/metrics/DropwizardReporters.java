package services.moleculer.metrics;

import java.util.concurrent.TimeUnit;

public interface DropwizardReporters {

	public static final int TYPE_CONSOLE  = 0;
	public static final int TYPE_LOGGER = 1;
	public static final int TYPE_JMX = 2;
	public static final int TYPE_CSV = 3;
	
	public void started(DefaultMetrics metrics, int type, long period, TimeUnit periodUnit, String param);

	public void stopped();
	
}
