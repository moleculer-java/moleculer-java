package services.moleculer.transporters;

@FunctionalInterface
public interface MessageHandler {

	void onMessage(Object data);
}
