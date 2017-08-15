package services.moleculer.transporters;

@FunctionalInterface
public interface AfterConnect {

	void onConnected(boolean wasReconnect);
	
}
