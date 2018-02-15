/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
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
package services.moleculer.repl.commands;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallingOptions;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;

/**
 * Measures the response time of a service.
 */
@Name("bench")
public class Bench extends Command {

	public Bench() {
		option("num <number>", "number of iterates");
		option("time <seconds>", "time of bench");
		option("nodeID <nodeID>", "nodeID (direct call)");
		option("max <number>", "max number of pending requests");
	}

	@Override
	public String getDescription() {
		return "Benchmark a service";
	}

	@Override
	public String getUsage() {
		return "bench <action> [jsonParams]";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 1;
	}

	protected ScheduledFuture<?> timer;

	protected ExecutorService executor;

	@Override
	public void onCommand(ServiceBroker broker, PrintStream out, String[] parameters) throws Exception {
		executor = broker.components().executor();

		// Parse parameters
		String action = parameters[0];
		Tree flags = parseFlags(1, parameters);
		long num = flags.get("num", 0);
		long time = flags.get("time", 0);
		String nodeID = flags.get("nodeID", "");
		int lastIndex = flags.get("lastIndex", 0);
		int max = flags.get("max", 100);		
		Tree params = getPayload(lastIndex + 1, parameters);

		if (num < 1 && time < 1) {
			time = 5;
		}
		if (max < 1) {
			max = 1;
		}
		CallingOptions.Options opts = null;
		if (nodeID != null && !nodeID.isEmpty()) {
			opts = CallingOptions.nodeID(nodeID);
		}

		// Start timer
		BenchData data = new BenchData(broker, opts, out, action, params, num);
		if (timer != null) {
			timer.cancel(true);
		}
		timer = broker.components().scheduler().schedule(() -> {
			data.timeout.set(true);
		}, time < 1 ? 60 : time, TimeUnit.SECONDS);

		// Start benchmark...
		String msg = num > 0 ? num + " times" : "for " + humanize(time * 1000000000);
		out.println("Calling service " + msg + ", please wait...");
		
		long req, res;
		while (!data.finished.get()) {
			req = data.reqCount.get();
			res = data.resCount.get();
			if (req - res < max) {
				doRequest(broker, data);
			} else {
				Thread.sleep(1);
			}
		}
		
		if (num == 0 && time > 0 && time < 10) {
			Thread.sleep(500L + (1000L * time));
		} else {
			out.println("The measurement is running in the background.");
		}
	}

	protected void doRequest(ServiceBroker broker, BenchData data) {
		data.reqCount.incrementAndGet();
		long startTime = System.nanoTime();		
		broker.call(data.action, data.params, data.opts).then(res -> {
			handleResponse(broker, data, startTime, null);
		}).Catch(cause -> {
			handleResponse(broker, data, startTime, cause);
		});
	}

	protected void handleResponse(ServiceBroker broker, BenchData data, long startTime, Throwable cause) {
		if (data.finished.get()) {
			return;
		}
		long count = data.resCount.incrementAndGet();
		if (cause != null) {
			data.errorCount.incrementAndGet();
		}
		long duration = System.nanoTime() - startTime;
		data.sumTime.addAndGet(duration);

		long currentMin = data.minTime.get();
		while (true) {
			if (duration < currentMin) {
				if (data.minTime.compareAndSet(currentMin, duration)) {
					break;
				}
				currentMin = data.minTime.get();
			} else {
				break;
			}
		}

		long currentMax = data.maxTime.get();
		while (true) {
			if (duration > currentMax) {
				if (data.maxTime.compareAndSet(currentMax, duration)) {
					break;
				}
				currentMax = data.maxTime.get();
			} else {
				break;
			}
		}

		if (data.timeout.get() || (data.num > 0 && count >= data.num)) {
			if (data.finished.compareAndSet(false, true)) {
				if (timer != null) {
					timer.cancel(true);
				}
				printResult(data);
			}
			return;
		}

		if (count % 100 > 0) {
			doRequest(broker, data);
		} else {
			executor.execute(() -> {
				doRequest(broker, data);
			});
		}
	}

	protected NumberFormat numberFormatter = DecimalFormat.getInstance();

