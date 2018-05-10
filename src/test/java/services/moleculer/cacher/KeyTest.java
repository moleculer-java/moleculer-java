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

		String json = "{'A':{'C0':false,'C1':true,'C2':true,'C3':'495f761d77d6294f'" + //
				",'C4':true},'B':{'C0':true,'C1':false,'C2':true,'C3':'5721c26bfddb7927'" + //
				",'C4':false},'C':{'C0':'5d9e85c124d5d09e','C1':true,'C2':5366,'C3':false" + //
				",'C4':false},'D':{'C0':false,'C1':true,'C2':'704473bca1242604','C3':false" + //
				",'C4':'6fc56107e69be769'},'E':{'C0':true,'C1':true,'C2':4881,'C3':true" + //
				",'C4':1418},'F':{'C0':true,'C1':false,'C2':false,'C3':false,'C4':true}" + //
				",'G':{'C0':false,'C1':true,'C2':false,'C3':6547,'C4':9565},'H':{'C0':true" + //
				",'C1':1848,'C2':'232e6552d0b8aa98','C3':'1d50627abe5c0463','C4':5251},'I':{'C0':'ecd0e4eae08e4f'" + //
				",'C1':'197bcb312fc17f60','C2':4755,'C3':true,'C4':9552},'J':{'C0':false" + //
				",'C1':'1cc45cadbbf240f','C2':'4dbb352b21c3c2f3','C3':5065,'C4':'792b19631c78d4f6'}" + //
				",'K':{'C0':'13c23a525adf9e1f','C1':true,'C2':true,'C3':'589d3499abbf6765'" + //
				",'C4':true},'L':{'C0':false,'C1':true,'C2':4350,'C3':'72f6c4f0e9beb03c'" + //
				",'C4':'434b74b5ff500609'},'M':{'C0':9228,'C1':'5254b36ec238c266','C2':true" + //
				",'C3':'27b040089b057684','C4':true},'N':{'C0':'35d3c608ef8aac5e','C1':'23fbdbd520d5ae7d'" + //
				",'C2':false,'C3':9061,'C4':true},'O':{'C0':true,'C1':true,'C2':'2382f9fe7834e0cc'" + //
				",'C3':true,'C4':false},'P':{'C0':true,'C1':false,'C2':'38c0d40b91a9d1f6'" + //
				",'C3':false,'C4':5512},'Q':{'C0':true,'C1':true,'C2':true,'C3':true,'C4':true}" + //
				",'R':{'C0':'70bd27c06b067734','C1':true,'C2':'5213493253b98636','C3':8272" + //
				",'C4':1264},'S':{'C0':'61044125008e634c','C1':9175,'C2':true,'C3':'225e3d912bfbc338'" + //
				",'C4':false},'T':{'C0':'38edc77387da030a','C1':false,'C2':'38d8b9e2525413fc'" + //
				",'C3':true,'C4':false},'U':{'C0':false,'C1':'4b3962c3d26bddd0','C2':'1e66b069bad46643'" + //
				",'C3':3642,'C4':9225},'V':{'C0':'1c40e44b54486080','C1':'5a560d81078bab02'" + //
				",'C2':'1c131259e1e9aa61','C3':true,'C4':9335},'W':{'C0':false,'C1':'7089b0ad438df2cb'" + //
				",'C2':'216aec98f513ac08','C3':true,'C4':false},'X':{'C0':'3b749354aac19f24'" + //
				",'C1':9626,'C2':true,'C3':false,'C4':false},'Y':{'C0':298,'C1':'224075dadd108ef9'" + //
				",'C2':3450,'C3':2548,'C4':true}}";

		String key = "abc.def:/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|/18Ct" + //
				"At7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys2 = null;
		check(json, key, keys2, 94);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95s" + //
				"jo=";
		String[] keys3 = null;
		check(json, key, keys3, 128);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8" + //
				"o+riN95sjo=";
		String[] keys4 = null;
		check(json, key, keys4, 136);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c1/" + //
				"18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys5 = null;
		check(json, key, keys5, 168);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys6 = null;
		check(json, key, keys6, 187);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|f/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sj" + //
				"o=";
		String[] keys7 = null;
		check(json, key, keys7, 197);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|fals/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN9" + //
				"5sjo=";
		String[] keys8 = null;
		check(json, key, keys8, 200);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+r" + //
				"iN95sjo=";
		String[] keys9 = null;
		check(json, key, keys9, 203);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|7/18Ct" + //
				"At7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys10 = null;
		check(json, key, keys10, 234);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|fals/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys11 = null;
		check(json, key, keys11, 257);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|false|C4|6fc56107e69be769|E|C0|true|C1|tr/18CtAt7Z+barI7" + //
				"S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys12 = null;
		check(json, key, keys12, 294);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|false|C4|6fc56107e69be769|E|C0|true|C1|true|C2|4881|C3|t" + //
				"rue|C4|1418|F|C0/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys13 = null;
		check(json, key, keys13, 325);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|false|C4|6fc56107e69be769|E|C0|true|C1|true|C2|4881|C3|t" + //
				"rue|C4|1418|F|C0|true|C1|/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys14 = null;
		check(json, key, keys14, 334);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|false|C4|6fc56107e69be769|E|C0|true|C1|true|C2|4881|C3|t" + //
				"rue|C4|1418|F|C0|true|C1|fal/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sj" + //
				"o=";
		String[] keys15 = null;
		check(json, key, keys15, 337);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|false|C4|6fc56107e69be769|E|C0|true|C1|true|C2|4881|C3|t" + //
				"rue|C4|1418|F|C0|true|C1|false|C/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN" + //
				"95sjo=";
		String[] keys16 = null;
		check(json, key, keys16, 341);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|false|C4|6fc56107e69be769|E|C0|true|C1|true|C2|4881|C3|t" + //
				"rue|C4|1418|F|C0|true|C1|false|C2|false|C3|false|C4|true|G|C0|false|C1" + //
				"|true|C2|fa/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys17 = null;
		check(json, key, keys17, 390);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|false|C4|6fc56107e69be769|E|C0|true|C1|true|C2|4881|C3|t" + //
				"rue|C4|1418|F|C0|true|C1|false|C2|false|C3|false|C4|true|G|C0|false|C1" + //
				"|true|C2|false|C3|6547|C4|9565|H|C0/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+" + //
				"riN95sjo=";
		String[] keys18 = null;
		check(json, key, keys18, 414);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|false|C4|6fc56107e69be769|E|C0|true|C1|true|C2|4881|C3|t" + //
				"rue|C4|1418|F|C0|true|C1|false|C2|false|C3|false|C4|true|G|C0|false|C1" + //
				"|true|C2|false|C3|6547|C4|9565|H|C0|true|C1|1848|C2|232e6552d0b8aa98|C" + //
				"3|1d50/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o+riN95sjo=";
		String[] keys19 = null;
		check(json, key, keys19, 455);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|495f761d77d6294f|C4|true|" + //
				"B|C0|true|C1|false|C2|true|C3|5721c26bfddb7927|C4|false|C|C0|5d9e85c12" + //
				"4d5d09e|C1|true|C2|5366|C3|false|C4|false|D|C0|false|C1|true|C2|704473" + //
				"bca1242604|C3|false|C4|6fc56107e69be769|E|C0|true|C1|true|C2|4881|C3|t" + //
				"rue|C4|1418|F|C0|true|C1|false|C2|false|C3|false|C4|true|G|C0|false|C1" + //
				"|true|C2|false|C3|6547|C4|9565|H|C0|true|C1|1848|C2|232e6552d0b8aa98|C" + //
				"3|1d50627abe5c0463|C4|5251|I|C0|ecd0/18CtAt7Z+barI7S7Ef+WTFQ23yVQ4VM8o" + //
				"+riN95sjo=";
		String[] keys20 = null;
		check(json, key, keys20, 485);

	}

	@Test
	public void testB() throws Exception {

		String json = "{'A':{'C0':false,'C1':false,'C2':false,'C3':false,'C4':false}" + //
				",'B':{'C0':false,'C1':true,'C2':true,'C3':'2c3cbe440fce25e0','C4':'79b08d3df79acc86'}" + //
				",'C':{'C0':true,'C1':'5f5c124a505a2543','C2':'609dcfbd11bea961','C3':true" + //
				",'C4':false},'D':{'C0':true,'C1':true,'C2':true,'C3':false,'C4':false}" + //
				",'E':{'C0':false,'C1':'3b91a51967ea8799','C2':'1cccd64ff4fab67e','C3':true" + //
				",'C4':false},'F':{'C0':'2d43ffd9093eaad0','C1':'14bd7fff9e9e873b','C2':'6cbe747e6aaf258f'" + //
				",'C3':false,'C4':'67e7604a7ab1f967'},'G':{'C0':true,'C1':5414,'C2':'4e5e26158dc1f2ef'" + //
				",'C3':7901,'C4':true},'H':{'C0':2831,'C1':false,'C2':true,'C3':true,'C4':true}" + //
				",'I':{'C0':false,'C1':false,'C2':3980,'C3':false,'C4':3687},'J':{'C0':1585" + //
				",'C1':5064,'C2':false,'C3':'45e474bd02b926f3','C4':6958},'K':{'C0':false" + //
				",'C1':false,'C2':'682ea72491e24d14','C3':true,'C4':9346},'L':{'C0':true" + //
				",'C1':'610a44b5e37db73d','C2':true,'C3':true,'C4':false},'M':{'C0':true" + //
				",'C1':true,'C2':163,'C3':5465,'C4':5484},'N':{'C0':3649,'C1':true,'C2':true" + //
				",'C3':9125,'C4':7115},'O':{'C0':'6b322955edd0cf3','C1':8553,'C2':true,'C3':'5d4028ca241065ad'" + //
				",'C4':551},'P':{'C0':true,'C1':6263,'C2':false,'C3':false,'C4':true},'Q':{'C0':9279" + //
				",'C1':3979,'C2':true,'C3':false,'C4':true},'R':{'C0':1477,'C1':false,'C2':'1f95e64df49db54b'" + //
				",'C3':'12873457c850dcb','C4':'42c009ab83577015'},'S':{'C0':'7eed9015db721773'" + //
				",'C1':true,'C2':4946,'C3':true,'C4':true},'T':{'C0':4394,'C1':false,'C2':false" + //
				",'C3':false,'C4':'575197afc4e8cbcb'},'U':{'C0':8244,'C1':true,'C2':false" + //
				",'C3':'69d551622315656','C4':true},'V':{'C0':true,'C1':3050,'C2':false" + //
				",'C3':false,'C4':9488},'W':{'C0':2469,'C1':true,'C2':false,'C3':false,'C4':true}" + //
				",'X':{'C0':true,'C1':'4be891114c9afcb2','C2':7775,'C3':true,'C4':true}" + //
				",'Y':{'C0':true,'C1':true,'C2':false,'C3':false,'C4':3326}}";

		String key = "abc.def:9/M/78c+dXKwf3w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|9/M/78c+" + //
				"dXKwf3w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys2 = null;
		check(json, key, keys2, 91);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|tr9/M/78c+dXKwf3w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys3 = null;
		check(json, key, keys3, 107);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fc9/M/78c+dXKwf3w3UNtzF3G/ItOpyIeDBoliZ" + //
				"9LHkRc=";
		String[] keys4 = null;
		check(json, key, keys4, 132);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df799/M/78c+dXKwf3w3U" + //
				"NtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys5 = null;
		check(json, key, keys5, 152);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true9/" + //
				"M/78c+dXKwf3w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys6 = null;
		check(json, key, keys6, 167);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd19/M/78c+dXKwf3w3UNtzF3G/ItOpyIeDBoliZ9L" + //
				"HkRc=";
		String[] keys7 = null;
		check(json, key, keys7, 200);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|fals9/M/78c+dXKwf3w3" + //
				"UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys8 = null;
		check(json, key, keys8, 223);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|false|9/M/78c+dXKwf3" + //
				"w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys9 = null;
		check(json, key, keys9, 225);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|false|D|C0|9/M/78c+d" + //
				"XKwf3w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys10 = null;
		check(json, key, keys10, 230);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|false|D|C0|true|C1|t" + //
				"rue|C2|true|C3|fals9/M/78c+dXKwf3w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys11 = null;
		check(json, key, keys11, 258);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|false|D|C0|true|C1|t" + //
				"rue|C2|true|C3|false|C4|false|E|C0|false|C1|3b91a51967ea8799|C29/M/78c" + //
				"+dXKwf3w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys12 = null;
		check(json, key, keys12, 302);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|false|D|C0|true|C1|t" + //
				"rue|C2|true|C3|false|C4|false|E|C0|false|C1|3b91a51967ea8799|C2|1cccd6" + //
				"4ff4fab67e|C3|true|C4|false|F|C0|2d43ffd909/M/78c+dXKwf3w3UNtzF3G/ItOp" + //
				"yIeDBoliZ9LHkRc=";
		String[] keys13 = null;
		check(json, key, keys13, 351);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|false|D|C0|true|C1|t" + //
				"rue|C2|true|C3|false|C4|false|E|C0|false|C1|3b91a51967ea8799|C2|1cccd6" + //
				"4ff4fab67e|C3|true|C4|false|F|C0|2d43ffd9093eaad0|C1|14bd7fff9e9e873b|" + //
				"C2|6cbe747e9/M/78c+dXKwf3w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys14 = null;
		check(json, key, keys14, 390);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|false|D|C0|true|C1|t" + //
				"rue|C2|true|C3|false|C4|false|E|C0|false|C1|3b91a51967ea8799|C2|1cccd6" + //
				"4ff4fab67e|C3|true|C4|false|F|C0|2d43ffd9093eaad0|C1|14bd7fff9e9e873b|" + //
				"C2|6cbe747e6aaf258f|C3|false|C4|67e7604a7ab1f967|G|C09/M/78c+dXKwf3w3U" + //
				"NtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys15 = null;
		check(json, key, keys15, 432);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|false|D|C0|true|C1|t" + //
				"rue|C2|true|C3|false|C4|false|E|C0|false|C1|3b91a51967ea8799|C2|1cccd6" + //
				"4ff4fab67e|C3|true|C4|false|F|C0|2d43ffd9093eaad0|C1|14bd7fff9e9e873b|" + //
				"C2|6cbe747e6aaf258f|C3|false|C4|67e7604a7ab1f967|G|C0|true|C1|5414|C2|" + //
				"4e5e2619/M/78c+dXKwf3w3UNtzF3G/ItOpyIeDBoliZ9LHkRc=";
		String[] keys16 = null;
		check(json, key, keys16, 456);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|false|B|C0|fal" + //
				"se|C1|true|C2|true|C3|2c3cbe440fce25e0|C4|79b08d3df79acc86|C|C0|true|C" + //
				"1|5f5c124a505a2543|C2|609dcfbd11bea961|C3|true|C4|false|D|C0|true|C1|t" + //
				"rue|C2|true|C3|false|C4|false|E|C0|false|C1|3b91a51967ea8799|C2|1cccd6" + //
				"4ff4fab67e|C3|true|C4|false|F|C0|2d43ffd9093eaad0|C1|14bd7fff9e9e873b|" + //
				"C2|6cbe747e6aaf258f|C3|false|C4|67e7604a7ab1f967|G|C0|true|C1|5414|C2|" + //
				"4e5e26158dc1f2ef|C3|7901|C4|true|H|C0|2831|C1|f9/M/78c+dXKwf3w3UNtzF3G" + //
				"/ItOpyIeDBoliZ9LHkRc=";
		String[] keys17 = null;
		check(json, key, keys17, 496);

	}

	@Test
	public void testC() throws Exception {

		String json = "{'A':{'C0':1233,'C1':'3174edf297d4867c','C2':false,'C3':false" + //
				",'C4':'1e419045e99857a9'},'B':{'C0':false,'C1':true,'C2':true,'C3':3053" + //
				",'C4':true},'C':{'C0':884,'C1':9300,'C2':true,'C3':true,'C4':false},'D':{'C0':false" + //
				",'C1':false,'C2':8003,'C3':7171,'C4':false},'E':{'C0':4350,'C1':9697,'C2':'3953a88645374e98'" + //
				",'C3':false,'C4':false},'F':{'C0':false,'C1':'47b30f6560c50d8e','C2':'25d180a9d5650c66'" + //
				",'C3':false,'C4':'5cfc740e40391018'},'G':{'C0':5626,'C1':'6930ac5056072c13'" + //
				",'C2':'174cfa2579fa0dbe','C3':false,'C4':'53dc3e5293202e8e'},'H':{'C0':2836" + //
				",'C1':false,'C2':8275,'C3':'11a83c4785b188c2','C4':true},'I':{'C0':true" + //
				",'C1':'468f41816d7d8f9f','C2':true,'C3':false,'C4':4318},'J':{'C0':true" + //
				",'C1':false,'C2':1955,'C3':false,'C4':'1a2f2cc7316629c7'},'K':{'C0':false" + //
				",'C1':5243,'C2':6747,'C3':'3eeb8ac5953c0434','C4':1620},'L':{'C0':true" + //
				",'C1':true,'C2':true,'C3':false,'C4':true},'M':{'C0':3686,'C1':true,'C2':8012" + //
				",'C3':4355,'C4':true},'N':{'C0':'6c0cb7cdc11e4ff4','C1':4290,'C2':true" + //
				",'C3':false,'C4':true},'O':{'C0':2484,'C1':'425e23f1e731b010','C2':false" + //
				",'C3':false,'C4':1895},'P':{'C0':'4996102be2d4b998','C1':false,'C2':true" + //
				",'C3':500,'C4':true},'Q':{'C0':7268,'C1':3031,'C2':true,'C3':true,'C4':false}" + //
				",'R':{'C0':false,'C1':false,'C2':false,'C3':true,'C4':true},'S':{'C0':false" + //
				",'C1':false,'C2':'5fe94907df18cea6','C3':true,'C4':true},'T':{'C0':'7adee5b6e6790d12'" + //
				",'C1':false,'C2':'408bb485490cd1a9','C3':true,'C4':false},'U':{'C0':true" + //
				",'C1':false,'C2':'15410d15835d146f','C3':true,'C4':false},'V':{'C0':false" + //
				",'C1':'1330af5feb6083f8','C2':false,'C3':true,'C4':true},'W':{'C0':false" + //
				",'C1':false,'C2':8206,'C3':'183a958a079f2b69','C4':'2029d2340f92a100'}" + //
				",'X':{'C0':true,'C1':false,'C2':7112,'C3':true,'C4':19},'Y':{'C0':true" + //
				",'C1':3835,'C2':true,'C3':true,'C4':false}}";

		String key = "abc.def:tfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|falsetfAxXkWOSiDMZXdCs" + //
				"rJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys2 = null;
		check(json, key, keys2, 82);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|ftfAxXkWOSiDM" + //
				"ZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys3 = null;
		check(json, key, keys3, 87);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|truetfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZ" + //
				"ndfw=";
		String[] keys4 = null;
		check(json, key, keys4, 130);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|tfAxXkWOSiDMZX" + //
				"dCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys5 = null;
		check(json, key, keys5, 155);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"0tfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys6 = null;
		check(json, key, keys6, 170);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2tfAxXkWOSiDMZXdCsrJF" + //
				"FtJY37gdxx672UEfoSZndfw=";
		String[] keys7 = null;
		check(json, key, keys7, 219);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|ftf" + //
				"AxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys8 = null;
		check(json, key, keys8, 237);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|tfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys9 = null;
		check(json, key, keys9, 244);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|CtfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys10 = null;
		check(json, key, keys10, 261);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|tfAxXkWOS" + //
				"iDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys11 = null;
		check(json, key, keys11, 300);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|CtfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys12 = null;
		check(json, key, keys12, 330);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|C2|tfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys13 = null;
		check(json, key, keys13, 332);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|C2|25d180a9d5650c66|CtfAxXkWOSiDMZXdCsrJFFtJY37gdx" + //
				"x672UEfoSZndfw=";
		String[] keys14 = null;
		check(json, key, keys14, 350);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|C2|25d180a9d5650c66|C3|false|C4|5cfc740e40391018|G" + //
				"|C0|5626|C1|6930actfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys15 = null;
		check(json, key, keys15, 397);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|C2|25d180a9d5650c66|C3|false|C4|5cfc740e40391018|G" + //
				"|C0|5626|C1|6930ac5056072c13|tfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZnd" + //
				"fw=";
		String[] keys16 = null;
		check(json, key, keys16, 408);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|C2|25d180a9d5650c66|C3|false|C4|5cfc740e40391018|G" + //
				"|C0|5626|C1|6930ac5056072c13|C2|174cfa2579fatfAxXkWOSiDMZXdCsrJFFtJY37" + //
				"gdxx672UEfoSZndfw=";
		String[] keys17 = null;
		check(json, key, keys17, 423);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|C2|25d180a9d5650c66|C3|false|C4|5cfc740e40391018|G" + //
				"|C0|5626|C1|6930ac5056072c13|C2|174cfa2579fa0dbe|tfAxXkWOSiDMZXdCsrJFF" + //
				"tJY37gdxx672UEfoSZndfw=";
		String[] keys18 = null;
		check(json, key, keys18, 428);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|C2|25d180a9d5650c66|C3|false|C4|5cfc740e40391018|G" + //
				"|C0|5626|C1|6930ac5056072c13|C2|174cfa2579fa0dbe|C3|false|C4|53dc3e529" + //
				"32tfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys19 = null;
		check(json, key, keys19, 451);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|C2|25d180a9d5650c66|C3|false|C4|5cfc740e40391018|G" + //
				"|C0|5626|C1|6930ac5056072c13|C2|174cfa2579fa0dbe|C3|false|C4|53dc3e529" + //
				"3202e8e|H|C0|2836|C1|ftfAxXkWOSiDMZXdCsrJFFtJY37gdxx672UEfoSZndfw=";
		String[] keys20 = null;
		check(json, key, keys20, 471);

		key = "abc.def:A|C0|1233|C1|3174edf297d4867c|C2|false|C3|false|C4|1e41" + //
				"9045e99857a9|B|C0|false|C1|true|C2|true|C3|3053|C4|true|C|C0|884|C1|93" + //
				"00|C2|true|C3|true|C4|false|D|C0|false|C1|false|C2|8003|C3|7171|C4|fal" + //
				"se|E|C0|4350|C1|9697|C2|3953a88645374e98|C3|false|C4|false|F|C0|false|" + //
				"C1|47b30f6560c50d8e|C2|25d180a9d5650c66|C3|false|C4|5cfc740e40391018|G" + //
				"|C0|5626|C1|6930ac5056072c13|C2|174cfa2579fa0dbe|C3|false|C4|53dc3e529" + //
				"3202e8e|H|C0|2836|C1|false|C2|8275|C3|tfAxXkWOSiDMZXdCsrJFFtJY37gdxx67" + //
				"2UEfoSZndfw=";
		String[] keys21 = null;
		check(json, key, keys21, 487);

	}

	@Test
	public void testD() throws Exception {

		String json = "{'A':{'C0':true,'C1':true,'C2':'f357aa0c4f25270','C3':true" + //
				",'C4':'3ab24e3e0694d13e'},'B':{'C0':true,'C1':8134,'C2':'569497d6aa506bdd'" + //
				",'C3':3060,'C4':'32723e36f513c53a'},'C':{'C0':true,'C1':'5c864b10d99aa732'" + //
				",'C2':9963,'C3':468,'C4':128},'D':{'C0':1413,'C1':'6e269d98f47d045d','C2':'78d29ca9d493f4e'" + //
				",'C3':false,'C4':false},'E':{'C0':false,'C1':'2e53d644ee398901','C2':'1c3bf85df94f7a50'" + //
				",'C3':'47a7499073f16c0e','C4':true},'F':{'C0':false,'C1':'560463eb43bc43d3'" + //
				",'C2':'79be8fb72967965e','C3':9777,'C4':'4092ce84695ff5a4'},'G':{'C0':7743" + //
				",'C1':'1bdc4e854af0f0ce','C2':2224,'C3':false,'C4':false},'H':{'C0':9835" + //
				",'C1':true,'C2':false,'C3':true,'C4':false},'I':{'C0':false,'C1':'59c15d6a53b33507'" + //
				",'C2':false,'C3':true,'C4':1633},'J':{'C0':'577efe8d37fb6cb8','C1':false" + //
				",'C2':true,'C3':2458,'C4':false},'K':{'C0':true,'C1':false,'C2':6698,'C3':false" + //
				",'C4':true},'L':{'C0':true,'C1':true,'C2':5398,'C3':false,'C4':true},'M':{'C0':true" + //
				",'C1':true,'C2':true,'C3':true,'C4':false},'N':{'C0':false,'C1':8022,'C2':3349" + //
				",'C3':true,'C4':614},'O':{'C0':false,'C1':'553e069b6338863a','C2':true" + //
				",'C3':true,'C4':false},'P':{'C0':'70a724920422a06c','C1':'def44676a7601af'" + //
				",'C2':'69518b27c2f03cc9','C3':1898,'C4':3981},'Q':{'C0':'1f1c885c80c60865'" + //
				",'C1':true,'C2':false,'C3':true,'C4':true},'R':{'C0':true,'C1':true,'C2':true" + //
				",'C3':6969,'C4':false},'S':{'C0':7992,'C1':5938,'C2':6777,'C3':'4c1dd4155f695886'" + //
				",'C4':true},'T':{'C0':true,'C1':true,'C2':5326,'C3':9951,'C4':true},'U':{'C0':3948" + //
				",'C1':false,'C2':'5756ab17497bb376','C3':true,'C4':false},'V':{'C0':'7e08d62792d4a81e'" + //
				",'C1':'1505529742f2813d','C2':false,'C3':true,'C4':'42c16bdb13eae96b'}" + //
				",'W':{'C0':'60ecdbdb56712ec8','C1':8775,'C2':1699,'C3':false,'C4':'5689a420e0536d8e'}" + //
				",'X':{'C0':true,'C1':8512,'C2':true,'C3':false,'C4':'3898d5a7f9bb138a'}" + //
				",'Y':{'C0':2790,'C1':true,'C2':'4ac5436d61f2e1e8','C3':false,'C4':true" + //
				"}}";

		String key = "abc.def:aeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|traeHGiUnu3Wcpp" + //
				"dpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys2 = null;
		check(json, key, keys2, 86);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497aeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGn" + //
				"eEMUr2CTIs=";
		String[] keys3 = null;
		check(json, key, keys3, 136);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|aeHGiUnu3WcppdpXQQeQcZ" + //
				"/RdOUVokRGneEMUr2CTIs=";
		String[] keys4 = null;
		check(json, key, keys4, 147);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|30aeHGiUnu3WcppdpXQ" + //
				"QeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys5 = null;
		check(json, key, keys5, 152);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aaaeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr" + //
				"2CTIs=";
		String[] keys6 = null;
		check(json, key, keys6, 201);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|99aeHGiUnu3WcppdpXQQeQcZ/RdOUVo" + //
				"kRGneEMUr2CTIs=";
		String[] keys7 = null;
		check(json, key, keys7, 210);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|ae" + //
				"HGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys8 = null;
		check(json, key, keys8, 237);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d0aeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys9 = null;
		check(json, key, keys9, 253);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d045d|C2|78daeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys10 = null;
		check(json, key, keys10, 263);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d045d|C2|78d29ca9d493f4e|C3|false|C4|false|E|C0|false|C1ae" + //
				"HGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys11 = null;
		check(json, key, keys11, 307);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d045d|C2|78d29ca9d493f4e|C3|false|C4|false|E|C0|false|C1|2" + //
				"e53d644ee398901|C2|1c3baeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys12 = null;
		check(json, key, keys12, 332);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d045d|C2|78d29ca9d493f4e|C3|false|C4|false|E|C0|false|C1|2" + //
				"e53d644ee398901|C2|1c3bf85df94f7a50|C3|47a7499073f16c0e|C4|true|F|C0|f" + //
				"aeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys13 = null;
		check(json, key, keys13, 379);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d045d|C2|78d29ca9d493f4e|C3|false|C4|false|E|C0|false|C1|2" + //
				"e53d644ee398901|C2|1c3bf85df94f7a50|C3|47a7499073f16c0e|C4|true|F|C0|f" + //
				"alse|C1|560463eb43aeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys14 = null;
		check(json, key, keys14, 397);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d045d|C2|78d29ca9d493f4e|C3|false|C4|false|E|C0|false|C1|2" + //
				"e53d644ee398901|C2|1c3bf85df94f7a50|C3|47a7499073f16c0e|C4|true|F|C0|f" + //
				"alse|C1|560463eb43bc43d3|C2|79be8fb7296796aeHGiUnu3WcppdpXQQeQcZ/RdOUV" + //
				"okRGneEMUr2CTIs=";
		String[] keys15 = null;
		check(json, key, keys15, 421);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d045d|C2|78d29ca9d493f4e|C3|false|C4|false|E|C0|false|C1|2" + //
				"e53d644ee398901|C2|1c3bf85df94f7a50|C3|47a7499073f16c0e|C4|true|F|C0|f" + //
				"alse|C1|560463eb43bc43d3|C2|79be8fb72967965e|C3|9777|C4|4092ce84695ff5" + //
				"a4|G|C0|7743|C1aeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys16 = null;
		check(json, key, keys16, 464);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d045d|C2|78d29ca9d493f4e|C3|false|C4|false|E|C0|false|C1|2" + //
				"e53d644ee398901|C2|1c3bf85df94f7a50|C3|47a7499073f16c0e|C4|true|F|C0|f" + //
				"alse|C1|560463eb43bc43d3|C2|79be8fb72967965e|C3|9777|C4|4092ce84695ff5" + //
				"a4|G|C0|7743|C1|1bdaeHGiUnu3WcppdpXQQeQcZ/RdOUVokRGneEMUr2CTIs=";
		String[] keys17 = null;
		check(json, key, keys17, 468);

		key = "abc.def:A|C0|true|C1|true|C2|f357aa0c4f25270|C3|true|C4|3ab24e3" + //
				"e0694d13e|B|C0|true|C1|8134|C2|569497d6aa506bdd|C3|3060|C4|32723e36f51" + //
				"3c53a|C|C0|true|C1|5c864b10d99aa732|C2|9963|C3|468|C4|128|D|C0|1413|C1" + //
				"|6e269d98f47d045d|C2|78d29ca9d493f4e|C3|false|C4|false|E|C0|false|C1|2" + //
				"e53d644ee398901|C2|1c3bf85df94f7a50|C3|47a7499073f16c0e|C4|true|F|C0|f" + //
				"alse|C1|560463eb43bc43d3|C2|79be8fb72967965e|C3|9777|C4|4092ce84695ff5" + //
				"a4|G|C0|7743|C1|1bdc4e854af0f0ce|C2|2224|C3|falsaeHGiUnu3WcppdpXQQeQcZ" + //
				"/RdOUVokRGneEMUr2CTIs=";
		String[] keys18 = null;
		check(json, key, keys18, 497);

	}

	@Test
	public void testE() throws Exception {

		String json = "{'A':{'C0':false,'C1':false,'C2':false,'C3':6726,'C4':false}" + //
				",'B':{'C0':false,'C1':true,'C2':true,'C3':'7ef768081f6cc0','C4':5969},'C':{'C0':3504" + //
				",'C1':488,'C2':'dc6aeb550ae174d','C3':true,'C4':6049},'D':{'C0':'58798ebc68196d8e'" + //
				",'C1':'704eaf4feba164d4','C2':8059,'C3':false,'C4':341},'E':{'C0':'5a3de6dda27bab47'" + //
				",'C1':false,'C2':'2f21dcbf45193ac1','C3':'7db49b715aa765c4','C4':'25f963dd94132d9e'}" + //
				",'F':{'C0':false,'C1':true,'C2':'4ef210a4c5f3fb0','C3':1174,'C4':'53ccceb49b5063a7'}" + //
				",'G':{'C0':true,'C1':false,'C2':true,'C3':false,'C4':3419},'H':{'C0':6994" + //
				",'C1':1363,'C2':6148,'C3':false,'C4':'39223a4f51ad14ff'},'I':{'C0':false" + //
				",'C1':false,'C2':true,'C3':976,'C4':false},'J':{'C0':4629,'C1':6841,'C2':false" + //
				",'C3':9046,'C4':4441},'K':{'C0':false,'C1':true,'C2':92,'C3':1289,'C4':true}" + //
				",'L':{'C0':'7ea117a4bc58cb5b','C1':false,'C2':'6df92317617f5a85','C3':4780" + //
				",'C4':8094},'M':{'C0':5935,'C1':'4334c82015bdf347','C2':'616e0ed7d1075d01'" + //
				",'C3':'70a67e09536c6571','C4':4211},'N':{'C0':3758,'C1':false,'C2':false" + //
				",'C3':false,'C4':394},'O':{'C0':9791,'C1':5388,'C2':'2a1299b319525b67'" + //
				",'C3':true,'C4':9567},'P':{'C0':false,'C1':8440,'C2':true,'C3':true,'C4':false}" + //
				",'Q':{'C0':'5096a4f49f4a84e9','C1':'2aff4e9fa5f366cf','C2':false,'C3':'7691e9f69ed47494'" + //
				",'C4':false},'R':{'C0':false,'C1':'2315899bd654c433','C2':false,'C3':true" + //
				",'C4':'37882f7d965389bb'},'S':{'C0':'6872fcd2186ba5e6','C1':'1e8ca141e16ace62'" + //
				",'C2':false,'C3':'5ce05daee7082534','C4':1595},'T':{'C0':true,'C1':false" + //
				",'C2':'df5f02910c7bcf4','C3':false,'C4':true},'U':{'C0':false,'C1':'326fe97d73b0dfc8'" + //
				",'C2':true,'C3':false,'C4':false},'V':{'C0':false,'C1':2675,'C2':'2d5d53e163150d47'" + //
				",'C3':'6967a436c1eee17e','C4':'7eea4fc5f91305f7'},'W':{'C0':236,'C1':true" + //
				",'C2':true,'C3':false,'C4':'7562114dca70df17'},'X':{'C0':9506,'C1':'6a33de88f342aca8'" + //
				",'C2':false,'C3':5162,'C4':'3043b7669b44cb36'},'Y':{'C0':8031,'C1':'65c1e7738d777292'" + //
				",'C2':'18b3b84080837d79','C3':3335,'C4':'76355ed154e9d563'}}";

		String key = "abc.def:OMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|OMT+bV509" + //
				"T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys2 = null;
		check(json, key, keys2, 90);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|OMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys3 = null;
		check(json, key, keys3, 109);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3OMT+bV509T6Xj2K+rGoy" + //
				"TMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys4 = null;
		check(json, key, keys4, 149);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dOMT+b" + //
				"V509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys5 = null;
		check(json, key, keys5, 164);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174OMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys6 = null;
		check(json, key, keys6, 177);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|OMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys7 = null;
		check(json, key, keys7, 182);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|5OMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06x" + //
				"fh7eI=";
		String[] keys8 = null;
		check(json, key, keys8, 201);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196dOMT+bV509T6Xj2K+rGoyTMi2+" + //
				"cFTMBVPEgX06xfh7eI=";
		String[] keys9 = null;
		check(json, key, keys9, 214);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eOMT+bV509T6Xj2K" + //
				"+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys10 = null;
		check(json, key, keys10, 224);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4febaOMT+bV50" + //
				"9T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys11 = null;
		check(json, key, keys11, 231);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4feba164d4|C2" + //
				"|8059|C3|false|C4|OMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys12 = null;
		check(json, key, keys12, 257);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4feba164d4|C2" + //
				"|8059|C3|false|C4|341|E|C0|5OMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7e" + //
				"I=";
		String[] keys13 = null;
		check(json, key, keys13, 267);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4feba164d4|C2" + //
				"|8059|C3|false|C4|341|E|C0|5a3de6dda27bab47|C1|false|C2|2f21dcbf45193a" + //
				"cOMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys14 = null;
		check(json, key, keys14, 310);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4feba164d4|C2" + //
				"|8059|C3|false|C4|341|E|C0|5a3de6dda27bab47|C1|false|C2|2f21dcbf45193a" + //
				"c1|C3|7db49b715aa765c4|COMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys15 = null;
		check(json, key, keys15, 333);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4feba164d4|C2" + //
				"|8059|C3|false|C4|341|E|C0|5a3de6dda27bab47|C1|false|C2|2f21dcbf45193a" + //
				"c1|C3|7db49b715aa765c4|C4|25f963dd94132d9e|F|C0|falsOMT+bV509T6Xj2K+rG" + //
				"oyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys16 = null;
		check(json, key, keys16, 361);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4feba164d4|C2" + //
				"|8059|C3|false|C4|341|E|C0|5a3de6dda27bab47|C1|false|C2|2f21dcbf45193a" + //
				"c1|C3|7db49b715aa765c4|C4|25f963dd94132d9e|F|C0|false|C1|true|C2|4ef21" + //
				"0a4c5f3fb0|COMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys17 = null;
		check(json, key, keys17, 391);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4feba164d4|C2" + //
				"|8059|C3|false|C4|341|E|C0|5a3de6dda27bab47|C1|false|C2|2f21dcbf45193a" + //
				"c1|C3|7db49b715aa765c4|C4|25f963dd94132d9e|F|C0|false|C1|true|C2|4ef21" + //
				"0a4c5f3fb0|C3|1174|C4|5OMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys18 = null;
		check(json, key, keys18, 402);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4feba164d4|C2" + //
				"|8059|C3|false|C4|341|E|C0|5a3de6dda27bab47|C1|false|C2|2f21dcbf45193a" + //
				"c1|C3|7db49b715aa765c4|C4|25f963dd94132d9e|F|C0|false|C1|true|C2|4ef21" + //
				"0a4c5f3fb0|C3|1174|C4|53ccceb49b5063a7|G|C0|true|C1|false|C2|true|C3|f" + //
				"alOMT+bV509T6Xj2K+rGoyTMi2+cFTMBVPEgX06xfh7eI=";
		String[] keys19 = null;
		check(json, key, keys19, 451);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|6726|C4|false|B|C0|fals" + //
				"e|C1|true|C2|true|C3|7ef768081f6cc0|C4|5969|C|C0|3504|C1|488|C2|dc6aeb" + //
				"550ae174d|C3|true|C4|6049|D|C0|58798ebc68196d8e|C1|704eaf4feba164d4|C2" + //
				"|8059|C3|false|C4|341|E|C0|5a3de6dda27bab47|C1|false|C2|2f21dcbf45193a" + //
				"c1|C3|7db49b715aa765c4|C4|25f963dd94132d9e|F|C0|false|C1|true|C2|4ef21" + //
				"0a4c5f3fb0|C3|1174|C4|53ccceb49b5063a7|G|C0|true|C1|false|C2|true|C3|f" + //
				"alse|C4|3419|H|C0|6994|C1|1363|C2|6148|C3|falsOMT+bV509T6Xj2K+rGoyTMi2" + //
				"+cFTMBVPEgX06xfh7eI=";
		String[] keys20 = null;
		check(json, key, keys20, 495);

	}

	@Test
	public void testF() throws Exception {

		String json = "{'A':{'C0':true,'C1':true,'C2':false,'C3':1688,'C4':false}" + //
				",'B':{'C0':false,'C1':'37777fefc162210d','C2':'301c4bd0dadd4583','C3':false" + //
				",'C4':9394},'C':{'C0':1358,'C1':'6733035f998d117b','C2':true,'C3':false" + //
				",'C4':true},'D':{'C0':false,'C1':true,'C2':false,'C3':2804,'C4':false}" + //
				",'E':{'C0':false,'C1':7540,'C2':true,'C3':true,'C4':false},'F':{'C0':6192" + //
				",'C1':9038,'C2':false,'C3':'5b35f661870b991e','C4':6466},'G':{'C0':true" + //
				",'C1':true,'C2':false,'C3':true,'C4':true},'H':{'C0':5496,'C1':true,'C2':false" + //
				",'C3':false,'C4':false},'I':{'C0':true,'C1':'399a0c9321521b62','C2':'3954f3b5dc41771b'" + //
				",'C3':false,'C4':false},'J':{'C0':'5dc43861f59e5600','C1':'134b35d48f2894af'" + //
				",'C2':true,'C3':false,'C4':'6982af3ee17bd14a'},'K':{'C0':'594f6487db13fc7e'" + //
				",'C1':true,'C2':false,'C3':'54f0e1df6f15a3ee','C4':'69dbda0c1f9f5072'}" + //
				",'L':{'C0':false,'C1':3892,'C2':true,'C3':'42251a357e48c9b9','C4':true}" + //
				",'M':{'C0':5813,'C1':false,'C2':5200,'C3':'205fe60a9bd2c0b7','C4':3577}" + //
				",'N':{'C0':8623,'C1':'55aa105000b132d8','C2':false,'C3':7372,'C4':true}" + //
				",'O':{'C0':'7104db6cbe69e4a8','C1':true,'C2':'4082d52d51fcd801','C3':9930" + //
				",'C4':1476},'P':{'C0':'3be4a813a2090800','C1':5510,'C2':false,'C3':'5428a236f4841944'" + //
				",'C4':false},'Q':{'C0':1216,'C1':true,'C2':true,'C3':'322c978bb591c2a9'" + //
				",'C4':true},'R':{'C0':'652195894ff01095','C1':'1c6d5efa9d062e7f','C2':'23fc806e97eabd64'" + //
				",'C3':true,'C4':'5a0b8810a7f3371b'},'S':{'C0':3430,'C1':true,'C2':true" + //
				",'C3':'703fe047b3e8c108','C4':true},'T':{'C0':'278ee70f3c7e6cdb','C1':true" + //
				",'C2':'9796fdf70182f73','C3':true,'C4':'6a66085729554623'},'U':{'C0':true" + //
				",'C1':true,'C2':false,'C3':4712,'C4':'73024756f475d128'},'V':{'C0':false" + //
				",'C1':3325,'C2':'42202bc94928d1c8','C3':false,'C4':true},'W':{'C0':false" + //
				",'C1':true,'C2':false,'C3':true,'C4':true},'X':{'C0':true,'C1':true,'C2':false" + //
				",'C3':3657,'C4':'193761e9d3252ca9'},'Y':{'C0':2881,'C1':true,'C2':'51a7c9e1c179ccd6'" + //
				",'C3':8019,'C4':false}}";

		String key = "abc.def:LDRjjI1NeSTWJlRvIyMdw/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|faLDRjjI1NeSTWJlR" + //
				"vIyMdw/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys2 = null;
		check(json, key, keys2, 84);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|falsLD" + //
				"RjjI1NeSTWJlRvIyMdw/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys3 = null;
		check(json, key, keys3, 97);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|fLDRjjI1NeSTWJlRvIyMdw/SZLZ" + //
				"h4BulmMWDiI8m2/D0=";
		String[] keys4 = null;
		check(json, key, keys4, 143);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733LDRjjI1NeSTWJlRvIyMdw/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys5 = null;
		check(json, key, keys5, 173);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733035f998d117b|C2|true|C3|false|C4|true|D|LDRjjI1NeSTWJlRvIyMdw/SZLZ" + //
				"h4BulmMWDiI8m2/D0=";
		String[] keys6 = null;
		check(json, key, keys6, 213);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733035f998d117b|C2|true|C3|false|C4|true|D|C0|false|C1|true|C2|falseL" + //
				"DRjjI1NeSTWJlRvIyMdw/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys7 = null;
		check(json, key, keys7, 238);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733035f998d117b|C2|true|C3|false|C4|true|D|C0|false|C1|true|C2|false|" + //
				"C3|2804|LDRjjI1NeSTWJlRvIyMdw/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys8 = null;
		check(json, key, keys8, 247);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733035f998d117b|C2|true|C3|false|C4|true|D|C0|false|C1|true|C2|false|" + //
				"C3|2804|C4|false|E|C0|false|C1|7540|C2|true|C3|trLDRjjI1NeSTWJlRvIyMdw" + //
				"/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys9 = null;
		check(json, key, keys9, 288);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733035f998d117b|C2|true|C3|false|C4|true|D|C0|false|C1|true|C2|false|" + //
				"C3|2804|C4|false|E|C0|false|C1|7540|C2|true|C3|true|C4|false|F|C0|6192" + //
				"|C1|9038|C2|false|C3|5b35f661LDRjjI1NeSTWJlRvIyMdw/SZLZh4BulmMWDiI8m2/" + //
				"D0=";
		String[] keys10 = null;
		check(json, key, keys10, 338);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733035f998d117b|C2|true|C3|false|C4|true|D|C0|false|C1|true|C2|false|" + //
				"C3|2804|C4|false|E|C0|false|C1|7540|C2|true|C3|true|C4|false|F|C0|6192" + //
				"|C1|9038|C2|false|C3|5b35f661870b991e|C4|6466|G|C0|true|C1|true|C2|fal" + //
				"se|C3LDRjjI1NeSTWJlRvIyMdw/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys11 = null;
		check(json, key, keys11, 384);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733035f998d117b|C2|true|C3|false|C4|true|D|C0|false|C1|true|C2|false|" + //
				"C3|2804|C4|false|E|C0|false|C1|7540|C2|true|C3|true|C4|false|F|C0|6192" + //
				"|C1|9038|C2|false|C3|5b35f661870b991e|C4|6466|G|C0|true|C1|true|C2|fal" + //
				"se|C3|true|C4|true|H|C0|5496|C1|true|C2|faLDRjjI1NeSTWJlRvIyMdw/SZLZh4" + //
				"BulmMWDiI8m2/D0=";
		String[] keys12 = null;
		check(json, key, keys12, 421);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733035f998d117b|C2|true|C3|false|C4|true|D|C0|false|C1|true|C2|false|" + //
				"C3|2804|C4|false|E|C0|false|C1|7540|C2|true|C3|true|C4|false|F|C0|6192" + //
				"|C1|9038|C2|false|C3|5b35f661870b991e|C4|6466|G|C0|true|C1|true|C2|fal" + //
				"se|C3|true|C4|true|H|C0|5496|C1|true|C2|false|C3|false|C4|false|I|C0|t" + //
				"rue|CLDRjjI1NeSTWJlRvIyMdw/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys13 = null;
		check(json, key, keys13, 454);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|1688|C4|false|B|C0|false|" + //
				"C1|37777fefc162210d|C2|301c4bd0dadd4583|C3|false|C4|9394|C|C0|1358|C1|" + //
				"6733035f998d117b|C2|true|C3|false|C4|true|D|C0|false|C1|true|C2|false|" + //
				"C3|2804|C4|false|E|C0|false|C1|7540|C2|true|C3|true|C4|false|F|C0|6192" + //
				"|C1|9038|C2|false|C3|5b35f661870b991e|C4|6466|G|C0|true|C1|true|C2|fal" + //
				"se|C3|true|C4|true|H|C0|5496|C1|true|C2|false|C3|false|C4|false|I|C0|t" + //
				"rue|C1|399a0c9321521b62|CLDRjjI1NeSTWJlRvIyMdw/SZLZh4BulmMWDiI8m2/D0=";
		String[] keys14 = null;
		check(json, key, keys14, 474);

	}

	@Test
	public void testG() throws Exception {

		String json = "{'A':false,'B':1793,'C':false,'D':1754,'E':false,'F':'4834136097b0dd00'" + //
				",'G':'67445fd42130a28c','H':true,'I':true,'J':true,'K':'96131ff89bcc5ce'" + //
				",'L':false,'M':true,'N':true,'O':false,'P':true,'Q':true,'R':false,'S':false" + //
				",'T':false,'U':true,'V':'26521290291edaf6','W':'783bb3ac0e559213','X':false" + //
				",'Y':4417}";

		String key = "abc.def:4TA2UBrG/X9ZfAWLHfh7Kie/Hh8rerS8Q/CLpUW/hX0=";
		String[] keys1 = { "K", "R", "I", "W", "N", "G", "D", "P", "B", "O", "H", "Y", "E", "M", "Q", "C", "U", "F",
				"V", "A", "J", "S", "L" };
		check(json, key, keys1, 44);

		key = "abc.def:false";
		String[] keys2 = { "E" };
		check(json, key, keys2, 76);

		key = "abc.def:true|false|true|4417|false|4834136097b0dd00|truVOlp0CL8" + //
				"Ck8MikT5mdo7AGc6wcQzSAOtrgRmY8n9AGo=";
		String[] keys3 = { "M", "T", "P", "Y", "O", "F", "I", "R", "U", "K", "W" };
		check(json, key, keys3, 91);

		key = "abc.def:true|true|783bb3ac0e559213|true|false|26521290291edaf6|" + //
				"false|4834136ZhalvrO7SrlWbVGPYJFhZXioM73Dgh1uuzjJoSb9010=";
		String[] keys4 = { "J", "Q", "W", "H", "O", "V", "T", "F", "K", "A", "I", "C", "U", "X", "B", "N", "G", "S",
				"L", "M", "R", "D" };
		check(json, key, keys4, 112);

		key = "abc.def:true|true|1754|false|false";
		String[] keys5 = { "N", "P", "D", "X", "S" };
		check(json, key, keys5, 120);

		key = "abc.def:true|true|1754|false|false|false";
		String[] keys6 = { "U", "M", "D", "O", "T", "S" };
		check(json, key, keys6, 154);

		key = "abc.def:26521290291edaf6|true|true|1754|4417|true|true|true|fal" + //
				"se|96131ff89bcc5ce|false|false|false|4834136097b0dd00|false|false|6744" + //
				"5fd42130a28c|1793";
		String[] keys7 = { "V", "P", "U", "D", "Y", "J", "H", "I", "L", "K", "A", "T", "E", "F", "S", "O", "G", "B" };
		check(json, key, keys7, 205);

		key = "abc.def:true|false";
		String[] keys8 = { "Q", "R" };
		check(json, key, keys8, 254);

		key = "abc.def:false|false|true|true|1793|true|26521290291edaf6";
		String[] keys9 = { "O", "X", "I", "Q", "B", "H", "V" };
		check(json, key, keys9, 302);

		key = "abc.def:26521290291edaf6|true|true|false|1754|true|false|67445f" + //
				"d42130a28c|4834136097b0dd00|false|false|4417|true|true|true|true|false" + //
				"";
		String[] keys10 = { "V", "P", "J", "O", "D", "M", "E", "G", "F", "X", "S", "Y", "I", "H", "U", "N", "T" };
		check(json, key, keys10, 305);

		key = "abc.def:false|false|96131ff89bcc5ce|1754|true|26521290291edaf6|" + //
				"1793|false|4834136097b0dd00|false|true|false|true|true|true|false|true" + //
				"|783bb3ac0e559213|true|true|false";
		String[] keys11 = { "O", "C", "K", "D", "H", "V", "B", "S", "F", "A", "J", "X", "P", "M", "U", "E", "Q", "W",
				"N", "I", "T" };
		check(json, key, keys11, 313);

		key = "abc.def:false|true|true|4417|true|1754|false|true|783bb3ac0e559" + //
				"213|false|true|false|false|67445fd42130a28c";
		String[] keys12 = { "R", "J", "P", "Y", "N", "D", "E", "U", "W", "X", "I", "T", "O", "G" };
		check(json, key, keys12, 320);

		key = "abc.def:false|true|false|true|false|false|1793|true|false|48341" + //
				"36097b0dd00|1754|26521290291edaf6|false|67445fd42130a28c|false|false|9" + //
				"6131ff89bcc5ce|true|true|true|true|true|false";
		String[] keys13 = { "R", "N", "C", "Q", "E", "O", "B", "I", "X", "F", "D", "V", "T", "G", "S", "L", "K", "P",
				"H", "U", "M", "J", "A" };
		check(json, key, keys13, 345);

		key = "abc.def:true|783bb3ac0e559213|false|false|67445fd42130a28c|true" + //
				"|false";
		String[] keys14 = { "J", "W", "X", "L", "G", "U", "A" };
		check(json, key, keys14, 364);

		key = "abc.def:96131ff89bcc5ce|false|false|false|1793|true|true|false|" + //
				"783bb3ac0e559213|true|4417|true|false";
		String[] keys15 = { "K", "L", "R", "X", "B", "M", "U", "A", "W", "I", "Y", "Q", "O" };
		check(json, key, keys15, 413);

		key = "abc.def:false";
		String[] keys16 = { "C" };
		check(json, key, keys16, 444);

		key = "abc.def:1793|false|26521290291edaf6|true|false|true|true|true|f" + //
				"alse|false|67445fd42130a28c";
		String[] keys17 = { "B", "R", "V", "M", "C", "N", "I", "J", "L", "E", "G" };
		check(json, key, keys17, 454);

		key = "abc.def:true|4834136097b0dd00|false|true|true|1754|4417|false|f" + //
				"alse|true";
		String[] keys18 = { "N", "F", "S", "I", "M", "D", "Y", "L", "O", "U" };
		check(json, key, keys18, 469);

	}

	@Test
	public void testH() throws Exception {

		String json = "{'A':6369,'B':4377,'C':true,'D':'660ee982f8521f58','E':true" + //
				",'F':false,'G':4443,'H':'b97658f692b91e2','I':false,'J':3414,'K':true,'L':'7a2a299beffa8ce0'" + //
				",'M':true,'N':'7e998050eaa28364','O':1368,'P':true,'Q':8770,'R':false,'S':'638739503744d623'" + //
				",'T':'415c8a93a7f6778b','U':true,'V':true,'W':'2ffd79a2f100a457','X':false" + //
				",'Y':9011}";

		String key = "abc.def:638739503744d623|7e998050eaa28364";
		String[] keys1 = { "S", "N" };
		check(json, key, keys1, 44);

		key = "abc.def:b97658f692b91e0D7RVyIcZT9qwNhNKALs6zOKqbx4f4Uusmbova/PW" + //
				"hM=";
		String[] keys2 = { "H", "J", "Y", "Q", "N", "E", "L", "V", "A", "O", "C", "K", "D", "M" };
		check(json, key, keys2, 58);

		key = "abc.def:true|7e998050eaa28364|638739503744d623";
		String[] keys3 = { "K", "N", "S" };
		check(json, key, keys3, 62);

		key = "abc.def:8770|2ffd79a2f100a457|415c8a93a7f6778b|sREtgacLf7EUbkaT" + //
				"kTj+Rx9JMLVTiBVo8g33uIYtIxQ=";
		String[] keys4 = { "Q", "W", "T", "U", "I", "P", "C", "D", "Y", "M", "R", "V", "K" };
		check(json, key, keys4, 83);

		key = "abc.def:true|1368|false|415c8a93a7f6778b|true|7a2a299beffa8ce0|" + //
				"7e998050eaa28364";
		String[] keys5 = { "U", "O", "X", "T", "K", "L", "N" };
		check(json, key, keys5, 96);

		key = "abc.def:false|4377|false|8770|true|false|false|true|660ee982f85" + //
				"21f58|true|true|7a2a29R0l6jT1m4gvaH6psYxobOZZchfTx8l8l06djqCBM/L0=";
		String[] keys6 = { "X", "B", "F", "Q", "V", "R", "I", "M", "D", "E", "K", "L", "H", "Y", "U", "O", "N", "C",
				"J", "T" };
		check(json, key, keys6, 121);

		key = "abc.def:660ee982f8521f58|415c8a93a7f6778b|7a2a299beffa8ce0|true" + //
				"|8770|6369|9011|638739503744d623|1368|false|false|true|false|true|fals" + //
				"e|4377";
		String[] keys7 = { "D", "T", "L", "U", "Q", "A", "Y", "S", "O", "I", "R", "V", "F", "C", "X", "B" };
		check(json, key, keys7, 158);

		key = "abc.def:true|false|true|2ffd79a2f100a457|660ee982f8521f58|true|" + //
				"9011|6369|true|7e998050eaa28364|1368|415c8a93a7f6778b|8770|true|false|" + //
				"7a2a299beffa8ce0|true|3414|b97658f692b91e2";
		String[] keys8 = { "K", "I", "P", "W", "D", "E", "Y", "A", "V", "N", "O", "T", "Q", "C", "X", "L", "U", "J",
				"H" };
		check(json, key, keys8, 189);

		key = "abc.def:7a2a299beffa8ce0|6369|false|true|4443|false|false";
		String[] keys9 = { "L", "A", "I", "P", "G", "X", "F" };
		check(json, key, keys9, 209);

		key = "abc.def:true|false|false|8770|4443|660ee982f8521f58|3414|true|7" + //
				"a2a299beffa8ce0|6369|1368|b97658f692b91e2|true|638739503744d623";
		String[] keys10 = { "M", "I", "F", "Q", "G", "D", "J", "E", "L", "A", "O", "H", "C", "S" };
		check(json, key, keys10, 252);

		key = "abc.def:2ffd79a2f100a457|true|false|true|7a2a299beffa8ce0|false" + //
				"|4377|7e998050eaa28364|true|4443|false|6369|true|true|3414|1368";
		String[] keys11 = { "W", "C", "R", "U", "L", "I", "B", "N", "M", "G", "F", "A", "V", "P", "J", "O" };
		check(json, key, keys11, 293);

		key = "abc.def:true|9011|8770|false|4443|4377|true|true|6369|false|tru" + //
				"e|b97658f692b91e2|638739503744d623|1368|2ffd79a2f100a457|415c8a93a7f67" + //
				"78b|660ee982f8521f58|true|false|7a2a299beffa8ce0|true|false";
		String[] keys12 = { "E", "Y", "Q", "R", "G", "B", "M", "C", "A", "I", "P", "H", "S", "O", "W", "T", "D", "V",
				"X", "L", "K", "F" };
		check(json, key, keys12, 310);

		key = "abc.def:660ee982f8521f58|true|3414|b97658f692b91e2|6369|4377|41" + //
				"5c8a93a7f6778b|9011|2ffd79a2f100a457|false|true|false|true";
		String[] keys13 = { "D", "M", "J", "H", "A", "B", "T", "Y", "W", "R", "U", "F", "E" };
		check(json, key, keys13, 335);

		key = "abc.def:7a2a299beffa8ce0|415c8a93a7f6778b|9011|true|true|true|6" + //
				"60ee982f8521f58|false|4377|7e998050eaa28364|4443|1368|true|true|8770";
		String[] keys14 = { "L", "T", "Y", "C", "K", "U", "D", "R", "B", "N", "G", "O", "P", "E", "Q" };
		check(json, key, keys14, 357);

		key = "abc.def:true|b97658f692b91e2|false|415c8a93a7f6778b|true|4443|4" + //
				"377|true|8770|660ee982f8521f58|638739503744d623";
		String[] keys15 = { "U", "H", "X", "T", "P", "G", "B", "C", "Q", "D", "S" };
		check(json, key, keys15, 377);

		key = "abc.def:8770|638739503744d623|false|b97658f692b91e2|false|false" + //
				"|true|4377|7e998050eaa28364|true|true|3414|415c8a93a7f6778b|false|2ffd" + //
				"79a2f100a457|true|1368|660ee982f8521f58|true|4443|9011|true|7a2a299bef" + //
				"fa8ce0|6369|true";
		String[] keys16 = { "Q", "S", "F", "H", "R", "I", "U", "B", "N", "P", "K", "J", "T", "X", "W", "C", "O", "D",
				"E", "G", "Y", "V", "L", "A", "M" };
		check(json, key, keys16, 380);

		key = "abc.def:true|638739503744d623|true|false|true|415c8a93a7f6778b|" + //
				"3414|9011|b97658f692b91e2|false|true|true|6369|4443|true|1368|4377|7a2" + //
				"a299beffa8ce0|false";
		String[] keys17 = { "U", "S", "C", "R", "P", "T", "J", "Y", "H", "X", "V", "M", "A", "G", "K", "O", "B", "L",
				"I" };
		check(json, key, keys17, 405);

		key = "abc.def:true|true|b97658f692b91e2|6369|true|false|true|1368|fal" + //
				"se|7e998050eaa28364|true|7a2a299beffa8ce0|false|8770|660ee982f8521f58|" + //
				"3414|9011|false|4377|638739503744d623|2ffd79a2f100a457|4443";
		String[] keys18 = { "V", "U", "H", "A", "E", "R", "C", "O", "F", "N", "K", "L", "I", "Q", "D", "J", "Y", "X",
				"B", "S", "W", "G" };
		check(json, key, keys18, 428);

		key = "abc.def:7e998050eaa28364|638739503744d623|2ffd79a2f100a457|4443" + //
				"|true|8770|7a2a299beffa8ce0|true|false|6369|false|1368|660ee982f8521f5" + //
				"8|false|false|b97658f692b91e2|4377|true|true|true";
		String[] keys19 = { "N", "S", "W", "G", "V", "Q", "L", "C", "I", "A", "F", "O", "D", "X", "R", "H", "B", "K",
				"M", "U" };
		check(json, key, keys19, 471);

		key = "abc.def:true|true|true|false|9011|true|660ee982f8521f58|b97658f" + //
				"692b91e2|4443|7a2a299beffa8ce0|true|4377|638739503744d623|8770|true|41" + //
				"5c8a93a7f6778b|7e998050eaa28364|1368|true";
		String[] keys20 = { "M", "E", "P", "F", "Y", "C", "D", "H", "G", "L", "U", "B", "S", "Q", "V", "T", "N", "O",
				"K" };
		check(json, key, keys20, 489);

	}

	@Test
	public void testI() throws Exception {

		String json = "{'A':'394c5813e5019356','B':2918,'C':true,'D':8133,'E':false" + //
				",'F':true,'G':4478,'H':false,'I':'558c927cde104d90','J':'11d8bf14141c7f1b'" + //
				",'K':5819,'L':true,'M':3054,'N':false,'O':true,'P':'742347f7b18133a0','Q':true" + //
				",'R':'7a1b45a04c3a1053','S':'6c4c34c7e3128c2f','T':false,'U':'6a350ff8441d427c'" + //
				",'V':false,'W':'556144b7070795f5','X':4695,'Y':7795}";

		String key = "abc.def:iEEfhmucm63GAxrGafWlfFZxhVH2IKGI497jmGGCiKk=";
		String[] keys1 = { "V", "C", "P", "M", "L", "H", "E", "F", "G", "O", "Q", "T", "D", "B", "J", "W", "N", "R",
				"I", "Y", "A" };
		check(json, key, keys1, 44);

		key = "abc.def:false4VwnMpJiRJdKNV+jC6qrj4U156WneC52FcKwJfPLgmU=";
		String[] keys2 = { "T", "C", "R", "E", "Q", "W", "M", "O", "S", "I", "G", "B", "D", "Y", "J", "K", "L", "N",
				"H", "V", "F", "X" };
		check(json, key, keys2, 49);

		key = "abc.def:false|4695";
		String[] keys3 = { "E", "X" };
		check(json, key, keys3, 90);

		key = "abc.def:true|false|394c5813e5019356|11d8bf14141c7f1b|556144b707" + //
				"0795f5|8133|true|NGY9lMEVc9hW234OfKV70R8lpZfxNjqovEM2ftmTYyI=";
		String[] keys4 = { "O", "V", "A", "J", "W", "D", "L", "Q", "S", "P", "B", "M", "N", "F", "H", "C", "E", "K",
				"Y", "X", "U", "I", "G" };
		check(json, key, keys4, 116);

		key = "abc.def:4478|5819|6c4c34c7e3128c2f|7795|7a1b45a04c3a1053|8133|t" + //
				"rue|false|6a350ff8441d427c|false|true|558c927cde104d90|394c5813e501935" + //
				"6";
		String[] keys5 = { "G", "K", "S", "Y", "R", "D", "F", "H", "U", "E", "Q", "I", "A" };
		check(json, key, keys5, 154);

		key = "abc.def:11d8bf14141c7f1b|false|558c927cde104d90|3054|742347f7b1" + //
				"8133a0|2918|false|4478|false|7795|7a1b45a04c3a1053|8133|true|6a350ff84" + //
				"41d427c|true|true|TkT6FccoUDT4luMUVMN1btAq6POUW+ndAHZRsHePBMk=";
		String[] keys6 = { "J", "T", "I", "M", "P", "B", "N", "G", "H", "Y", "R", "D", "O", "U", "L", "C", "A", "V",
				"E", "Q", "F", "K", "S" };
		check(json, key, keys6, 187);

		key = "abc.def:742347f7b18133a0|false|4695|true|3054|6c4c34c7e3128c2f|" + //
				"7a1b45a04c3a1053|true|false|4478|false|394c5813e5019356|2918|true|fals" + //
				"e|11d8bf14141c7f1b|true|556144b7070795f5|true|false|8133";
		String[] keys7 = { "P", "T", "X", "F", "M", "S", "R", "L", "H", "G", "E", "A", "B", "C", "V", "J", "O", "W",
				"Q", "N", "D" };
		check(json, key, keys7, 206);

		key = "abc.def:true|true|8133|false|11d8bf14141c7f1b|6c4c34c7e3128c2f|" + //
				"556144b7070795f5|false|6a350ff8441d427c|7a1b45a04c3a1053|false|742347f" + //
				"7b18133a0|5819|558c927cde104d90|3054";
		String[] keys8 = { "Q", "C", "D", "E", "J", "S", "W", "V", "U", "R", "N", "P", "K", "I", "M" };
		check(json, key, keys8, 226);

		key = "abc.def:6a350ff8441d427c|false|true|8133";
		String[] keys9 = { "U", "V", "C", "D" };
		check(json, key, keys9, 276);

		key = "abc.def:6c4c34c7e3128c2f|false|8133|2918|742347f7b18133a0|11d8b" + //
				"f14141c7f1b|false";
		String[] keys10 = { "S", "N", "D", "B", "P", "J", "T" };
		check(json, key, keys10, 299);

		key = "abc.def:6c4c34c7e3128c2f|false|false|742347f7b18133a0|3054|4478" + //
				"|2918|false|7795|true|true|false|558c927cde104d90|8133|true|6a350ff844" + //
				"1d427c|7a1b45a04c3a1053|false|556144b7070795f5|true|11d8bf14141c7f1b";
		String[] keys11 = { "S", "T", "V", "P", "M", "G", "B", "N", "Y", "O", "Q", "E", "I", "D", "C", "U", "R", "H",
				"W", "L", "J" };
		check(json, key, keys11, 306);

		key = "abc.def:false|6c4c34c7e3128c2f|false|true|7a1b45a04c3a1053|5819" + //
				"|false|false|false";
		String[] keys12 = { "V", "S", "N", "F", "R", "K", "T", "E", "H" };
		check(json, key, keys12, 334);

		key = "abc.def:false|true|4478|8133|11d8bf14141c7f1b";
		String[] keys13 = { "H", "C", "G", "D", "J" };
		check(json, key, keys13, 343);

		key = "abc.def:7795";
		String[] keys14 = { "Y" };
		check(json, key, keys14, 354);

		key = "abc.def:4695|394c5813e5019356|742347f7b18133a0|true|5819|556144" + //
				"b7070795f5|true|4478|true|6c4c34c7e3128c2f|7a1b45a04c3a1053|2918|false" + //
				"|3054|false|true|false|6a350ff8441d427c|558c927cde104d90|11d8bf14141c7" + //
				"f1b|false|7795";
		String[] keys15 = { "X", "A", "P", "L", "K", "W", "C", "G", "Q", "S", "R", "B", "H", "M", "V", "F", "E", "U",
				"I", "J", "T", "Y" };
		check(json, key, keys15, 402);

		key = "abc.def:true|11d8bf14141c7f1b|true|558c927cde104d90|7795|2918|4" + //
				"478|true|true|3054|false|394c5813e5019356|true|7a1b45a04c3a1053|6c4c34" + //
				"c7e3128c2f|false|742347f7b18133a0|4695|6a350ff8441d427c";
		String[] keys16 = { "F", "J", "O", "I", "Y", "B", "G", "L", "C", "M", "T", "A", "Q", "R", "S", "N", "P", "X",
				"U" };
		check(json, key, keys16, 434);

		key = "abc.def:394c5813e5019356";
		String[] keys17 = { "A" };
		check(json, key, keys17, 454);

		key = "abc.def:false|6c4c34c7e3128c2f|7795|394c5813e5019356|false|true" + //
				"";
		String[] keys18 = { "T", "S", "Y", "A", "E", "L" };
		check(json, key, keys18, 467);

	}

	@Test
	public void testJ() throws Exception {

		String json = "{'A':6669,'B':'56274a6d923c3b85','C':551,'D':false,'E':5467" + //
				",'F':false,'G':8904,'H':false,'I':true,'J':true,'K':false,'L':false,'M':'55b5c4ecb417b911'" + //
				",'N':9135,'O':'7c31e2d3121e877d','P':675,'Q':true,'R':true,'S':true,'T':false" + //
				",'U':true,'V':'22a1522ced5ea567','W':true,'X':6671,'Y':true}";

		String key = "abc.def:7rB9s4VgOp8Nf2WifmVqOP1wxOH7OwcL6LSS17/TK5E=";
		String[] keys1 = { "V", "K", "L", "H", "P", "E", "O", "J", "R", "B", "W", "M", "X", "G", "S", "C", "Y", "N",
				"A", "Q", "D", "I", "T", "F", "U" };
		check(json, key, keys1, 44);

		key = "abc.def:8904|56274a6d923c3b85|true|6669|false|55b5c4ecb417b911|" + //
				"675";
		String[] keys2 = { "G", "B", "R", "A", "L", "M", "P" };
		check(json, key, keys2, 65);

		key = "abc.def:false|true|true|true|8904|false|true|fals35nt1XFNsncemT" + //
				"6gFUXTNNxbm17vrAST4BwWkTtPzgE=";
		String[] keys3 = { "H", "I", "W", "S", "G", "L", "Q", "T", "C", "O", "B", "K", "D", "A", "P", "X", "E", "M",
				"R", "V" };
		check(json, key, keys3, 85);

		key = "abc.def:false|true";
		String[] keys4 = { "L", "Y" };
		check(json, key, keys4, 91);

		key = "abc.def:5467|551|true|6671|6669|false|675|true|9135|7c31e2d3121" + //
				"e877d|8904|false|22a1522ced5ea567|p0lfS+gCtLVUok6PhNWDuQ135CPrK3bWRBHZ" + //
				"50vDugU=";
		String[] keys5 = { "E", "C", "R", "X", "A", "D", "P", "S", "N", "O", "G", "T", "V", "B", "H", "Y", "I", "U",
				"W", "F", "J", "M", "K" };
		check(json, key, keys5, 133);

		key = "abc.def:false|false";
		String[] keys6 = { "L", "D" };
		check(json, key, keys6, 181);

		key = "abc.def:true|false|true|false|true";
		String[] keys7 = { "U", "F", "I", "D", "Q" };
		check(json, key, keys7, 199);

		key = "abc.def:6669|9135|55b5c4ecb417b911|false|true|22a1522ced5ea567";
		String[] keys8 = { "A", "N", "M", "H", "J", "V" };
		check(json, key, keys8, 216);

		key = "abc.def:false|false|false|false|true|true|true|true|8904|55b5c4" + //
				"ecb417b911|true|22a1522ced5ea567";
		String[] keys9 = { "H", "D", "L", "K", "I", "S", "Y", "J", "G", "M", "W", "V" };
		check(json, key, keys9, 243);

		key = "abc.def:6671|false|551|true|22a1522ced5ea567|true|false|56274a6" + //
				"d923c3b85|5467";
		String[] keys10 = { "X", "T", "C", "R", "V", "W", "K", "B", "E" };
		check(json, key, keys10, 285);

		key = "abc.def:675|true|56274a6d923c3b85|6669|true|false|true|true|546" + //
				"7|7c31e2d3121e877d|55b5c4ecb417b911|true|false|true|false|false|true|t" + //
				"rue|9135|6671";
		String[] keys11 = { "P", "Y", "B", "A", "R", "L", "Q", "U", "E", "O", "M", "W", "T", "J", "F", "K", "S", "I",
				"N", "X" };
		check(json, key, keys11, 288);

		key = "abc.def:true|false|false|9135";
		String[] keys12 = { "Q", "L", "H", "N" };
		check(json, key, keys12, 319);

		key = "abc.def:false|true|551|5467|true|22a1522ced5ea567|8904|true|7c3" + //
				"1e2d3121e877d|true|9135|false|false|false|675|true|true|false|true";
		String[] keys13 = { "K", "R", "C", "E", "J", "V", "G", "W", "O", "Y", "N", "D", "L", "T", "P", "S", "I", "F",
				"Q" };
		check(json, key, keys13, 334);

		key = "abc.def:675|false|true|false|7c31e2d3121e877d|55b5c4ecb417b911|" + //
				"true|false|22a1522ced5ea567|6669|5467|6671|9135|8904|true|true|false|f" + //
				"alse|true|false|551|true|true|true";
		String[] keys14 = { "P", "T", "I", "L", "O", "M", "Y", "D", "V", "A", "E", "X", "N", "G", "U", "S", "H", "F",
				"Q", "K", "C", "W", "J", "R" };
		check(json, key, keys14, 384);

		key = "abc.def:true|false|false|false|true|true|true|true|false|5467|6" + //
				"671|55b5c4ecb417b911|56274a6d923c3b85|9135|true|22a1522ced5ea567|7c31e" + //
				"2d3121e877d|8904|675|false";
		String[] keys15 = { "R", "D", "F", "T", "Q", "I", "U", "Y", "K", "E", "X", "M", "B", "N", "W", "V", "O", "G",
				"P", "L" };
		check(json, key, keys15, 427);

		key = "abc.def:true|true|true|true|false";
		String[] keys16 = { "J", "Y", "R", "S", "T" };
		check(json, key, keys16, 476);

		key = "abc.def:true|6671|false|6669|true|56274a6d923c3b85|false|true|2" + //
				"2a1522ced5ea567|8904|675";
		String[] keys17 = { "W", "X", "F", "A", "Q", "B", "H", "U", "V", "G", "P" };
		check(json, key, keys17, 482);

	}

	@Test
	public void testK() throws Exception {

		String json = "{'A':false,'B':'19a1887c6c432e8e','C':'6f94898e5d842ad'" + //
				",'D':false,'E':true,'F':false,'G':'385812a469985c44','H':'7bbeac05d586e4df'" + //
				",'I':true,'J':false,'K':'5c2e63610f72c483','L':'71b83f15453714b8','M':true" + //
				",'N':'71a321eeb6da971e','O':1067,'P':true,'Q':false,'R':'1cb6fe503d337af7'" + //
				",'S':false,'T':2752,'U':false,'V':false,'W':true,'X':false,'Y':'5c34e82f071689ae'" + //
				"}";

		String key = "abc.def:false|true|false|7bbeac05d586e4df";
		String[] keys1 = { "A", "E", "S", "H" };
		check(json, key, keys1, 44);

		key = "abc.def:19a1887c6c432e8e|5c34e82f071689ae|false|fRdghScYECbaUsQ" + //
				"1U8qr+ay8lRYd2a/4uhRBQ2yUEYFU=";
		String[] keys2 = { "B", "Y", "S", "J", "K", "I", "O", "L", "W", "C", "Q", "R", "N", "T", "V", "D", "F", "A",
				"E", "X", "H", "U", "P", "M", "G" };
		check(json, key, keys2, 85);

		key = "abc.def:2752|true|5c34e82f071689ae|19a1887c6c432e8e|7bbeac05d58" + //
				"2i6hEUoeKZ9WZD4eyorTDSaB/GyIbCwVjeNnFvWMenY=";
		String[] keys3 = { "T", "P", "Y", "B", "H", "M", "G", "A", "J", "K", "I", "N", "E", "Q", "V", "R", "O", "S",
				"U", "W", "D" };
		check(json, key, keys3, 99);

		key = "abc.def:19a1887c6c432e8e|false|1cb6fe503d337af7|385812a469985c4" + //
				"4|true|false|1067|false|5c34e82f071689ae|2752|falsoCUpqvUdxbSQefJM7XzT" + //
				"tYS2cYuNlGkqBRfNuGCSiiQ=";
		String[] keys4 = { "B", "V", "R", "G", "I", "F", "O", "D", "Y", "T", "J", "H", "Q", "C", "E", "P", "S", "X",
				"M" };
		check(json, key, keys4, 149);

		key = "abc.def:19a1887c6c432e8e|true|1067|false|false|true|true|false|" + //
				"true|false|true|385812a469985c44|1cb6fe503d337af7|5c34e82f071689ae|fal" + //
				"se|false|5c2e63610f72c483|71b83f15453714b8";
		String[] keys5 = { "B", "I", "O", "D", "A", "E", "P", "F", "M", "X", "W", "G", "R", "Y", "U", "V", "K", "L" };
		check(json, key, keys5, 191);

		key = "abc.def:true|false|2752|6f94898e5d842ad|false|false|385812a4699" + //
				"85c44|5c34e82f071689ae|true|true|false|5c2e63610f72c483|1067|1cb6fe503" + //
				"d337af7";
		String[] keys6 = { "P", "V", "T", "C", "J", "A", "G", "Y", "M", "E", "F", "K", "O", "R" };
		check(json, key, keys6, 194);

		key = "abc.def:1067|1cb6fe503d337af7|true|true|5c2e63610f72c483|5c34e8" + //
				"2f071689ae|71b83f15453714b8|false|false|true";
		String[] keys7 = { "O", "R", "W", "M", "K", "Y", "L", "F", "J", "P" };
		check(json, key, keys7, 210);

		key = "abc.def:1067";
		String[] keys8 = { "O" };
		check(json, key, keys8, 240);

		key = "abc.def:true|5c2e63610f72c483|6f94898e5d842ad|385812a469985c44|" + //
				"19a1887c6c432e8e";
		String[] keys9 = { "P", "K", "C", "G", "B" };
		check(json, key, keys9, 261);

		key = "abc.def:false|true|5c2e63610f72c483|false|5c34e82f071689ae|71a3" + //
				"21eeb6da971e|true|true|7bbeac05d586e4df|385812a469985c44|true|false|1c" + //
				"b6fe503d337af7|71b83f15453714b8|6f94898e5d842ad|1067|true|false|false|" + //
				"false|false|19a1887c6c432e8e|false|2752|false";
		String[] keys10 = { "D", "W", "K", "A", "Y", "N", "P", "E", "H", "G", "M", "V", "R", "L", "C", "O", "I", "S",
				"X", "F", "J", "B", "Q", "T", "U" };
		check(json, key, keys10, 310);

		key = "abc.def:71a321eeb6da971e|385812a469985c44|71b83f15453714b8|true" + //
				"|false|5c2e63610f72c483|true|false|2752";
		String[] keys11 = { "N", "G", "L", "I", "D", "K", "W", "S", "T" };
		check(json, key, keys11, 322);

		key = "abc.def:true|false|false|true|false|6f94898e5d842ad|5c2e63610f7" + //
				"2c483|5c34e82f071689ae|385812a469985c44|false|2752|71b83f15453714b8|fa" + //
				"lse|1067|true|71a321eeb6da971e|false|1cb6fe503d337af7";
		String[] keys12 = { "W", "D", "A", "E", "S", "C", "K", "Y", "G", "V", "T", "L", "U", "O", "I", "N", "J", "R" };
		check(json, key, keys12, 365);

		key = "abc.def:false|true|1067|71a321eeb6da971e|true|false|false|true|" + //
				"1cb6fe503d337af7|2752|false|5c2e63610f72c483|19a1887c6c432e8e|71b83f15" + //
				"453714b8|false|false|6f94898e5d842ad|false|true|false|7bbeac05d586e4df" + //
				"|false|5c34e82f071689ae";
		String[] keys13 = { "F", "E", "O", "N", "I", "V", "J", "P", "R", "T", "X", "K", "B", "L", "U", "S", "C", "Q",
				"W", "D", "H", "A", "Y" };
		check(json, key, keys13, 382);

		key = "abc.def:false|19a1887c6c432e8e|false";
		String[] keys14 = { "V", "B", "X" };
		check(json, key, keys14, 403);

		key = "abc.def:true|71a321eeb6da971e|2752|false|true|true";
		String[] keys15 = { "P", "N", "T", "U", "M", "E" };
		check(json, key, keys15, 438);

		key = "abc.def:true";
		String[] keys16 = { "W" };
		check(json, key, keys16, 486);

		key = "abc.def:false|5c34e82f071689ae|false|385812a469985c44|false|106" + //
				"7|true|5c2e63610f72c483|false|19a1887c6c432e8e|71a321eeb6da971e";
		String[] keys17 = { "A", "Y", "S", "G", "D", "O", "M", "K", "U", "B", "N" };
		check(json, key, keys17, 493);

	}

	@Test
	public void testL() throws Exception {

		String json = "{'A':9849,'B':true,'C':false,'D':true,'E':false,'F':true" + //
				",'G':'668757d614c10e5a','H':true,'I':6457,'J':'d688887ec87e33d','K':'f43f87afeb6c35e'" + //
				",'L':false,'M':false,'N':false,'O':false,'P':true,'Q':'5afb7aee389ec77c'" + //
				",'R':true,'S':8522,'T':'6fecddc902b3ab81','U':7334,'V':false,'W':'69b3ef17ede7a6c7'" + //
				",'X':1239,'Y':'73b96eed35c523'}";

		String key = "abc.def:Osx6lAD0QW87eRSaMIO+0xx7V14+nlRXKh3R/uiZak4=";
		String[] keys1 = { "V", "T", "X", "Y", "G", "K", "E", "B", "M", "A", "P", "S", "R", "F" };
		check(json, key, keys1, 44);

		key = "abc.def:false|false|false|false|5afb7aeed9prn6PBt1EwEMDHRPyuuQL" + //
				"GTz8bLV+y4EO+/PtBOWc=";
		String[] keys2 = { "O", "V", "E", "C", "Q", "P", "B", "S", "U", "F", "J", "T", "A", "I", "K" };
		check(json, key, keys2, 76);

		key = "abc.def:false|false|false|true|true|5afb7aee389ec77c|123LtNZG2z" + //
				"h41x/bnCnsAlYW46lv8fmDopRQT29GXDm0KU=";
		String[] keys3 = { "N", "V", "E", "H", "F", "Q", "X", "I", "W", "C", "R", "K", "T", "A", "M", "B", "D", "P",
				"G" };
		check(json, key, keys3, 92);

		key = "abc.def:false|false|69b3ef17ede7a6c7|true|true|6fecddc902b3ab81" + //
				"|false|d688887ec87e33d|false|true|9849|7334";
		String[] keys4 = { "V", "O", "W", "D", "B", "T", "N", "J", "C", "H", "A", "U" };
		check(json, key, keys4, 141);

		key = "abc.def:9849|true|true|668757d614c10e5a|1239|false|6fecddc902b3" + //
				"ab81|6457|false|true|f43f87afeb6c35e|7334|5afb7aee389ec77c|true|d68888" + //
				"7ec87e33d|false|69b3ef17ede7a6c7|true|73b96eed35c523";
		String[] keys5 = { "A", "P", "R", "G", "X", "N", "T", "I", "E", "H", "K", "U", "Q", "F", "J", "L", "W", "D",
				"Y" };
		check(json, key, keys5, 192);

		key = "abc.def:true|true|8522|69b3ef17ede7a6c7|6457|f43f87afeb6c35e|tr" + //
				"ue|false|false|9849|false|false|6fecddc902b3ab81|false|668757d614c10e5" + //
				"a|d688887ec87e33d|73b96eed35c523|7334|1239|true|true|false";
		String[] keys6 = { "R", "F", "S", "W", "I", "K", "B", "E", "N", "A", "L", "V", "T", "O", "G", "J", "Y", "U",
				"X", "P", "H", "M" };
		check(json, key, keys6, 208);

		key = "abc.def:d688887ec87e33d|f43f87afeb6c35e|false|true|6457|true|6f" + //
				"ecddc902b3ab81|true|false|73b96eed35c523";
		String[] keys7 = { "J", "K", "E", "R", "I", "D", "T", "P", "V", "Y" };
		check(json, key, keys7, 259);

		key = "abc.def:1239|d688887ec87e33d|false|69b3ef17ede7a6c7|false|false" + //
				"|false|73b96eed35c523|true|true|5afb7aee389ec77c|false|true";
		String[] keys8 = { "X", "J", "L", "W", "C", "N", "M", "Y", "F", "H", "Q", "E", "P" };
		check(json, key, keys8, 275);

		key = "abc.def:f43f87afeb6c35e|6fecddc902b3ab81|9849|668757d614c10e5a|" + //
				"8522|true|false";
		String[] keys9 = { "K", "T", "A", "G", "S", "F", "V" };
		check(json, key, keys9, 277);

		key = "abc.def:6fecddc902b3ab81|f43f87afeb6c35e|d688887ec87e33d|true|f" + //
				"alse|true|69b3ef17ede7a6c7|1239|8522|668757d614c10e5a|73b96eed35c523|t" + //
				"rue|true|false|false|false";
		String[] keys10 = { "T", "K", "J", "F", "L", "B", "W", "X", "S", "G", "Y", "D", "P", "N", "M", "O" };
		check(json, key, keys10, 319);

		key = "abc.def:668757d614c10e5a|5afb7aee389ec77c|d688887ec87e33d|8522|" + //
				"false|6457|true|73b96eed35c523|true|true|false|true|false|false|f43f87" + //
				"afeb6c35e|6fecddc902b3ab81|9849|69b3ef17ede7a6c7|7334|true|true|false|" + //
				"1239";
		String[] keys11 = { "G", "Q", "J", "S", "C", "I", "F", "Y", "R", "D", "N", "H", "V", "E", "K", "T", "A", "W",
				"U", "P", "B", "L", "X" };
		check(json, key, keys11, 338);

		key = "abc.def:false|668757d614c10e5a|73b96eed35c523|true|6fecddc902b3" + //
				"ab81|true|7334|6457|5afb7aee389ec77c|69b3ef17ede7a6c7|8522|9849|true|f" + //
				"43f87afeb6c35e|false";
		String[] keys12 = { "N", "G", "Y", "H", "T", "P", "U", "I", "Q", "W", "S", "A", "D", "K", "L" };
		check(json, key, keys12, 371);

		key = "abc.def:false|f43f87afeb6c35e|9849|false|8522|true|true|668757d" + //
				"614c10e5a|73b96eed35c523|true|6457|true|5afb7aee389ec77c|false|69b3ef1" + //
				"7ede7a6c7|false|d688887ec87e33d|false|true|false|true|1239|false";
		String[] keys13 = { "V", "K", "A", "L", "S", "D", "R", "G", "Y", "F", "I", "P", "Q", "N", "W", "M", "J", "E",
				"B", "O", "H", "X", "C" };
		check(json, key, keys13, 410);

		key = "abc.def:9849|73b96eed35c523|false|false|true|d688887ec87e33d|66" + //
				"8757d614c10e5a|1239|true|false|6fecddc902b3ab81|8522|true|false|false|" + //
				"true|true|false|7334|6457|69b3ef17ede7a6c7|5afb7aee389ec77c|f43f87afeb" + //
				"6c35e|false";
		String[] keys14 = { "A", "Y", "L", "V", "R", "J", "G", "X", "F", "O", "T", "S", "D", "C", "N", "B", "P", "E",
				"U", "I", "W", "Q", "K", "M" };
		check(json, key, keys14, 456);

		key = "abc.def:false|false|6fecddc902b3ab81|8522|false|true|false|true" + //
				"|true|false|9849|true|6457|f43f87afeb6c35e";
		String[] keys15 = { "N", "V", "T", "S", "O", "F", "L", "H", "B", "C", "A", "P", "I", "K" };
		check(json, key, keys15, 479);

	}

	// --- COMMON KEY TESTER METHOD ---

	protected RedisCacher cacher = new RedisCacher();

	protected void check(String json, String key, String[] keys, int maxKeyLength) throws Exception {
		json = json.replace('\'', '\"');
		Tree params = new Tree(json, "JsonBuiltin");
		cacher.setMaxParamsLength(maxKeyLength);
		String testKey = cacher.getCacheKey("abc.def", params, keys);
		assertEquals(key, testKey);
	}

}