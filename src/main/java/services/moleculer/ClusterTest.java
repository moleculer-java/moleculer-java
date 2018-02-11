package services.moleculer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import services.moleculer.transporter.TcpTransporter;

public class ClusterTest {

	private static final int NODES = 100;

	public static void main(String[] args) throws Exception {
		System.out.println("START");
		try {

			// Load Sigar DLLs
			String nativeDir = "./native";
			System.setProperty("java.library.path", nativeDir);
			
			ExecutorService executor = Executors.newFixedThreadPool(Math.min(NODES * 2, 150));

			ServiceBroker[] brokers = new ServiceBroker[NODES];
			for (int i = 0; i < NODES; i++) {
				TcpTransporter transporter = new TcpTransporter();
				transporter.setGossipPeriod(5);
				transporter.setDebug(false);		
				ServiceBroker broker = ServiceBroker.builder().executor(executor).transporter(transporter)
						.nodeID("node-" + i).build();
				brokers[i] = broker;
			}
			for (int i = 0; i < NODES; i++) {
				brokers[i].start();
				System.out.println("node-" + i + " started.");
			}

			while (true) {
				Thread.sleep(1000);
				System.out.println();
				
				for (int i = 0; i < NODES; i++) {
					ServiceBroker broker = brokers[i];
					TcpTransporter transporter = (TcpTransporter) broker.components.transporter();
					System.out.print("node-" + i);
					if (i < 10) {
						System.out.print(" ");
					}
					if (i < 100) {
						System.out.print(" ");
					}
					System.out.print(" ");
					for (int n = 0; n < NODES; n++) {
						boolean online = transporter.isOnline("node-" + n);
						if (online) {
							System.out.print("#");
						} else {
							System.out.print(".");
						}
					}
					System.out.println();
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("STOP");
	}

}
