package services.moleculer;

public class Test {

	public static void main(String[] args) throws Exception {

		ServiceBroker broker = new ServiceBroker("moleculer.json");
		broker.start();
		
	}

}