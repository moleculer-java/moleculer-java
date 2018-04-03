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
package services.moleculer.cacher;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;

/**
 * Generated test methods for testing the key generator of distributed cachers.
 */
public class KeyTest extends TestCase {

	@Test
	public void testA() throws Exception {

		String json = "{'A':{'C0':3488,'C1':9576,'C2':false,'C3':false,'C4':false}" + //
				",'B':{'C0':6076,'C1':6227,'C2':false,'C3':false,'C4':false},'C':{'C0':'45d0ce622a393302'" + //
				",'C1':'d408fcbd811d738','C2':1413,'C3':'339d1fb7008a44f9','C4':7903},'D':{'C0':true" + //
				",'C1':'6e49c6cf570f3964','C2':4659,'C3':5338,'C4':8381},'E':{'C0':true" + //
				",'C1':false,'C2':6787,'C3':true,'C4':5672},'F':{'C0':false,'C1':6887,'C2':8487" + //
				",'C3':448,'C4':'7f7215dddba8d547'},'G':{'C0':true,'C1':4518,'C2':true,'C3':false" + //
				",'C4':true},'H':{'C0':'9b6694f1cf2eaed','C1':6158,'C2':true,'C3':true,'C4':'481a78867e1b874'}" + //
				",'I':{'C0':false,'C1':true,'C2':'5af58fd004cbba97','C3':'2438af1871beae45'" + //
				",'C4':'20f5702be623930d'},'J':{'C0':3203,'C1':true,'C2':false,'C3':false" + //
				",'C4':'66d175e58683df0d'},'K':{'C0':false,'C1':false,'C2':8326,'C3':false" + //
				",'C4':'44f9d2cae6616c7c'},'L':{'C0':true,'C1':4722,'C2':true,'C3':true" + //
				",'C4':false},'M':{'C0':true,'C1':true,'C2':false,'C3':4671,'C4':false}" + //
				",'N':{'C0':true,'C1':4001,'C2':8402,'C3':8735,'C4':true},'O':{'C0':true" + //
				",'C1':true,'C2':6520,'C3':true,'C4':false},'P':{'C0':false,'C1':false,'C2':true" + //
				",'C3':1726,'C4':true},'Q':{'C0':true,'C1':1622,'C2':true,'C3':'1da0f76964a01006'" + //
				",'C4':'6945b336c360158e'},'R':{'C0':5337,'C1':7877,'C2':true,'C3':4078" + //
				",'C4':true},'S':{'C0':'47215a2e792ce897','C1':false,'C2':'27e1dc62d59ff86'" + //
				",'C3':false,'C4':'2cbdcc936aa2c637'},'T':{'C0':true,'C1':7877,'C2':5965" + //
				",'C3':8156,'C4':false},'U':{'C0':true,'C1':'6b78eb8ac52bf1da','C2':9711" + //
				",'C3':3100,'C4':'45ea45d5637eb205'},'V':{'C0':false,'C1':'2232c3a7e2fccd31'" + //
				",'C2':false,'C3':true,'C4':6908},'W':{'C0':603,'C1':true,'C2':'7fd43ff8cf66ffcb'" + //
				",'C3':true,'C4':true},'X':{'C0':6486,'C1':'376b53b8e8e4cb3a','C2':true" + //
				",'C3':false,'C4':3899},'Y':{'C0':3313,'C1':false,'C2':5550,'C3':true,'C4':false" + //
				"}}";

		String key = "UdNEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,CUdNEbnozMLVv+gnv" + //
				"RS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys2 = null;
		check(json, key, keys2, 91);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:faUdNEbnozMLVv" + //
				"+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys3 = null;
		check(json, key, keys3, 95);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:UdNEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys4 = null;
		check(json, key, keys4, 114);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a3UdNEbnozMLVv+gnv" + //
				"RS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys5 = null;
		check(json, key, keys5, 164);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fUd" + //
				"NEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys6 = null;
		check(json, key, keys6, 178);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2UdNEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys7 = null;
		check(json, key, keys7, 191);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903UdNEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fC" + //
				"ST8kQ7OMs=";
		String[] keys8 = null;
		check(json, key, keys8, 224);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,UdNEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys9 = null;
		check(json, key, keys9, 273);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,C4:8381},E:{C0:true,C1:false,C2:6787,C3UdNEbnozMLVv+gnvRS+VxQ" + //
				"9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys10 = null;
		check(json, key, keys10, 312);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,C4:8381},E:{C0:true,C1:false,C2:6787,C3:true,C4:5672},F:{C0:false" + //
				",C1:6887,C2:8487,C3:UdNEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys11 = null;
		check(json, key, keys11, 358);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,C4:8381},E:{C0:true,C1:false,C2:6787,C3:true,C4:5672},F:{C0:false" + //
				",C1:6887,C2:8487,C3:448,C4:UdNEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs" + //
				"=";
		String[] keys12 = null;
		check(json, key, keys12, 365);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,C4:8381},E:{C0:true,C1:false,C2:6787,C3:true,C4:5672},F:{C0:false" + //
				",C1:6887,C2:8487,C3:448,C4:7f7215dddba8d547}UdNEbnozMLVv+gnvRS+VxQ9h3j" + //
				"d9yCe0fCST8kQ7OMs=";
		String[] keys13 = null;
		check(json, key, keys13, 382);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,C4:8381},E:{C0:true,C1:false,C2:6787,C3:true,C4:5672},F:{C0:false" + //
				",C1:6887,C2:8487,C3:448,C4:7f7215dddba8d547},G:UdNEbnozMLVv+gnvRS+VxQ9" + //
				"h3jd9yCe0fCST8kQ7OMs=";
		String[] keys14 = null;
		check(json, key, keys14, 385);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,C4:8381},E:{C0:true,C1:false,C2:6787,C3:true,C4:5672},F:{C0:false" + //
				",C1:6887,C2:8487,C3:448,C4:7f7215dddba8d547},G:{C0:true,C1:4518,UdNEbn" + //
				"ozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys15 = null;
		check(json, key, keys15, 402);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,C4:8381},E:{C0:true,C1:false,C2:6787,C3:true,C4:5672},F:{C0:false" + //
				",C1:6887,C2:8487,C3:448,C4:7f7215dddba8d547},G:{C0:true,C1:4518,C2:true" + //
				",C3:false,C4:true},H:{C0UdNEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys16 = null;
		check(json, key, keys16, 433);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,C4:8381},E:{C0:true,C1:false,C2:6787,C3:true,C4:5672},F:{C0:false" + //
				",C1:6887,C2:8487,C3:448,C4:7f7215dddba8d547},G:{C0:true,C1:4518,C2:true" + //
				",C3:false,C4:true},H:{C0:9b6694f1cf2eaed,C1:6158,C2:true,C3:true,CUdNE" + //
				"bnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys17 = null;
		check(json, key, keys17, 475);

		key = "abc.def:{A:{C0:3488,C1:9576,C2:false,C3:false,C4:false},B:{C0:6076" + //
				",C1:6227,C2:false,C3:false,C4:false},C:{C0:45d0ce622a393302,C1:d408fcbd811d738" + //
				",C2:1413,C3:339d1fb7008a44f9,C4:7903},D:{C0:true,C1:6e49c6cf570f3964,C2:4659" + //
				",C3:5338,C4:8381},E:{C0:true,C1:false,C2:6787,C3:true,C4:5672},F:{C0:false" + //
				",C1:6887,C2:8487,C3:448,C4:7f7215dddba8d547},G:{C0:true,C1:4518,C2:true" + //
				",C3:false,C4:true},H:{C0:9b6694f1cf2eaed,C1:6158,C2:true,C3:true,C4:48" + //
				"1a7UdNEbnozMLVv+gnvRS+VxQ9h3jd9yCe0fCST8kQ7OMs=";
		String[] keys18 = null;
		check(json, key, keys18, 482);

	}

