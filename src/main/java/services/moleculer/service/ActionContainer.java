/**
 * This software is licensed under MIT license.<br>
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
package services.moleculer.service;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.context.CallingOptions;
import services.moleculer.context.Context;

/**
 * Interface of Local or Remote actions. Sample action:<br>
 * <br>
 * &#64;Name("math")<br>
 * public class MathService extends Service {<br>
 * <br>
 * &#64;Cache(keys = { "a", "b" }, ttl = 30)<br>
 * public Action add = (ctx) -> {<br>
 * return ctx.params().get("a", 0) + ctx.params().get("b", 0);<br>
 * };<br>
 * <br>
 * }
 */
public interface ActionContainer {

	// --- INVOKE THIS ACTION ---

	public Promise call(Tree params, CallingOptions opts, Context parent);
	
	// --- PROPERTY GETTERS ---

	public String name();

	public String nodeID();

	public boolean local();

	public boolean cached();

	public String[] cacheKeys();

	public int defaultTimeout();
	
	public int ttl();
	
}