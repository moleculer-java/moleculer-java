package services.moleculer.transporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JFrame;
import javax.swing.JPanel;

import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.transporter.tcp.NodeDescriptor;

public class ClusterTest extends JFrame implements Runnable {

	// --- UID ---

	private static final long serialVersionUID = 3131186457398972610L;

	// --- CONSTANTS ---

	private static int NODES = 100;

	private static int PIXEL_SIZE = 10;

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
			g.setFont(new Font("Dialog", Font.BOLD, PIXEL_SIZE - 2));
		}

		@Override
		public void paint(Graphics g) {
			g.drawImage(image, 0, 0, this);
		}

		public void draw(int x, int y, Color color, Long seq) {
			g.setColor(color);
			int rx = x * PIXEL_SIZE;
			int ry = y * PIXEL_SIZE;
			if (PIXEL_SIZE < 10) {

				// Small boxes wihout "seq" number
				g.fillRect(rx, ry, PIXEL_SIZE, PIXEL_SIZE);
				
			} else {
				
				// Larger boxes with "seq" number
				g.fill3DRect(rx, ry, PIXEL_SIZE - 1, PIXEL_SIZE - 1, true);
				if (seq != null) {
					g.setColor(Color.BLACK);
					g.drawString(Long.toString(seq), rx, ry + PIXEL_SIZE - 3);
				}
			}
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

			ExecutorService executor = Executors.newFixedThreadPool(Math.min(NODES, 100));
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Math.min(NODES, 100));

			String[] urls = new String[NODES];
			for (int i = 0; i < NODES; i++) {
				int port = 6000 + i;
				urls[i] = "tcp://127.0.0.1:" + port + "/node-" + i;
			}

			ServiceBroker[] brokers = new ServiceBroker[NODES];
			for (int i = 0; i < NODES; i++) {

				TcpTransporter transporter = new TcpTransporter(); // urls
				transporter.setGossipPeriod(3);
				transporter.setDebug(false);
				transporter.setOfflineTimeout(0);

				ServiceBrokerConfig settings = new ServiceBrokerConfig();
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

			boolean turnOn = true;

			HashMap<String, Long> maxSeqs = new HashMap<>();
			ServiceBroker broker;
			TcpTransporter transporter;
			int counter = 0;
			while (true) {
				Thread.sleep(1000);

				counter++;
				if (counter % 70 == 0) {
					if (turnOn) {
						for (int n = 0; n < NODES / 3; n++) {
							int i = rnd.nextInt(NODES);
							broker = brokers[i];
							transporter = (TcpTransporter) broker.getConfig().getTransporter();
							if (transporter.writer == null) {
								transporter.connect();
							}
						}
					}
					int i = rnd.nextInt(NODES);
					broker = brokers[i];
					transporter = (TcpTransporter) broker.getConfig().getTransporter();
					if (transporter.writer != null) {
						transporter.nodes.clear();
						transporter.disconnect();
					}
				}

				maxSeqs.clear();
				for (int i = 0; i < NODES; i++) {
					broker = brokers[i];
					transporter = (TcpTransporter) broker.getConfig().getTransporter();
					maxSeqs.put(broker.getNodeID(), transporter.getDescriptor().cpuSeq);
				}

				for (int i = 0; i < NODES; i++) {
					broker = brokers[i];
					transporter = (TcpTransporter) broker.getConfig().getTransporter();
					for (int n = 0; n < NODES; n++) {
						NodeDescriptor d = transporter.nodes.get("node-" + n);
						boolean online = transporter.isOnline("node-" + n);
						Color color;
						Long seq = null;
						if (online) {
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
								seq = d.seq;
							} else {
								color = Color.YELLOW;
							}
						} else {
							color = Color.RED;
							if (d != null) {
								seq = d.seq;
							}
						}
						image.draw(n, i, color, seq);
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
