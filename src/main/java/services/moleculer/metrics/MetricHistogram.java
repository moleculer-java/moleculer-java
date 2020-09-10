/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2020 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.metrics;

public abstract class MetricHistogram extends AbstractMetric {

	// --- CONSTRUCTOR ---

	public MetricHistogram(String name, String description, String[] tagValuePairs) {
		super(name, description, tagValuePairs);
	}

	// --- HISTOGRAM METHODS ---

	public abstract void addValue(long value);
	
	public abstract long getMax();
	
	public abstract double getMean();
	
	public abstract double get75thPercentile();
	
	public abstract double get95thPercentile();

	public abstract double get98thPercentile();

	public abstract double get99thPercentile();

	public abstract double get999thPercentile();
	
	// --- TO STRING ---
	
	@Override
	public String toString() {
		return name + ":{max: " + getMax() + ", mean: " + getMean() + ", 75% <= " + get75thPercentile()
				+ ", 95% <= " + get95thPercentile() + ", 98% <= " + get98thPercentile() + ", 99% <= "
				+ get99thPercentile() + ", 99.9% <= " + get999thPercentile() + "}";
	}
	
}