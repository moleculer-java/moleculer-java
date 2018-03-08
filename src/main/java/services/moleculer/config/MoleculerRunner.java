package services.moleculer.config;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
public final class MoleculerRunner {

	// --- UDP PORT TO STOP MOLECULER SERVICE ---

	private static int stopPort = 6786;

	// --- UDP MESSAGE TO STOP MOLECULER SERVICE ---

	private static String stopMessage = "c8j3H9eV";

	// --- SERVICE BROKER INSTANCE ---

	private static final AtomicReference<AbstractXmlApplicationContext> context = new AtomicReference<>();

	// --- MAIN ENTRY POINT (START / STOP SERVICE BROKER) ---

	/**
	 * Starts/stops Moleculer as a standalone application or as a Windows
	 * service.
	 *
	 * @param args
	 *            configuration path or "STOP" to stop service, UDP port, and a
	 *            "secret message" to stop Moleculer service
	 */
	public static final void main(String[] args) throws Exception {
		try {
			if (args != null && args.length > 0) {

				// Second optional argument is stop port
				if (args.length > 1) {
					try {
						stopPort = Integer.parseInt(args[1]);
					} catch (Exception e) {
						System.err.println("Invalid port number (" + args[1] + ")!");
						return;
					}
				}

				// Third optional argument is stop command
				if (args.length > 2) {
					stopMessage = args[2];
				}

				// First argument is "stop" OR config path (= start broker)
				if (args[0].equalsIgnoreCase("stop")) {
					DatagramSocket socket = null;
					try {
						socket = new DatagramSocket();
						byte[] bytes = stopMessage.getBytes();
						DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost(),
								stopPort);
						socket.send(packet);
						return;
					} finally {
						try {
							if (socket != null) {
								socket.close();
							}
						} catch (Exception ignored) {
						}
					}
				}
			}
			if (context.get() != null) {
				return;
			}

			// Stop broker with CTRL + C
			Thread hook = new Thread() {
				public final void run() {
					stopBroker();
				}
			};
			hook.setDaemon(true);
			Runtime.getRuntime().addShutdownHook(hook);

			// Start UDP listener
			Thread udp = new Thread() {

				private DatagramSocket serverSocket = null;

				public void run() {
					try {
						serverSocket = new DatagramSocket(stopPort);
						byte[] buf = new byte[stopMessage.length()];
						while (serverSocket != null) {
							DatagramPacket packet = new DatagramPacket(buf, buf.length);
							serverSocket.receive(packet);
							byte[] data = packet.getData();
							if (data == null) {
								continue;
							}
							String received = new String(data);
							if (received.equalsIgnoreCase(stopMessage)) {
								stopBroker();
								return;
							}
						}
					} catch (Exception cause) {
						cause.printStackTrace();
					} finally {
						try {
							if (serverSocket != null) {
								serverSocket.close();
							}
						} catch (Exception ignored) {
						}
					}
				}
			};
			udp.setDaemon(true);
			udp.start();

			// Start ServiceBroker
			String configPath = "/moleculer.config.xml";
			if (args != null && args.length > 0) {
				configPath = args[0];
			}
			AbstractXmlApplicationContext ctx = null;
			File file = new File(configPath);
			if (file.isFile()) {
				ctx = new FileSystemXmlApplicationContext(configPath);
			} else {
				ctx = new ClassPathXmlApplicationContext(configPath);
			}
			context.set(ctx);
			ctx.start();

		} catch (Exception cause) {

			// Fatal error -> please check the classpath
			System.err.println("Unable to start Moleculer Service Broker!");
			cause.printStackTrace();
		}
	}

	// --- STOP SERVICE BROKER ---

	private static final void stopBroker() {
		AbstractXmlApplicationContext instance = context.getAndSet(null);
		if (instance != null) {
			Thread safetyTimer = new Thread() {

				public void run() {
					try {
						Thread.sleep(30000);
						System.exit(0);
					} catch (Exception interrupted) {
					}
				}

			};
			safetyTimer.setDaemon(true);
			safetyTimer.start();
			try {
				instance.stop();
			} catch (Throwable ignored) {
			}
			try {
				Thread.sleep(600);
			} catch (Throwable interrupted) {
			}
			System.exit(0);
		}
	}

}