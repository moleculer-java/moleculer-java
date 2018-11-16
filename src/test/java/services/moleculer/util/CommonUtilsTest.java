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
package services.moleculer.util;

import org.junit.Test;

import junit.framework.TestCase;

public class CommonUtilsTest extends TestCase {

	@Test
	public void testUtils() throws Exception {

		assertEquals("", CommonUtils.formatPath(null));
		assertEquals("", CommonUtils.formatPath(""));
		assertEquals("/abc", CommonUtils.formatPath("abc"));
		assertEquals("/abc", CommonUtils.formatPath("abc///"));

		assertEquals("123", CommonUtils.formatNumber(123));
		assertEquals("1,234", CommonUtils.formatNumber(1234));
		assertEquals("1,234,567", CommonUtils.formatNumber(1234567));

		assertEquals("1 nanoseconds", CommonUtils.formatNamoSec(1));
		assertEquals("1,000 nanoseconds", CommonUtils.formatNamoSec(1000));
		assertEquals("1 millisecond (1,000,000 nanoseconds)", CommonUtils.formatNamoSec(1000000));
		assertEquals("1 second", CommonUtils.formatNamoSec(1000000000));
		assertEquals("123 milliseconds (123,456,789 nanoseconds)", CommonUtils.formatNamoSec(123456789));

		assertEquals("json", CommonUtils.getFormat("/abc/xyz.json"));
		assertNull(CommonUtils.getFormat("/abc/xyz.ignorethiserror"));

		assertEquals(123, CommonUtils.resolveUnit("123 msec"));
		assertEquals(123000, CommonUtils.resolveUnit("123 sec"));
		assertEquals(7380000, CommonUtils.resolveUnit("123 min"));
		assertEquals(442800000, CommonUtils.resolveUnit("123 hour"));

		assertEquals(86400000, CommonUtils.resolveUnit("1 day"));
		assertEquals(604800000, CommonUtils.resolveUnit("1 week"));
		assertEquals(1471228928, CommonUtils.resolveUnit("1 year"));

		assertEquals(1, CommonUtils.resolveUnit("1 byte"));
		assertEquals(1024, CommonUtils.resolveUnit("1 kbyte"));
		assertEquals(1048576, CommonUtils.resolveUnit("1 mbyte"));
		assertEquals(1073741824, CommonUtils.resolveUnit("1 gbyte"));
	}

}
