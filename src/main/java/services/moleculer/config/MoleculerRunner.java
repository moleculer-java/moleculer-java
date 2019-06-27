/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.config;

import java.io.File;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Runs Service Broker as a standalone Java application (or Windows service).
 * The configuration is loaded with Spring Framework.
 */
public final class MoleculerRunner {

	// --- UDP PORT TO STOP MOLECULER SERVICE ---

	private static int stopPort = 6786;

	// --- UDP MESSAGE TO STOP MOLECULER SERVICE ---

	private static String stopMessage = "c8j3H9eV";

	// --- SERVICE BROKER'S SPRING CONTEXT (XML-BASED OR SPRING BOOT) ---

	protected static final AtomicReference<ConfigurableApplicationContext> context = new AtomicReference<>();

	// --- MAIN ENTRY POINT (START / STOP SERVICE BROKER) ---

	/**
	 * Starts/stops Moleculer as a standalone application or as a Windows
	 * service. Optional start parameters:
	 * <ul>
	 * <li>First: Relative or absolute config path (eg.
	 * "/conf/moleculer.config.xml") or Spring Boot main class
	 * <li>Second: port number (eg. "6788")
	 * <li>Third: command to stop service (eg. "secret432")
	 * </ul>
	 * Optional stop parameters:
	 * <ul>
	 * <li>First: STOP (exactly this word)
	 * <li>Second: port number (eg. "6788")
	 * <li>Third: command to stop service (eg. "secret432")
	 * </ul>
	 * 
	 * @param args
	 *            configuration path or "STOP" to stop service, UDP port, and a
	 *            "secret message" to stop Moleculer service
	 * 
	 * @throws Exception
	 *             fatal exception (unable to find JARs, invalid Java version,
	 *             etc.)
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

				public void run() {
					DatagramSocket serverSocket = null;
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

			// Load config
			String configPath = "/moleculer.config.xml";
			if (args != null && args.length > 0) {
				configPath = args[0];
			}
			ConfigurableApplicationContext ctx = null;
			if (configPath.toLowerCase().endsWith(".xml")) {
				
				// XML-based "classic" Spring config
				File file = new File(configPath);
				if (file.isFile()) {
					ctx = new FileSystemXmlApplicationContext(configPath);
				} else {
					ctx = new ClassPathXmlApplicationContext(configPath);
				}
				context.set(ctx);
				ctx.start();

			} else {

				// Spring Boot config
				String springAppName = "org.springframework.boot.SpringApplication";
				Class<?> springAppClass = Class.forName(springAppName);

				// Input types of "run" method
				Class<?>[] types = new Class[2];
				types[0] = Class.class;
				types[1] = new String[0].getClass();
				Method m = springAppClass.getDeclaredMethod("run", types);

				// Input objects of "run" method
				Object[] in = new Object[2];
				
				// "configPath" = Name of main class
				in[0] = Class.forName(configPath);
				in[1] = args;

				// Load app with Spring Boot
				ctx = (ConfigurableApplicationContext) m.invoke(null, in);
				context.set(ctx);
			}

		} catch (Exception cause) {

			// Fatal error -> please check the classpath
			System.err.println("Unable to start Moleculer Service Broker!");
			cause.printStackTrace();
		}
	}

	// --- STOP SERVICE BROKER ---

	private static final void stopBroker() {
		ConfigurableApplicationContext ctx = context.getAndSet(null);
		if (ctx != null) {
			Thread safetyTimer = new Thread() {

				public void run() {
					try {
						Thread.sleep(15000);
						Runtime.getRuntime().halt(1);
					} catch (Throwable ignored) {
					}
				}

			};
			safetyTimer.setDaemon(true);
			safetyTimer.start();
			try {
				ctx.stop();
			} catch (Throwable ignored) {
			}
			try {
				Thread.sleep(600);
			} catch (Throwable interrupted) {
			}
			Runtime.getRuntime().halt(0);
		}
	}

}