	@Test
	public void testB() throws Exception {

		String json = "{'A':{'C0':1411,'C1':false,'C2':true,'C3':true,'C4':true}" + //
				",'B':{'C0':true,'C1':true,'C2':'4f2a4c935d41facb','C3':'1147a9ba8810d41b'" + //
				",'C4':6031},'C':{'C0':false,'C1':'4b30e5aa79663072','C2':'2fe0105f5ac6758d'" + //
				",'C3':4768,'C4':true},'D':{'C0':false,'C1':'6d600830d2714514','C2':true" + //
				",'C3':true,'C4':'46037a7b6f82f57e'},'E':{'C0':false,'C1':'2c3ec82e1a5a2eea'" + //
				",'C2':false,'C3':2539,'C4':false},'F':{'C0':'3ed2af8effc42a6d','C1':2272" + //
				",'C2':'4965d2777b643247','C3':false,'C4':true},'G':{'C0':false,'C1':false" + //
				",'C2':true,'C3':3331,'C4':true},'H':{'C0':false,'C1':false,'C2':false,'C3':true" + //
				",'C4':183},'I':{'C0':'728e7dba2d9aa0e3','C1':true,'C2':'730df32f9ccc3261'" + //
				",'C3':true,'C4':6983},'J':{'C0':'199d44387b736c3','C1':'72e76f9e6d1be3d6'" + //
				",'C2':true,'C3':'2f63e0fcbc442c50','C4':8785},'K':{'C0':true,'C1':'2b992ee4b4e4557c'" + //
				",'C2':'740a572099e8521b','C3':9061,'C4':true},'L':{'C0':'5b164c1e9dd53208'" + //
				",'C1':'777bbe10f5b8080b','C2':true,'C3':6562,'C4':false},'M':{'C0':9540" + //
				",'C1':false,'C2':3113,'C3':true,'C4':699},'N':{'C0':false,'C1':'532efb5bed385325'" + //
				",'C2':'737311d785042b15','C3':true,'C4':true},'O':{'C0':'6abee4b9b5ebadaf'" + //
				",'C1':false,'C2':false,'C3':true,'C4':false},'P':{'C0':true,'C1':true,'C2':false" + //
				",'C3':true,'C4':'54f824768c144ae6'},'Q':{'C0':false,'C1':false,'C2':'62c2654b0d8ec822'" + //
				",'C3':'75b87d577dcc84aa','C4':false},'R':{'C0':false,'C1':386,'C2':false" + //
				",'C3':8714,'C4':5522},'S':{'C0':4784,'C1':false,'C2':5914,'C3':5316,'C4':false}" + //
				",'T':{'C0':'4f39eee9b3062b97','C1':'6deff0990d20bd','C2':false,'C3':7870" + //
				",'C4':true},'U':{'C0':false,'C1':'5a01b8f606b504c','C2':'1ed62a57f91c66cf'" + //
				",'C3':'28a9b4198772b4b3','C4':'501338c4f851a432'},'V':{'C0':false,'C1':'2a244501cbcb892e'" + //
				",'C2':false,'C3':'78b2f3cfe9c04343','C4':false},'W':{'C0':'668f576d6346c82c'" + //
				",'C1':true,'C2':3532,'C3':'1dad371e06e35805','C4':false},'X':{'C0':false" + //
				",'C1':'116ebbad85ad52d6','C2':'26c59990961bce00','C3':3632,'C4':7289},'Y':{'C0':true" + //
				",'C1':'4dc3e3ffd3b2a67f','C2':5063,'C3':4914,'C4':4451}}";

		String key = "tvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:{A:{C0:1411,C1:false,C2:trtvkkHVF+dGDnMB9g4Dtbtg9e5VKwW" + //
				"rG5MyRuyFJDaKc=";
		String[] keys2 = null;
		check(json, key, keys2, 78);

		key = "abc.def:{A:{C0:1411,C1:false,C2:truetvkkHVF+dGDnMB9g4Dtbtg9e5VK" + //
				"wWrG5MyRuyFJDaKc=";
		String[] keys3 = null;
		check(json, key, keys3, 80);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3tvkkHVF+dGDnMB9g4Dtbtg9e" + //
				"5VKwWrG5MyRuyFJDaKc=";
		String[] keys4 = null;
		check(json, key, keys4, 83);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:tvkkHVF+dGDnMB9" + //
				"g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys5 = null;
		check(json, key, keys5, 92);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:ttv" + //
				"kkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys6 = null;
		check(json, key, keys6, 105);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810dtvkkHVF+dGDnMB9g4Dtbtg9e5" + //
				"VKwWrG5MyRuyFJDaKc=";
		String[] keys7 = null;
		check(json, key, keys7, 153);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false," + //
				"C1:4b30tvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys8 = null;
		check(json, key, keys8, 185);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105tvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys9 = null;
		check(json, key, keys9, 208);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6tvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys10 = null;
		check(json, key, keys10, 213);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1tvkkHVF+dGDnMB9g4D" + //
				"tbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys11 = null;
		check(json, key, keys11, 249);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037atvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys12 = null;
		check(json, key, keys12, 292);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037a7b6f82f57e},E:{CtvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRu" + //
				"yFJDaKc=";
		String[] keys13 = null;
		check(json, key, keys13, 308);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037a7b6f82f57e},E:{C0:false,C1:2ctvkkHVF+dGDnMB9g4Dtbtg9" + //
				"e5VKwWrG5MyRuyFJDaKc=";
		String[] keys14 = null;
		check(json, key, keys14, 321);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037a7b6f82f57e},E:{C0:false,C1:2c3ec8tvkkHVF+dGDnMB9g4Dt" + //
				"btg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys15 = null;
		check(json, key, keys15, 325);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037a7b6f82f57e},E:{C0:false,C1:2c3ec82e1a5a2eea,C2:false" + //
				",C3:2539,C4:false},tvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys16 = null;
		check(json, key, keys16, 363);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037a7b6f82f57e},E:{C0:false,C1:2c3ec82e1a5a2eea,C2:false" + //
				",C3:2539,C4:false},F:{C0:3ed2af8effc42a6d,C1:2272,C2:49tvkkHVF+dGDnMB9" + //
				"g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys17 = null;
		check(json, key, keys17, 399);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037a7b6f82f57e},E:{C0:false,C1:2c3ec82e1a5a2eea,C2:false" + //
				",C3:2539,C4:false},F:{C0:3ed2af8effc42a6d,C1:2272,C2:4965d2777b643247,C3:false" + //
				",C4:true},tvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys18 = null;
		check(json, key, keys18, 432);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037a7b6f82f57e},E:{C0:false,C1:2c3ec82e1a5a2eea,C2:false" + //
				",C3:2539,C4:false},F:{C0:3ed2af8effc42a6d,C1:2272,C2:4965d2777b643247,C3:false" + //
				",C4:true},G:{C0:faltvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys19 = null;
		check(json, key, keys19, 441);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037a7b6f82f57e},E:{C0:false,C1:2c3ec82e1a5a2eea,C2:false" + //
				",C3:2539,C4:false},F:{C0:3ed2af8effc42a6d,C1:2272,C2:4965d2777b643247,C3:false" + //
				",C4:true},G:{C0:false,C1:false,C2:true,CtvkkHVF+dGDnMB9g4Dtbtg9e5VKwWr" + //
				"G5MyRuyFJDaKc=";
		String[] keys20 = null;
		check(json, key, keys20, 462);

		key = "abc.def:{A:{C0:1411,C1:false,C2:true,C3:true,C4:true},B:{C0:true" + //
				",C1:true,C2:4f2a4c935d41facb,C3:1147a9ba8810d41b,C4:6031},C:{C0:false,C1:4b30e5aa79663072" + //
				",C2:2fe0105f5ac6758d,C3:4768,C4:true},D:{C0:false,C1:6d600830d2714514,C2:true" + //
				",C3:true,C4:46037a7b6f82f57e},E:{C0:false,C1:2c3ec82e1a5a2eea,C2:false" + //
				",C3:2539,C4:false},F:{C0:3ed2af8effc42a6d,C1:2272,C2:4965d2777b643247,C3:false" + //
				",C4:true},G:{C0:false,C1:false,C2:true,C3:3331,C4:true},H:{C0:false,C1" + //
				"tvkkHVF+dGDnMB9g4Dtbtg9e5VKwWrG5MyRuyFJDaKc=";
		String[] keys21 = null;
		check(json, key, keys21, 492);

	}

	@Test
	public void testC() throws Exception {

		String json = "{'A':{'C0':false,'C1':true,'C2':'22fcd2b54a2d3bc3','C3':true" + //
				",'C4':false},'B':{'C0':true,'C1':true,'C2':'16e8f2f258239f9a','C3':5762" + //
				",'C4':true},'C':{'C0':false,'C1':true,'C2':true,'C3':1433,'C4':'3b2c4cca9d23e309'}" + //
				",'D':{'C0':true,'C1':2600,'C2':1685,'C3':true,'C4':'39aae5ba2065660e'}" + //
				",'E':{'C0':false,'C1':'17d74778b14a4ca9','C2':5834,'C3':true,'C4':7807}" + //
				",'F':{'C0':640,'C1':'6d05db374d91491a','C2':'5c9741ae5ae9d58e','C3':false" + //
				",'C4':false},'G':{'C0':'4ecfc529d8e637c4','C1':true,'C2':'3797a57b1e8e57ed'" + //
				",'C3':false,'C4':false},'H':{'C0':false,'C1':5308,'C2':'7f7fc4fd77380223'" + //
				",'C3':'7d25d6739c555492','C4':1267},'I':{'C0':2019,'C1':false,'C2':'159e71d50df87ad6'" + //
				",'C3':false,'C4':false},'J':{'C0':false,'C1':false,'C2':false,'C3':5619" + //
				",'C4':'5af314809ca6459b'},'K':{'C0':'32b038ea9fd55ebf','C1':'1ddcd15b618f749f'" + //
				",'C2':'2b79f759951006d6','C3':false,'C4':'41f05702b48cb740'},'L':{'C0':false" + //
				",'C1':true,'C2':'5ef1058c04c07be1','C3':1258,'C4':7617},'M':{'C0':3479" + //
				",'C1':'11c08457b04328c3','C2':4308,'C3':true,'C4':false},'N':{'C0':false" + //
				",'C1':'19a827e1726417f5','C2':5815,'C3':'50ed39ad06d28f6a','C4':5833},'O':{'C0':'47eba338dbdb80fa'" + //
				",'C1':2241,'C2':true,'C3':'54c946dc7b2f706b','C4':false},'P':{'C0':false" + //
				",'C1':true,'C2':true,'C3':false,'C4':'209970d4f287a3b2'},'Q':{'C0':'7ff26543ffef3289'" + //
				",'C1':true,'C2':true,'C3':'5d178859188d85db','C4':7806},'R':{'C0':false" + //
				",'C1':9893,'C2':7257,'C3':'7ed4f63e1d107a36','C4':true},'S':{'C0':'4a3f10e0e10ce314'" + //
				",'C1':229,'C2':4076,'C3':6902,'C4':true},'T':{'C0':true,'C1':false,'C2':false" + //
				",'C3':'ab122e8ab9cd7ee','C4':500},'U':{'C0':9401,'C1':'3ab15abad6874e82'" + //
				",'C2':'5d26dbd2a1bbabb8','C3':'12103c4410e34b32','C4':'5497390c94b31011'}" + //
				",'V':{'C0':true,'C1':true,'C2':'2fab478b23e45e86','C3':3931,'C4':true}" + //
				",'W':{'C0':'51d4294d5304c5dc','C1':267,'C2':false,'C3':false,'C4':'56126200a7c6c282'}" + //
				",'X':{'C0':false,'C1':false,'C2':true,'C3':3766,'C4':true},'Y':{'C0':7739" + //
				",'C1':6255,'C2':'6616e9eed690f40f','C3':'7a62df314b7e8403','C4':5432}}" + //
				"";

		String key = "8m+6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C8m+6D+3fv2/ct" + //
				"uNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys2 = null;
		check(json, key, keys2, 94);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C8m+6D" + //
				"+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys3 = null;
		check(json, key, keys3, 102);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,8m+6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys4 = null;
		check(json, key, keys4, 130);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:fals8m+" + //
				"6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys5 = null;
		check(json, key, keys5, 177);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:false,C1:true" + //
				",C2:true,C3:1433,C4:3b2c4cca8m+6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4im" + //
				"c=";
		String[] keys6 = null;
		check(json, key, keys6, 214);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:false,C1:true" + //
				",C2:true,C3:1433,C4:3b2c4cca9d23e309},D:{C0:true,C1:28m+6D+3fv2/ctuNqF" + //
				"2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys7 = null;
		check(json, key, keys7, 239);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:false,C1:true" + //
				",C2:true,C3:1433,C4:3b2c4cca9d23e309},D:{C0:true,C1:2600,C2:1685,C3:true" + //
				",C4:39aae58m+6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys8 = null;
		check(json, key, keys8, 268);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:false,C1:true" + //
				",C2:true,C3:1433,C4:3b2c4cca9d23e309},D:{C0:true,C1:2600,C2:1685,C3:true" + //
				",C4:39aae5ba2065660e},E:{C0:false,C1:17d74778b14a8m+6D+3fv2/ctuNqF2CRO" + //
				"MZtx8JVpBS3UG6B4FB4imc=";
		String[] keys9 = null;
		check(json, key, keys9, 307);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:false,C1:true" + //
				",C2:true,C3:1433,C4:3b2c4cca9d23e309},D:{C0:true,C1:2600,C2:1685,C3:true" + //
				",C4:39aae5ba2065660e},E:{C0:false,C1:17d74778b14a4ca9,C2:5834,C3:true,C4:7807}" + //
				",F:{C0:640,C18m+6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys10 = null;
		check(json, key, keys10, 349);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:false,C1:true" + //
				",C2:true,C3:1433,C4:3b2c4cca9d23e309},D:{C0:true,C1:2600,C2:1685,C3:true" + //
				",C4:39aae5ba2065660e},E:{C0:false,C1:17d74778b14a4ca9,C2:5834,C3:true,C4:7807}" + //
				",F:{C0:640,C1:6d05db374d914918m+6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4i" + //
				"mc=";
		String[] keys11 = null;
		check(json, key, keys11, 365);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:false,C1:true" + //
				",C2:true,C3:1433,C4:3b2c4cca9d23e309},D:{C0:true,C1:2600,C2:1685,C3:true" + //
				",C4:39aae5ba2065660e},E:{C0:false,C1:17d74778b14a4ca9,C2:5834,C3:true,C4:7807}" + //
				",F:{C0:640,C1:6d05db374d91491a,C2:5c9741ae5ae9d58e,C3:false,C4:false}," + //
				"G:{C8m+6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys12 = null;
		check(json, key, keys12, 410);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:false,C1:true" + //
				",C2:true,C3:1433,C4:3b2c4cca9d23e309},D:{C0:true,C1:2600,C2:1685,C3:true" + //
				",C4:39aae5ba2065660e},E:{C0:false,C1:17d74778b14a4ca9,C2:5834,C3:true,C4:7807}" + //
				",F:{C0:640,C1:6d05db374d91491a,C2:5c9741ae5ae9d58e,C3:false,C4:false}," + //
				"G:{C0:4ecfc529d88m+6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys13 = null;
		check(json, key, keys13, 422);

		key = "abc.def:{A:{C0:false,C1:true,C2:22fcd2b54a2d3bc3,C3:true,C4:false}" + //
				",B:{C0:true,C1:true,C2:16e8f2f258239f9a,C3:5762,C4:true},C:{C0:false,C1:true" + //
				",C2:true,C3:1433,C4:3b2c4cca9d23e309},D:{C0:true,C1:2600,C2:1685,C3:true" + //
				",C4:39aae5ba2065660e},E:{C0:false,C1:17d74778b14a4ca9,C2:5834,C3:true,C4:7807}" + //
				",F:{C0:640,C1:6d05db374d91491a,C2:5c9741ae5ae9d58e,C3:false,C4:false},G:{C0:4ecfc529d8e637c4" + //
				",C1:true,C2:3797a57b1e8e8m+6D+3fv2/ctuNqF2CROMZtx8JVpBS3UG6B4FB4imc=";
		String[] keys14 = null;
		check(json, key, keys14, 452);

	}