	protected void printResult(BenchData data) {
		PrintStream out = data.out;
		try {
			long now = System.nanoTime();

			BigDecimal errorCount = new BigDecimal(data.errorCount.get());
			BigDecimal resCount = new BigDecimal(data.resCount.get());
			BigDecimal sumTime = new BigDecimal(data.sumTime.get());

			long total = now - data.startTime;
			BigDecimal totalTime = new BigDecimal(total);

			BigDecimal nano = new BigDecimal(1000000000);
			BigDecimal reqPerSec = nano.multiply(resCount).divide(totalTime, RoundingMode.HALF_UP);
			long reqPer = Long.parseLong(reqPerSec.toBigInteger().toString());

			BigDecimal duration = sumTime.divide(resCount, RoundingMode.HALF_UP);
			long dur = Long.parseLong(duration.toBigInteger().toString());
			BigDecimal inSec = duration.divide(nano);

			String errStr;
			if (errorCount.compareTo(BigDecimal.ZERO) == 1) {
				String percent = errorCount.multiply(new BigDecimal(100)).divide(resCount, RoundingMode.HALF_UP)
						.toBigInteger().toString();
				errStr = numberFormatter.format(data.errorCount) + " error(s) " + percent + "%";
			} else {
				errStr = "0 error";
			}
			out.println("Benchmark results:");
			out.println(
					"  " + numberFormatter.format(data.resCount) + " requests in " + humanize(total) + ", " + errStr);
			out.println("  Requests per second: " + numberFormatter.format(reqPer));
			out.println("  Latency: ");
			out.println("    Average: " + humanize(dur) + " (" + inSec.toPlainString() + " second)");
			if (data.minTime.get() != Long.MAX_VALUE) {
				out.println("    Minimum: " + humanize(data.minTime.get()));
			}
			if (data.maxTime.get() != Long.MIN_VALUE) {
				out.println("    Maximum: " + humanize(data.maxTime.get()));
			}
		} catch (Exception e) {
			e.printStackTrace(out);
		}
	}

	protected String humanize(long nanoSec) {
		String test, test2;
		if ((test = test(nanoSec, TimeUnit.HOURS, "hour")) != null) {
			test2 = test(nanoSec, TimeUnit.MINUTES, "minute");
			return test2 == null ? test : test + " (" + test2 + ")";
		}
		if ((test = test(nanoSec, TimeUnit.MINUTES, "minute")) != null) {
			test2 = test(nanoSec, TimeUnit.SECONDS, "second");
			return test2 == null ? test : test + " (" + test2 + ")";
		}
		if ((test = test(nanoSec, TimeUnit.SECONDS, "second")) != null) {
			test2 = test(nanoSec, TimeUnit.MILLISECONDS, "millisecond");
			return test2 == null ? test : test + " (" + test2 + ")";
		}
		if ((test = test(nanoSec, TimeUnit.MILLISECONDS, "millisecond")) != null) {
			return test + " (" + numberFormatter.format(nanoSec) + " nanoseconds)";
		}
		return numberFormatter.format(nanoSec) + " nanoseconds";
	}

	protected String test(long nanoSec, TimeUnit unit, String postfix) {
		long converted = unit.convert(nanoSec, TimeUnit.NANOSECONDS);
		if (converted > 0 && converted < 1000) {
			if (converted == 1) {
				return converted + " " + postfix;
			}
			return converted + " " + postfix + "s";
		}
		return null;
	}

	protected static final class BenchData {

		protected final long startTime;

		protected final ServiceBroker broker;
		protected final CallingOptions.Options opts;
		protected final PrintStream out;
		protected final String action;
		protected final Tree params;
		protected final long num;

		protected final AtomicLong reqCount = new AtomicLong();
		protected final AtomicLong resCount = new AtomicLong();
		protected final AtomicLong errorCount = new AtomicLong();
		protected final AtomicLong sumTime = new AtomicLong();

		protected final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
		protected final AtomicLong maxTime = new AtomicLong(Long.MIN_VALUE);

		protected final AtomicBoolean timeout = new AtomicBoolean();
		protected final AtomicBoolean finished = new AtomicBoolean();

		protected BenchData(ServiceBroker broker, CallingOptions.Options opts, PrintStream out, String action,
				Tree params, long num) {
			this.broker = broker;
			this.opts = opts;
			this.out = out;
			this.action = action;
			this.params = params;
			this.num = num;
			this.startTime = System.nanoTime();
		}

	}

}