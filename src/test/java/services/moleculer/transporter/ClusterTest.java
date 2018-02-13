package services.moleculer.transporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JFrame;
import javax.swing.JPanel;

import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerSettings;
import services.moleculer.transporter.tcp.NodeDescriptor;

public class ClusterTest extends JFrame implements Runnable {

	// --- UID ---

	private static final long serialVersionUID = 3131186457398972610L;

	// --- CONSTANTS ---

	private static final int NODES = 50;

	private static final int PIXEL_SIZE = 20;

	// --- ENTRY POINT ---

	public static void main(String[] args) throws Exception {
		new ClusterTest();
	}

	// --- IMAGE INSTANCE ---

	private final ImagePanel image;

	// --- CONSTRUCTOR ---

	public ClusterTest() {
		setTitle("Simulation of " + NODES + " nodes");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container root = getContentPane();
		root.setLayout(new BorderLayout());
		Dimension size = new Dimension(NODES * PIXEL_SIZE, NODES * PIXEL_SIZE);
		image = new ImagePanel(size);
		root.add(image, BorderLayout.CENTER);
		root.setSize(size);
		root.setPreferredSize(size);
		pack();
		setVisible(true);
		Thread t = new Thread(this);
		t.start();
	}

	// --- IMAGE CLASS ---

	private final static class ImagePanel extends JPanel {

		// --- UID ---

		private static final long serialVersionUID = 6785167063848346866L;

		// --- IMAGE ---

		private final BufferedImage image;
		private final Graphics g;

		// --- CONSTRUCTOR ---

		private ImagePanel(Dimension size) {
			image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
			g = image.getGraphics();
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, size.width, size.height);
		}

		@Override
		public void paint(Graphics g) {
			g.drawImage(image, 0, 0, this);
		}

		public void draw(int x, int y, Color color) {
			g.setColor(color);
			g.fill3DRect(x * PIXEL_SIZE, y * PIXEL_SIZE, PIXEL_SIZE - 1, PIXEL_SIZE - 1, true);
			repaint(200);
		}

	}

	// --- SIMULATION'S LOOP ---

	private final Random rnd = new Random();

	public void run() {
		System.out.println("START");
		try {

			// Load Sigar DLLs
			String nativeDir = "./native";
			System.setProperty("java.library.path", nativeDir);

			ExecutorService executor = Executors.newFixedThreadPool(Math.min(NODES * 2, 150));
			ScheduledExecutorService scheduler = Executors
					.newScheduledThreadPool(ForkJoinPool.commonPool().getParallelism());

			ServiceBroker[] brokers = new ServiceBroker[NODES];
			for (int i = 0; i < NODES; i++) {

				TcpTransporter transporter = new TcpTransporter();
				transporter.setGossipPeriod(2);
				transporter.setDebug(false);
				transporter.setOfflineTimeout(0);

				ServiceBrokerSettings settings = new ServiceBrokerSettings();
				settings.setShutDownThreadPools(false);
				settings.setExecutor(executor);
				settings.setScheduler(scheduler);
				settings.setTransporter(transporter);
				settings.setNodeID("node-" + i);

				ServiceBroker broker = new ServiceBroker(settings);
				brokers[i] = broker;
			}
			for (int i = 0; i < NODES; i++) {
				brokers[i].start();
				System.out.println("node-" + i + " started.");
			}

			boolean turnOn = false;

			HashMap<String, Long> maxSeqs = new HashMap<>();
			ServiceBroker broker;
			TcpTransporter transporter;
			int counter = 0;
			while (true) {
				Thread.sleep(500);

				counter++;
				if (counter % 50 == 0) {
					int i = rnd.nextInt(NODES);
					broker = brokers[i];
					transporter = (TcpTransporter) broker.components().transporter();
					if (transporter.writer != null) {
						transporter.nodes.clear();
						transporter.disconnect();
					}
					if (turnOn) {
						for (int n = 0; n < NODES / 3; n++) {
							i = rnd.nextInt(NODES);
							broker = brokers[i];
							transporter = (TcpTransporter) broker.components().transporter();
							if (transporter.writer == null) {
								transporter.connect();
							}
						}
					}
				}

				maxSeqs.clear();
				for (int i = 0; i < NODES; i++) {
					broker = brokers[i];
					transporter = (TcpTransporter) broker.components().transporter();
					maxSeqs.put(broker.nodeID(), transporter.getDescriptor().cpuSeq);
				}

				for (int i = 0; i < NODES; i++) {
					broker = brokers[i];
					transporter = (TcpTransporter) broker.components().transporter();
					for (int n = 0; n < NODES; n++) {
						boolean online = transporter.isOnline("node-" + n);
						Color color;
						if (online) {
							NodeDescriptor d = transporter.nodes.get("node-" + n);
							if (d != null) {
								long cpuSeq = d.cpuSeq;
								long maxSeq = maxSeqs.get("node-" + n);
								if (maxSeq > 0) {
									if (maxSeq < cpuSeq) {
										maxSeq = cpuSeq;
										maxSeqs.put("node-" + n, maxSeq);
									}
									long c = Math.max(0, 255 - 20 * (maxSeq - cpuSeq));
									color = new Color(0, (int) c, 0);
								} else {
									color = Color.WHITE;
								}
							} else {
								color = Color.YELLOW;
							}
						} else {
							color = Color.RED;
						}
						image.draw(n, i, color);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("STOP");
		System.exit(0);
	}

}