	@Test
	public void testD() throws Exception {

		String json = "{'A':{'C0':'55d6608d26958fca','C1':true,'C2':5503,'C3':true" + //
				",'C4':'6321796ac5f3629b'},'B':{'C0':1046,'C1':'698510b7d646aaeb','C2':3666" + //
				",'C3':7073,'C4':7429},'C':{'C0':false,'C1':'1e99613e8f82807b','C2':7603" + //
				",'C3':'4a3632276887c21c','C4':5201},'D':{'C0':false,'C1':true,'C2':2020" + //
				",'C3':7055,'C4':9362},'E':{'C0':'1bc33c3a26db14c7','C1':true,'C2':9972" + //
				",'C3':3628,'C4':true},'F':{'C0':'5a1aecd36b242567','C1':1605,'C2':true" + //
				",'C3':'2d3ecd88adf5e358','C4':true},'G':{'C0':false,'C1':true,'C2':'4e73666fbd8da3ea'" + //
				",'C3':true,'C4':'b38285100a36a31'},'H':{'C0':false,'C1':true,'C2':'1a2f64b699d529b7'" + //
				",'C3':false,'C4':4118},'I':{'C0':false,'C1':true,'C2':6623,'C3':9556,'C4':true}" + //
				",'J':{'C0':true,'C1':false,'C2':false,'C3':'3790604085f813cc','C4':5347}" + //
				",'K':{'C0':true,'C1':604,'C2':'3a1ee8984ef8d1a','C3':true,'C4':true},'L':{'C0':3011" + //
				",'C1':false,'C2':true,'C3':9918,'C4':false},'M':{'C0':false,'C1':false" + //
				",'C2':'70f4ae94d47b35d2','C3':6786,'C4':false},'N':{'C0':false,'C1':false" + //
				",'C2':'70ef897915503aaf','C3':false,'C4':4429},'O':{'C0':true,'C1':'58f04926e28c4056'" + //
				",'C2':true,'C3':true,'C4':false},'P':{'C0':'1f2d77d1442b744e','C1':'3a02bc3b27371e4f'" + //
				",'C2':'af1dde023162c3c','C3':5126,'C4':true},'Q':{'C0':'76e6aacc9312a62f'" + //
				",'C1':true,'C2':false,'C3':false,'C4':8539},'R':{'C0':false,'C1':true,'C2':false" + //
				",'C3':'32dc48900281b9e8','C4':'2b1ffc5540d8739c'},'S':{'C0':true,'C1':true" + //
				",'C2':5300,'C3':true,'C4':'1a767d5217441678'},'T':{'C0':true,'C1':6711" + //
				",'C2':true,'C3':true,'C4':true},'U':{'C0':true,'C1':false,'C2':false,'C3':'232b3afea8c7bdbb'" + //
				",'C4':4182},'V':{'C0':'300ae29a0d2e2c43','C1':5765,'C2':'412d4800f442ef0'" + //
				",'C3':false,'C4':true},'W':{'C0':true,'C1':true,'C2':false,'C3':'199b7c260cf62e00'" + //
				",'C4':5248},'X':{'C0':4916,'C1':true,'C2':true,'C3':true,'C4':true},'Y':{'C0':true" + //
				",'C1':'4a6df087a6c6cf0','C2':true,'C3':'d82cd897e65c39','C4':8623}}";

		String key = "menUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503menUhABxBAX86kQH" + //
				"metqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys2 = null;
		check(json, key, keys2, 91);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:menUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys3 = null;
		check(json, key, keys3, 123);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,menUhABxBAX86kQHmetqaonTqNfKyF" + //
				"RilOWN4SlxmHA=";
		String[] keys4 = null;
		check(json, key, keys4, 160);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C" + //
				"1menUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys5 = null;
		check(json, key, keys5, 191);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C" + //
				"1:1e99613emenUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys6 = null;
		check(json, key, keys6, 200);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:menUhABxBAX86kQHmetqaonTqNfKyF" + //
				"RilOWN4SlxmHA=";
		String[] keys7 = null;
		check(json, key, keys7, 248);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:truemenUhABxBAX86" + //
				"kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys8 = null;
		check(json, key, keys8, 265);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7" + //
				"0menUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys9 = null;
		check(json, key, keys9, 279);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7055" + //
				",C4:9362},E:{C0:1bc33c3a26db14c7,C1:true,C2:menUhABxBAX86kQHmetqaonTqN" + //
				"fKyFRilOWN4SlxmHA=";
		String[] keys10 = null;
		check(json, key, keys10, 325);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7055" + //
				",C4:9362},E:{C0:1bc33c3a26db14c7,C1:true,C2:99menUhABxBAX86kQHmetqaonT" + //
				"qNfKyFRilOWN4SlxmHA=";
		String[] keys11 = null;
		check(json, key, keys11, 327);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7055" + //
				",C4:9362},E:{C0:1bc33c3a26db14c7,C1:true,C2:9972,C3:3628,C4:true},menU" + //
				"hABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys12 = null;
		check(json, key, keys12, 347);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7055" + //
				",C4:9362},E:{C0:1bc33c3a26db14c7,C1:true,C2:9972,C3:3628,C4:true},F:{C0:5a1aecd36b242567" + //
				",C1:1605menUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys13 = null;
		check(json, key, keys13, 377);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7055" + //
				",C4:9362},E:{C0:1bc33c3a26db14c7,C1:true,C2:9972,C3:3628,C4:true},F:{C0:5a1aecd36b242567" + //
				",C1:1605,C2:trmenUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys14 = null;
		check(json, key, keys14, 383);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7055" + //
				",C4:9362},E:{C0:1bc33c3a26db14c7,C1:true,C2:9972,C3:3628,C4:true},F:{C0:5a1aecd36b242567" + //
				",C1:1605,C2:true,C3:2d3ecd88adf5menUhABxBAX86kQHmetqaonTqNfKyFRilOWN4S" + //
				"lxmHA=";
		String[] keys15 = null;
		check(json, key, keys15, 401);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7055" + //
				",C4:9362},E:{C0:1bc33c3a26db14c7,C1:true,C2:9972,C3:3628,C4:true},F:{C0:5a1aecd36b242567" + //
				",C1:1605,C2:true,C3:2d3ecd88adf5e358,C4:true},G:{C0:false,C1:true,C2:4" + //
				"e73666menUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys16 = null;
		check(json, key, keys16, 445);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7055" + //
				",C4:9362},E:{C0:1bc33c3a26db14c7,C1:true,C2:9972,C3:3628,C4:true},F:{C0:5a1aecd36b242567" + //
				",C1:1605,C2:true,C3:2d3ecd88adf5e358,C4:true},G:{C0:false,C1:true,C2:4e73666fbd8da3ea" + //
				",C3:true,C4menUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys17 = null;
		check(json, key, keys17, 465);

		key = "abc.def:{A:{C0:55d6608d26958fca,C1:true,C2:5503,C3:true,C4:6321796ac5f3629b}" + //
				",B:{C0:1046,C1:698510b7d646aaeb,C2:3666,C3:7073,C4:7429},C:{C0:false,C1:1e99613e8f82807b" + //
				",C2:7603,C3:4a3632276887c21c,C4:5201},D:{C0:false,C1:true,C2:2020,C3:7055" + //
				",C4:9362},E:{C0:1bc33c3a26db14c7,C1:true,C2:9972,C3:3628,C4:true},F:{C0:5a1aecd36b242567" + //
				",C1:1605,C2:true,C3:2d3ecd88adf5e358,C4:true},G:{C0:false,C1:true,C2:4e73666fbd8da3ea" + //
				",C3:true,C4:b3828menUhABxBAX86kQHmetqaonTqNfKyFRilOWN4SlxmHA=";
		String[] keys18 = null;
		check(json, key, keys18, 471);

	}

