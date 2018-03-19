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
package services.moleculer.serializer;

import services.moleculer.service.Name;

/**
 * <b>Amazon ION Serializer</b><br>
 * <br>
 * Description: Amazon Ion is a richly-typed, self-describing, hierarchical data
 * serialization format offering interchangeable binary and text
 * representations. The binary representation is efficient to store, transmit,
 * and skip-scan parse. The rich type system provides unambiguous semantics for
 * long-term preservation of business data which can survive multiple
 * generations of software evolution. Ion was built to solve the rapid
 * development, decoupling, and efficiency challenges faced every day while
 * engineering large-scale, service-oriented architectures.<br>
 * <br>
 * This serializer is NOT compatible with the JavaScript/Node version of
 * Moleculer.<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * https://mvnrepository.com/artifact/software.amazon.ion/ion-java<br>
 * compile group: 'software.amazon.ion', name: 'ion-java', version: '1.0.3'
 */
@Name("Amazon ION Serializer")
public class IonSerializer extends Serializer {

	// --- CONSTRUCTOR ---

	public IonSerializer() {
		super("ion");
	}

}