	@Test
	public void testE() throws Exception {

		String json = "{'A':{'C0':3827,'C1':false,'C2':'418785241aee62bc','C3':false" + //
				",'C4':8940},'B':{'C0':true,'C1':1197,'C2':false,'C3':false,'C4':true},'C':{'C0':'2b1b9e19844089e6'" + //
				",'C1':9962,'C2':'26f7510b5e724619','C3':294,'C4':5960},'D':{'C0':1893,'C1':false" + //
				",'C2':9659,'C3':'40fb198bc3d0a3f1','C4':true},'E':{'C0':3925,'C1':'59374877eb26dc2b'" + //
				",'C2':false,'C3':'5d03f26ba6bc54d5','C4':false},'F':{'C0':false,'C1':7431" + //
				",'C2':8549,'C3':4454,'C4':'6cd6dbd9c03db859'},'G':{'C0':false,'C1':true" + //
				",'C2':false,'C3':false,'C4':'574c71a97a497d35'},'H':{'C0':4828,'C1':false" + //
				",'C2':true,'C3':1716,'C4':5914},'I':{'C0':528,'C1':true,'C2':true,'C3':'96448357c806d9d'" + //
				",'C4':false},'J':{'C0':2968,'C1':6574,'C2':false,'C3':'18ab71996baf69c6'" + //
				",'C4':'1c497dd98e8aade3'},'K':{'C0':false,'C1':'ffbfae95d75c1a8','C2':true" + //
				",'C3':6114,'C4':5087},'L':{'C0':true,'C1':true,'C2':true,'C3':'27e7846aab1e4ff1'" + //
				",'C4':8298},'M':{'C0':'21eebbe36c3c1d6','C1':2113,'C2':true,'C3':900,'C4':false}" + //
				",'N':{'C0':9113,'C1':3465,'C2':true,'C3':'213729539a477d6a','C4':9491}" + //
				",'O':{'C0':'979c880c13dfc5e','C1':8525,'C2':true,'C3':false,'C4':5913}" + //
				",'P':{'C0':false,'C1':1640,'C2':false,'C3':false,'C4':false},'Q':{'C0':7372" + //
				",'C1':false,'C2':'22d7e91742e254db','C3':false,'C4':true},'R':{'C0':'2f7f021309507794'" + //
				",'C1':true,'C2':false,'C3':true,'C4':'1337765e0bcf1502'},'S':{'C0':false" + //
				",'C1':false,'C2':8277,'C3':'383a8440d7613a76','C4':false},'T':{'C0':false" + //
				",'C1':true,'C2':true,'C3':false,'C4':false},'U':{'C0':false,'C1':false" + //
				",'C2':true,'C3':4121,'C4':'16e25a7737f9949f'},'V':{'C0':true,'C1':false" + //
				",'C2':false,'C3':true,'C4':8069},'W':{'C0':'591fab8efe46434e','C1':7313" + //
				",'C2':'17d642641b4a8665','C3':true,'C4':true},'X':{'C0':'43f859e9c0670fc0'" + //
				",'C1':7602,'C2':'5f61b46ece6aadaa','C3':false,'C4':true},'Y':{'C0':true" + //
				",'C1':false,'C2':3542,'C3':'5ddfc45776189dac','C4':true}}";

		String key = "agR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aeagR2pei/ix2z2m+e7S4w" + //
				"fSImWENd5CcVuyVR0VHMb6M=";
		String[] keys2 = null;
		check(json, key, keys2, 87);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8a" + //
				"gR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys3 = null;
		check(json, key, keys3, 106);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,CagR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys4 = null;
		check(json, key, keys4, 123);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,agR2pei/ix2z2m+e7S4wfSImWENd5CcV" + //
				"uyVR0VHMb6M=";
		String[] keys5 = null;
		check(json, key, keys5, 148);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2bagR2pei/ix2z2m+" + //
				"e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys6 = null;
		check(json, key, keys6, 165);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26agR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys7 = null;
		check(json, key, keys7, 193);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:falsagR2pei/ix2z2m+" + //
				"e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys8 = null;
		check(json, key, keys8, 242);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:39agR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys9 = null;
		check(json, key, keys9, 289);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:falseagR2pei/ix2z2m+e7S4wf" + //
				"SImWENd5CcVuyVR0VHMb6M=";
		String[] keys10 = null;
		check(json, key, keys10, 320);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:false,C3:5dagR2pei/ix2z2m+" + //
				"e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys11 = null;
		check(json, key, keys11, 326);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:false,C3:5d03f26ba6bc54d5,C4:false}" + //
				",FagR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys12 = null;
		check(json, key, keys12, 352);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:false,C3:5d03f26ba6bc54d5,C4:false}" + //
				",F:{CagR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys13 = null;
		check(json, key, keys13, 355);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:false,C3:5d03f26ba6bc54d5,C4:false}" + //
				",F:{C0:false,C1:7431,agR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys14 = null;
		check(json, key, keys14, 371);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:false,C3:5d03f26ba6bc54d5,C4:false}" + //
				",F:{C0:false,C1:7431,C2:854agR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M" + //
				"=";
		String[] keys15 = null;
		check(json, key, keys15, 377);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:false,C3:5d03f26ba6bc54d5,C4:false}" + //
				",F:{C0:false,C1:7431,C2:8549,C3:4454,C4:6cd6dbd9c03dbagR2pei/ix2z2m+e7" + //
				"S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys16 = null;
		check(json, key, keys16, 403);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:false,C3:5d03f26ba6bc54d5,C4:false}" + //
				",F:{C0:false,C1:7431,C2:8549,C3:4454,C4:6cd6dbd9c03db859},G:{C0:false,C1:true" + //
				",C2:false,C3:false,C4:5agR2pei/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys17 = null;
		check(json, key, keys17, 450);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:false,C3:5d03f26ba6bc54d5,C4:false}" + //
				",F:{C0:false,C1:7431,C2:8549,C3:4454,C4:6cd6dbd9c03db859},G:{C0:false,C1:true" + //
				",C2:false,C3:false,C4:574c71a97a497d35},H:{C0:4828,C1agR2pei/ix2z2m+e7" + //
				"S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys18 = null;
		check(json, key, keys18, 480);

		key = "abc.def:{A:{C0:3827,C1:false,C2:418785241aee62bc,C3:false,C4:8940}" + //
				",B:{C0:true,C1:1197,C2:false,C3:false,C4:true},C:{C0:2b1b9e19844089e6,C1:9962" + //
				",C2:26f7510b5e724619,C3:294,C4:5960},D:{C0:1893,C1:false,C2:9659,C3:40fb198bc3d0a3f1" + //
				",C4:true},E:{C0:3925,C1:59374877eb26dc2b,C2:false,C3:5d03f26ba6bc54d5,C4:false}" + //
				",F:{C0:false,C1:7431,C2:8549,C3:4454,C4:6cd6dbd9c03db859},G:{C0:false,C1:true" + //
				",C2:false,C3:false,C4:574c71a97a497d35},H:{C0:4828,C1:false,C2:tagR2pe" + //
				"i/ix2z2m+e7S4wfSImWENd5CcVuyVR0VHMb6M=";
		String[] keys19 = null;
		check(json, key, keys19, 491);

	}

	@Test
	public void testF() throws Exception {

		String json = "{'A':{'C0':9838,'C1':false,'C2':3563,'C3':true,'C4':true}" + //
				",'B':{'C0':false,'C1':true,'C2':'5829ac69a71fa128','C3':true,'C4':true}" + //
				",'C':{'C0':3989,'C1':true,'C2':1818,'C3':false,'C4':true},'D':{'C0':'304d87492a72c99e'" + //
				",'C1':'35bb857b3f48e93d','C2':false,'C3':826,'C4':8069},'E':{'C0':false" + //
				",'C1':true,'C2':7618,'C3':true,'C4':true},'F':{'C0':false,'C1':false,'C2':true" + //
				",'C3':'33fe1377da5b3a76','C4':'3f64d65245589794'},'G':{'C0':5098,'C1':'6971c0b40bfbb684'" + //
				",'C2':true,'C3':7814,'C4':'49ee178e8edc6811'},'H':{'C0':2630,'C1':3203" + //
				",'C2':true,'C3':9062,'C4':false},'I':{'C0':4759,'C1':'92d5eb1827abfc7'" + //
				",'C2':'7696225cdd722a38','C3':'5dc8630c7ca0f313','C4':true},'J':{'C0':false" + //
				",'C1':'4f36c4e6fa1e0ac9','C2':'5a4c84cf303ca90e','C3':false,'C4':9268}" + //
				",'K':{'C0':false,'C1':8483,'C2':true,'C3':'4b6bf3276c59d067','C4':2723}" + //
				",'L':{'C0':false,'C1':true,'C2':false,'C3':'776e1180fa9826de','C4':true}" + //
				",'M':{'C0':true,'C1':'26398d361293342b','C2':'7cc826defc526be3','C3':true" + //
				",'C4':false},'N':{'C0':true,'C1':2492,'C2':'13f7a4ef69b0c4dc','C3':false" + //
				",'C4':9710},'O':{'C0':'5d40935a1c8cc67a','C1':'47aa013ecac9e46','C2':false" + //
				",'C3':1524,'C4':false},'P':{'C0':true,'C1':'43a98197daa2141a','C2':true" + //
				",'C3':true,'C4':true},'Q':{'C0':true,'C1':true,'C2':true,'C3':'521455695b0ec32e'" + //
				",'C4':true},'R':{'C0':'3a0f89cb9e8096ec','C1':1865,'C2':true,'C3':false" + //
				",'C4':false},'S':{'C0':3269,'C1':false,'C2':true,'C3':'429d2f59e88e6e1f'" + //
				",'C4':false},'T':{'C0':'8d845de13d807f','C1':'127cd99a56b212ff','C2':3121" + //
				",'C3':'3e51e2cd8e75b594','C4':true},'U':{'C0':true,'C1':'272794aa0062fb96'" + //
				",'C2':'29b27c079fa27408','C3':'13a194d3b0137518','C4':true},'V':{'C0':true" + //
				",'C1':false,'C2':true,'C3':'bada38f8f2e6847','C4':true},'W':{'C0':true" + //
				",'C1':false,'C2':6962,'C3':'202784b59bb761a4','C4':'282e709e6eb032cf'}" + //
				",'X':{'C0':5686,'C1':false,'C2':false,'C3':true,'C4':false},'Y':{'C0':true" + //
				",'C1':false,'C2':2978,'C3':3222,'C4':'51a990b07530986b'}}";

		String key = "pYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:{A:{C0:9838,C1:falpYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8Ib" + //
				"Va3Kqc=";
		String[] keys2 = null;
		check(json, key, keys2, 70);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:pYV9h4TaIbHu1as7qy5doss" + //
				"wTZdwMwfvZo8IbVa3Kqc=";
		String[] keys3 = null;
		check(json, key, keys3, 84);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:pYV9h4TaIbHu1as" + //
				"7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys4 = null;
		check(json, key, keys4, 92);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,pYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys5 = null;
		check(json, key, keys5, 118);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,pYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3K" + //
				"qc=";
		String[] keys6 = null;
		check(json, key, keys6, 138);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:trupYV9h4TaIbHu1as7qy5dosswTZd" + //
				"wMwfvZo8IbVa3Kqc=";
		String[] keys7 = null;
		check(json, key, keys7, 152);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:18" + //
				"pYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys8 = null;
		check(json, key, keys8, 179);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492pYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8I" + //
				"bVa3Kqc=";
		String[] keys9 = null;
		check(json, key, keys9, 215);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492a72c99e,C1:35bb857b3f48e93d,C2:false" + //
				",C3:826,C4:8pYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys10 = null;
		check(json, key, keys10, 263);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492a72c99e,C1:35bb857b3f48e93d,C2:false" + //
				",C3:826,C4:8069},E:{C0:falspYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc" + //
				"=";
		String[] keys11 = null;
		check(json, key, keys11, 278);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492a72c99e,C1:35bb857b3f48e93d,C2:false" + //
				",C3:826,C4:8069},E:{C0:false,C1:true,C2:7618pYV9h4TaIbHu1as7qy5dosswTZ" + //
				"dwMwfvZo8IbVa3Kqc=";
		String[] keys12 = null;
		check(json, key, keys12, 295);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492a72c99e,C1:35bb857b3f48e93d,C2:false" + //
				",C3:826,C4:8069},E:{C0:false,C1:true,C2:7618,C3:true,C4:true},F:{C0:false" + //
				",C1:false,C2:true,C3pYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys13 = null;
		check(json, key, keys13, 344);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492a72c99e,C1:35bb857b3f48e93d,C2:false" + //
				",C3:826,C4:8069},E:{C0:false,C1:true,C2:7618,C3:true,C4:true},F:{C0:false" + //
				",C1:false,C2:true,C3:33fe1377da5b3a76,C4:3f64d65245589794},pYV9h4TaIbH" + //
				"u1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys14 = null;
		check(json, key, keys14, 383);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492a72c99e,C1:35bb857b3f48e93d,C2:false" + //
				",C3:826,C4:8069},E:{C0:false,C1:true,C2:7618,C3:true,C4:true},F:{C0:false" + //
				",C1:false,C2:true,C3:33fe1377da5b3a76,C4:3f64d65245589794},G:{C0:5098,C1:6971c0b40bfbb684" + //
				",pYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys15 = null;
		check(json, key, keys15, 414);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492a72c99e,C1:35bb857b3f48e93d,C2:false" + //
				",C3:826,C4:8069},E:{C0:false,C1:true,C2:7618,C3:true,C4:true},F:{C0:false" + //
				",C1:false,C2:true,C3:33fe1377da5b3a76,C4:3f64d65245589794},G:{C0:5098,C1:6971c0b40bfbb684" + //
				",C2:true,pYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys16 = null;
		check(json, key, keys16, 422);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492a72c99e,C1:35bb857b3f48e93d,C2:false" + //
				",C3:826,C4:8069},E:{C0:false,C1:true,C2:7618,C3:true,C4:true},F:{C0:false" + //
				",C1:false,C2:true,C3:33fe1377da5b3a76,C4:3f64d65245589794},G:{C0:5098,C1:6971c0b40bfbb684" + //
				",C2:true,C3:7814,C4:49ee178e8edc6811},H:{C0:2630,C1:3203,C2:pYV9h4TaIb" + //
				"Hu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys17 = null;
		check(json, key, keys17, 473);

		key = "abc.def:{A:{C0:9838,C1:false,C2:3563,C3:true,C4:true},B:{C0:false" + //
				",C1:true,C2:5829ac69a71fa128,C3:true,C4:true},C:{C0:3989,C1:true,C2:1818" + //
				",C3:false,C4:true},D:{C0:304d87492a72c99e,C1:35bb857b3f48e93d,C2:false" + //
				",C3:826,C4:8069},E:{C0:false,C1:true,C2:7618,C3:true,C4:true},F:{C0:false" + //
				",C1:false,C2:true,C3:33fe1377da5b3a76,C4:3f64d65245589794},G:{C0:5098,C1:6971c0b40bfbb684" + //
				",C2:true,C3:7814,C4:49ee178e8edc6811},H:{C0:2630,C1:3203,C2:true,C3:9062" + //
				",C4:fapYV9h4TaIbHu1as7qy5dosswTZdwMwfvZo8IbVa3Kqc=";
		String[] keys18 = null;
		check(json, key, keys18, 491);

	}

	@Test
	public void testG() throws Exception {

		String json = "{'A':false,'B':7402,'C':4751,'D':8972,'E':true,'F':4407" + //
				",'G':false,'H':'19f18ec9a484f480','I':'5d920e41ad3fff06','J':false,'K':false" + //
				",'L':2690,'M':true,'N':true,'O':false,'P':false,'Q':false,'R':true,'S':8715" + //
				",'T':false,'U':false,'V':380,'W':true,'X':false,'Y':'58b74df79be14bca'" + //
				"}";

		String key = "M2mJsec7RN1ro7fBbC2rmKkAJd2k+LvxGlzoJMY4sE0=";
		String[] keys1 = { "N", "F", "O", "H", "K", "S", "T", "L", "W", "B", "J" };
		check(json, key, keys1, 44);

		key = "abc.def:truCZ9KY+GJfKtx+JTT9/3jxlJy7Md/ClF6U+tqkZTATSY=";
		String[] keys2 = { "R", "O", "N", "W", "B", "L", "E", "Y", "T" };
		check(json, key, keys2, 55);

		key = "abc.def:7402|false|false|trDtPzD69KaEl8mjSED2rP0zoWMFcNhmnXs7Z9" + //
				"7GjnpIE=";
		String[] keys3 = { "B", "A", "T", "E", "F", "Q", "G", "J", "S", "H", "W", "D", "V", "I", "R", "L", "U", "X",
				"M", "C", "N" };
		check(json, key, keys3, 71);

		key = "abc.def:false|false|false|true|true|false|4751|2690|true";
		String[] keys4 = { "J", "T", "P", "E", "R", "A", "C", "L", "W" };
		check(json, key, keys4, 83);

		key = "abc.def:4751|4407|380|8715|true|true|false|8972|19f18ec9a484f48" + //
				"0|58b74df79be14bca";
		String[] keys5 = { "C", "F", "V", "S", "M", "N", "O", "D", "H", "Y" };
		check(json, key, keys5, 104);

		key = "abc.def:8715|false|false|false|false|false|4751|true";
		String[] keys6 = { "S", "J", "G", "K", "P", "A", "C", "M" };
		check(json, key, keys6, 135);

		key = "abc.def:4751|19f18ec9a484f480|false|380|8972|false|false|4407|f" + //
				"alse|2690|true";
		String[] keys7 = { "C", "H", "A", "V", "D", "K", "J", "F", "T", "L", "E" };
		check(json, key, keys7, 159);

		key = "abc.def:7402|4751|2690|false|false|5d920e41ad3fff06|true|8715|t" + //
				"rue|false|false|19f18ec9a484f480|4407|58b74df79be14bca|false";
		String[] keys8 = { "B", "C", "L", "K", "Q", "I", "W", "S", "N", "T", "A", "H", "F", "Y", "P" };
		check(json, key, keys8, 182);

		key = "abc.def:380|true|false|true|false|false|2690|false|7402|5d920e4" + //
				"1ad3fff06|false|false|8972|false|true|19f18ec9a484f480|false|58b74df79" + //
				"be14bca|8715|false|4407|true|4751|false|true";
		String[] keys9 = { "V", "W", "P", "M", "O", "Q", "L", "T", "B", "I", "G", "J", "D", "A", "E", "H", "K", "Y",
				"S", "U", "F", "R", "C", "X", "N" };
		check(json, key, keys9, 202);

		key = "abc.def:true|false|false|58b74df79be14bca|19f18ec9a484f480|8972" + //
				"|false|4407|true";
		String[] keys10 = { "W", "G", "O", "Y", "H", "D", "K", "F", "M" };
		check(json, key, keys10, 229);

		key = "abc.def:58b74df79be14bca|2690|19f18ec9a484f480|false|false|fals" + //
				"e|true|true|8972|7402|8715|4407|false|true|5d920e41ad3fff06|380|false|" + //
				"false|4751|true";
		String[] keys11 = { "Y", "L", "H", "T", "P", "U", "E", "R", "D", "B", "S", "F", "K", "M", "I", "V", "O", "Q",
				"C", "W" };
		check(json, key, keys11, 245);

		key = "abc.def:2690|true|false|4407|8972|8715|58b74df79be14bca|380|tru" + //
				"e|false|false|5d920e41ad3fff06|false";
		String[] keys12 = { "L", "E", "Q", "F", "D", "S", "Y", "V", "N", "G", "A", "I", "O" };
		check(json, key, keys12, 256);

		key = "abc.def:true|false|false|8715|false|380|false|false|19f18ec9a48" + //
				"4f480|true|5d920e41ad3fff06|false|4751|true|2690|8972|false|4407|false" + //
				"|false|true|false|58b74df79be14bca|7402|true";
		String[] keys13 = { "R", "U", "J", "S", "Q", "V", "O", "G", "H", "E", "I", "K", "C", "W", "L", "D", "T", "F",
				"P", "A", "N", "X", "Y", "B", "M" };
		check(json, key, keys13, 281);

		key = "abc.def:7402|false|380|false|true|false|false|false|4751|true|1" + //
				"9f18ec9a484f480";
		String[] keys14 = { "B", "O", "V", "T", "R", "U", "Q", "G", "C", "W", "H" };
		check(json, key, keys14, 286);

		key = "abc.def:false|false|false|7402|true|true|8715|58b74df79be14bca|" + //
				"false";
		String[] keys15 = { "O", "T", "X", "B", "W", "E", "S", "Y", "U" };
		check(json, key, keys15, 309);

		key = "abc.def:5d920e41ad3fff06|4407|4751";
		String[] keys16 = { "I", "F", "C" };
		check(json, key, keys16, 352);

		key = "abc.def:true|false|false|false|false|4751|2690|7402|8972|380|tr" + //
				"ue|false|true|false|false|8715|false|4407|false|true";
		String[] keys17 = { "E", "O", "X", "P", "K", "C", "L", "B", "D", "V", "R", "U", "W", "J", "T", "S", "Q", "F",
				"A", "M" };
		check(json, key, keys17, 364);

		key = "abc.def:false|58b74df79be14bca|true|false|5d920e41ad3fff06|4407" + //
				"|2690|true|false|false|true|8715|false";
		String[] keys18 = { "T", "Y", "E", "O", "I", "F", "L", "N", "J", "A", "W", "S", "X" };
		check(json, key, keys18, 370);

		key = "abc.def:false|5d920e41ad3fff06|380|true|19f18ec9a484f480|8972|t" + //
				"rue|7402|4407|true|false|false|true|false|8715|true|false|false|false|" + //
				"false|false|58b74df79be14bca|4751|2690";
		String[] keys19 = { "K", "I", "V", "W", "H", "D", "R", "B", "F", "M", "X", "A", "E", "P", "S", "N", "U", "J",
				"Q", "O", "G", "Y", "C", "L" };
		check(json, key, keys19, 377);

		key = "abc.def:4751|true|8715|false|true|false|false|58b74df79be14bca|" + //
				"7402|true|false";
		String[] keys20 = { "C", "M", "S", "X", "E", "A", "O", "Y", "B", "W", "G" };
		check(json, key, keys20, 428);

		key = "abc.def:false|false|5d920e41ad3fff06|false|false|4407|19f18ec9a" + //
				"484f480|2690|8715|true|true";
		String[] keys21 = { "G", "P", "I", "T", "K", "F", "H", "L", "S", "E", "N" };
		check(json, key, keys21, 459);

		key = "abc.def:false|58b74df79be14bca|7402|4751|false|true|true|false|" + //
				"5d920e41ad3fff06|false|false|false|8715|true|false|2690|380|true|4407|" + //
				"false|true|19f18ec9a484f480|false|8972|false";
		String[] keys22 = { "K", "Y", "B", "C", "U", "R", "M", "J", "I", "T", "A", "X", "S", "W", "O", "L", "V", "E",
				"F", "Q", "N", "H", "G", "D", "P" };
		check(json, key, keys22, 497);

	}

	@Test
	public void testH() throws Exception {

		String json = "{'A':2908,'B':true,'C':'69212998fddbfc07','D':'2984650cae9022f3'" + //
				",'E':true,'F':true,'G':'2bcefadb92563738','H':true,'I':true,'J':7801,'K':false" + //
				",'L':'3126397e23b9e7b9','M':7273,'N':'2b11c049e892239f','O':true,'P':false" + //
				",'Q':'183b3840b9f874b2','R':6600,'S':false,'T':true,'U':true,'V':true,'W':false" + //
				",'X':false,'Y':false}";

		String key = "iOrOzUpx1CarR5lPZEj/gB/aSywiPapGtQXJ6kFyB2A=";
		String[] keys1 = { "L", "W", "N" };
		check(json, key, keys1, 44);

		key = "abc.def:6921PA8MJv79wqqxsTUo4V2IaUyNtX9KXLlQfX6TBcGIZVQ=";
		String[] keys2 = { "C", "B", "A", "P", "K", "O", "Q", "F", "J", "V", "T", "L", "Y" };
		check(json, key, keys2, 56);

		key = "abc.def:false|7273|true|false|true|2908";
		String[] keys3 = { "Y", "M", "O", "X", "U", "A" };
		check(json, key, keys3, 83);

		key = "abc.def:183b3840b9f874b2|true|true|true|true|2908|false|2bcefad" + //
				"b92563738|true|false|false|true|true|2b11c049e892239f";
		String[] keys4 = { "Q", "B", "I", "E", "U", "A", "P", "G", "V", "S", "K", "O", "F", "N" };
		check(json, key, keys4, 134);

		key = "abc.def:false|6600|true|true|true|2984650cae9022f3|false|true|f" + //
				"alse|7801|false|true";
		String[] keys5 = { "K", "R", "T", "O", "E", "D", "Y", "H", "W", "J", "S", "F" };
		check(json, key, keys5, 140);

		key = "abc.def:false";
		String[] keys6 = { "Y" };
		check(json, key, keys6, 161);

		key = "abc.def:3126397e23b9e7b9|true|false|true|true|true|true|true|tr" + //
				"ue|2908|2984650cae9022f3|false";
		String[] keys7 = { "L", "V", "S", "B", "O", "H", "F", "U", "I", "A", "D", "X" };
		check(json, key, keys7, 204);

		key = "abc.def:2908|true|true|3126397e23b9e7b9|true|7801|false|false|1" + //
				"83b3840b9f874b2|7273|false|true|2984650cae9022f3|true|false|2bcefadb92" + //
				"563738|6600";
		String[] keys8 = { "A", "U", "V", "L", "O", "J", "K", "Y", "Q", "M", "X", "H", "D", "F", "P", "G", "R" };
		check(json, key, keys8, 214);

		key = "abc.def:3126397e23b9e7b9|true|false|true|true|2b11c049e892239f|" + //
				"true|true|true|2908|7801|false|2bcefadb92563738|6600|false|true|true";
		String[] keys9 = { "L", "U", "K", "H", "F", "N", "B", "V", "T", "A", "J", "P", "G", "R", "S", "I", "E" };
		check(json, key, keys9, 216);

		key = "abc.def:true|true|3126397e23b9e7b9|2bcefadb92563738|true|298465" + //
				"0cae9022f3|false|false|2908|false|true|7801|true|false|true|7273|false" + //
				"|true|69212998fddbfc07|183b3840b9f874b2|true|true|false";
		String[] keys10 = { "H", "O", "L", "G", "V", "D", "X", "P", "A", "K", "F", "J", "U", "W", "I", "M", "Y", "E",
				"C", "Q", "T", "B", "S" };
		check(json, key, keys10, 229);

		key = "abc.def:true|true|true|false|true|7273";
		String[] keys11 = { "B", "U", "E", "X", "I", "M" };
		check(json, key, keys11, 269);

		key = "abc.def:3126397e23b9e7b9|true|183b3840b9f874b2|false|false|6921" + //
				"2998fddbfc07|6600|false|true|true|false|true|false|7801|true|2bcefadb9" + //
				"2563738|true|2908|2984650cae9022f3|true|2b11c049e892239f";
		String[] keys12 = { "L", "V", "Q", "S", "X", "C", "R", "W", "T", "E", "P", "U", "Y", "J", "O", "G", "I", "A",
				"D", "H", "N" };
		check(json, key, keys12, 309);

		key = "abc.def:2bcefadb92563738|true|6600|true|false|false|true|true|t" + //
				"rue|false|3126397e23b9e7b9|true|183b3840b9f874b2|false|2984650cae9022f" + //
				"3|false|7273|69212998fddbfc07|false|2908|2b11c049e892239f|true|true";
		String[] keys13 = { "G", "O", "R", "U", "W", "Y", "B", "E", "F", "S", "L", "H", "Q", "K", "D", "P", "M", "C",
				"X", "A", "N", "T", "I" };
		check(json, key, keys13, 344);

		key = "abc.def:true|true|7273|false|69212998fddbfc07|2b11c049e892239f|" + //
				"true|false|true|7801|true|6600|true|true|3126397e23b9e7b9|2908|false";
		String[] keys14 = { "T", "V", "M", "Y", "C", "N", "B", "K", "H", "J", "F", "R", "I", "E", "L", "A", "P" };
		check(json, key, keys14, 356);

		key = "abc.def:false|false|true|false|2b11c049e892239f|false|true|fals" + //
				"e|7273|183b3840b9f874b2|69212998fddbfc07";
		String[] keys15 = { "P", "W", "B", "Y", "N", "K", "O", "X", "M", "Q", "C" };
		check(json, key, keys15, 359);

		key = "abc.def:183b3840b9f874b2|true|true|false";
		String[] keys16 = { "Q", "O", "V", "W" };
		check(json, key, keys16, 373);

		key = "abc.def:false|false|false|2b11c049e892239f|true|7273|true|7801|" + //
				"true|true|true|true|false|false|false|2908|2984650cae9022f3|6600|true|" + //
				"3126397e23b9e7b9|183b3840b9f874b2|true|69212998fddbfc07|true|2bcefadb9" + //
				"2563738";
		String[] keys17 = { "S", "X", "W", "N", "U", "M", "O", "J", "V", "T", "I", "H", "Y", "P", "K", "A", "D", "R",
				"B", "L", "Q", "F", "C", "E", "G" };
		check(json, key, keys17, 423);

		key = "abc.def:false|true|true|183b3840b9f874b2|true|true|false|true|t" + //
				"rue|7273|7801|false|2908|3126397e23b9e7b9|true|6600";
		String[] keys18 = { "W", "F", "U", "Q", "T", "V", "K", "I", "E", "M", "J", "S", "A", "L", "H", "R" };
		check(json, key, keys18, 440);

		key = "abc.def:2908|6600|7801|true";
		String[] keys19 = { "A", "R", "J", "E" };
		check(json, key, keys19, 456);

		key = "abc.def:false|true|false|183b3840b9f874b2|7273|true|3126397e23b" + //
				"9e7b9|false|69212998fddbfc07|2984650cae9022f3|2bcefadb92563738|true";
		String[] keys20 = { "Y", "B", "K", "Q", "M", "O", "L", "X", "C", "D", "G", "U" };
		check(json, key, keys20, 466);

	}

	@Test
	public void testI() throws Exception {

		String json = "{'A':false,'B':'7b536386f36fe3ee','C':'2a6328cfc6680df1'" + //
				",'D':1373,'E':false,'F':true,'G':'44400e6f08ff8e66','H':true,'I':false" + //
				",'J':7472,'K':'33889ea4271ce580','L':false,'M':false,'N':false,'O':'274b91c528f50969'" + //
				",'P':false,'Q':'5573799a94e7292f','R':true,'S':1227,'T':8548,'U':false" + //
				",'V':8546,'W':'5c62d4aa6d6c9a6b','X':false,'Y':652}";

		String key = "fYPOy91Zt3hORWMM/QDvOCXpS66jvzBehhrau+bGYEs=";
		String[] keys1 = { "U", "D", "P", "B", "L", "N", "C", "O", "Q", "H", "G", "T", "X" };
		check(json, key, keys1, 44);

		key = "abc.def:false|false|1373|falKBDow031tb82TMBjEuWjfSNg2XtJi0edFDb" + //
				"jkTteNdM=";
		String[] keys2 = { "P", "M", "D", "A", "O", "I", "X", "Y", "G", "C", "L", "F", "H", "N", "S", "T", "V", "J",
				"K", "E" };
		check(json, key, keys2, 72);

		key = "abc.def:7b536386f36fe3ee|true|3BzfZhqnmTHoArjVOv/jYFnypQDY7aLQm" + //
				"v2pxUg0SWWA=";
		String[] keys3 = { "B", "R", "K", "C", "O", "E", "M", "X", "L", "Y", "G", "H", "T", "Q", "F", "A", "P" };
		check(json, key, keys3, 75);

		key = "abc.def:652|274b91c528f50969|5c62d4LcpNuNbcAHQlV5DgNc4RDU1wxfbX" + //
				"0JllWHZhukdV43A=";
		String[] keys4 = { "Y", "O", "W", "M", "B", "V", "N", "S", "F", "K", "J", "U", "L", "G" };
		check(json, key, keys4, 79);

		key = "abc.def:8546|false|false|false|7472|false|false|1373|652|33889e" + //
				"a4271N90hVZoBv9Bct/yiDkPYRPj+eVGTb6dxAtCXLaMhQvM=";
		String[] keys5 = { "V", "P", "M", "N", "J", "X", "E", "D", "Y", "K", "W", "I", "L", "H", "G", "F", "S", "C",
				"U", "A", "R" };
		check(json, key, keys5, 112);

		key = "abc.def:false|false|44400e6f08ff8e66|false";
		String[] keys6 = { "E", "U", "G", "M" };
		check(json, key, keys6, 116);

		key = "abc.def:false|274b91c528f50969|false|33889ea4271ce580|1373|1227" + //
				"|8548|557378aOsLJqbDMJuvkcUQryq0SLcS8xyWh/HZjFGIfGSEec=";
		String[] keys7 = { "P", "O", "M", "K", "D", "S", "T", "Q", "I", "Y", "U", "X", "C", "E", "R", "J", "A" };
		check(json, key, keys7, 118);

		key = "abc.def:true|33889ea4271ce580|5573799a94e7292f|44400e6f08ff8e66" + //
				"|652|true|274b91c528f50969|1373|true|74EDGJyIrHX8GFV+NrsJ2snUrs2Bv8Y7w" + //
				"vSBisWoMSb8o=";
		String[] keys8 = { "H", "K", "Q", "G", "Y", "R", "O", "D", "F", "J", "T", "P", "E", "I", "N", "M", "X", "L",
				"V", "A", "C", "B", "U", "W", "S" };
		check(json, key, keys8, 146);

		key = "abc.def:true|7472|652";
		String[] keys9 = { "F", "J", "Y" };
		check(json, key, keys9, 154);

		key = "abc.def:274b91c528f50969|8548|652|false|44400e6f08ff8e66|false|" + //
				"8546|true|true";
		String[] keys10 = { "O", "T", "Y", "L", "G", "U", "V", "R", "H" };
		check(json, key, keys10, 168);

		key = "abc.def:true|1373|7472|5c62d4aa6d6c9a6b|false|false|8546|false|" + //
				"8548|false|44400e6f08ff8e66|1227|false";
		String[] keys11 = { "R", "D", "J", "W", "L", "M", "V", "N", "T", "P", "G", "S", "X" };
		check(json, key, keys11, 205);

		key = "abc.def:true|1373|true|8548|false";
		String[] keys12 = { "R", "D", "H", "T", "U" };
		check(json, key, keys12, 249);

		key = "abc.def:2a6328cfc6680df1|false|true";
		String[] keys13 = { "C", "N", "H" };
		check(json, key, keys13, 286);

		key = "abc.def:false";
		String[] keys14 = { "U" };
		check(json, key, keys14, 322);

		key = "abc.def:true|1373|false";
		String[] keys15 = { "F", "D", "P" };
		check(json, key, keys15, 372);

		key = "abc.def:8548|7b536386f36fe3ee|5c62d4aa6d6c9a6b|false|true|false" + //
				"|false|2a6328cfc6680df1|false|33889ea4271ce580|false|274b91c528f50969|" + //
				"5573799a94e7292f|false|7472|true|false|8546|1373|44400e6f08ff8e66";
		String[] keys16 = { "T", "B", "W", "I", "F", "X", "N", "C", "M", "K", "U", "O", "Q", "P", "J", "R", "L", "V",
				"D", "G" };
		check(json, key, keys16, 378);

		key = "abc.def:7472|274b91c528f50969|false|8546|33889ea4271ce580|1373|" + //
				"false|2a6328cfc6680df1";
		String[] keys17 = { "J", "O", "P", "V", "K", "D", "E", "C" };
		check(json, key, keys17, 381);

		key = "abc.def:false|true|33889ea4271ce580|5573799a94e7292f|5c62d4aa6d" + //
				"6c9a6b|false|false|8548|false|false|274b91c528f50969|44400e6f08ff8e66|" + //
				"8546|false";
		String[] keys18 = { "L", "F", "K", "Q", "W", "U", "M", "T", "I", "A", "O", "G", "V", "E" };
		check(json, key, keys18, 421);

		key = "abc.def:false|true|8546|true|false|8548|5573799a94e7292f|7b5363" + //
				"86f36fe3ee";
		String[] keys19 = { "X", "H", "V", "F", "I", "T", "Q", "B" };
		check(json, key, keys19, 441);

		key = "abc.def:true|5573799a94e7292f|7472|5c62d4aa6d6c9a6b|false|1227|" + //
				"8546|652|false|false|true|2a6328cfc6680df1|false|44400e6f08ff8e66|fals" + //
				"e|33889ea4271ce580|false";
		String[] keys20 = { "H", "Q", "J", "W", "E", "S", "V", "Y", "L", "X", "F", "C", "U", "G", "I", "K", "P" };
		check(json, key, keys20, 464);

	}

	@Test
	public void testJ() throws Exception {

		String json = "{'A':false,'B':false,'C':2542,'D':5943,'E':false,'F':false" + //
				",'G':false,'H':true,'I':6146,'J':true,'K':true,'L':'54da61807a868822','M':true" + //
				",'N':2208,'O':'152f1387cbcc6dab','P':6586,'Q':true,'R':3269,'S':false,'T':6295" + //
				",'U':'42d919f4422418b8','V':'22591a7634a9baf3','W':'17f8b8f5cba74f11','X':true" + //
				",'Y':false}";

		String key = "+VdKbLV4uEu6IX3LXjLOyW9vdHr/TeKfjEDK/LXdN0U=";
		String[] keys1 = { "P", "X", "Q", "V", "N", "S", "M", "Y", "L", "W", "C" };
		check(json, key, keys1, 44);

		key = "abc.def:false|6586|true|false|true|false|false|false|true";
		String[] keys2 = { "F", "P", "M", "S", "X", "B", "Y", "A", "J" };
		check(json, key, keys2, 59);

		key = "abc.def:true|152f138RAwwISPytL5j2i7TwaZGyPD8E/2Q/9f7C1XLLbGQ+I=" + //
				"";
		String[] keys3 = { "Q", "O", "P", "S", "D", "U", "C", "R" };
		check(json, key, keys3, 63);

		key = "abc.def:true|true|false|54da61807a8688RHbqCMa5BemzGX9UgbrUmOMJ7" + //
				"dG6iMhN3PF5nLiGJsM=";
		String[] keys4 = { "M", "X", "Y", "L", "K", "D", "E", "T", "Q", "F", "G", "B", "C", "I" };
		check(json, key, keys4, 82);

		key = "abc.def:2208|6295|5943|false";
		String[] keys5 = { "N", "T", "D", "F" };
		check(json, key, keys5, 101);

		key = "abc.def:true|true|true|17f8b8f5cba74f11|6146|true|42d919f442241" + //
				"8b8|22591a7634a9baf3";
		String[] keys6 = { "K", "J", "X", "W", "I", "Q", "U", "V" };
		check(json, key, keys6, 113);

		key = "abc.def:true|false|6586|false|5943|false|6295|false|3269|false|" + //
				"2208|17f8b8f5cba74f11|152f1387cbcc6dab|42d9175lRXabumseQkgvzuEIQaTHErJ" + //
				"TM/qgdHEMGtRznf7Q=";
		String[] keys7 = { "H", "F", "P", "B", "D", "E", "T", "G", "R", "A", "N", "W", "O", "U", "Y", "C", "M", "I",
				"S", "J", "L", "V" };
		check(json, key, keys7, 151);

		key = "abc.def:true|22591a7634a9baf3|2542|false|true|false|true|3269|6" + //
				"146|true|54da61807a868822|false|42d919f4422418b8|6295|true|5943|152f13" + //
				"87cbcc6dab|false|2208";
		String[] keys8 = { "H", "V", "C", "F", "Q", "A", "X", "R", "I", "K", "L", "E", "U", "T", "M", "D", "O", "B",
				"N" };
		check(json, key, keys8, 169);

		key = "abc.def:152f1387cbcc6dab|5943|false|2542|54da61807a868822|true|" + //
				"6146|false|3269|false|17f8b8f5cba74f11|6295|false|true|false|6586|fals" + //
				"e|false|true|2208|true|true|22591a7634a9baf3|true|42d919f4422418b8";
		String[] keys9 = { "O", "D", "G", "C", "L", "H", "I", "S", "R", "B", "W", "T", "E", "M", "A", "P", "F", "Y",
				"Q", "N", "K", "X", "V", "J", "U" };
		check(json, key, keys9, 208);

		key = "abc.def:false|5943|false|true|22591a7634a9baf3|true|false|3269|" + //
				"false|6146|true|6586|2208|54da61807a868822|6295|152f1387cbcc6dab|false" + //
				"|true|17f8b8f5cba74f11|true|42d919f4422418b8|false|true";
		String[] keys10 = { "S", "D", "A", "J", "V", "M", "F", "R", "B", "I", "X", "P", "N", "L", "T", "O", "E", "Q",
				"W", "K", "U", "G", "H" };
		check(json, key, keys10, 228);

		key = "abc.def:false|22591a7634a9baf3|false|6586";
		String[] keys11 = { "Y", "V", "F", "P" };
		check(json, key, keys11, 255);

		key = "abc.def:54da61807a868822|false|true|3269|true|42d919f4422418b8|" + //
				"22591a7634a9baf3|17f8b8f5cba74f11|2208|false|true|152f1387cbcc6dab|254" + //
				"2|6586|false|true|6295|false|false";
		String[] keys12 = { "L", "Y", "Q", "R", "J", "U", "V", "W", "N", "A", "X", "O", "C", "P", "G", "K", "T", "B",
				"S" };
		check(json, key, keys12, 282);

		key = "abc.def:false|false|true|false|17f8b8f5cba74f11|54da61807a86882" + //
				"2|true|true|true|true|6586|true|2542|false|6146|152f1387cbcc6dab|2208|" + //
				"false|6295|5943|42d919f4422418b8|false|22591a7634a9baf3|3269|false";
		String[] keys13 = { "G", "B", "J", "S", "W", "L", "M", "X", "K", "Q", "P", "H", "C", "E", "I", "O", "N", "Y",
				"T", "D", "U", "A", "V", "R", "F" };
		check(json, key, keys13, 318);

		key = "abc.def:false|false|2208|42d919f4422418b8|5943|false|17f8b8f5cb" + //
				"a74f11|true|false|3269|54da61807a868822|false|true|false";
		String[] keys14 = { "S", "E", "N", "U", "D", "A", "W", "J", "B", "R", "L", "F", "Q", "G" };
		check(json, key, keys14, 358);

		key = "abc.def:true|152f1387cbcc6dab|true|2208|22591a7634a9baf3|false|" + //
				"true|false|true|false|42d919f4422418b8|5943|false|false|54da61807a8688" + //
				"22|6586|2542";
		String[] keys15 = { "M", "O", "K", "N", "V", "G", "J", "A", "H", "F", "U", "D", "Y", "S", "L", "P", "C" };
		check(json, key, keys15, 406);

		key = "abc.def:6295|true|6146|false|6586|true|true|5943|false|false|fa" + //
				"lse|true|152f1387cbcc6dab|17f8b8f5cba74f11|42d919f4422418b8|false|true" + //
				"|3269|2542|2208";
		String[] keys16 = { "T", "Q", "I", "Y", "P", "X", "K", "D", "G", "E", "S", "M", "O", "W", "U", "B", "H", "R",
				"C", "N" };
		check(json, key, keys16, 410);

		key = "abc.def:true|true|5943|false|false|true|22591a7634a9baf3|2542|6" + //
				"146|false|42d919f4422418b8|false|152f1387cbcc6dab|54da61807a868822|fal" + //
				"se|2208|true|3269|6295|17f8b8f5cba74f11";
		String[] keys17 = { "J", "K", "D", "G", "B", "H", "V", "C", "I", "Y", "U", "F", "O", "L", "E", "N", "M", "R",
				"T", "W" };
		check(json, key, keys17, 438);

		key = "abc.def:3269|true|42d919f4422418b8|true|false|152f1387cbcc6dab|" + //
				"true|6146|false|5943|22591a7634a9baf3|true|2542|54da61807a868822|6586";
		String[] keys18 = { "R", "K", "U", "H", "A", "O", "Q", "I", "B", "D", "V", "X", "C", "L", "P" };
		check(json, key, keys18, 481);

		key = "abc.def:6146|3269|true|false|42d919f4422418b8|false|true|true|2" + //
				"208|2542|true|6586|true|false|false|6295|17f8b8f5cba74f11|54da61807a86" + //
				"8822|5943|152f1387cbcc6dab|false|22591a7634a9baf3|false|true";
		String[] keys19 = { "I", "R", "H", "A", "U", "F", "M", "J", "N", "C", "K", "P", "X", "G", "B", "T", "W", "L",
				"D", "O", "S", "V", "Y", "Q" };
		check(json, key, keys19, 490);

	}

	@Test
	public void testK() throws Exception {

		String json = "{'A':'14eff65f5bd2377c','B':1714,'C':true,'D':false,'E':'ff5b3b56d78a6f9'" + //
				",'F':true,'G':false,'H':'44d67f8b2b7c6491','I':5050,'J':false,'K':false" + //
				",'L':'15853f29a0672c2a','M':8492,'N':false,'O':false,'P':true,'Q':false" + //
				",'R':7004,'S':true,'T':375,'U':true,'V':4904,'W':'a9975050eef9a93','X':true" + //
				",'Y':'4b8b1f8b8865c56f'}";

		String key = "FtJZ7JIEo5/knawKnnobU7k0mxp344bCpXVO8mEYe78=";
		String[] keys1 = { "R", "Q", "P", "C", "Y", "T", "E" };
		check(json, key, keys1, 44);

		key = "abc.def:true";
		String[] keys2 = { "X" };
		check(json, key, keys2, 59);

		key = "abc.def:15853f29a0672c2a|tr2Zq/ZE4xvSao6zNsoFKG+TlBCCws4cDNoR5V" + //
				"4bCBL2k=";
		String[] keys3 = { "L", "U", "E", "J", "G", "T", "X", "N", "S", "F", "V", "O", "P", "Q", "W", "Y", "B", "I",
				"D", "M", "R", "K", "H" };
		check(json, key, keys3, 71);

		key = "abc.def:false|true|false|false|375|false|4b8b1f8bNzChzMALS1RW2x" + //
				"bJY/Mw8AUcaWpudRNxWGubSmZyC54=";
		String[] keys4 = { "N", "P", "Q", "K", "T", "O", "Y", "X", "U", "J", "G", "W", "L", "A", "R", "B", "H", "E",
				"I", "M", "S", "C", "V", "F" };
		check(json, key, keys4, 93);

		key = "abc.def:4b8b1f8b8865c56f|15853f29a0672c2a";
		String[] keys5 = { "Y", "L" };
		check(json, key, keys5, 134);

		key = "abc.def:true|4b8b1f8b8865c56f|5050|false|375|false|true|8492|tr" + //
				"ue|44d67f8b2b7c6491|false";
		String[] keys6 = { "S", "Y", "I", "K", "T", "O", "X", "M", "F", "H", "D" };
		check(json, key, keys6, 152);

		key = "abc.def:44d67f8b2b7c6491|375|7004|true|ff5b3b56d78a6f9|a9975050" + //
				"eef9a93|false|false|true|false|false|true|false";
		String[] keys7 = { "H", "T", "R", "P", "E", "W", "N", "D", "F", "Q", "G", "C", "J" };
		check(json, key, keys7, 166);

		key = "abc.def:14eff65f5bd2377c|a9975050eef9a93|4b8b1f8b8865c56f|false" + //
				"|true|true|375|5050|false|false|true|7004|44d67f8b2b7c6491|false|false" + //
				"|truEpv9Asj99GBBuviWaTLVxcCk2w1u6aukM3Jl5e/5BYQ=";
		String[] keys8 = { "A", "W", "Y", "K", "X", "U", "T", "I", "D", "G", "S", "R", "H", "Q", "O", "F", "M", "N",
				"L", "J", "C", "V", "B" };
		check(json, key, keys8, 181);

		key = "abc.def:a9975050eef9a93|true|4b8b1f8b8865c56f|true|15853f29a067" + //
				"2c2a";
		String[] keys9 = { "W", "F", "Y", "C", "L" };
		check(json, key, keys9, 197);

		key = "abc.def:8492|false|true|true|true";
		String[] keys10 = { "M", "Q", "P", "S", "X" };
		check(json, key, keys10, 240);

		key = "abc.def:true|true|7004|true|375|a9975050eef9a93|4904|15853f29a0" + //
				"672c2a|false|false|44d67f8b2b7c6491";
		String[] keys11 = { "U", "S", "R", "X", "T", "W", "V", "L", "D", "N", "H" };
		check(json, key, keys11, 258);

		key = "abc.def:14eff65f5bd2377c|true|a9975050eef9a93|7004|1714|44d67f8" + //
				"b2b7c6491";
		String[] keys12 = { "A", "S", "W", "R", "B", "H" };
		check(json, key, keys12, 274);

		key = "abc.def:8492|4b8b1f8b8865c56f|5050|false|false|true|4904";
		String[] keys13 = { "M", "Y", "I", "O", "J", "U", "V" };
		check(json, key, keys13, 316);

		key = "abc.def:false|false|44d67f8b2b7c6491|false|4904|1714|true|true|" + //
				"8492|ff5b3b56d78a6f9|false|4b8b1f8b8865c56f|a9975050eef9a93|true";
		String[] keys14 = { "N", "D", "H", "K", "V", "B", "F", "C", "M", "E", "Q", "Y", "W", "S" };
		check(json, key, keys14, 335);

		key = "abc.def:false|ff5b3b56d78a6f9|8492|true|true|false";
		String[] keys15 = { "N", "E", "M", "S", "X", "G" };
		check(json, key, keys15, 370);

		key = "abc.def:15853f29a0672c2a|false|false|true|false|4904";
		String[] keys16 = { "L", "G", "O", "F", "Q", "V" };
		check(json, key, keys16, 421);

		key = "abc.def:375|false|true|true|ff5b3b56d78a6f9|false|7004|false|fa" + //
				"lse|1714|true|14eff65f5bd2377c|false|true|true|4904|false|15853f29a067" + //
				"2c2a";
		String[] keys17 = { "T", "J", "U", "S", "E", "O", "R", "K", "N", "B", "F", "A", "G", "P", "X", "V", "Q", "L" };
		check(json, key, keys17, 451);

	}

	@Test
	public void testL() throws Exception {

		String json = "{'A':false,'B':false,'C':false,'D':false,'E':7205,'F':true" + //
				",'G':'22b43c735926f635','H':true,'I':5172,'J':true,'K':true,'L':129,'M':'7cde944c75d668a3'" + //
				",'N':false,'O':false,'P':'50de6af065071492','Q':false,'R':3506,'S':3487" + //
				",'T':true,'U':8789,'V':true,'W':6898,'X':false,'Y':6770}";

		String key = "abc.def:true|false|true";
		String[] keys1 = { "J", "Q", "F" };
		check(json, key, keys1, 44);

		key = "abc.def:7cde944c75d668a3|false|falseuNp1Evyz8Mxld0Tkr3asgqFVnJ4" + //
				"/TBpKMbVNBQBQAM=";
		String[] keys2 = { "M", "A", "B", "F", "O", "T", "U", "J", "P", "X", "C", "G", "Q", "D", "Y", "L", "I", "N" };
		check(json, key, keys2, 79);

		key = "abc.def:false|6770|7205|8789|true|false|true|true|3487";
		String[] keys3 = { "C", "Y", "E", "U", "F", "Q", "T", "J", "S" };
		check(json, key, keys3, 129);

		key = "abc.def:false|false|6898|7cde944c75d668a3|true|true|5172|false|" + //
				"50de6af065071492|false|3487|22b43c735926f635|6770|true|true";
		String[] keys4 = { "D", "X", "W", "M", "V", "T", "I", "C", "P", "A", "S", "G", "Y", "H", "J" };
		check(json, key, keys4, 132);

		key = "abc.def:true|false|false|false|6898|false";
		String[] keys5 = { "T", "N", "Q", "O", "W", "B" };
		check(json, key, keys5, 178);

		key = "abc.def:false|true|7205";
		String[] keys6 = { "A", "V", "E" };
		check(json, key, keys6, 197);

		key = "abc.def:3487|true|false|6770|3506|50de6af065071492|false";
		String[] keys7 = { "S", "K", "D", "Y", "R", "P", "X" };
		check(json, key, keys7, 209);

		key = "abc.def:3506|false|false|false";
		String[] keys8 = { "R", "A", "N", "D" };
		check(json, key, keys8, 253);

		key = "abc.def:7cde944c75d668a3|false|8789";
		String[] keys9 = { "M", "B", "U" };
		check(json, key, keys9, 274);

		key = "abc.def:3487|false|false|5172|true|false|6770|true|22b43c735926" + //
				"f635|false|7205|false|true|50de6af065071492|7cde944c75d668a3|true|129|" + //
				"6898|true";
		String[] keys10 = { "S", "Q", "A", "I", "T", "X", "Y", "F", "G", "N", "E", "D", "K", "P", "M", "J", "L", "W",
				"V" };
		check(json, key, keys10, 322);

		key = "abc.def:true|6898|true|129|5172|true|3487|false";
		String[] keys11 = { "V", "W", "J", "L", "I", "K", "S", "A" };
		check(json, key, keys11, 336);

		key = "abc.def:true|7205|false|true|false|5172|true|false|22b43c735926" + //
				"f635|true|false|false|false|8789|3506|true|false|false|3487|6770|true|" + //
				"50de6af065071492|6898";
		String[] keys12 = { "K", "E", "A", "H", "D", "I", "J", "X", "G", "F", "B", "O", "N", "U", "R", "V", "C", "Q",
				"S", "Y", "T", "P", "W" };
		check(json, key, keys12, 360);

		key = "abc.def:true|6770|true|7205|false|false|false|true|5172|false|t" + //
				"rue|22b43c735926f635|true|false|7cde944c75d668a3|3487|3506|129|8789|50" + //
				"de6af065071492|false|false|false";
		String[] keys13 = { "H", "Y", "K", "E", "N", "X", "B", "V", "I", "Q", "T", "G", "J", "O", "M", "S", "R", "L",
				"U", "P", "C", "D", "A" };
		check(json, key, keys13, 407);

		key = "abc.def:7cde944c75d668a3|5172|false|false|true|6898|true|3506|8" + //
				"789|7205|6770|true|false|false|false|50de6af065071492|false|22b43c7359" + //
				"26f635|true|3487|129|true|true";
		String[] keys14 = { "M", "I", "B", "O", "K", "W", "H", "R", "U", "E", "Y", "J", "A", "X", "D", "P", "C", "G",
				"T", "S", "L", "V", "F" };
		check(json, key, keys14, 418);

		key = "abc.def:6770|22b43c735926f635|6898|false|true|7cde944c75d668a3|" + //
				"50de6af065071492|false|true|true|false|5172|3506|false|3487|true";
		String[] keys15 = { "Y", "G", "W", "X", "J", "M", "P", "A", "H", "V", "D", "I", "R", "Q", "S", "F" };
		check(json, key, keys15, 449);

		key = "abc.def:50de6af065071492|true|false|3487|22b43c735926f635|129|f" + //
				"alse|false|true|7cde944c75d668a3|false|false|6898|true|3506|7205|6770|" + //
				"5172|false";
		String[] keys16 = { "P", "K", "Q", "S", "G", "L", "A", "N", "J", "M", "O", "X", "W", "H", "R", "E", "Y", "I",
				"D" };
		check(json, key, keys16, 487);

	}

	// --- COMMON KEY TESTER METHOD ---

	protected RedisCacher cacher = new RedisCacher();

	protected void check(String json, String key, String[] keys, int maxKeyLength) throws Exception {
		json = json.replace('\'', '\"');
		Tree params = new Tree(json, "JsonBuiltin");
		cacher.setMaxKeyLength(maxKeyLength);
		String testKey = cacher.getCacheKey("abc.def", params, keys);
		assertEquals(key, testKey);
	}

}