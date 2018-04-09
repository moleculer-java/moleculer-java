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
	public void testA1() throws Exception {

		String json = "{'A':{'C0':[0,4,6],'C1':true,'C2':3342,'C3':'5530af6f0cb29229'" + //
				",'C4':'643ded40b0da2745'},'B':{'C0':'5a75b699c41d9a75','C1':false,'C2':true" + //
				",'C3':'14e77e2edd0dcb98','C4':true},'C':{'C0':[5,9,7],'C1':true,'C2':[6" + //
				",9,2],'C3':false,'C4':[9,5,0]},'D':{'C0':true,'C1':883,'C2':false,'C3':5645" + //
				",'C4':2633},'E':{'C0':'4119cd276d9db0d1','C1':'50ed180e9583e17d','C2':true" + //
				",'C3':false,'C4':[8,2,9]},'F':{'C0':'42146325b8cbca02','C1':false,'C2':true" + //
				",'C3':5434,'C4':'55997c3e66920def'},'G':{'C0':false,'C1':false,'C2':[4" + //
				",6,3],'C3':'41782dd5a2348223','C4':true},'H':{'C0':2337,'C1':6906,'C2':false" + //
				",'C3':'40d74a450b623175','C4':true},'I':{'C0':true,'C1':[2,8,4],'C2':[1" + //
				",8,7],'C3':false,'C4':true},'J':{'C0':true,'C1':false,'C2':'4e96dbf3f282df0c'" + //
				",'C3':2548,'C4':'3aa6fb7043976492'},'K':{'C0':'394e2f2f68f510b7','C1':5776" + //
				",'C2':[1,0,1],'C3':'248693f7ff03ae','C4':true},'L':{'C0':8210,'C1':true" + //
				",'C2':false,'C3':true,'C4':true},'M':{'C0':[5,5,3],'C1':3579,'C2':2352" + //
				",'C3':true,'C4':false},'N':{'C0':[8,3,8],'C1':6946,'C2':true,'C3':true" + //
				",'C4':[8,3,5]},'O':{'C0':false,'C1':true,'C2':[3,4,5],'C3':false,'C4':7694}" + //
				",'P':{'C0':true,'C1':[9,5,3],'C2':false,'C3':'160aff0aa355691c','C4':'1e1ac668a9ce211c'}" + //
				",'Q':{'C0':'226ed51c1a3ab8ba','C1':[4,3,0],'C2':8623,'C3':true,'C4':'6d8d97befef92ca7'}" + //
				",'R':{'C0':true,'C1':'49fdbe5c79a9392e','C2':9540,'C3':[0,6,5],'C4':[9" + //
				",6,7]},'S':{'C0':'7de076f622f9c370','C1':[6,6,5],'C2':'5a341d690938b35b'" + //
				",'C3':false,'C4':true},'T':{'C0':7086,'C1':false,'C2':'18c0266dd067362d'" + //
				",'C3':[2,9,8],'C4':true},'U':{'C0':false,'C1':6132,'C2':false,'C3':false" + //
				",'C4':false},'V':{'C0':'33736e82b9b2a3f7','C1':false,'C2':8390,'C3':351" + //
				",'C4':'1f19281ac4924648'},'W':{'C0':[1,9,3],'C1':973,'C2':[5,7,2],'C3':true" + //
				",'C4':false},'X':{'C0':true,'C1':'4c311f7f595e47bb','C2':true,'C3':false" + //
				",'C4':true},'Y':{'C0':false,'C1':'542e39e1c1fc1691','C2':true,'C3':[3,5" + //
				",2],'C4':'1a8499f7dd494ff4'}}";

		String key = "leEVLQOr1sLXP4804uTspK48flzqbi4vFr1VUuBrf20=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|0|4|6|C1|true|CleEVLQOr1sLXP4804uTspK48flzqbi4vFr1" + //
				"VUuBrf20=";
		String[] keys2 = null;
		check(json, key, keys2, 72);

		key = "abc.def:A|C0|0|4|6|C1|true|C2|3342|C3|5530af6f0cb29229|C4|643de" + //
				"d40b0da2745|B|C0|5a75b699c41d9a75|C1|false|C2|true|C3|14e77e2edd0dcb98" + //
				"|C4|true|C|C0|5|9|7|C1|true|C2|6|9|2|C3|false|C4|9|5|0|D|C0|true|C1|88" + //
				"3|C2|false|C3|5645|C4|2633|E|C0|4119cd276d9db0d1|C1|50ed180e9583e17d|C" + //
				"2|true|C3|false|C4|8|2|9|F|C0|4214leEVLQOr1sLXP4804uTspK48flzqbi4vFr1V" + //
				"UuBrf20=";
		String[] keys3 = null;
		check(json, key, keys3, 351);

		key = "abc.def:A|C0|0|4|6|C1|true|C2|3342|C3|5530af6f0cb29229|C4|643de" + //
				"d40b0da2745|B|C0|5a75b699c41d9a75|C1|false|C2|true|C3|14e77e2edd0dcb98" + //
				"|C4|true|C|C0|5|9|7|C1|true|C2|6|9|2|C3|false|C4|9|5|0|D|C0|true|C1|88" + //
				"3|C2|false|C3|5645|C4|2633|E|C0|4119cd276d9db0d1|C1|50ed180e9583e17d|C" + //
				"2|true|C3|false|C4|8|2|9|F|C0|42146325b8cbca02|C1|false|C2|true|C3|543" + //
				"4|C4|55997c3e66920def|G|C0|false|C1|false|C2|4|6|3|C3|41782dd5a2348223" + //
				"|C4|true|H|C0|2337|C1|6906|C2|false|C3|40d74a450b623175|C4|true|I|C0|t" + //
				"rue|C1|2|8|4|C2|1|8|7|C3|false|C4|true|J|C0|true|C1|false|C2|4e96dbf3f" + //
				"282df0c|C3|2548|C4|3aa6fb7043976492|K|C0|394e2f2f68f510b7|C1|5776|C2|1" + //
				"|0|1|C3|248693f7ff03ae|C4|true|L|C0|8210|C1|true|C2|false|C3|true|C4|t" + //
				"rue|M|C0|5|5|3|C1|3579|C2|2352|C3|leEVLQOr1sLXP4804uTspK48flzqbi4vFr1V" + //
				"UuBrf20=";
		String[] keys4 = null;
		check(json, key, keys4, 771);

	}

	@Test
	public void testB1() throws Exception {

		String json = "{'A':{'C0':false,'C1':false,'C2':true,'C3':3346,'C4':false}" + //
				",'B':{'C0':'311d78a1e57ba02f','C1':false,'C2':9336,'C3':2584,'C4':[0,2" + //
				",2]},'C':{'C0':[1,8,2],'C1':8822,'C2':true,'C3':[3,2,2],'C4':true},'D':{'C0':[6" + //
				",2,1],'C1':7468,'C2':true,'C3':false,'C4':false},'E':{'C0':false,'C1':1918" + //
				",'C2':[1,9,4],'C3':true,'C4':true},'F':{'C0':4767,'C1':false,'C2':true" + //
				",'C3':[0,6,1],'C4':true},'G':{'C0':'5a85413d926dbb00','C1':'879fd6462bcbe6b'" + //
				",'C2':2184,'C3':true,'C4':false},'H':{'C0':false,'C1':4381,'C2':false,'C3':6328" + //
				",'C4':false},'I':{'C0':false,'C1':true,'C2':9881,'C3':false,'C4':true}" + //
				",'J':{'C0':522,'C1':false,'C2':true,'C3':[1,5,8],'C4':4471},'K':{'C0':'7a4c1b6958dee3bb'" + //
				",'C1':[7,6,1],'C2':false,'C3':true,'C4':true},'L':{'C0':'62c6cfedef549b2d'" + //
				",'C1':'22054644e3075112','C2':'3c5e9bf9caab9972','C3':true,'C4':false}" + //
				",'M':{'C0':'34bab0773a85f5d2','C1':true,'C2':false,'C3':[3,8,2],'C4':'34d4e92ae6a23153'}" + //
				",'N':{'C0':true,'C1':'34d9c370fef5f8ca','C2':8994,'C3':[4,7,2],'C4':6183}" + //
				",'O':{'C0':[2,0,8],'C1':false,'C2':8591,'C3':true,'C4':2740},'P':{'C0':[4" + //
				",2,6],'C1':true,'C2':[3,9,1],'C3':false,'C4':false},'Q':{'C0':[6,2,9],'C1':[7" + //
				",9,9],'C2':6396,'C3':8552,'C4':4498},'R':{'C0':true,'C1':false,'C2':'1a1c777fb6ffab11'" + //
				",'C3':'520cbb2303569428','C4':false},'S':{'C0':4438,'C1':false,'C2':'2a3e8ada4ac34e19'" + //
				",'C3':[9,6,2],'C4':false},'T':{'C0':[2,7,4],'C1':[4,0,2],'C2':true,'C3':'b295303fdd40fe4'" + //
				",'C4':[1,0,9]},'U':{'C0':2504,'C1':4102,'C2':false,'C3':true,'C4':2953}" + //
				",'V':{'C0':true,'C1':'34ef8657172fcd6a','C2':false,'C3':true,'C4':6250}" + //
				",'W':{'C0':[4,3,0],'C1':'3d2a6248b6bfb47','C2':true,'C3':[2,4,4],'C4':[1" + //
				",4,8]},'X':{'C0':4249,'C1':true,'C2':true,'C3':true,'C4':8886},'Y':{'C0':false" + //
				",'C1':[3,6,7],'C2':true,'C3':[5,7,9],'C4':7427}}";

		String key = "8KO3JBzr2trU7+bej0Dox2DjGHmaGMYoZmRxtvbjkEI=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|false|C2|true|C3|3346|C4|false|B|C0|311d7" + //
				"8a1e57ba02f|C1|false|C2|9336|C3|2584|C4|0|2|2|C|C0|1|8|2|C1|8822|C2|tr" + //
				"ue|C3|3|2|2|C4|true|D|C0|6|2|1|C1|7468|C2|true|C3|false|C4|false|E|C0|" + //
				"false|C1|1918|C2|1|9|4|C3|true|C4|true|F|C0|4767|C1|false|C2|true|C3|0" + //
				"|6|1|C4|true|G|C0|5a85413d926dbb00|C1|879fd6462bcbe6b|C2|2188KO3JBzr2t" + //
				"rU7+bej0Dox2DjGHmaGMYoZmRxtvbjkEI=";
		String[] keys2 = null;
		check(json, key, keys2, 377);

		key = "abc.def:A|C0|false|C1|false|C2|true|C3|3346|C4|false|B|C0|311d7" + //
				"8a1e57ba02f|C1|false|C2|9336|C3|2584|C4|0|2|2|C|C0|1|8|2|C1|8822|C2|tr" + //
				"ue|C3|3|2|2|C4|true|D|C0|6|2|1|C1|7468|C2|true|C3|false|C4|false|E|C0|" + //
				"false|C1|1918|C2|1|9|4|C3|true|C4|true|F|C0|4767|C1|false|C2|true|C3|0" + //
				"|6|1|C4|true|G|C0|5a85413d926dbb00|C1|879fd6462bcbe6b|C2|2184|C3|true|" + //
				"C4|false|H|C0|false|C1|4381|C2|false|C3|6328|C4|false|I|C0|false|C1|tr" + //
				"ue|C2|9881|C3|false|C4|true|J|C0|522|C1|false|C2|true|C3|1|5|8|C4|4471" + //
				"|K|C0|7a4c1b68KO3JBzr2trU7+bej0Dox2DjGHmaGMYoZmRxtvbjkEI=";
		String[] keys3 = null;
		check(json, key, keys3, 540);

		key = "abc.def:A|C0|false|C1|false|C2|true|C3|3346|C4|false|B|C0|311d7" + //
				"8a1e57ba02f|C1|false|C2|9336|C3|2584|C4|0|2|2|C|C0|1|8|2|C1|8822|C2|tr" + //
				"ue|C3|3|2|2|C4|true|D|C0|6|2|1|C1|7468|C2|true|C3|false|C4|false|E|C0|" + //
				"false|C1|1918|C2|1|9|4|C3|true|C4|true|F|C0|4767|C1|false|C2|true|C3|0" + //
				"|6|1|C4|true|G|C0|5a85413d926dbb00|C1|879fd6462bcbe6b|C2|2184|C3|true|" + //
				"C4|false|H|C0|false|C1|4381|C2|false|C3|6328|C4|false|I|C0|false|C1|tr" + //
				"ue|C2|9881|C3|false|C4|true|J|C0|522|C1|false|C2|true|C3|1|5|8|C4|4471" + //
				"|K|C0|7a4c1b6958dee3bb|C1|7|6|1|C2|false|C3|true|C4|true|L|C0|62c6cfed" + //
				"ef549b2d|C1|22054644e3075112|C2|3c5e9bf9c8KO3JBzr2trU7+bej0Dox2DjGHmaG" + //
				"MYoZmRxtvbjkEI=";
		String[] keys4 = null;
		check(json, key, keys4, 638);

		key = "abc.def:A|C0|false|C1|false|C2|true|C3|3346|C4|false|B|C0|311d7" + //
				"8a1e57ba02f|C1|false|C2|9336|C3|2584|C4|0|2|2|C|C0|1|8|2|C1|8822|C2|tr" + //
				"ue|C3|3|2|2|C4|true|D|C0|6|2|1|C1|7468|C2|true|C3|false|C4|false|E|C0|" + //
				"false|C1|1918|C2|1|9|4|C3|true|C4|true|F|C0|4767|C1|false|C2|true|C3|0" + //
				"|6|1|C4|true|G|C0|5a85413d926dbb00|C1|879fd6462bcbe6b|C2|2184|C3|true|" + //
				"C4|false|H|C0|false|C1|4381|C2|false|C3|6328|C4|false|I|C0|false|C1|tr" + //
				"ue|C2|9881|C3|false|C4|true|J|C0|522|C1|false|C2|true|C3|1|5|8|C4|4471" + //
				"|K|C0|7a4c1b6958dee3bb|C1|7|6|1|C2|false|C3|true|C4|true|L|C0|62c6cfed" + //
				"ef549b2d|C1|22054644e3075112|C2|3c5e9bf9caab9972|C3|true|C48KO3JBzr2tr" + //
				"U7+bej0Dox2DjGHmaGMYoZmRxtvbjkEI=";
		String[] keys5 = null;
		check(json, key, keys5, 656);

		key = "abc.def:A|C0|false|C1|false|C2|true|C3|3346|C4|false|B|C0|311d7" + //
				"8a1e57ba02f|C1|false|C2|9336|C3|2584|C4|0|2|2|C|C0|1|8|2|C1|8822|C2|tr" + //
				"ue|C3|3|2|2|C4|true|D|C0|6|2|1|C1|7468|C2|true|C3|false|C4|false|E|C0|" + //
				"false|C1|1918|C2|1|9|4|C3|true|C4|true|F|C0|4767|C1|false|C2|true|C3|0" + //
				"|6|1|C4|true|G|C0|5a85413d926dbb00|C1|879fd6462bcbe6b|C2|2184|C3|true|" + //
				"C4|false|H|C0|false|C1|4381|C2|false|C3|6328|C4|false|I|C0|false|C1|tr" + //
				"ue|C2|9881|C3|false|C4|true|J|C0|522|C1|false|C2|true|C3|1|5|8|C4|4471" + //
				"|K|C0|7a4c1b6958dee3bb|C1|7|6|1|C2|false|C3|true|C4|true|L|C0|62c6cfed" + //
				"ef549b2d|C1|22054644e3075112|C2|3c5e9bf9caab9972|C3|true|C4|false|M|C0" + //
				"|34bab0773a85f5d2|C1|true|C2|false|C3|3|8|2|C4|34d8KO3JBzr2trU7+bej0Do" + //
				"x2DjGHmaGMYoZmRxtvbjkEI=";
		String[] keys6 = null;
		check(json, key, keys6, 717);

	}

	@Test
	public void testC1() throws Exception {

		String json = "{'A':{'C0':false,'C1':true,'C2':true,'C3':true,'C4':true}" + //
				",'B':{'C0':'77a1f51db0983996','C1':false,'C2':[9,6,8],'C3':5567,'C4':true}" + //
				",'C':{'C0':true,'C1':819,'C2':false,'C3':3261,'C4':'4a8a58e7de16c2b8'}" + //
				",'D':{'C0':[2,7,5],'C1':true,'C2':'4a59477e424af521','C3':'6c81842bc8ef6cac'" + //
				",'C4':false},'E':{'C0':7460,'C1':724,'C2':true,'C3':true,'C4':'77905d9a17895657'}" + //
				",'F':{'C0':[9,4,8],'C1':3351,'C2':'1018e54007389b21','C3':'1e88daba2c321d73'" + //
				",'C4':'3f8b9bdc7b1c9c76'},'G':{'C0':'47f6c42759cc550b','C1':'2a0f86f0adc6a388'" + //
				",'C2':'16262bb0ecdf10bb','C3':[0,2,8],'C4':9637},'H':{'C0':'5b25b003fcda1e0a'" + //
				",'C1':true,'C2':6013,'C3':true,'C4':true},'I':{'C0':'4ff8f5d8fd886c7b'" + //
				",'C1':4256,'C2':true,'C3':true,'C4':false},'J':{'C0':'2f08018009189e23'" + //
				",'C1':false,'C2':5131,'C3':false,'C4':'339dda690c68984c'},'K':{'C0':2187" + //
				",'C1':'7588394a3ef57da6','C2':[8,7,9],'C3':true,'C4':'1c75cefe74bed74d'}" + //
				",'L':{'C0':545,'C1':false,'C2':[8,7,8],'C3':'7caa12ccf1b8ae1f','C4':true}" + //
				",'M':{'C0':false,'C1':false,'C2':true,'C3':'116d0b92855ba5e0','C4':false}" + //
				",'N':{'C0':true,'C1':true,'C2':true,'C3':'58b50433b116e1ed','C4':3439}" + //
				",'O':{'C0':'62dfbf45a4e2043a','C1':[2,9,2],'C2':false,'C3':[0,0,7],'C4':[0" + //
				",3,6]},'P':{'C0':961,'C1':false,'C2':[7,9,5],'C3':7819,'C4':8520},'Q':{'C0':false" + //
				",'C1':true,'C2':[0,4,7],'C3':6524,'C4':[4,6,3]},'R':{'C0':2641,'C1':'30fd36e2851f91c0'" + //
				",'C2':false,'C3':[7,6,2],'C4':'35c48af8d3278fc6'},'S':{'C0':true,'C1':616" + //
				",'C2':false,'C3':3948,'C4':'1d5d8bc22e72b82'},'T':{'C0':4918,'C1':true" + //
				",'C2':6716,'C3':true,'C4':true},'U':{'C0':1619,'C1':8262,'C2':true,'C3':7432" + //
				",'C4':544},'V':{'C0':[1,5,6],'C1':'62f4008618866b51','C2':'288d4c1efd189ad3'" + //
				",'C3':[6,9,0],'C4':true},'W':{'C0':[8,3,3],'C1':false,'C2':'4a03e34bb98ca58b'" + //
				",'C3':2457,'C4':'5505494475248b1b'},'X':{'C0':false,'C1':[5,2,8],'C2':[2" + //
				",3,6],'C3':3859,'C4':true},'Y':{'C0':true,'C1':false,'C2':4790,'C3':[9" + //
				",4,6],'C4':false}}";

		String key = "nqJ6ZG92ghIGQWgcaofmOAdHmBjujP2qjabEYPtIsLg=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|true|C4|true|B|C0|77a1f51" + //
				"db0983996|C1|false|C2|9|6|8|C3|5567|C4|true|C|C0|true|C1|819|C2|false|" + //
				"C3|3261|C4|4a8a58e7de16c2b8|D|C0|2|7|5|C1|true|C2|4a59477e424af521|C3|" + //
				"6c81842bc8ef6cac|C4|false|E|C0|7460|C1|724|C2|true|C3|true|C4|77905d9a" + //
				"17895657|F|C0|9|4|8|C1|3351|C2|1018e54007389b21|C3|1e88daba2c321d73|C4" + //
				"|3f8b9bdc7b1c9c76|G|C0|47f6c42759cc550b|C1|2a0f86f0adc6a388|C2|16262bb" + //
				"0ecdf10bb|C3|0|2|8|C4|9637|H|C0|5b25b003fcdnqJ6ZG92ghIGQWgcaofmOAdHmBj" + //
				"ujP2qjabEYPtIsLg=";
		String[] keys2 = null;
		check(json, key, keys2, 500);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|true|C4|true|B|C0|77a1f51" + //
				"db0983996|C1|false|C2|9|6|8|C3|5567|C4|true|C|C0|true|C1|819|C2|false|" + //
				"C3|3261|C4|4a8a58e7de16c2b8|D|C0|2|7|5|C1|true|C2|4a59477e424af521|C3|" + //
				"6c81842bc8ef6cac|C4|false|E|C0|7460|C1|724|C2|true|C3|true|C4|77905d9a" + //
				"17895657|F|C0|9|4|8|C1|3351|C2|1018e54007389b21|C3|1e88daba2c321d73|C4" + //
				"|3f8b9bdc7b1c9c76|G|C0|47f6c42759cc550b|C1|2a0f86f0adc6a388|C2|16262bb" + //
				"0ecdf10bb|C3|0|2|8|C4|9637|H|C0|5b25b003fcda1e0a|C1|true|C2|6013|C3|tr" + //
				"ue|C4|true|I|C0|4ff8f5d8fd886c7b|C1|4256|C2|true|C3|true|C4|false|J|C0" + //
				"|2f08018009189e23|C1|false|C2|5131|C3|false|C4|339dda690c68984c|K|C0|2" + //
				"187|C1|7588394a3ef57da6|C2|8|7|9|C3|true|C4|1c75cefe74bed74d|L|C0|545|" + //
				"C1|false|C2|8|7|8|C3|7caa12ccf1b8ae1f|C4|true|M|C0|false|C1|false|C2|t" + //
				"rue|C3|116d0b92855ba5e0|C4|false|N|C0|true|C1|true|C2|true|C3|58b50433" + //
				"b116e1ed|C4|3439|O|C0|62dfbf45a4e2043a|C1|2|9|2|C2|falsnqJ6ZG92ghIGQWg" + //
				"caofmOAdHmBjujP2qjabEYPtIsLg=";
		String[] keys3 = null;
		check(json, key, keys3, 932);

		key = "abc.def:A|C0|false|C1|true|C2|true|C3|true|C4|true|B|C0|77a1f51" + //
				"db0983996|C1|false|C2|9|6|8|C3|5567|C4|true|C|C0|true|C1|819|C2|false|" + //
				"C3|3261|C4|4a8a58e7de16c2b8|D|C0|2|7|5|C1|true|C2|4a59477e424af521|C3|" + //
				"6c81842bc8ef6cac|C4|false|E|C0|7460|C1|724|C2|true|C3|true|C4|77905d9a" + //
				"17895657|F|C0|9|4|8|C1|3351|C2|1018e54007389b21|C3|1e88daba2c321d73|C4" + //
				"|3f8b9bdc7b1c9c76|G|C0|47f6c42759cc550b|C1|2a0f86f0adc6a388|C2|16262bb" + //
				"0ecdf10bb|C3|0|2|8|C4|9637|H|C0|5b25b003fcda1e0a|C1|true|C2|6013|C3|tr" + //
				"ue|C4|true|I|C0|4ff8f5d8fd886c7b|C1|4256|C2|true|C3|true|C4|false|J|C0" + //
				"|2f08018009189e23|C1|false|C2|5131|C3|false|C4|339dda690c68984c|K|C0|2" + //
				"187|C1|7588394a3ef57da6|C2|8|7|9|C3|true|C4|1c75cefe74bed74d|L|C0|545|" + //
				"C1|false|C2|8|7|8|C3|7caa12ccf1b8ae1f|C4|true|M|C0|false|C1|false|C2|t" + //
				"rue|C3|116d0b92855ba5e0|C4|false|N|C0|true|C1|true|C2|true|C3|58b50433" + //
				"b116e1ed|C4|3439|O|C0|62dfbf45a4e2043a|C1|2|9|2|C2|false|C3|0|0|7|C4|0" + //
				"|3|6|P|C0|961nqJ6ZG92ghIGQWgcaofmOAdHmBjujP2qjabEYPtIsLg=";
		String[] keys4 = null;
		check(json, key, keys4, 960);

	}

	@Test
	public void testD1() throws Exception {

		String json = "{'A':{'C0':'2eba687a1a1941e6','C1':false,'C2':'5a7307f852fade79'" + //
				",'C3':6260,'C4':[6,1,7]},'B':{'C0':false,'C1':8399,'C2':false,'C3':[5,3" + //
				",2],'C4':5011},'C':{'C0':false,'C1':true,'C2':true,'C3':6627,'C4':false}" + //
				",'D':{'C0':false,'C1':2444,'C2':1533,'C3':[7,4,4],'C4':[6,7,8]},'E':{'C0':true" + //
				",'C1':8103,'C2':false,'C3':[6,7,1],'C4':true},'F':{'C0':'69fde17dcc558152'" + //
				",'C1':[4,8,8],'C2':4915,'C3':5614,'C4':false},'G':{'C0':5556,'C1':false" + //
				",'C2':3286,'C3':true,'C4':true},'H':{'C0':true,'C1':false,'C2':'3a78c9a6bb858da0'" + //
				",'C3':false,'C4':9455},'I':{'C0':[5,0,2],'C1':'65af100daf6884b3','C2':[4" + //
				",8,0],'C3':[9,0,8],'C4':'247eeffa7822cbed'},'J':{'C0':'394795ebe0f22610'" + //
				",'C1':'33b6e33a6e1a4565','C2':[1,9,9],'C3':[1,1,4],'C4':true},'K':{'C0':'390cc2b4b46db6cc'" + //
				",'C1':6669,'C2':500,'C3':'7684e2ec448982db','C4':[3,6,3]},'L':{'C0':false" + //
				",'C1':9241,'C2':'69cb6452d14cce64','C3':true,'C4':[4,9,4]},'M':{'C0':false" + //
				",'C1':false,'C2':true,'C3':'3138fcfc2704b2b0','C4':true},'N':{'C0':false" + //
				",'C1':false,'C2':[0,9,2],'C3':[0,0,0],'C4':[9,6,4]},'O':{'C0':[9,0,3],'C1':true" + //
				",'C2':false,'C3':'63c796bf194bc4a','C4':'612caa4ab181e1db'},'P':{'C0':[4" + //
				",1,8],'C1':false,'C2':1047,'C3':true,'C4':'20edb862b6ca7435'},'Q':{'C0':true" + //
				",'C1':false,'C2':746,'C3':false,'C4':3378},'R':{'C0':'783ed364db2eb32e'" + //
				",'C1':true,'C2':false,'C3':1544,'C4':false},'S':{'C0':false,'C1':3083,'C2':'3ba85fc9c6665230'" + //
				",'C3':true,'C4':false},'T':{'C0':'23e74d6921b84d4d','C1':false,'C2':false" + //
				",'C3':false,'C4':6682},'U':{'C0':[4,0,5],'C1':[6,2,5],'C2':'724e0989e8f2392f'" + //
				",'C3':3384,'C4':8557},'V':{'C0':false,'C1':false,'C2':false,'C3':true,'C4':'65c1aa9895bf4991'}" + //
				",'W':{'C0':true,'C1':1647,'C2':[8,3,2],'C3':true,'C4':false},'X':{'C0':true" + //
				",'C1':true,'C2':[9,6,0],'C3':'4b061d64ad70c166','C4':true},'Y':{'C0':false" + //
				",'C1':8358,'C2':7106,'C3':false,'C4':'765976cf71be37de'}}";

		String key = "AAW+0xFB51NYiXCRmUFShb91IEpNgSJs1QV+odNWPa4=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|2eba687a1a1941e6|C1|false|C2|5a7307f852fade79|C3|6" + //
				"260|C4|6|1|7|B|C0|false|C1|8399|C2|false|C3|5|3|2|C4|5011|C|C0|false|C" + //
				"1|true|C2|true|C3|66AAW+0xFB51NYiXCRmUFShb91IEpNgSJs1QV+odNWPa4=";
		String[] keys2 = null;
		check(json, key, keys2, 197);

		key = "abc.def:A|C0|2eba687a1a1941e6|C1|false|C2|5a7307f852fade79|C3|6" + //
				"260|C4|6|1|7|B|C0|false|C1|8399|C2|false|C3|5|3|2|C4|5011|C|C0|false|C" + //
				"1|true|C2|true|C3|6627|C4|false|D|C0|false|C1|2444|C2|1533|C3|7|4|4|C4" + //
				"|6|7|8|E|C0|true|C1|8103|C2|false|C3|6|7|1|C4|true|F|C0|69fde17dcc5581" + //
				"52|C1|4|8|8|C2|4915|C3|5614|C4|false|G|C0|5556|C1|false|C2|3286|C3|tru" + //
				"e|C4|true|H|C0|true|C1|false|C2|3a78c9a6bb858da0|C3|false|C4|9455|I|C0" + //
				"|5|0|2|C1|65af100daf6884b3|C2|4|8|0|C3|9|0|8|C4|247eeffa7822cbed|J|C0|" + //
				"394795ebe0f22610|C1|33b6e33a6e1a4565|C2|1|9|9|C3|1|1|4|C4|true|K|C0|39" + //
				"0cc2b4b46AAW+0xFB51NYiXCRmUFShb91IEpNgSJs1QV+odNWPa4=";
		String[] keys3 = null;
		check(json, key, keys3, 606);

		key = "abc.def:A|C0|2eba687a1a1941e6|C1|false|C2|5a7307f852fade79|C3|6" + //
				"260|C4|6|1|7|B|C0|false|C1|8399|C2|false|C3|5|3|2|C4|5011|C|C0|false|C" + //
				"1|true|C2|true|C3|6627|C4|false|D|C0|false|C1|2444|C2|1533|C3|7|4|4|C4" + //
				"|6|7|8|E|C0|true|C1|8103|C2|false|C3|6|7|1|C4|true|F|C0|69fde17dcc5581" + //
				"52|C1|4|8|8|C2|4915|C3|5614|C4|false|G|C0|5556|C1|false|C2|3286|C3|tru" + //
				"e|C4|true|H|C0|true|C1|false|C2|3a78c9a6bb858da0|C3|false|C4|9455|I|C0" + //
				"|5|0|2|C1|65af100daf6884b3|C2|4|8|0|C3|9|0|8|C4|247eeffa7822cbed|J|C0|" + //
				"394795ebe0f22610|C1|33b6e33a6e1a4565|C2|1|9|9|C3|1|1|4|C4|true|K|C0|39" + //
				"0cc2b4b46db6cc|C1|6669|C2|500|C3|7684e2ec448982db|C4|3|6|3|L|C0|false|" + //
				"C1|9241|C2|69cb6452d14cce64|C3|true|C4|4|9|4|M|C0|false|C1|false|C2|tr" + //
				"ue|C3|3138fcfc2704b2b0|C4|true|N|C0|false|C1|false|C2|0|9|2|C3|0|0|0|C" + //
				"4|9|6|4|O|C0|9|0|3|C1|true|C2|false|C3|63c796bf194bc4a|C4|612caa4ab181" + //
				"e1db|P|C0|4|1|8|C1|false|C2|1047|C3|true|CAAW+0xFB51NYiXCRmUFShb91IEpN" + //
				"gSJs1QV+odNWPa4=";
		String[] keys4 = null;
		check(json, key, keys4, 919);

		key = "abc.def:A|C0|2eba687a1a1941e6|C1|false|C2|5a7307f852fade79|C3|6" + //
				"260|C4|6|1|7|B|C0|false|C1|8399|C2|false|C3|5|3|2|C4|5011|C|C0|false|C" + //
				"1|true|C2|true|C3|6627|C4|false|D|C0|false|C1|2444|C2|1533|C3|7|4|4|C4" + //
				"|6|7|8|E|C0|true|C1|8103|C2|false|C3|6|7|1|C4|true|F|C0|69fde17dcc5581" + //
				"52|C1|4|8|8|C2|4915|C3|5614|C4|false|G|C0|5556|C1|false|C2|3286|C3|tru" + //
				"e|C4|true|H|C0|true|C1|false|C2|3a78c9a6bb858da0|C3|false|C4|9455|I|C0" + //
				"|5|0|2|C1|65af100daf6884b3|C2|4|8|0|C3|9|0|8|C4|247eeffa7822cbed|J|C0|" + //
				"394795ebe0f22610|C1|33b6e33a6e1a4565|C2|1|9|9|C3|1|1|4|C4|true|K|C0|39" + //
				"0cc2b4b46db6cc|C1|6669|C2|500|C3|7684e2ec448982db|C4|3|6|3|L|C0|false|" + //
				"C1|9241|C2|69cb6452d14cce64|C3|true|C4|4|9|4|M|C0|false|C1|false|C2|tr" + //
				"ue|C3|3138fcfc2704b2b0|C4|true|N|C0|false|C1|false|C2|0|9|2|C3|0|0|0|C" + //
				"4|9|6|4|O|C0|9|0|3|C1|true|C2|false|C3|63c796bf194bc4a|C4|612caa4ab181" + //
				"e1db|P|C0|4|1|8|C1|false|C2|1047|C3|true|C4|20edb862b6ca7435|Q|C0|true" + //
				"|C1|false|C2|746|AAW+0xFB51NYiXCRmUFShb91IEpNgSJs1QV+odNWPa4=";
		String[] keys5 = null;
		check(json, key, keys5, 964);

	}

	@Test
	public void testE1() throws Exception {

		String json = "{'A':{'C0':false,'C1':false,'C2':[2,7,3],'C3':false,'C4':'fc4aec7d52f9ceb'}" + //
				",'B':{'C0':'12a48bf74869da8b','C1':true,'C2':'6eea577278d5442a','C3':6911" + //
				",'C4':'693b23662c4b4036'},'C':{'C0':false,'C1':[7,5,3],'C2':[2,7,0],'C3':[6" + //
				",9,1],'C4':true},'D':{'C0':'64c760151fbbdf5','C1':'6b4286d25cdcdc6','C2':true" + //
				",'C3':[1,4,1],'C4':433},'E':{'C0':false,'C1':5505,'C2':7039,'C3':true,'C4':true}" + //
				",'F':{'C0':true,'C1':'6fe3d1b972386de6','C2':5803,'C3':9733,'C4':true}" + //
				",'G':{'C0':false,'C1':9251,'C2':true,'C3':[0,1,2],'C4':false},'H':{'C0':7029" + //
				",'C1':2551,'C2':true,'C3':8537,'C4':'610f0e8c011421e0'},'I':{'C0':true" + //
				",'C1':6561,'C2':[4,4,6],'C3':true,'C4':'72ebffb09019ec67'},'J':{'C0':8295" + //
				",'C1':'2ee173bf7a675d5b','C2':false,'C3':true,'C4':true},'K':{'C0':true" + //
				",'C1':[5,3,3],'C2':true,'C3':'25126ff6b5af311a','C4':[3,8,9]},'L':{'C0':[6" + //
				",8,9],'C1':'1929efc46883db79','C2':753,'C3':2407,'C4':4687},'M':{'C0':false" + //
				",'C1':[6,3,8],'C2':[6,4,8],'C3':'14bcac3fea9a8d33','C4':[1,1,5]},'N':{'C0':false" + //
				",'C1':3936,'C2':2373,'C3':true,'C4':true},'O':{'C0':true,'C1':false,'C2':5044" + //
				",'C3':true,'C4':true},'P':{'C0':[5,7,8],'C1':'2fc5c9719dfac764','C2':8069" + //
				",'C3':8063,'C4':'2a3ae12c2d6a136'},'Q':{'C0':true,'C1':802,'C2':[2,4,7]" + //
				",'C3':'547e298c4f3987d4','C4':'22250382cfc15578'},'R':{'C0':[1,6,6],'C1':8552" + //
				",'C2':'e58f80f9506050f','C3':[3,7,3],'C4':true},'S':{'C0':[0,1,9],'C1':6629" + //
				",'C2':'723ae404eb9e2361','C3':[1,1,7],'C4':true},'T':{'C0':true,'C1':true" + //
				",'C2':true,'C3':false,'C4':2797},'U':{'C0':false,'C1':false,'C2':1609,'C3':3502" + //
				",'C4':false},'V':{'C0':false,'C1':false,'C2':false,'C3':414,'C4':[0,5,5]}" + //
				",'W':{'C0':true,'C1':'758661bc34f81ef0','C2':[6,0,4],'C3':628,'C4':false}" + //
				",'X':{'C0':[6,3,2],'C1':true,'C2':'7289e9fb8e7b7628','C3':9608,'C4':true}" + //
				",'Y':{'C0':false,'C1':[4,4,4],'C2':false,'C3':false,'C4':true}}";

		String key = "hYeiWpMvankSztZJ92F1WlBixmQ3prtP/pELs3KdOJo=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|false|C2|2|7|3|C3|false|C4|fc4aec7d52f9ce" + //
				"b|B|C0|12a48bf748hYeiWpMvankSztZJ92F1WlBixmQ3prtP/pELs3KdOJo=";
		String[] keys2 = null;
		check(json, key, keys2, 124);

		key = "abc.def:A|C0|false|C1|false|C2|2|7|3|C3|false|C4|fc4aec7d52f9ce" + //
				"b|B|C0|12a48bf74869da8b|C1|true|C2|6eea577278d5442a|C3|6911|C4|693b236" + //
				"62c4b4036|C|C0|false|C1|7|5|3|C2|2|7|0|C3|6|9|1|C4|true|D|C0|64c760151" + //
				"fbbdf5|C1|6b4286d25cdcdc6|C2|true|C3|1|4|1|C4|433|E|C0|false|C1|5505|C" + //
				"2|7039|C3|true|C4|true|F|C0|true|C1|6fe3d1b972386de6|C2|5803|C3|9733|C" + //
				"4|true|G|C0|false|C1|9251|C2|truhYeiWpMvankSztZJ92F1WlBixmQ3prtP/pELs3" + //
				"KdOJo=";
		String[] keys3 = null;
		check(json, key, keys3, 419);

		key = "abc.def:A|C0|false|C1|false|C2|2|7|3|C3|false|C4|fc4aec7d52f9ce" + //
				"b|B|C0|12a48bf74869da8b|C1|true|C2|6eea577278d5442a|C3|6911|C4|693b236" + //
				"62c4b4036|C|C0|false|C1|7|5|3|C2|2|7|0|C3|6|9|1|C4|true|D|C0|64c760151" + //
				"fbbdf5|C1|6b4286d25cdcdc6|C2|true|C3|1|4|1|C4|433|E|C0|false|C1|5505|C" + //
				"2|7039|C3|true|C4|true|F|C0|true|C1|6fe3d1b972386de6|C2|5803|C3|9733|C" + //
				"4|true|G|C0|false|C1|9251|C2|true|C3|0|1|2|C4|false|H|C0|7029|C1|2551|" + //
				"C2|true|C3|8537|C4|610f0e8c01142hYeiWpMvankSztZJ92F1WlBixmQ3prtP/pELs3" + //
				"KdOJo=";
		String[] keys4 = null;
		check(json, key, keys4, 489);

		key = "abc.def:A|C0|false|C1|false|C2|2|7|3|C3|false|C4|fc4aec7d52f9ce" + //
				"b|B|C0|12a48bf74869da8b|C1|true|C2|6eea577278d5442a|C3|6911|C4|693b236" + //
				"62c4b4036|C|C0|false|C1|7|5|3|C2|2|7|0|C3|6|9|1|C4|true|D|C0|64c760151" + //
				"fbbdf5|C1|6b4286d25cdcdc6|C2|true|C3|1|4|1|C4|433|E|C0|false|C1|5505|C" + //
				"2|7039|C3|true|C4|true|F|C0|true|C1|6fe3d1b972386de6|C2|5803|C3|9733|C" + //
				"4|true|G|C0|false|C1|9251|C2|true|C3|0|1|2|C4|false|H|C0|7029|C1|2551|" + //
				"C2|true|C3|8537|C4|610f0e8c011421e0|I|C0|true|C1|6561|C2|4|4|6|C3|true" + //
				"|C4|72ebfhYeiWpMvankSztZJ92F1WlBixmQ3prtP/pELs3KdOJo=";
		String[] keys5 = null;
		check(json, key, keys5, 536);

		key = "abc.def:A|C0|false|C1|false|C2|2|7|3|C3|false|C4|fc4aec7d52f9ce" + //
				"b|B|C0|12a48bf74869da8b|C1|true|C2|6eea577278d5442a|C3|6911|C4|693b236" + //
				"62c4b4036|C|C0|false|C1|7|5|3|C2|2|7|0|C3|6|9|1|C4|true|D|C0|64c760151" + //
				"fbbdf5|C1|6b4286d25cdcdc6|C2|true|C3|1|4|1|C4|433|E|C0|false|C1|5505|C" + //
				"2|7039|C3|true|C4|true|F|C0|true|C1|6fe3d1b972386de6|C2|5803|C3|9733|C" + //
				"4|true|G|C0|false|C1|9251|C2|true|C3|0|1|2|C4|false|H|C0|7029|C1|2551|" + //
				"C2|true|C3|8537|C4|610f0e8c011421e0|I|C0|true|C1|6561|C2|4|4|6|C3|true" + //
				"|C4|72ebffb09019ec67|J|C0|8295|C1|2ee173bf7a675d5b|C2|false|C3|true|C4" + //
				"|true|K|C0|true|C1|5|3|3|C2|true|C3|25126ff6b5af311a|C4|3|8|9|L|C0|6|8" + //
				"|9|C1|1929efc46883db79|C2|753|C3|2407|C4|4687|M|C0|false|C1|6|3|8|C2|6" + //
				"|4|8|C3|14bcac3fea9a8d33|C4|1|1|5|N|C0|false|C1|3936|C2|2373|C3|true|C" + //
				"4|true|O|C0|true|C1|false|C2|5044|C3|true|C4|true|P|C0|5|7|8|C1|2fc5c9" + //
				"719dfac764|C2|8069|C3|8hYeiWpMvankSztZJ92F1WlBixmQ3prtP/pELs3KdOJo=";
		String[] keys6 = null;
		check(json, key, keys6, 900);

		key = "abc.def:A|C0|false|C1|false|C2|2|7|3|C3|false|C4|fc4aec7d52f9ce" + //
				"b|B|C0|12a48bf74869da8b|C1|true|C2|6eea577278d5442a|C3|6911|C4|693b236" + //
				"62c4b4036|C|C0|false|C1|7|5|3|C2|2|7|0|C3|6|9|1|C4|true|D|C0|64c760151" + //
				"fbbdf5|C1|6b4286d25cdcdc6|C2|true|C3|1|4|1|C4|433|E|C0|false|C1|5505|C" + //
				"2|7039|C3|true|C4|true|F|C0|true|C1|6fe3d1b972386de6|C2|5803|C3|9733|C" + //
				"4|true|G|C0|false|C1|9251|C2|true|C3|0|1|2|C4|false|H|C0|7029|C1|2551|" + //
				"C2|true|C3|8537|C4|610f0e8c011421e0|I|C0|true|C1|6561|C2|4|4|6|C3|true" + //
				"|C4|72ebffb09019ec67|J|C0|8295|C1|2ee173bf7a675d5b|C2|false|C3|true|C4" + //
				"|true|K|C0|true|C1|5|3|3|C2|true|C3|25126ff6b5af311a|C4|3|8|9|L|C0|6|8" + //
				"|9|C1|1929efc46883db79|C2|753|C3|2407|C4|4687|M|C0|false|C1|6|3|8|C2|6" + //
				"|4|8|C3|14bcac3fea9a8d33|C4|1|1|5|N|C0|false|C1|3936|C2|2373|C3|true|C" + //
				"4|true|O|C0|true|C1|false|C2|5044|C3|true|C4|true|P|C0|5|7|8|C1|2fc5c9" + //
				"719dfac764|C2|8069|C3|8063|C4|2a3ae12c2d6a136|Q|C0|true|C1|802|C2|2|4|" + //
				"7|C3|547e298c4f3987d4|C4|22250382cfc15578|R|C0|1|6|6|C1|hYeiWpMvankSzt" + //
				"ZJ92F1WlBixmQ3prtP/pELs3KdOJo=";
		String[] keys7 = null;
		check(json, key, keys7, 1003);

	}

	@Test
	public void testF1() throws Exception {

		String json = "{'A':{'C0':[8,0,7],'C1':true,'C2':6488,'C3':true,'C4':true}" + //
				",'B':{'C0':false,'C1':2693,'C2':true,'C3':'758159b1354556ff','C4':8593}" + //
				",'C':{'C0':[8,8,0],'C1':5349,'C2':'3ee1aa6b0750067f','C3':false,'C4':false}" + //
				",'D':{'C0':false,'C1':305,'C2':[3,5,2],'C3':true,'C4':[8,8,1]},'E':{'C0':'7d1d896dacb9d36a'" + //
				",'C1':false,'C2':[2,5,4],'C3':true,'C4':[0,1,0]},'F':{'C0':[9,2,2],'C1':[1" + //
				",0,8],'C2':7729,'C3':false,'C4':6676},'G':{'C0':[8,3,4],'C1':false,'C2':'585570f0e98639a2'" + //
				",'C3':false,'C4':'4b5a734c33c346f9'},'H':{'C0':false,'C1':4953,'C2':'5d6a5716d2d845bc'" + //
				",'C3':'c736712a8353294','C4':false},'I':{'C0':true,'C1':'684eedebed91409'" + //
				",'C2':true,'C3':5175,'C4':false},'J':{'C0':true,'C1':'75260c8b4dabbeaa'" + //
				",'C2':false,'C3':false,'C4':128},'K':{'C0':false,'C1':[2,4,0],'C2':[0,8" + //
				",6],'C3':'59d28add9d46fbdd','C4':true},'L':{'C0':[6,8,1],'C1':[6,8,7],'C2':[3" + //
				",5,5],'C3':[3,8,2],'C4':560},'M':{'C0':'1acabed2dd2f09e1','C1':true,'C2':false" + //
				",'C3':true,'C4':'7c2a6608f86a7e6d'},'N':{'C0':false,'C1':738,'C2':true" + //
				",'C3':false,'C4':true},'O':{'C0':'46ef60be0f5cc48c','C1':'54082461afdc6fd4'" + //
				",'C2':false,'C3':5963,'C4':false},'P':{'C0':false,'C1':false,'C2':true" + //
				",'C3':[6,4,4],'C4':6407},'Q':{'C0':1272,'C1':'df8ea8e9d33036f','C2':2444" + //
				",'C3':[8,9,5],'C4':[9,3,3]},'R':{'C0':false,'C1':false,'C2':9699,'C3':true" + //
				",'C4':7192},'S':{'C0':9449,'C1':'41900b0c49522326','C2':false,'C3':true" + //
				",'C4':true},'T':{'C0':'1a90d06211b34b69','C1':[7,7,5],'C2':true,'C3':5724" + //
				",'C4':2896},'U':{'C0':true,'C1':1388,'C2':[3,4,2],'C3':false,'C4':'467592f33544f212'}" + //
				",'V':{'C0':565,'C1':8431,'C2':[3,2,5],'C3':true,'C4':true},'W':{'C0':8112" + //
				",'C1':false,'C2':[1,4,5],'C3':false,'C4':true},'X':{'C0':[5,2,9],'C1':false" + //
				",'C2':[0,1,2],'C3':'63d624ec12822612','C4':true},'Y':{'C0':4318,'C1':2203" + //
				",'C2':[2,4,1],'C3':[8,3,7],'C4':'29dbb53ea7a2f3c7'}}";

		String key = "huXp1tRhAJ9mtsZ9hQlzDhdmViGGIis5LClbY7+AQDA=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|8|0|7|C1|true|C2|6488|C3|true|C4|true|B|C0|false|C" + //
				"1|2693|C2|true|C3|758159b1354556ff|C4|8593|C|C0|8|8|0|C1|5349|C2|3ee1a" + //
				"a6b0750067f|C3|false|C4|false|D|C0|false|C1|305|C2|3|5|2|C3|true|C4|8|" + //
				"8|1|E|C0|7d1d896dacb9d36a|C1|false|C2|2|5|4|C3|true|C4|0|1|0|F|C0|9|2|" + //
				"2|C1|1|0|8|C2|7729|C3|false|C4|6676|G|C0|8|3|4|C1|false|C2|585570f0e98" + //
				"639a2|C3|false|C4|4b5a734c33c346f9|H|C0|false|C1|4953|C2|5d6a5716d2d84" + //
				"5bc|C3|c736712a8353294|C4|false|I|C0|true|C1|684eedebhuXp1tRhAJ9mtsZ9h" + //
				"QlzDhdmViGGIis5LClbY7+AQDA=";
		String[] keys2 = null;
		check(json, key, keys2, 510);

		key = "abc.def:A|C0|8|0|7|C1|true|C2|6488|C3|true|C4|true|B|C0|false|C" + //
				"1|2693|C2|true|C3|758159b1354556ff|C4|8593|C|C0|8|8|0|C1|5349|C2|3ee1a" + //
				"a6b0750067f|C3|false|C4|false|D|C0|false|C1|305|C2|3|5|2|C3|true|C4|8|" + //
				"8|1|E|C0|7d1d896dacb9d36a|C1|false|C2|2|5|4|C3|true|C4|0|1|0|F|C0|9|2|" + //
				"2|C1|1|0|8|C2|7729|C3|false|C4|6676|G|C0|8|3|4|C1|false|C2|585570f0e98" + //
				"639a2|C3|false|C4|4b5a734c33c346f9|H|C0|false|C1|4953|C2|5d6a5716d2d84" + //
				"5bc|C3|c736712a8353294|C4|false|I|C0|true|C1|684eedebed91409|C2|true|C" + //
				"3|5175|C4|false|J|C0|true|C1|75260c8b4dabbeaa|C2|false|C3|false|C4|128" + //
				"|K|C0|false|C1|2|4|0|C2|0|8|6|C3|59d28add9d46fbdd|C4|true|L|C0|6|8|1|C" + //
				"1|6|8|7|C2|3|5|5|C3|3|8|2|C4|560|M|C0|1acabed2dd2f09e1|C1|true|C2|fals" + //
				"e|C3|true|C4|7c2a6608f86a7e6d|N|C0|false|C1|738|C2|true|C3|false|C4|tr" + //
				"ue|O|C0|46ef60be0f5cc48c|C1|54082461afdc6huXp1tRhAJ9mtsZ9hQlzDhdmViGGI" + //
				"is5LClbY7+AQDA=";
		String[] keys3 = null;
		check(json, key, keys3, 848);

	}

	@Test
	public void testG1() throws Exception {

		String json = "{'A':{'C0':'261b7f2a31a60d76','C1':[9,5,0],'C2':[4,5,4]" + //
				",'C3':true,'C4':'4c19b2c787159dc2'},'B':{'C0':[4,5,9],'C1':true,'C2':false" + //
				",'C3':true,'C4':9809},'C':{'C0':[6,7,8],'C1':false,'C2':true,'C3':false" + //
				",'C4':3459},'D':{'C0':true,'C1':1960,'C2':'134fa50d0d465b31','C3':true" + //
				",'C4':'65033db7c4b65be1'},'E':{'C0':false,'C1':false,'C2':true,'C3':6194" + //
				",'C4':1966},'F':{'C0':1614,'C1':[4,6,1],'C2':'99a16be3212fa7d','C3':9327" + //
				",'C4':'508ae32adc73da4a'},'G':{'C0':false,'C1':false,'C2':3864,'C3':'6eb0a6fa4569285'" + //
				",'C4':[5,7,0]},'H':{'C0':'41232ff43c633da8','C1':false,'C2':false,'C3':false" + //
				",'C4':false},'I':{'C0':4743,'C1':'547ebe72223fd561','C2':false,'C3':[7" + //
				",8,8],'C4':'5c0e569b417f9e66'},'J':{'C0':true,'C1':3435,'C2':5702,'C3':true" + //
				",'C4':6887},'K':{'C0':'56c7768ecc48b743','C1':8913,'C2':[3,7,8],'C3':false" + //
				",'C4':'5122c511a1ff0bd2'},'L':{'C0':[3,5,5],'C1':'668bf3443b92212','C2':[8" + //
				",6,5],'C3':9390,'C4':[0,0,8]},'M':{'C0':true,'C1':'b5ad8f37e5ee041','C2':[9" + //
				",2,1],'C3':2593,'C4':false},'N':{'C0':'64a790423472427c','C1':false,'C2':2092" + //
				",'C3':[8,3,3],'C4':true},'O':{'C0':false,'C1':true,'C2':4422,'C3':false" + //
				",'C4':false},'P':{'C0':[1,0,8],'C1':false,'C2':2119,'C3':false,'C4':[2" + //
				",8,0]},'Q':{'C0':[6,4,2],'C1':true,'C2':true,'C3':true,'C4':[5,6,1]},'R':{'C0':true" + //
				",'C1':false,'C2':7072,'C3':'1d5f3910aba11c7b','C4':7976},'S':{'C0':[2,4" + //
				",0],'C1':true,'C2':false,'C3':true,'C4':'6c8c6190345b3f23'},'T':{'C0':false" + //
				",'C1':183,'C2':true,'C3':false,'C4':[7,9,7]},'U':{'C0':8418,'C1':true,'C2':3810" + //
				",'C3':true,'C4':4957},'V':{'C0':[6,9,2],'C1':1146,'C2':[5,0,6],'C3':true" + //
				",'C4':true},'W':{'C0':4398,'C1':false,'C2':9325,'C3':8142,'C4':[2,5,3]}" + //
				",'X':{'C0':false,'C1':4797,'C2':true,'C3':[7,7,8],'C4':true},'Y':{'C0':true" + //
				",'C1':true,'C2':true,'C3':1759,'C4':true}}";

		String key = "jBM4KkD7JFy8UUdmpu6u0fidyRsHvz8oyyq1VKLgBkA=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|261b7f2a31a60d76|C1|9|5|0|C2|4|5|4|C3|true|C4|4c19" + //
				"b2c787159dc2|B|C0|4|5|9|C1|true|C2|false|C3|true|C4|9809|C|C0|6|7|8|C1" + //
				"|false|C2|true|C3|false|C4|3459|D|C0|true|C1|19jBM4KkD7JFy8UUdmpu6u0fi" + //
				"dyRsHvz8oyyq1VKLgBkA=";
		String[] keys2 = null;
		check(json, key, keys2, 224);

		key = "abc.def:A|C0|261b7f2a31a60d76|C1|9|5|0|C2|4|5|4|C3|true|C4|4c19" + //
				"b2c787159dc2|B|C0|4|5|9|C1|true|C2|false|C3|true|C4|9809|C|C0|6|7|8|C1" + //
				"|false|C2|true|C3|false|C4|3459|D|C0|true|C1|1960|C2|134fa50d0d465b31|" + //
				"C3|true|C4|65033db7c4b65be1|E|C0|false|C1|false|C2|true|C3|6194|C4|196" + //
				"6|F|C0|1614|C1|4|6|1|C2|99a16be3212fa7d|C3|9327|C4|508ae32adc73da4a|G|" + //
				"C0|false|C1|false|C2|3864|C3|6eb0a6fa4569285|C4|5|7|0|H|C0|41232ff43c6" + //
				"33da8|C1|false|C2|false|C3|false|C4|false|I|C0|4743|C1|547ebe72223fd56" + //
				"1|C2|false|C3|7|8|8|C4|5c0e569b417f9e6jBM4KkD7JFy8UUdmpu6u0fidyRsHvz8o" + //
				"yyq1VKLgBkA=";
		String[] keys3 = null;
		check(json, key, keys3, 565);

		key = "abc.def:A|C0|261b7f2a31a60d76|C1|9|5|0|C2|4|5|4|C3|true|C4|4c19" + //
				"b2c787159dc2|B|C0|4|5|9|C1|true|C2|false|C3|true|C4|9809|C|C0|6|7|8|C1" + //
				"|false|C2|true|C3|false|C4|3459|D|C0|true|C1|1960|C2|134fa50d0d465b31|" + //
				"C3|true|C4|65033db7c4b65be1|E|C0|false|C1|false|C2|true|C3|6194|C4|196" + //
				"6|F|C0|1614|C1|4|6|1|C2|99a16be3212fa7d|C3|9327|C4|508ae32adc73da4a|G|" + //
				"C0|false|C1|false|C2|3864|C3|6eb0a6fa4569285|C4|5|7|0|H|C0|41232ff43c6" + //
				"33da8|C1|false|C2|false|C3|false|C4|false|I|C0|4743|C1|547ebe72223fd56" + //
				"1|C2|false|C3|7|8|8|C4|5c0e569b417f9e66|J|C0|true|C1|3435|C2|5702|C3|t" + //
				"rue|C4|6887|K|C0|56c7768ecc48b743|C1|8913|C2|3|7|8|C3|false|C4|5122c51" + //
				"1a1ff0bd2|L|C0|3|5|5|C1|668bf3443b9jBM4KkD7JFy8UUdmpu6u0fidyRsHvz8oyyq" + //
				"1VKLgBkA=";
		String[] keys4 = null;
		check(json, key, keys4, 702);

		key = "abc.def:A|C0|261b7f2a31a60d76|C1|9|5|0|C2|4|5|4|C3|true|C4|4c19" + //
				"b2c787159dc2|B|C0|4|5|9|C1|true|C2|false|C3|true|C4|9809|C|C0|6|7|8|C1" + //
				"|false|C2|true|C3|false|C4|3459|D|C0|true|C1|1960|C2|134fa50d0d465b31|" + //
				"C3|true|C4|65033db7c4b65be1|E|C0|false|C1|false|C2|true|C3|6194|C4|196" + //
				"6|F|C0|1614|C1|4|6|1|C2|99a16be3212fa7d|C3|9327|C4|508ae32adc73da4a|G|" + //
				"C0|false|C1|false|C2|3864|C3|6eb0a6fa4569285|C4|5|7|0|H|C0|41232ff43c6" + //
				"33da8|C1|false|C2|false|C3|false|C4|false|I|C0|4743|C1|547ebe72223fd56" + //
				"1|C2|false|C3|7|8|8|C4|5c0e569b417f9e66|J|C0|true|C1|3435|C2|5702|C3|t" + //
				"rue|C4|6887|K|C0|56c7768ecc48b743|C1|8913|C2|3|7|8|C3|false|C4|5122c51" + //
				"1a1ff0bd2|L|C0|3|5|5|C1|668bf3443b92212|C2|8|6|5|C3|9390|C4|0|0|8|M|C0" + //
				"|truejBM4KkD7JFy8UUdmpu6u0fidyRsHvz8oyyq1VKLgBkA=";
		String[] keys5 = null;
		check(json, key, keys5, 742);

	}

	@Test
	public void testH1() throws Exception {

		String json = "{'A':{'C0':true,'C1':[1,9,5],'C2':false,'C3':6425,'C4':false}" + //
				",'B':{'C0':8834,'C1':true,'C2':true,'C3':false,'C4':3362},'C':{'C0':true" + //
				",'C1':3739,'C2':[8,5,4],'C3':'51fc31c628106f8d','C4':false},'D':{'C0':false" + //
				",'C1':false,'C2':6226,'C3':'16d23e3fc92eb73d','C4':7996},'E':{'C0':6701" + //
				",'C1':true,'C2':[5,0,4],'C3':6978,'C4':false},'F':{'C0':false,'C1':false" + //
				",'C2':true,'C3':false,'C4':[0,1,7]},'G':{'C0':[2,8,5],'C1':false,'C2':false" + //
				",'C3':[9,7,2],'C4':true},'H':{'C0':5278,'C1':true,'C2':[8,8,9],'C3':[0" + //
				",4,5],'C4':3139},'I':{'C0':6947,'C1':4145,'C2':[2,7,5],'C3':'3610813563f4337d'" + //
				",'C4':[5,1,0]},'J':{'C0':[3,1,4],'C1':false,'C2':false,'C3':[9,7,1],'C4':[7" + //
				",5,6]},'K':{'C0':[6,7,5],'C1':[3,6,8],'C2':640,'C3':'39af3d3a4c998361'" + //
				",'C4':'42bdd6f5c74981ee'},'L':{'C0':true,'C1':true,'C2':true,'C3':[3,7" + //
				",8],'C4':[2,0,5]},'M':{'C0':[0,9,6],'C1':false,'C2':5057,'C3':true,'C4':true}" + //
				",'N':{'C0':true,'C1':2805,'C2':'4fc43b421366d74a','C3':true,'C4':1139}" + //
				",'O':{'C0':false,'C1':'54a45c94f5e15ed5','C2':[1,4,8],'C3':[1,4,2],'C4':false}" + //
				",'P':{'C0':'1a0a6992d0d0e347','C1':false,'C2':369,'C3':false,'C4':5931}" + //
				",'Q':{'C0':[2,6,7],'C1':true,'C2':[4,2,6],'C3':5797,'C4':'19e8d91178e2d66b'}" + //
				",'R':{'C0':[8,0,8],'C1':'cfb960493e1a009','C2':'83415caa57e7f40','C3':false" + //
				",'C4':[9,7,0]},'S':{'C0':true,'C1':false,'C2':504,'C3':'18465cdda95e83f'" + //
				",'C4':true},'T':{'C0':'3ebf71f2e7e673c3','C1':true,'C2':false,'C3':false" + //
				",'C4':[2,5,5]},'U':{'C0':false,'C1':'398b5747c0782400','C2':'1196c9af9b39cdf7'" + //
				",'C3':8430,'C4':'52043ddd2240f256'},'V':{'C0':true,'C1':false,'C2':'4b94f0858cd388e1'" + //
				",'C3':true,'C4':[7,3,3]},'W':{'C0':true,'C1':false,'C2':false,'C3':4631" + //
				",'C4':242},'X':{'C0':true,'C1':6151,'C2':1877,'C3':true,'C4':[2,9,6]},'Y':{'C0':false" + //
				",'C1':'3888affce86d6dcc','C2':true,'C3':[2,9,2],'C4':false}}";

		String key = "NclstZfHgKq6yYZ3R3iGHF4by+cvb6CttNbBQLzVzZc=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|true|C1|1|9|5|C2|false|C3|6425|C4|false|B|C0|8834|" + //
				"C1|trNclstZfHgKq6yYZ3R3iGHF4by+cvb6CttNbBQLzVzZc=";
		String[] keys2 = null;
		check(json, key, keys2, 112);

		key = "abc.def:A|C0|true|C1|1|9|5|C2|false|C3|6425|C4|false|B|C0|8834|" + //
				"C1|true|C2|true|C3|false|C4|3362|C|C0|true|C1|3739|C2|8|5|4|C3|51fc31c" + //
				"628106f8d|C4|false|D|C0|false|C1|false|C2|6226|C3|16d23e3fc92eb73d|C4|" + //
				"7996|E|C0|6701|C1|true|C2|5|0|4|C3|6978|C4|false|F|C0|false|C1|false|C" + //
				"2|true|C3|false|C4|0|1|7|G|C0|2|8|5|C1|false|C2|false|C3|9|7|2|C4|true" + //
				"|H|C0|5278|C1|true|C2|8|8|9|C3|0|4|5|C4|3139|I|C0|6947|C1|4145|C2|2|7|" + //
				"5|C3|3610813563f4337d|C4|5|1|0|J|C0|3|1|4|C1|false|C2|false|C3|9|7|1|C" + //
				"4NclstZfHgKq6yYZ3R3iGHF4by+cvb6CttNbBQLzVzZc=";
		String[] keys3 = null;
		check(json, key, keys3, 528);

		key = "abc.def:A|C0|true|C1|1|9|5|C2|false|C3|6425|C4|false|B|C0|8834|" + //
				"C1|true|C2|true|C3|false|C4|3362|C|C0|true|C1|3739|C2|8|5|4|C3|51fc31c" + //
				"628106f8d|C4|false|D|C0|false|C1|false|C2|6226|C3|16d23e3fc92eb73d|C4|" + //
				"7996|E|C0|6701|C1|true|C2|5|0|4|C3|6978|C4|false|F|C0|false|C1|false|C" + //
				"2|true|C3|false|C4|0|1|7|G|C0|2|8|5|C1|false|C2|false|C3|9|7|2|C4|true" + //
				"|H|C0|5278|C1|true|C2|8|8|9|C3|0|4|5|C4|3139|I|C0|6947|C1|4145|C2|2|7|" + //
				"5|C3|3610813563f4337d|C4|5|1|0|J|C0|3|1|4|C1|false|C2|false|C3|9|7|1|C" + //
				"4|7|5|6|K|C0|6|7|5|C1|3|6|8|C2|640|C3|39af3d3a4c998361|C4|42bdd6f5c749" + //
				"81ee|L|C0|true|C1|true|C2|true|C3|3|7|8|C4|2|0|5|M|C0|0|9|6|C1|false|C" + //
				"2|5057|C3|true|C4|true|N|C0|truNclstZfHgKq6yYZ3R3iGHF4by+cvb6CttNbBQLz" + //
				"VzZc=";
		String[] keys4 = null;
		check(json, key, keys4, 698);

	}

	@Test
	public void testI1() throws Exception {

		String json = "{'A':{'C0':true,'C1':true,'C2':false,'C3':true,'C4':false}" + //
				",'B':{'C0':'3795366d4f5cb870','C1':false,'C2':[5,2,7],'C3':5707,'C4':[1" + //
				",2,4]},'C':{'C0':'20f9a7a6297a3971','C1':2939,'C2':true,'C3':[2,4,1],'C4':'62e553d22974d6cf'}" + //
				",'D':{'C0':'991d58516cc79d2','C1':'770edd31a8d0659f','C2':'48ee1dbfa22d5f5a'" + //
				",'C3':[8,1,5],'C4':false},'E':{'C0':9584,'C1':true,'C2':true,'C3':true" + //
				",'C4':false},'F':{'C0':false,'C1':false,'C2':false,'C3':3561,'C4':'115516b8fe3aa776'}" + //
				",'G':{'C0':true,'C1':5955,'C2':true,'C3':true,'C4':[6,8,6]},'H':{'C0':6288" + //
				",'C1':false,'C2':'34c12c7a88fad8b6','C3':true,'C4':6265},'I':{'C0':false" + //
				",'C1':[7,5,9],'C2':'400a7acb91b4b216','C3':true,'C4':true},'J':{'C0':[2" + //
				",9,8],'C1':'25d4b733a22cc0f','C2':8731,'C3':6274,'C4':'4d8a44c20248a7a5'}" + //
				",'K':{'C0':false,'C1':false,'C2':3638,'C3':'754c2ad95c3e40c1','C4':false}" + //
				",'L':{'C0':'2c70398b07f98c64','C1':'4952df14e86b0895','C2':'4c1284cf993ea09d'" + //
				",'C3':false,'C4':6171},'M':{'C0':false,'C1':false,'C2':true,'C3':'78df452ddb737366'" + //
				",'C4':[8,2,3]},'N':{'C0':false,'C1':'23df413d47bd921b','C2':[8,0,2],'C3':true" + //
				",'C4':true},'O':{'C0':false,'C1':'7c7d38123c2f554d','C2':false,'C3':'4023abfceb52c4d3'" + //
				",'C4':true},'P':{'C0':[8,3,5],'C1':true,'C2':799,'C3':9397,'C4':true},'Q':{'C0':8031" + //
				",'C1':'32f36026a6d7edcc','C2':false,'C3':8434,'C4':9781},'R':{'C0':false" + //
				",'C1':[7,8,5],'C2':true,'C3':[3,9,0],'C4':false},'S':{'C0':[8,0,9],'C1':false" + //
				",'C2':true,'C3':884,'C4':'6918d7f6d2ffd1a5'},'T':{'C0':'5630819ca424a3b9'" + //
				",'C1':4709,'C2':false,'C3':false,'C4':7844},'U':{'C0':true,'C1':[6,3,2]" + //
				",'C2':false,'C3':'3fc5d78511ef9782','C4':false},'V':{'C0':9027,'C1':false" + //
				",'C2':[5,1,8],'C3':'5483ecb89155b821','C4':7022},'W':{'C0':[3,7,3],'C1':false" + //
				",'C2':5809,'C3':false,'C4':4165},'X':{'C0':[8,2,9],'C1':false,'C2':false" + //
				",'C3':'736c59ae723b7fe7','C4':false},'Y':{'C0':false,'C1':2088,'C2':[6" + //
				",6,1],'C3':[1,3,5],'C4':'6ccf20d0bc978f64'}}";

		String key = "ziKBIPZt4Db4m3twNPCM1eS2tmd4xVWoY3KjnvzDGVs=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|true|C4|false|B|C0|379536" + //
				"6d4f5cb870|C1|false|C2|5|2|7|C3|5707|C4|1|2|4|C|C0|20f9a7a6297a3971|C1" + //
				"|2939|C2|true|C3|2|4|1|C4|62e553d22974d6cf|D|C0|991d58516cc79d2|C1|770" + //
				"edd31a8d0659f|C2|48ee1dbfa22d5f5a|C3|8|1|5|C4|false|E|C0|9584|C1|true|" + //
				"C2|true|C3|true|C4|false|F|C0|false|C1|false|C2|false|C3|3561|C4|11551" + //
				"6b8fe3aa776|G|C0|true|C1|5955|C2|true|C3|true|C4|6|8|6|H|C0|6288|C1|fa" + //
				"lse|C2|34c12c7a88fad8b6|C3|true|C4|6265|I|C0|false|C1|7|5|9ziKBIPZt4Db" + //
				"4m3twNPCM1eS2tmd4xVWoY3KjnvzDGVs=";
		String[] keys2 = null;
		check(json, key, keys2, 516);

		key = "abc.def:A|C0|true|C1|true|C2|false|C3|true|C4|false|B|C0|379536" + //
				"6d4f5cb870|C1|false|C2|5|2|7|C3|5707|C4|1|2|4|C|C0|20f9a7a6297a3971|C1" + //
				"|2939|C2|true|C3|2|4|1|C4|62e553d22974d6cf|D|C0|991d58516cc79d2|C1|770" + //
				"edd31a8d0659f|C2|48ee1dbfa22d5f5a|C3|8|1|5|C4|false|E|C0|9584|C1|true|" + //
				"C2|true|C3|true|C4|false|F|C0|false|C1|false|C2|false|C3|3561|C4|11551" + //
				"6b8fe3aa776|G|C0|true|C1|5955|C2|true|C3|true|C4|6|8|6|H|C0|6288|C1|fa" + //
				"lse|C2|34c12c7a88fad8b6|C3|true|C4|6265|I|C0|false|C1|7|5|9|C2|400a7ac" + //
				"b91b4b216|C3|true|C4|true|J|C0|2|9|8|C1|25d4b733a22cc0f|C2|8731|C3|627" + //
				"4|C4|4d8a44c20248a7a5|K|C0|false|C1|false|C2|3638|C3|754c2ad95c3e40c1|" + //
				"C4|false|L|C0|2c70398b07f98c64|C1|4952df14e86b0895|C2|4c1284cf993ea09d" + //
				"|C3|false|C4|6171|M|C0|false|C1|false|C2|true|C3|78df452ddb737366|C4|8" + //
				"|2|3|N|C0|ziKBIPZt4Db4m3twNPCM1eS2tmd4xVWoY3KjnvzDGVs=";
		String[] keys3 = null;
		check(json, key, keys3, 817);

	}

	@Test
	public void testJ1() throws Exception {

		String json = "{'A':{'C0':2849,'C1':true,'C2':[1,7,0],'C3':false,'C4':5362}" + //
				",'B':{'C0':[1,4,2],'C1':false,'C2':true,'C3':5885,'C4':7676},'C':{'C0':'46abaa35dd239eaa'" + //
				",'C1':false,'C2':4338,'C3':false,'C4':[6,9,7]},'D':{'C0':false,'C1':'3f99e11e11da1593'" + //
				",'C2':'4915003e6464c05a','C3':[0,1,3],'C4':[5,5,2]},'E':{'C0':true,'C1':false" + //
				",'C2':[8,0,9],'C3':false,'C4':'13441052619aefce'},'F':{'C0':[9,4,6],'C1':7815" + //
				",'C2':false,'C3':[6,1,4],'C4':'7032d5bdcb88ca63'},'G':{'C0':false,'C1':[6" + //
				",2,2],'C2':'24a5e7ef2f392d0a','C3':[7,3,9],'C4':[4,9,6]},'H':{'C0':[9,3" + //
				",2],'C1':[8,4,6],'C2':true,'C3':true,'C4':false},'I':{'C0':[4,9,1],'C1':false" + //
				",'C2':true,'C3':[3,9,0],'C4':true},'J':{'C0':true,'C1':false,'C2':9842" + //
				",'C3':[1,6,1],'C4':'6cca98f7361c8945'},'K':{'C0':'9c41b537afb5af1','C1':5135" + //
				",'C2':true,'C3':'325aea7a82dad477','C4':[5,5,3]},'L':{'C0':false,'C1':'124035a0ae138ca8'" + //
				",'C2':1671,'C3':696,'C4':false},'M':{'C0':2284,'C1':'129d0b4342dfa161'" + //
				",'C2':[1,9,0],'C3':3587,'C4':false},'N':{'C0':[1,7,2],'C1':4501,'C2':[6" + //
				",4,6],'C3':false,'C4':'6bfefa8503804b26'},'O':{'C0':false,'C1':[9,4,6]" + //
				",'C2':'10da922380b6bd08','C3':true,'C4':736},'P':{'C0':true,'C1':4736,'C2':'62288a16a134ca15'" + //
				",'C3':false,'C4':[6,3,4]},'Q':{'C0':false,'C1':false,'C2':true,'C3':true" + //
				",'C4':true},'R':{'C0':true,'C1':[1,9,6],'C2':true,'C3':false,'C4':5517}" + //
				",'S':{'C0':true,'C1':false,'C2':'6143aa10068970c4','C3':true,'C4':false}" + //
				",'T':{'C0':6172,'C1':'461955b7dac43798','C2':[5,4,6],'C3':false,'C4':[7" + //
				",9,7]},'U':{'C0':true,'C1':'2318f8257bfdefa3','C2':'53208c6f160d7d02','C3':[0" + //
				",7,8],'C4':false},'V':{'C0':true,'C1':true,'C2':8337,'C3':[4,8,0],'C4':1546}" + //
				",'W':{'C0':[4,7,7],'C1':false,'C2':[7,1,6],'C3':668,'C4':false},'X':{'C0':[1" + //
				",5,5],'C1':1440,'C2':false,'C3':false,'C4':false},'Y':{'C0':9627,'C1':'26a101c21ccf6525'" + //
				",'C2':'25d63872d57ce8ff','C3':'3610806303fb4cfb','C4':true}}";

		String key = "cdfVZUJslh+wQU/Zs+vnb2+YF32gPP+pqB/wLZTbM/s=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|2849|C1|true|C2|1|7|0|C3|false|C4|5362|B|C0|1|4|2|" + //
				"C1|false|C2|true|C3|5885|C4|7676|C|C0|46abaa35dd239eaa|C1|false|C2|433" + //
				"8|C3|false|C4|6|9|7|D|C0|false|C1|3f99e11e11da1593|C2|4915003e6464c05a" + //
				"|C3|0|1|3|C4|5|5|2|E|C0|true|C1|false|C2|8|0|9|C3|false|C4|13441052619" + //
				"aefce|F|C0|9|4|6|C1|7815|C2|cdfVZUJslh+wQU/Zs+vnb2+YF32gPP+pqB/wLZTbM/" + //
				"s=";
		String[] keys2 = null;
		check(json, key, keys2, 345);

		key = "abc.def:A|C0|2849|C1|true|C2|1|7|0|C3|false|C4|5362|B|C0|1|4|2|" + //
				"C1|false|C2|true|C3|5885|C4|7676|C|C0|46abaa35dd239eaa|C1|false|C2|433" + //
				"8|C3|false|C4|6|9|7|D|C0|false|C1|3f99e11e11da1593|C2|4915003e6464c05a" + //
				"|C3|0|1|3|C4|5|5|2|E|C0|true|C1|false|C2|8|0|9|C3|false|C4|13441052619" + //
				"aefce|F|C0|9|4|6|C1|7815|C2|false|C3|6|1|4|C4|7032d5bdcb88ca63|G|C0|fa" + //
				"lse|C1|6|2|2|C2|24a5e7ef2f392d0a|C3|7|3|9|C4|4|9|6|H|C0|9|3|2|C1|8|4|6" + //
				"|C2|true|C3|true|C4|false|I|C0|4|9|1|C1|false|C2|true|C3|3|9|0|C4|true" + //
				"|J|C0|true|C1|false|C2|9842|C3|1|6|1|C4|6cca98f7361c8945|K|C0|9c41b537" + //
				"afb5af1|C1|5135|C2|true|C3|325aea7a82dad477|C4|5|5|3|L|C0|false|C1|12c" + //
				"dfVZUJslh+wQU/Zs+vnb2+YF32gPP+pqB/wLZTbM/s=";
		String[] keys3 = null;
		check(json, key, keys3, 666);

	}

	@Test
	public void testK1() throws Exception {

		String json = "{'A':{'C0':false,'C1':3730,'C2':false,'C3':true,'C4':6334}" + //
				",'B':{'C0':[9,7,0],'C1':[9,7,6],'C2':false,'C3':'262489397bde9627','C4':[6" + //
				",6,4]},'C':{'C0':[0,0,8],'C1':[8,8,9],'C2':false,'C3':'6e9d5ff2a4f0a377'" + //
				",'C4':191},'D':{'C0':false,'C1':'23d6ce1883e573fa','C2':3487,'C3':false" + //
				",'C4':true},'E':{'C0':true,'C1':[5,6,0],'C2':'4e502aad504a2ef4','C3':7813" + //
				",'C4':'2049fec8f2c10c47'},'F':{'C0':6847,'C1':[3,9,4],'C2':true,'C3':true" + //
				",'C4':true},'G':{'C0':[7,9,8],'C1':[6,4,3],'C2':'664799e85508f1a','C3':3826" + //
				",'C4':[7,8,5]},'H':{'C0':[1,2,8],'C1':true,'C2':3208,'C3':true,'C4':false}" + //
				",'I':{'C0':5013,'C1':'3a50e5554751a3f5','C2':false,'C3':true,'C4':[6,9" + //
				",8]},'J':{'C0':'53bee4d872965a0','C1':'794c1272713d7558','C2':false,'C3':'6c5aa9fe4e441786'" + //
				",'C4':false},'K':{'C0':'3accb73577f62a90','C1':'44a5fb74de7cdf4d','C2':true" + //
				",'C3':true,'C4':false},'L':{'C0':false,'C1':false,'C2':'6975a41a41b2d892'" + //
				",'C3':'44c172445a498a92','C4':'745e1b41e0fc016a'},'M':{'C0':false,'C1':'873fe6dff835506'" + //
				",'C2':'389650e89d1ef3e1','C3':[0,0,2],'C4':'3bd97fde173ebb74'},'N':{'C0':2039" + //
				",'C1':true,'C2':[9,6,5],'C3':9522,'C4':[3,1,7]},'O':{'C0':[0,5,8],'C1':2078" + //
				",'C2':[7,5,2],'C3':[9,1,9],'C4':[0,2,6]},'P':{'C0':4575,'C1':6578,'C2':'2fed42c437f6e2e5'" + //
				",'C3':4073,'C4':[6,2,6]},'Q':{'C0':false,'C1':'2fdc472343bd897c','C2':8428" + //
				",'C3':false,'C4':false},'R':{'C0':true,'C1':2988,'C2':true,'C3':true,'C4':[1" + //
				",5,2]},'S':{'C0':true,'C1':true,'C2':false,'C3':false,'C4':false},'T':{'C0':true" + //
				",'C1':8273,'C2':false,'C3':[7,4,0],'C4':true},'U':{'C0':'63e258d301130c21'" + //
				",'C1':'3bcad9ee1b204e5b','C2':true,'C3':'15034d8243fce3ca','C4':[4,9,0]}" + //
				",'V':{'C0':[5,6,9],'C1':[8,7,9],'C2':false,'C3':'91e995c50778731','C4':'1df23b4cb472b181'}" + //
				",'W':{'C0':'879362eff911eea','C1':false,'C2':false,'C3':[3,3,2],'C4':8488}" + //
				",'X':{'C0':true,'C1':false,'C2':[7,1,9],'C3':[1,1,4],'C4':[3,8,8]},'Y':{'C0':'7562495eb33b2fb0'" + //
				",'C1':'52ab10ab6221b68e','C2':false,'C3':false,'C4':[5,3,5]}}";

		String key = "ONNYf0euJA7QdMuU5T/C5FmXpkpO0e1DnOl1c5LNUqY=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|3730|C2|false|C3|true|C4|6334|B|C0|9|7|0|" + //
				"C1|9|7|6|C2|false|C3|262489397bde9627|C4|6|6|4|C|C0|0|0|8|C1|8|8|9|C2|" + //
				"false|C3|6e9d5ff2a4f0a377|C4|191|D|C0|false|C1|23d6ce1883e573fa|C2|348" + //
				"7|C3|false|C4|true|E|C0|true|C1|5|6|0|C2|4e502aad504a2eONNYf0euJA7QdMu" + //
				"U5T/C5FmXpkpO0e1DnOl1c5LNUqY=";
		String[] keys2 = null;
		check(json, key, keys2, 302);

		key = "abc.def:A|C0|false|C1|3730|C2|false|C3|true|C4|6334|B|C0|9|7|0|" + //
				"C1|9|7|6|C2|false|C3|262489397bde9627|C4|6|6|4|C|C0|0|0|8|C1|8|8|9|C2|" + //
				"false|C3|6e9d5ff2a4f0a377|C4|191|D|C0|false|C1|23d6ce1883e573fa|C2|348" + //
				"7|C3|false|C4|true|E|C0|true|C1|5|6|0|C2|4e502aad504a2ef4|C3|7813|C4|2" + //
				"049fec8f2c10c47|F|C0|6847|C1|3|9|4|C2|true|C3|true|C4|true|G|C0|7|9|8|" + //
				"C1|6|4|3|C2|664799e85508f1a|C3|3826|C4|7|8|5|H|C0|1|2|8|C1|true|C2|320" + //
				"8|C3|true|C4|false|I|C0|5013|C1|3a50e5554751a3f5|C2|false|C3|true|C4|6" + //
				"|9|8|J|C0|53bee4d872965a0|C1|794c1272713d7558|C2|false|C3|6c5aa9fe4e44" + //
				"1786|C4|false|K|C0|3accb73577f62a90|C1|44a5fb74de7cdf4d|C2|true|C3|tru" + //
				"e|C4|false|L|C0|false|C1|false|C2|697ONNYf0euJA7QdMuU5T/C5FmXpkpO0e1Dn" + //
				"Ol1c5LNUqY=";
		String[] keys3 = null;
		check(json, key, keys3, 704);

		key = "abc.def:A|C0|false|C1|3730|C2|false|C3|true|C4|6334|B|C0|9|7|0|" + //
				"C1|9|7|6|C2|false|C3|262489397bde9627|C4|6|6|4|C|C0|0|0|8|C1|8|8|9|C2|" + //
				"false|C3|6e9d5ff2a4f0a377|C4|191|D|C0|false|C1|23d6ce1883e573fa|C2|348" + //
				"7|C3|false|C4|true|E|C0|true|C1|5|6|0|C2|4e502aad504a2ef4|C3|7813|C4|2" + //
				"049fec8f2c10c47|F|C0|6847|C1|3|9|4|C2|true|C3|true|C4|true|G|C0|7|9|8|" + //
				"C1|6|4|3|C2|664799e85508f1a|C3|3826|C4|7|8|5|H|C0|1|2|8|C1|true|C2|320" + //
				"8|C3|true|C4|false|I|C0|5013|C1|3a50e5554751a3f5|C2|false|C3|true|C4|6" + //
				"|9|8|J|C0|53bee4d872965a0|C1|794c1272713d7558|C2|false|C3|6c5aa9fe4e44" + //
				"1786|C4|false|K|C0|3accb73577f62a90|C1|44a5fb74de7cdf4d|C2|true|C3|tru" + //
				"e|C4|false|L|C0|false|C1|false|C2|6975a41a41b2d89ONNYf0euJA7QdMuU5T/C5" + //
				"FmXpkpO0e1DnOl1c5LNUqY=";
		String[] keys4 = null;
		check(json, key, keys4, 716);

	}

	@Test
	public void testL1() throws Exception {

		String json = "{'A':{'C0':'7cda3b31fef14118','C1':3153,'C2':'179ea68f100bff0f'" + //
				",'C3':8545,'C4':[1,8,0]},'B':{'C0':true,'C1':false,'C2':5283,'C3':false" + //
				",'C4':[4,3,7]},'C':{'C0':'69bdbc556ecdc858','C1':false,'C2':true,'C3':[3" + //
				",5,4],'C4':[7,2,8]},'D':{'C0':true,'C1':false,'C2':true,'C3':'233b13df95f8edba'" + //
				",'C4':2113},'E':{'C0':true,'C1':6946,'C2':1772,'C3':true,'C4':8383},'F':{'C0':'5998f8ec6fbcabd5'" + //
				",'C1':[2,9,6],'C2':'1007338b86d23227','C3':[5,8,9],'C4':true},'G':{'C0':[3" + //
				",5,1],'C1':true,'C2':4634,'C3':true,'C4':false},'H':{'C0':true,'C1':'4fb3a938e365a86'" + //
				",'C2':'b9483f9e173a8eb','C3':true,'C4':false},'I':{'C0':false,'C1':'2100356c387c0572'" + //
				",'C2':false,'C3':true,'C4':'a13065abe367988'},'J':{'C0':[5,8,8],'C1':false" + //
				",'C2':5804,'C3':'78d98d25df1fc7fb','C4':'1ce3449411e9e8'},'K':{'C0':8668" + //
				",'C1':6450,'C2':false,'C3':false,'C4':6138},'L':{'C0':false,'C1':true,'C2':true" + //
				",'C3':[3,1,8],'C4':'6854ba9d6a520e9c'},'M':{'C0':true,'C1':false,'C2':true" + //
				",'C3':false,'C4':[2,7,0]},'N':{'C0':false,'C1':[2,5,2],'C2':'357ed41a5fc112aa'" + //
				",'C3':[5,9,4],'C4':true},'O':{'C0':true,'C1':1375,'C2':'73a06725ca6cad1c'" + //
				",'C3':[6,0,2],'C4':7887},'P':{'C0':false,'C1':'7e51e86d996694fa','C2':true" + //
				",'C3':false,'C4':true},'Q':{'C0':'73175d2345ff26e','C1':[8,7,7],'C2':false" + //
				",'C3':true,'C4':false},'R':{'C0':386,'C1':1088,'C2':'58a26fa7286c19be'" + //
				",'C3':'916b22718b1186c','C4':'544376702b52b516'},'S':{'C0':false,'C1':false" + //
				",'C2':[4,5,1],'C3':'3850e3d404d03d8b','C4':true},'T':{'C0':false,'C1':888" + //
				",'C2':'3c561eb7441c632b','C3':'1f7a9f30f840f179','C4':4737},'U':{'C0':[0" + //
				",5,5],'C1':true,'C2':true,'C3':6700,'C4':'2d637d70db936bfd'},'V':{'C0':[4" + //
				",7,5],'C1':false,'C2':8569,'C3':9354,'C4':false},'W':{'C0':'3d35fcdc12b87af3'" + //
				",'C1':'4a63e710eb4e563c','C2':5408,'C3':'702665dc2c11a1aa','C4':false}" + //
				",'X':{'C0':[8,9,6],'C1':true,'C2':true,'C3':[0,3,6],'C4':false},'Y':{'C0':'276b4b13f616ac89'" + //
				",'C1':1872,'C2':false,'C3':'73a0216d3590d8d9','C4':7018}}";

		String key = "UERXo5qqzAf/XXj/gupj6/rC7GoQzDBH0gasNXRZrG8=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|7cda3b31fef14118|C1|3153|C2|179ea68f100bff0f|C3|85" + //
				"45|C4|1|8|0|B|C0|true|C1|false|C2|5283|C3|false|C4|4|3|7|C|C0|69bdbc55" + //
				"6ecdc858|C1|false|C2|true|C3|3|5|4|C4|7|2|8|D|C0|true|C1|false|C2|true" + //
				"|C3|233b13df95f8edba|C4|2113|E|C0|true|C1|6946|C2|1772|C3|true|C4|8383" + //
				"|F|C0|5998f8ec6fbcabd5|C1|2|9|6|C2|1007338b86d23227|C3|5|8|9|C4|true|G" + //
				"|C0|3|5|1|C1|true|C2|4634|C3|true|C4|false|H|C0|true|C1|4fb3a938e365a8" + //
				"6|C2|b9483f9e173a8eb|C3|true|C4|false|I|C0|false|C1|2100356c38UERXo5qq" + //
				"zAf/XXj/gupj6/rC7GoQzDBH0gasNXRZrG8=";
		String[] keys2 = null;
		check(json, key, keys2, 519);

		key = "abc.def:A|C0|7cda3b31fef14118|C1|3153|C2|179ea68f100bff0f|C3|85" + //
				"45|C4|1|8|0|B|C0|true|C1|false|C2|5283|C3|false|C4|4|3|7|C|C0|69bdbc55" + //
				"6ecdc858|C1|false|C2|true|C3|3|5|4|C4|7|2|8|D|C0|true|C1|false|C2|true" + //
				"|C3|233b13df95f8edba|C4|2113|E|C0|true|C1|6946|C2|1772|C3|true|C4|8383" + //
				"|F|C0|5998f8ec6fbcabd5|C1|2|9|6|C2|1007338b86d23227|C3|5|8|9|C4|true|G" + //
				"|C0|3|5|1|C1|true|C2|4634|C3|true|C4|false|H|C0|true|C1|4fb3a938e365a8" + //
				"6|C2|b9483f9e173a8eb|C3|true|C4|false|I|C0|false|C1|2100356c387c0572|C" + //
				"2|false|C3|true|C4|a13065abe367988|J|C0|5|8|8|C1|false|C2|5804|C3|78d9" + //
				"8d25df1fc7fb|C4|1ce3449411e9e8|K|C0|8668|C1|6450|C2|false|C3|false|C4|" + //
				"6138|L|C0|false|C1|true|C2|true|C3|3|1|8|C4|6854ba9d6a520e9c|M|C0|true" + //
				"|C1|false|C2|true|C3|false|C4|2|7|0|N|C0|false|C1|2|5|2|C2|357ed41a5fc" + //
				"112aa|C3|5|9|4|C4|true|O|C0|true|C1|1375|C2|73a06725ca6cad1c|C3|6|0|2|" + //
				"C4|7887|P|C0|false|C1|7e51e86d996694fa|C2|true|UERXo5qqzAf/XXj/gupj6/r" + //
				"C7GoQzDBH0gasNXRZrG8=";
		String[] keys3 = null;
		check(json, key, keys3, 924);

	}

	@Test
	public void testM1() throws Exception {

		String json = "{'A':{'C0':false,'C1':'3ac0c68d9e2f43f5','C2':439,'C3':true" + //
				",'C4':false},'B':{'C0':[3,0,8],'C1':false,'C2':false,'C3':false,'C4':true}" + //
				",'C':{'C0':true,'C1':true,'C2':98,'C3':5456,'C4':true},'D':{'C0':[8,6,9]" + //
				",'C1':'5b4e5327a0e7c34d','C2':true,'C3':[7,2,2],'C4':'4d4a7633a50595ed'}" + //
				",'E':{'C0':'1ae4ef90a4f7e1de','C1':true,'C2':'53590fa819912f72','C3':false" + //
				",'C4':993},'F':{'C0':[9,4,3],'C1':false,'C2':'287a99f3d94fa73a','C3':'428f0c532fd5cdff'" + //
				",'C4':8894},'G':{'C0':2427,'C1':true,'C2':false,'C3':851,'C4':7245},'H':{'C0':'4b020bb6f464e12a'" + //
				",'C1':[6,3,8],'C2':[5,8,4],'C3':'71b2a9c57b3e4a43','C4':[0,0,3]},'I':{'C0':[0" + //
				",5,9],'C1':[7,1,8],'C2':'1a5c5dbe1c9a949b','C3':false,'C4':[9,7,0]},'J':{'C0':false" + //
				",'C1':false,'C2':'f023fe7af0212df','C3':true,'C4':false},'K':{'C0':7002" + //
				",'C1':'26f8c77b7d7fe37c','C2':7665,'C3':true,'C4':'49c8a87ecab09692'},'L':{'C0':false" + //
				",'C1':5962,'C2':false,'C3':[0,2,3],'C4':[3,4,7]},'M':{'C0':[6,8,3],'C1':6791" + //
				",'C2':true,'C3':true,'C4':true},'N':{'C0':6812,'C1':[7,3,0],'C2':false" + //
				",'C3':'563b935dff8fad7d','C4':'18bd6eb3eed2e274'},'O':{'C0':'2d22e1f96d4d13fe'" + //
				",'C1':[0,3,0],'C2':true,'C3':1151,'C4':9625},'P':{'C0':false,'C1':true" + //
				",'C2':2078,'C3':true,'C4':789},'Q':{'C0':'2f2a32dfb13a9a52','C1':1522,'C2':'11f91da77dff17c5'" + //
				",'C3':560,'C4':true},'R':{'C0':'59e0a076161c6c7b','C1':true,'C2':[8,6,6]" + //
				",'C3':[8,8,1],'C4':false},'S':{'C0':false,'C1':'391bce246d143fef','C2':2576" + //
				",'C3':[8,8,2],'C4':[5,9,6]},'T':{'C0':6186,'C1':true,'C2':9556,'C3':[2" + //
				",3,5],'C4':7641},'U':{'C0':false,'C1':'5e9d240f52c85e2e','C2':[2,6,9],'C3':true" + //
				",'C4':[7,9,3]},'V':{'C0':true,'C1':true,'C2':true,'C3':false,'C4':true}" + //
				",'W':{'C0':[3,2,6],'C1':true,'C2':'6883621d4948e43','C3':8099,'C4':[5,9" + //
				",4]},'X':{'C0':7585,'C1':'582cabbb88add0a9','C2':[4,5,9],'C3':2618,'C4':true}" + //
				",'Y':{'C0':2408,'C1':'6525db55f35a186f','C2':'66aa706aaee480c1','C3':[3" + //
				",2,1],'C4':8113}}";

		String key = "IEN7wg918b/eOsy2NtOdR69qNA0Cgj3kH2L1vmhU9uw=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|3ac0c68d9e2f43f5|C2|439|C3|true|C4|false|" + //
				"B|C0|3|0|8|C1|false|C2|false|C3|false|C4|true|C|C0|true|C1|true|C2|98|" + //
				"C3|5456|C4|true|D|C0|8|6|9|C1|5b4e5327a0e7c34d|C2|true|C3|7|2|2|C4|4d4" + //
				"a7633a50595ed|E|C0|1ae4ef90a4f7e1de|C1|true|C2|53590fa819912f72|C3|fal" + //
				"se|C4|993|F|C0|9|4|3|C1|false|C2|287a99f3d94fa73a|C3|428f0c5IEN7wg918b" + //
				"/eOsy2NtOdR69qNA0Cgj3kH2L1vmhU9uw=";
		String[] keys2 = null;
		check(json, key, keys2, 377);

		key = "abc.def:A|C0|false|C1|3ac0c68d9e2f43f5|C2|439|C3|true|C4|false|" + //
				"B|C0|3|0|8|C1|false|C2|false|C3|false|C4|true|C|C0|true|C1|true|C2|98|" + //
				"C3|5456|C4|true|D|C0|8|6|9|C1|5b4e5327a0e7c34d|C2|true|C3|7|2|2|C4|4d4" + //
				"a7633a50595ed|E|C0|1ae4ef90a4f7e1de|C1|true|C2|53590fa819912f72|C3|fal" + //
				"se|C4|993|F|C0|9|4|3|C1|false|C2|287a99f3d94fa73a|C3|428f0c532fd5cdff|" + //
				"C4|8894|G|C0|2427|C1|true|C2|false|C3|851|C4|7245|H|C0|4b020bb6f464e12" + //
				"a|C1|6|3|8|C2IEN7wg918b/eOsy2NtOdR69qNA0Cgj3kH2L1vmhU9uw=";
		String[] keys3 = null;
		check(json, key, keys3, 470);

		key = "abc.def:A|C0|false|C1|3ac0c68d9e2f43f5|C2|439|C3|true|C4|false|" + //
				"B|C0|3|0|8|C1|false|C2|false|C3|false|C4|true|C|C0|true|C1|true|C2|98|" + //
				"C3|5456|C4|true|D|C0|8|6|9|C1|5b4e5327a0e7c34d|C2|true|C3|7|2|2|C4|4d4" + //
				"a7633a50595ed|E|C0|1ae4ef90a4f7e1de|C1|true|C2|53590fa819912f72|C3|fal" + //
				"se|C4|993|F|C0|9|4|3|C1|false|C2|287a99f3d94fa73a|C3|428f0c532fd5cdff|" + //
				"C4|8894|G|C0|2427|C1|true|C2|false|C3|851|C4|7245|H|C0|4b020bb6f464e12" + //
				"a|C1|6|3|8|C2|5|8|4|C3|71b2a9c57b3e4a43|C4|0|0|3|I|C0|0|5|9|C1|7|1|8|C" + //
				"2|1a5c5dbe1c9a949b|C3|false|C4|9|7|0|J|C0|false|C1|false|C2|f023fe7af0" + //
				"212df|C3|true|C4|false|K|C0|7002|C1|26f8c77b7d7fe37c|C2|7665|C3|true|C" + //
				"4|49c8a87ecab09692|L|IEN7wg918b/eOsy2NtOdR69qNA0Cgj3kH2L1vmhU9uw=";
		String[] keys4 = null;
		check(json, key, keys4, 688);

		key = "abc.def:A|C0|false|C1|3ac0c68d9e2f43f5|C2|439|C3|true|C4|false|" + //
				"B|C0|3|0|8|C1|false|C2|false|C3|false|C4|true|C|C0|true|C1|true|C2|98|" + //
				"C3|5456|C4|true|D|C0|8|6|9|C1|5b4e5327a0e7c34d|C2|true|C3|7|2|2|C4|4d4" + //
				"a7633a50595ed|E|C0|1ae4ef90a4f7e1de|C1|true|C2|53590fa819912f72|C3|fal" + //
				"se|C4|993|F|C0|9|4|3|C1|false|C2|287a99f3d94fa73a|C3|428f0c532fd5cdff|" + //
				"C4|8894|G|C0|2427|C1|true|C2|false|C3|851|C4|7245|H|C0|4b020bb6f464e12" + //
				"a|C1|6|3|8|C2|5|8|4|C3|71b2a9c57b3e4a43|C4|0|0|3|I|C0|0|5|9|C1|7|1|8|C" + //
				"2|1a5c5dbe1c9a949b|C3|false|C4|9|7|0|J|C0|false|C1|false|C2|f023fe7af0" + //
				"212df|C3|true|C4|false|K|C0|7002|C1|26f8c77b7d7fe37c|C2|7665|C3|true|C" + //
				"4|49c8a87ecab09692|L|C0|false|C1|5962|C2|false|C3|0|2|3|C4|3|4|7|M|C0|" + //
				"6|8|3|C1|6791|C2|true|C3|true|C4|true|N|C0|6812|C1|7|3|0|C2|false|C3|5" + //
				"63b935dff8fad7d|C4|18bd6eb3eed2e274|O|C0|2d22e1f96d4d13fe|C1|0|3|0|C2|" + //
				"true|C3|1151|C4|9625|P|C0|false|C1|trIEN7wg918b/eOsy2NtOdR69qNA0Cgj3kH" + //
				"2L1vmhU9uw=";
		String[] keys5 = null;
		check(json, key, keys5, 914);

		key = "abc.def:A|C0|false|C1|3ac0c68d9e2f43f5|C2|439|C3|true|C4|false|" + //
				"B|C0|3|0|8|C1|false|C2|false|C3|false|C4|true|C|C0|true|C1|true|C2|98|" + //
				"C3|5456|C4|true|D|C0|8|6|9|C1|5b4e5327a0e7c34d|C2|true|C3|7|2|2|C4|4d4" + //
				"a7633a50595ed|E|C0|1ae4ef90a4f7e1de|C1|true|C2|53590fa819912f72|C3|fal" + //
				"se|C4|993|F|C0|9|4|3|C1|false|C2|287a99f3d94fa73a|C3|428f0c532fd5cdff|" + //
				"C4|8894|G|C0|2427|C1|true|C2|false|C3|851|C4|7245|H|C0|4b020bb6f464e12" + //
				"a|C1|6|3|8|C2|5|8|4|C3|71b2a9c57b3e4a43|C4|0|0|3|I|C0|0|5|9|C1|7|1|8|C" + //
				"2|1a5c5dbe1c9a949b|C3|false|C4|9|7|0|J|C0|false|C1|false|C2|f023fe7af0" + //
				"212df|C3|true|C4|false|K|C0|7002|C1|26f8c77b7d7fe37c|C2|7665|C3|true|C" + //
				"4|49c8a87ecab09692|L|C0|false|C1|5962|C2|false|C3|0|2|3|C4|3|4|7|M|C0|" + //
				"6|8|3|C1|6791|C2|true|C3|true|C4|true|N|C0|6812|C1|7|3|0|C2|false|C3|5" + //
				"63b935dff8fad7d|C4|18bd6eb3eed2e274|O|C0|2d22e1f96d4d13fe|C1|0|3|0|C2|" + //
				"true|C3|1151|C4|9625|P|C0|false|C1|true|C2|2078|C3|true|C4|789|Q|C0|2f" + //
				"2a32dfIEN7wg918b/eOsy2NtOdR69qNA0Cgj3kH2L1vmhU9uw=";
		String[] keys6 = null;
		check(json, key, keys6, 953);

	}

	@Test
	public void testN1() throws Exception {

		String json = "{'A':{'C0':true,'C1':[7,9,0],'C2':false,'C3':1747,'C4':false}" + //
				",'B':{'C0':'355b3a4646a41ea0','C1':[2,4,9],'C2':4190,'C3':'7752f2e2919441ec'" + //
				",'C4':4517},'C':{'C0':[8,8,3],'C1':[3,1,4],'C2':[4,0,7],'C3':true,'C4':5023}" + //
				",'D':{'C0':'6531cab37406811e','C1':false,'C2':8473,'C3':'64789a92f2e61eb'" + //
				",'C4':[8,6,7]},'E':{'C0':9517,'C1':false,'C2':true,'C3':[2,0,0],'C4':false}" + //
				",'F':{'C0':true,'C1':'6375291172274e07','C2':'7c01f05a92fd131f','C3':[2" + //
				",5,7],'C4':2626},'G':{'C0':6953,'C1':4416,'C2':[3,8,2],'C3':true,'C4':true}" + //
				",'H':{'C0':'3196724e44a9eb1b','C1':'14ea6316504df7ca','C2':'3c6e7ba87c0da8af'" + //
				",'C3':[3,6,9],'C4':false},'I':{'C0':false,'C1':false,'C2':8491,'C3':'6221e35033cd371f'" + //
				",'C4':2951},'J':{'C0':4381,'C1':[2,9,4],'C2':8657,'C3':[1,6,4],'C4':2933}" + //
				",'K':{'C0':false,'C1':1599,'C2':1350,'C3':9602,'C4':'6668f5713a7af1df'}" + //
				",'L':{'C0':'2a9bca13517d59e9','C1':[1,5,3],'C2':false,'C3':true,'C4':[3" + //
				",0,6]},'M':{'C0':9976,'C1':[4,3,7],'C2':true,'C3':false,'C4':[8,7,8]},'N':{'C0':false" + //
				",'C1':4793,'C2':'64e9fe3eed58a137','C3':false,'C4':false},'O':{'C0':'90c9273cfdd6104'" + //
				",'C1':false,'C2':[6,9,2],'C3':[3,2,7],'C4':'62779ac9da6516d'},'P':{'C0':1903" + //
				",'C1':9283,'C2':6369,'C3':[2,2,8],'C4':[1,2,7]},'Q':{'C0':[4,2,0],'C1':false" + //
				",'C2':9862,'C3':true,'C4':[2,3,4]},'R':{'C0':'113a8e909a778b12','C1':5792" + //
				",'C2':[5,1,5],'C3':7536,'C4':false},'S':{'C0':'39d73841f39ea4da','C1':false" + //
				",'C2':'5cc358e3530da03f','C3':'64dcfe4cd2069554','C4':true},'T':{'C0':5373" + //
				",'C1':2798,'C2':'26476f97f2efcd78','C3':'2d755b1abed1cc02','C4':false}" + //
				",'U':{'C0':'3721e83cb9f6f1b9','C1':'43fb7261d59d028b','C2':'326024851a24982d'" + //
				",'C3':true,'C4':'7b507d0dc411bd60'},'V':{'C0':3346,'C1':[3,6,4],'C2':true" + //
				",'C3':8875,'C4':8065},'W':{'C0':[1,1,9],'C1':true,'C2':true,'C3':6202,'C4':false}" + //
				",'X':{'C0':6416,'C1':[4,0,0],'C2':false,'C3':false,'C4':true},'Y':{'C0':'2ef299fbfc1f3fbb'" + //
				",'C1':1168,'C2':777,'C3':[9,8,4],'C4':false}}";

		String key = "/Pmz0v0EO1W72t//9mt+vcDodItC/dFwUMn+eR3EHSI=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|true|C1|7|9|0|C2|false|C3|1747|C4|false|B|C0|355b3" + //
				"a4646a41ea0|C1|2|4|9|C2|4190|C3|7752f2e2919441ec|C4|4517|C|C0|8|8|3|C1" + //
				"|3|1|4|C2|4|0|7|C3|true|C4|5023|D|C0|6531cab37406811e|C1|false|C2|8473" + //
				"|C3|64789a92f2e61eb|C4|8|6|7|E|C0|9517|C1|false|C2|true|C3|2|0|0|C4|fa" + //
				"lse|F|C0|true|C1|6375291172274e07|C2|7c01f05a/Pmz0v0EO1W72t//9mt+vcDod" + //
				"ItC/dFwUMn+eR3EHSI=";
		String[] keys2 = null;
		check(json, key, keys2, 362);

		key = "abc.def:A|C0|true|C1|7|9|0|C2|false|C3|1747|C4|false|B|C0|355b3" + //
				"a4646a41ea0|C1|2|4|9|C2|4190|C3|7752f2e2919441ec|C4|4517|C|C0|8|8|3|C1" + //
				"|3|1|4|C2|4|0|7|C3|true|C4|5023|D|C0|6531cab37406811e|C1|false|C2|8473" + //
				"|C3|64789a92f2e61eb|C4|8|6|7|E|C0|9517|C1|false|C2|true|C3|2|0|0|C4|fa" + //
				"lse|F|C0|true|C1|6375291172274e07|C2|7c01f05a92fd131f|C3|2|5|7|/Pmz0v0" + //
				"EO1W72t//9mt+vcDodItC/dFwUMn+eR3EHSI=";
		String[] keys3 = null;
		check(json, key, keys3, 380);

		key = "abc.def:A|C0|true|C1|7|9|0|C2|false|C3|1747|C4|false|B|C0|355b3" + //
				"a4646a41ea0|C1|2|4|9|C2|4190|C3|7752f2e2919441ec|C4|4517|C|C0|8|8|3|C1" + //
				"|3|1|4|C2|4|0|7|C3|true|C4|5023|D|C0|6531cab37406811e|C1|false|C2|8473" + //
				"|C3|64789a92f2e61eb|C4|8|6|7|E|C0|9517|C1|false|C2|true|C3|2|0|0|C4|fa" + //
				"lse|F|C0|true|C1|6375291172274e07|C2|7c01f05a92fd131f|C3|2|5|7|C4|2626" + //
				"|G|C0|6953|C1|4416|C2|3|8|2|C3|true|C4|true|H|C0|3196724e44a9eb1b|C1|1" + //
				"4ea6316504df7ca|C2|3c6e7ba87c0da8af|C3|3|6|9|C4|false|I|C0|false|C1|fa" + //
				"lse|C2|8491|C3|6221e35033cd371f|C4|2951|J|C0|4381|C1|2|9|4|C2|8657|C3|" + //
				"1|6|4|C4|2933|K|C0|false|C1|1599|C2|1350|C3|9602|C4|/Pmz0v0EO1W72t//9m" + //
				"t+vcDodItC/dFwUMn+eR3EHSI=";
		String[] keys4 = null;
		check(json, key, keys4, 649);

		key = "abc.def:A|C0|true|C1|7|9|0|C2|false|C3|1747|C4|false|B|C0|355b3" + //
				"a4646a41ea0|C1|2|4|9|C2|4190|C3|7752f2e2919441ec|C4|4517|C|C0|8|8|3|C1" + //
				"|3|1|4|C2|4|0|7|C3|true|C4|5023|D|C0|6531cab37406811e|C1|false|C2|8473" + //
				"|C3|64789a92f2e61eb|C4|8|6|7|E|C0|9517|C1|false|C2|true|C3|2|0|0|C4|fa" + //
				"lse|F|C0|true|C1|6375291172274e07|C2|7c01f05a92fd131f|C3|2|5|7|C4|2626" + //
				"|G|C0|6953|C1|4416|C2|3|8|2|C3|true|C4|true|H|C0|3196724e44a9eb1b|C1|1" + //
				"4ea6316504df7ca|C2|3c6e7ba87c0da8af|C3|3|6|9|C4|false|I|C0|false|C1|fa" + //
				"lse|C2|8491|C3|6221e35033cd371f|C4|2951|J|C0|4381|C1|2|9|4|C2|8657|C3|" + //
				"1|6|4|C4|2933|K|C0|false|C1|1599|C2|1350|C3|9602|C4|6668f5713a7af1df|L" + //
				"|C0|2a9bca13517d59e9|C1|1|5|3|C2|false|C3|true|C4|3|0|6|M|C0|9976|C1|4" + //
				"|3|7|C2|true|C3|false|C4|8|7|8|N|C0|false|C1|4793|C2|64e9fe3eed58a137|" + //
				"C3|false|C4|false|O|C0|90c9273cfdd6104|C1|false|C2|6|9|2|C3|3|2|7|C4|6" + //
				"2779ac9da6516d|P|C0|1903|C1|9283|C2|6369|C3|2|2|8|C4|1|2|7|Q|C0|4|2|0|" + //
				"C1|false|C2|9862|C3|true|C4|2|3|4|R|C0|/Pmz0v0EO1W72t//9mt+vcDodItC/dF" + //
				"wUMn+eR3EHSI=";
		String[] keys5 = null;
		check(json, key, keys5, 986);

	}

	@Test
	public void testO1() throws Exception {

		String json = "{'A':{'C0':'79ab57ae7728c3e0','C1':[7,5,2],'C2':true,'C3':[1" + //
				",3,1],'C4':false},'B':{'C0':true,'C1':6191,'C2':[7,5,3],'C3':false,'C4':'1d8e530988f54c2a'}" + //
				",'C':{'C0':true,'C1':[7,8,5],'C2':false,'C3':'6c0099fb212be57d','C4':false}" + //
				",'D':{'C0':true,'C1':false,'C2':false,'C3':true,'C4':[8,3,0]},'E':{'C0':'3408104935a28f73'" + //
				",'C1':true,'C2':'63d4f2149c4a9ec1','C3':'6398a0f5b5cde7bd','C4':'5b5198d2ced0a83f'}" + //
				",'F':{'C0':true,'C1':false,'C2':8222,'C3':[1,8,0],'C4':4476},'G':{'C0':true" + //
				",'C1':[3,3,6],'C2':false,'C3':false,'C4':[6,0,1]},'H':{'C0':'493ef4eb609277'" + //
				",'C1':false,'C2':true,'C3':[1,2,5],'C4':4609},'I':{'C0':4913,'C1':[6,0" + //
				",0],'C2':'12ae405919b842c3','C3':5336,'C4':false},'J':{'C0':true,'C1':[9" + //
				",8,8],'C2':'d3f585c97e45ff9','C3':'de27b8d71766757','C4':true},'K':{'C0':true" + //
				",'C1':'4882f2a7a33a45d1','C2':true,'C3':4837,'C4':'4e9b45415db2dfba'},'L':{'C0':'2fcdd6cdbe3e42d3'" + //
				",'C1':[4,9,5],'C2':true,'C3':6282,'C4':true},'M':{'C0':[0,2,0],'C1':false" + //
				",'C2':[0,7,2],'C3':[6,2,6],'C4':true},'N':{'C0':[1,3,3],'C1':false,'C2':[7" + //
				",5,8],'C3':8319,'C4':8575},'O':{'C0':true,'C1':false,'C2':true,'C3':'a6e405f8bbae46e'" + //
				",'C4':[3,4,2]},'P':{'C0':[0,6,0],'C1':4712,'C2':792,'C3':[4,8,1],'C4':'45d497fc0e5ea960'}" + //
				",'Q':{'C0':false,'C1':false,'C2':[1,2,3],'C3':4633,'C4':[9,2,5]},'R':{'C0':true" + //
				",'C1':[7,5,0],'C2':true,'C3':true,'C4':true},'S':{'C0':false,'C1':true" + //
				",'C2':4490,'C3':true,'C4':5519},'T':{'C0':4660,'C1':'440564639a9521bf'" + //
				",'C2':false,'C3':2213,'C4':'2e7c1ffee21c8817'},'U':{'C0':true,'C1':4965" + //
				",'C2':false,'C3':[0,3,4],'C4':false},'V':{'C0':9346,'C1':'2efc45b741999990'" + //
				",'C2':8245,'C3':false,'C4':4248},'W':{'C0':false,'C1':[2,2,8],'C2':7016" + //
				",'C3':2036,'C4':[1,4,1]},'X':{'C0':[5,3,4],'C1':false,'C2':763,'C3':[6" + //
				",8,9],'C4':true},'Y':{'C0':false,'C1':true,'C2':true,'C3':true,'C4':6709" + //
				"}}";

		String key = "aHA2FIE7m837/znS/hNt3i4oEK1/ASnP7kxajCIe8U8=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|79ab57ae7728c3e0|C1|7|5|2|C2|true|C3|1|3|1|C4|fals" + //
				"e|B|C0|true|C1|6191|C2|7|5|3|aHA2FIE7m837/znS/hNt3i4oEK1/ASnP7kxajCIe8" + //
				"U8=";
		String[] keys2 = null;
		check(json, key, keys2, 136);

		key = "abc.def:A|C0|79ab57ae7728c3e0|C1|7|5|2|C2|true|C3|1|3|1|C4|fals" + //
				"e|B|C0|true|C1|6191|C2|7|5|3|C3|false|C4|1d8e530988f54c2a|C|C0|true|C1" + //
				"|7|8|5|C2|false|C3|6c0099fb212be57d|C4|false|D|C0|true|C1|false|C2|fal" + //
				"se|C3|true|C4|8|3|0|E|C0|3408104935a28f73|C1|true|C2|63d4f2149c4a9ec1|" + //
				"C3|6398a0f5b5cde7bd|C4|5b5198d2ced0a83f|F|C0|true|C1|false|C2|8222|C3|" + //
				"1|8|0|C4|4476|G|C0|true|C1|3|3|6|C2|false|C3|false|C4|6|0|1|H|C0|493ef" + //
				"4eb609277|C1|false|C2|true|C3|1|2|5|C4|4609|I|C0|4913|C1|6|0|0|C2|12ae" + //
				"405919b842c3|C3|5336|C4|false|J|C0|true|C1|9|8|8|C2|d3f585c97e45ff9|C3" + //
				"|de27b8d71766757|C4|aHA2FIE7m837/znS/hNt3i4oEK1/ASnP7kxajCIe8U8=";
		String[] keys3 = null;
		check(json, key, keys3, 617);

		key = "abc.def:A|C0|79ab57ae7728c3e0|C1|7|5|2|C2|true|C3|1|3|1|C4|fals" + //
				"e|B|C0|true|C1|6191|C2|7|5|3|C3|false|C4|1d8e530988f54c2a|C|C0|true|C1" + //
				"|7|8|5|C2|false|C3|6c0099fb212be57d|C4|false|D|C0|true|C1|false|C2|fal" + //
				"se|C3|true|C4|8|3|0|E|C0|3408104935a28f73|C1|true|C2|63d4f2149c4a9ec1|" + //
				"C3|6398a0f5b5cde7bd|C4|5b5198d2ced0a83f|F|C0|true|C1|false|C2|8222|C3|" + //
				"1|8|0|C4|4476|G|C0|true|C1|3|3|6|C2|false|C3|false|C4|6|0|1|H|C0|493ef" + //
				"4eb609277|C1|false|C2|true|C3|1|2|5|C4|4609|I|C0|4913|C1|6|0|0|C2|12ae" + //
				"405919b842c3|C3|5336|C4|false|J|C0|true|C1|9|8|8|C2|d3f585c97e45ff9|C3" + //
				"|de27b8d71766757|C4|true|K|C0|true|C1|4882f2a7a33a45d1|C2|true|C3|4837" + //
				"|C4|4e9b45415db2dfba|L|C0|2fcdd6cdbe3e42d3|C1|4|9|5|C2|true|C3|6282|C4" + //
				"|true|M|C0|0|2|0|C1|false|C2|0|7|2|C3|6|2|6|C4|true|N|C0|1|3aHA2FIE7m8" + //
				"37/znS/hNt3i4oEK1/ASnP7kxajCIe8U8=";
		String[] keys4 = null;
		check(json, key, keys4, 797);

		key = "abc.def:A|C0|79ab57ae7728c3e0|C1|7|5|2|C2|true|C3|1|3|1|C4|fals" + //
				"e|B|C0|true|C1|6191|C2|7|5|3|C3|false|C4|1d8e530988f54c2a|C|C0|true|C1" + //
				"|7|8|5|C2|false|C3|6c0099fb212be57d|C4|false|D|C0|true|C1|false|C2|fal" + //
				"se|C3|true|C4|8|3|0|E|C0|3408104935a28f73|C1|true|C2|63d4f2149c4a9ec1|" + //
				"C3|6398a0f5b5cde7bd|C4|5b5198d2ced0a83f|F|C0|true|C1|false|C2|8222|C3|" + //
				"1|8|0|C4|4476|G|C0|true|C1|3|3|6|C2|false|C3|false|C4|6|0|1|H|C0|493ef" + //
				"4eb609277|C1|false|C2|true|C3|1|2|5|C4|4609|I|C0|4913|C1|6|0|0|C2|12ae" + //
				"405919b842c3|C3|5336|C4|false|J|C0|true|C1|9|8|8|C2|d3f585c97e45ff9|C3" + //
				"|de27b8d71766757|C4|true|K|C0|true|C1|4882f2a7a33a45d1|C2|true|C3|4837" + //
				"|C4|4e9b45415db2dfba|L|C0|2fcdd6cdbe3e42d3|C1|4|9|5|C2|true|C3|6282|C4" + //
				"|true|M|C0|0|2|0|C1|false|C2|0|7|2|C3|6|2|6|C4|true|N|C0|1|3|3|C1|fals" + //
				"e|C2|7|5|8|C3|8319|C4|8575|O|C0|true|C1|falseaHA2FIE7m837/znS/hNt3i4oE" + //
				"K1/ASnP7kxajCIe8U8=";
		String[] keys5 = null;
		check(json, key, keys5, 852);

	}

	@Test
	public void testP1() throws Exception {

		String json = "{'A':{'C0':'2e275d4cbdff6595','C1':7475,'C2':false,'C3':false" + //
				",'C4':true},'B':{'C0':[9,1,8],'C1':'720d2af65fac07c9','C2':5136,'C3':'26a4d2a44e6d8c29'" + //
				",'C4':6712},'C':{'C0':false,'C1':true,'C2':136,'C3':[7,0,3],'C4':[2,3,1]}" + //
				",'D':{'C0':false,'C1':3105,'C2':'7363e1d2ff6f3419','C3':[4,4,3],'C4':[1" + //
				",2,6]},'E':{'C0':false,'C1':false,'C2':2443,'C3':[9,6,0],'C4':[0,5,4]}" + //
				",'F':{'C0':[5,4,5],'C1':2871,'C2':'3f29e0eed182ca5e','C3':8569,'C4':true}" + //
				",'G':{'C0':[8,3,6],'C1':5457,'C2':true,'C3':8349,'C4':true},'H':{'C0':[8" + //
				",3,0],'C1':[8,2,0],'C2':[2,5,1],'C3':'3eff3d96f2e1a07c','C4':false},'I':{'C0':false" + //
				",'C1':3389,'C2':'596ce21b0b8e7859','C3':6487,'C4':'36abaa19ec3755dc'},'J':{'C0':2249" + //
				",'C1':'5aff0cf0daaaeb54','C2':true,'C3':'1ad7d885d495a662','C4':[4,1,3]}" + //
				",'K':{'C0':true,'C1':'71e875fb716a713a','C2':[0,1,9],'C3':[2,4,3],'C4':4884}" + //
				",'L':{'C0':false,'C1':1826,'C2':true,'C3':false,'C4':2368},'M':{'C0':false" + //
				",'C1':184,'C2':[7,5,2],'C3':9674,'C4':false},'N':{'C0':[4,2,1],'C1':false" + //
				",'C2':'468408396ba1aa2a','C3':true,'C4':true},'O':{'C0':false,'C1':true" + //
				",'C2':6577,'C3':'656a22ff7a59156b','C4':false},'P':{'C0':true,'C1':'42fad42f26bf8914'" + //
				",'C2':6975,'C3':true,'C4':[1,8,6]},'Q':{'C0':2005,'C1':'5a05a6fc7e8b0731'" + //
				",'C2':'79e67a2e2439b13c','C3':true,'C4':[0,6,5]},'R':{'C0':4753,'C1':[6" + //
				",3,8],'C2':8638,'C3':[1,6,5],'C4':[2,5,7]},'S':{'C0':8803,'C1':7053,'C2':true" + //
				",'C3':'351364427f2a8aa2','C4':2794},'T':{'C0':true,'C1':[2,6,6],'C2':[9" + //
				",5,6],'C3':[9,9,3],'C4':9619},'U':{'C0':false,'C1':false,'C2':[2,2,1],'C3':[3" + //
				",6,3],'C4':[3,5,0]},'V':{'C0':[3,4,0],'C1':[9,2,4],'C2':'7f06eda40a36ea4c'" + //
				",'C3':[7,4,0],'C4':true},'W':{'C0':7415,'C1':true,'C2':8651,'C3':[1,1,9]" + //
				",'C4':false},'X':{'C0':7025,'C1':[2,2,3],'C2':false,'C3':[2,3,9],'C4':[8" + //
				",2,0]},'Y':{'C0':3770,'C1':false,'C2':[6,5,7],'C3':[8,6,2],'C4':false}" + //
				"}";

		String key = "GLsUiPGuN4YaE0f9Ijrrpos+03Yi04ip4X1SQaN4Dhs=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|2e275d4cbdff6595|C1|7475|C2|false|C3|false|C4|true" + //
				"|B|C0|9|1|8|C1|720d2af65fac07c9|C2|5136|C3|26a4d2a44e6d8c29|C4|6712|C|" + //
				"C0|false|C1|true|C2|136|C3|7|0|3|C4|2|3|1|D|C0|false|C1|3105|C2|7363e1" + //
				"d2ff6f3419|C3|4|4|3|C4|1|2|6|E|C0|false|C1|false|C2|2443|C3|9|6|0|C4|0" + //
				"|5|4|F|C0|5|4|5|C1|2871|C2|3f29e0eed182ca5e|C3|8569|C4|true|G|C0|8|3|6" + //
				"|C1|5457|C2|true|C3|8349|C4|true|H|C0|8|3|0|C1|8|2|0|C2|2|5|1|C3|3eff3" + //
				"d96f2e1a07c|C4|false|I|C0|GLsUiPGuN4YaE0f9Ijrrpos+03Yi04ip4X1SQaN4Dhs=" + //
				"";
		String[] keys2 = null;
		check(json, key, keys2, 483);

		key = "abc.def:A|C0|2e275d4cbdff6595|C1|7475|C2|false|C3|false|C4|true" + //
				"|B|C0|9|1|8|C1|720d2af65fac07c9|C2|5136|C3|26a4d2a44e6d8c29|C4|6712|C|" + //
				"C0|false|C1|true|C2|136|C3|7|0|3|C4|2|3|1|D|C0|false|C1|3105|C2|7363e1" + //
				"d2ff6f3419|C3|4|4|3|C4|1|2|6|E|C0|false|C1|false|C2|2443|C3|9|6|0|C4|0" + //
				"|5|4|F|C0|5|4|5|C1|2871|C2|3f29e0eed182ca5e|C3|8569|C4|true|G|C0|8|3|6" + //
				"|C1|5457|C2|true|C3|8349|C4|true|H|C0|8|3|0|C1|8|2|0|C2|2|5|1|C3|3eff3" + //
				"d96f2e1a07c|C4|false|I|C0|false|C1|3389|C2|596ce21b0b8e7859|C3|6487|C4" + //
				"|36abaa19ec3755dc|J|C0|2249|C1|5aff0cf0daaaeb54|C2|true|C3|1ad7d885d49" + //
				"5a662|C4|4|1|3|K|C0|true|C1|71e875fb716a713a|C2|0|1|9|C3|2|4|3|C4|4884" + //
				"|L|C0|false|C1|1826|C2|true|C3|false|C4|2368|M|C0|false|C1|184|C2|7|5|" + //
				"2|C3|9674|C4|false|N|C0|4|2|1|C1|false|C2|468408396ba1aa2a|C3|true|C4|" + //
				"truGLsUiPGuN4YaE0f9Ijrrpos+03Yi04ip4X1SQaN4Dhs=";
		String[] keys3 = null;
		check(json, key, keys3, 810);

		key = "abc.def:A|C0|2e275d4cbdff6595|C1|7475|C2|false|C3|false|C4|true" + //
				"|B|C0|9|1|8|C1|720d2af65fac07c9|C2|5136|C3|26a4d2a44e6d8c29|C4|6712|C|" + //
				"C0|false|C1|true|C2|136|C3|7|0|3|C4|2|3|1|D|C0|false|C1|3105|C2|7363e1" + //
				"d2ff6f3419|C3|4|4|3|C4|1|2|6|E|C0|false|C1|false|C2|2443|C3|9|6|0|C4|0" + //
				"|5|4|F|C0|5|4|5|C1|2871|C2|3f29e0eed182ca5e|C3|8569|C4|true|G|C0|8|3|6" + //
				"|C1|5457|C2|true|C3|8349|C4|true|H|C0|8|3|0|C1|8|2|0|C2|2|5|1|C3|3eff3" + //
				"d96f2e1a07c|C4|false|I|C0|false|C1|3389|C2|596ce21b0b8e7859|C3|6487|C4" + //
				"|36abaa19ec3755dc|J|C0|2249|C1|5aff0cf0daaaeb54|C2|true|C3|1ad7d885d49" + //
				"5a662|C4|4|1|3|K|C0|true|C1|71e875fb716a713a|C2|0|1|9|C3|2|4|3|C4|4884" + //
				"|L|C0|false|C1|1826|C2|true|C3|false|C4|2368|M|C0|false|C1|184|C2|7|5|" + //
				"2|C3|9674|C4|false|N|C0|4|2|1|C1|false|C2|468408396ba1aa2a|C3|true|C4|" + //
				"true|O|C0|false|C1|true|C2|6577|C3|656a22GLsUiPGuN4YaE0f9Ijrrpos+03Yi0" + //
				"4ip4X1SQaN4Dhs=";
		String[] keys4 = null;
		check(json, key, keys4, 848);

	}

	@Test
	public void testQ1() throws Exception {

		String json = "{'A':{'C0':true,'C1':false,'C2':'c80c1df8270a9ef','C3':false" + //
				",'C4':8075},'B':{'C0':true,'C1':true,'C2':false,'C3':[5,1,1],'C4':5139}" + //
				",'C':{'C0':false,'C1':[2,4,0],'C2':[5,0,4],'C3':false,'C4':[3,4,4]},'D':{'C0':'49bf5582d28805f0'" + //
				",'C1':false,'C2':[0,5,7],'C3':[4,1,0],'C4':false},'E':{'C0':false,'C1':2828" + //
				",'C2':3592,'C3':2030,'C4':true},'F':{'C0':true,'C1':false,'C2':6317,'C3':'57bc01cfdb0e69b0'" + //
				",'C4':true},'G':{'C0':true,'C1':'524fa6b538fbb369','C2':'3c57f262f7448f80'" + //
				",'C3':true,'C4':true},'H':{'C0':false,'C1':true,'C2':false,'C3':'4c5a14c5c60da04d'" + //
				",'C4':false},'I':{'C0':false,'C1':true,'C2':true,'C3':453,'C4':[4,1,8]}" + //
				",'J':{'C0':'5444517a6ef06dff','C1':'73769dcc08191951','C2':'31fd8c871d5dd1e9'" + //
				",'C3':true,'C4':[8,4,1]},'K':{'C0':3555,'C1':true,'C2':true,'C3':true,'C4':2988}" + //
				",'L':{'C0':'3ed0f0427ca03d93','C1':6916,'C2':false,'C3':'513b16430bb6e528'" + //
				",'C4':2022},'M':{'C0':true,'C1':'c2b4bc3f4a0673d','C2':[3,7,1],'C3':'4eb13d5d2882eecc'" + //
				",'C4':'4776424fa875790d'},'N':{'C0':7884,'C1':false,'C2':6800,'C3':false" + //
				",'C4':true},'O':{'C0':false,'C1':'2671cc61b3184338','C2':[1,5,8],'C3':true" + //
				",'C4':[6,3,0]},'P':{'C0':false,'C1':'479a14d7480a4a99','C2':2708,'C3':'5de5e763f479b64b'" + //
				",'C4':2803},'Q':{'C0':8183,'C1':'278053b1fea40b6','C2':false,'C3':8986" + //
				",'C4':6749},'R':{'C0':[1,3,2],'C1':'27d35a4478de785b','C2':false,'C3':true" + //
				",'C4':false},'S':{'C0':[9,2,9],'C1':5561,'C2':true,'C3':'2af8934c0e3832a7'" + //
				",'C4':false},'T':{'C0':'5add823839f3b32','C1':'797b2e6a71409b80','C2':4796" + //
				",'C3':3632,'C4':2431},'U':{'C0':[3,3,0],'C1':true,'C2':[8,0,6],'C3':true" + //
				",'C4':[6,6,5]},'V':{'C0':2869,'C1':226,'C2':'243cb04ffcd85804','C3':3120" + //
				",'C4':[6,3,5]},'W':{'C0':8278,'C1':9594,'C2':9448,'C3':'63e5dff064c21eac'" + //
				",'C4':'51964eb03ba7d0b'},'X':{'C0':7416,'C1':'59ae7c38a41b0487','C2':false" + //
				",'C3':true,'C4':'6a140f32f394062a'},'Y':{'C0':[3,2,6],'C1':false,'C2':true" + //
				",'C3':5490,'C4':'3bb738d8f35a9b0e'}}";

		String key = "radhIico7zHX/yjhoy912TdhA5ZZZaPFWHNjuJ+vIFg=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|true|C1|false|C2|c80c1df8270a9ef|C3|false|C4|8075|" + //
				"B|C0|true|C1|true|C2|false|C3|5|1|1|C4|5139|C|C0|false|C1|2|4|0|C2|5|0" + //
				"|4|C3|false|C4|3|4|4|D|C0|49bf5582d28805f0|C1|false|C2|0|5|7|C3|4|1|0|" + //
				"C4|false|E|C0|false|C1|2828|C2|35radhIico7zHX/yjhoy912TdhA5ZZZaPFWHNju" + //
				"J+vIFg=";
		String[] keys2 = null;
		check(json, key, keys2, 280);

		key = "abc.def:A|C0|true|C1|false|C2|c80c1df8270a9ef|C3|false|C4|8075|" + //
				"B|C0|true|C1|true|C2|false|C3|5|1|1|C4|5139|C|C0|false|C1|2|4|0|C2|5|0" + //
				"|4|C3|false|C4|3|4|4|D|C0|49bf5582d28805f0|C1|false|C2|0|5|7|C3|4|1|0|" + //
				"C4|false|E|C0|false|C1|2828|C2|3592|C3|2030|C4|true|F|C0|true|C1|false" + //
				"|C2|6317|C3|57bc01cfdb0e69b0|C4|true|G|C0|true|C1|524fa6b538fbb369|C2|" + //
				"3c57f262f7448f80|C3|true|C4|true|H|C0|false|C1|true|C2|radhIico7zHX/yj" + //
				"hoy912TdhA5ZZZaPFWHNjuJ+vIFg=";
		String[] keys3 = null;
		check(json, key, keys3, 442);

		key = "abc.def:A|C0|true|C1|false|C2|c80c1df8270a9ef|C3|false|C4|8075|" + //
				"B|C0|true|C1|true|C2|false|C3|5|1|1|C4|5139|C|C0|false|C1|2|4|0|C2|5|0" + //
				"|4|C3|false|C4|3|4|4|D|C0|49bf5582d28805f0|C1|false|C2|0|5|7|C3|4|1|0|" + //
				"C4|false|E|C0|false|C1|2828|C2|3592|C3|2030|C4|true|F|C0|true|C1|false" + //
				"|C2|6317|C3|57bc01cfdb0e69b0|C4|true|G|C0|true|C1|524fa6b538fbb369|C2|" + //
				"3c57f262f7448f80|C3|true|C4|true|H|C0|false|C1|true|C2|false|C3|4c5a14" + //
				"c5c60da04d|C4|false|I|C0|false|C1|true|C2|true|C3|453|C4|4|1|8|J|C0|54" + //
				"44517a6ef06dff|C1|73769dcc08191951|C2|31fd8c871d5dd1e9|C3|true|C4|8|4|" + //
				"1|K|C0|3555|C1|true|C2|true|C3|true|C4|2988|L|C0|3ed0f0427ca03d93|C1|6" + //
				"916|C2|false|C3|513b16430bb6e528|C4|2022|M|C0|true|C1|c2b4bc3f4a0673d|" + //
				"C2|3|7|1|C3|4eb13d5d2882eecc|C4|4776424fa875790d|N|C0|7884|C1|false|C2" + //
				"|6800|C3|false|C4|true|O|C0|false|C1|2671cc61b3184338|C2|1|5|8|C3|true" + //
				"|C4|6|3|0|P|C0|false|C1|479a1radhIico7zHX/yjhoy912TdhA5ZZZaPFWHNjuJ+vI" + //
				"Fg=";
		String[] keys4 = null;
		check(json, key, keys4, 906);

	}

	@Test
	public void testR1() throws Exception {

		String json = "{'A':{'C0':4149,'C1':[1,0,7],'C2':'46b67794b3404e0','C3':false" + //
				",'C4':true},'B':{'C0':true,'C1':510,'C2':5838,'C3':7510,'C4':'2b05969d484aae52'}" + //
				",'C':{'C0':'1d2e2652a3bb674a','C1':[9,2,6],'C2':[5,0,0],'C3':'4fd6847f8a475619'" + //
				",'C4':[6,0,2]},'D':{'C0':true,'C1':251,'C2':true,'C3':[7,0,2],'C4':true}" + //
				",'E':{'C0':'2abdacad0d824298','C1':[6,3,8],'C2':true,'C3':[8,0,8],'C4':7691}" + //
				",'F':{'C0':true,'C1':2192,'C2':[1,6,5],'C3':false,'C4':2911},'G':{'C0':6195" + //
				",'C1':'508c3544140edf23','C2':'748bc84d2911e787','C3':false,'C4':[8,7,2]}" + //
				",'H':{'C0':false,'C1':true,'C2':true,'C3':3705,'C4':false},'I':{'C0':'2061b87c67199ee9'" + //
				",'C1':'2f2b74f0581ca527','C2':[3,1,7],'C3':false,'C4':'146372221bf165b5'}" + //
				",'J':{'C0':false,'C1':'73fb47be1e632360','C2':345,'C3':true,'C4':[0,1,3]}" + //
				",'K':{'C0':true,'C1':false,'C2':false,'C3':275,'C4':7750},'L':{'C0':true" + //
				",'C1':'a7881f81c815c07','C2':[6,2,4],'C3':[2,7,5],'C4':true},'M':{'C0':true" + //
				",'C1':[0,0,3],'C2':4411,'C3':[4,1,5],'C4':[4,4,3]},'N':{'C0':6335,'C1':true" + //
				",'C2':[4,0,9],'C3':'1d929821611ca079','C4':true},'O':{'C0':false,'C1':true" + //
				",'C2':true,'C3':'5222e9be57d2bd6','C4':true},'P':{'C0':false,'C1':[1,9" + //
				",0],'C2':'6fb03846a7853ccb','C3':'f9ea437a8bc593e','C4':6169},'Q':{'C0':5212" + //
				",'C1':'ffc4602cef0b44d','C2':7485,'C3':true,'C4':3875},'R':{'C0':true,'C1':[0" + //
				",2,4],'C2':1040,'C3':[9,7,6],'C4':[7,2,3]},'S':{'C0':false,'C1':true,'C2':9987" + //
				",'C3':false,'C4':'199513ec4dc52b7f'},'T':{'C0':'42e52f89e31461c2','C1':true" + //
				",'C2':2314,'C3':true,'C4':true},'U':{'C0':false,'C1':[9,7,5],'C2':[1,0" + //
				",4],'C3':2588,'C4':false},'V':{'C0':[1,9,5],'C1':true,'C2':true,'C3':316" + //
				",'C4':'2177e6a3566eb567'},'W':{'C0':false,'C1':'17d9f1776a8a2c82','C2':[5" + //
				",7,4],'C3':[1,5,8],'C4':[5,5,3]},'X':{'C0':[2,0,3],'C1':false,'C2':4543" + //
				",'C3':3126,'C4':false},'Y':{'C0':false,'C1':[5,3,9],'C2':false,'C3':5021" + //
				",'C4':false}}";

		String key = "UHkbSCJNbmrpxNXiTgsQu5Em057MOfNHLafAlsHpmj4=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|4149|C1|1|0|7|C2|46b67794b3404e0|C3|false|C4|true|" + //
				"B|C0|true|C1|510|C2|5838|C3|7510|C4|2b05969d484aae52|C|C0|1d2e2652a3bb" + //
				"674a|C1|9|2|6|C2|5UHkbSCJNbmrpxNXiTgsQu5Em057MOfNHLafAlsHpmj4=";
		String[] keys2 = null;
		check(json, key, keys2, 195);

		key = "abc.def:A|C0|4149|C1|1|0|7|C2|46b67794b3404e0|C3|false|C4|true|" + //
				"B|C0|true|C1|510|C2|5838|C3|7510|C4|2b05969d484aae52|C|C0|1d2e2652a3bb" + //
				"674a|C1|9|2|6|C2|5|0|0|C3|4fd6847f8a475619|CUHkbSCJNbmrpxNXiTgsQu5Em05" + //
				"7MOfNHLafAlsHpmj4=";
		String[] keys3 = null;
		check(json, key, keys3, 221);

		key = "abc.def:A|C0|4149|C1|1|0|7|C2|46b67794b3404e0|C3|false|C4|true|" + //
				"B|C0|true|C1|510|C2|5838|C3|7510|C4|2b05969d484aae52|C|C0|1d2e2652a3bb" + //
				"674a|C1|9|2|6|C2|5|0|0|C3|4fd6847f8a475619|C4|6|UHkbSCJNbmrpxNXiTgsQu5" + //
				"Em057MOfNHLafAlsHpmj4=";
		String[] keys4 = null;
		check(json, key, keys4, 225);

		key = "abc.def:A|C0|4149|C1|1|0|7|C2|46b67794b3404e0|C3|false|C4|true|" + //
				"B|C0|true|C1|510|C2|5838|C3|7510|C4|2b05969d484aae52|C|C0|1d2e2652a3bb" + //
				"674a|C1|9|2|6|C2|5|0|0|C3|4fd6847f8a475619|C4|6|0|2|D|C0|true|C1|251|C" + //
				"2|true|C3|7|0|2|C4|true|E|C0|2abdacad0d824298|C1|6|3|8|C2|true|C3|8|0|" + //
				"8|C4|7691|F|C0|true|C1|2192|C2|1|6|5|C3|false|C4|2911|G|C0|6195|C1|508" + //
				"c3544140edf23|C2|748bc84d2911e787|C3|false|C4|8|7|2|H|C0|false|C1|true" + //
				"|C2|true|C3|3705|C4|false|I|C0|2061b87c67199ee9|C1|2f2b74f0581ca527|C2" + //
				"|3|1|7|C3|false|C4|146372221bf165b5|J|C0|false|C1|73fb47be1e632360|C2|" + //
				"345|C3|true|C4|0|1|3|K|C0|true|C1|false|C2|false|C3|275|C4|7750|L|UHkb" + //
				"SCJNbmrpxNXiTgsQu5Em057MOfNHLafAlsHpmj4=";
		String[] keys5 = null;
		check(json, key, keys5, 663);

		key = "abc.def:A|C0|4149|C1|1|0|7|C2|46b67794b3404e0|C3|false|C4|true|" + //
				"B|C0|true|C1|510|C2|5838|C3|7510|C4|2b05969d484aae52|C|C0|1d2e2652a3bb" + //
				"674a|C1|9|2|6|C2|5|0|0|C3|4fd6847f8a475619|C4|6|0|2|D|C0|true|C1|251|C" + //
				"2|true|C3|7|0|2|C4|true|E|C0|2abdacad0d824298|C1|6|3|8|C2|true|C3|8|0|" + //
				"8|C4|7691|F|C0|true|C1|2192|C2|1|6|5|C3|false|C4|2911|G|C0|6195|C1|508" + //
				"c3544140edf23|C2|748bc84d2911e787|C3|false|C4|8|7|2|H|C0|false|C1|true" + //
				"|C2|true|C3|3705|C4|false|I|C0|2061b87c67199ee9|C1|2f2b74f0581ca527|C2" + //
				"|3|1|7|C3|false|C4|146372221bf165b5|J|C0|false|C1|73fb47be1e632360|C2|" + //
				"345|C3|true|C4|0|1|3|K|C0|true|C1|false|C2|false|C3|275|C4|7750|L|C0|t" + //
				"rue|C1|a7881f81c815c07|C2|6|2|4|C3|2|7|5|C4|true|M|C0|true|C1|0|0|3|C2" + //
				"|4411|C3|4|1|5|C4|4|4|3|N|C0|6335|C1|true|C2|4|0|9|C3|1d929821611ca079" + //
				"|C4|true|O|C0|false|C1|true|C2|true|C3|5222e9be57d2bd6UHkbSCJNbmrpxNXi" + //
				"TgsQu5Em057MOfNHLafAlsHpmj4=";
		String[] keys6 = null;
		check(json, key, keys6, 861);

	}

	@Test
	public void testS1() throws Exception {

		String json = "{'A':{'C0':'73b49d8a15c83dbc','C1':4324,'C2':'154351a76e9eb140'" + //
				",'C3':'2d7a9d8786727b31','C4':false},'B':{'C0':[6,7,6],'C1':[7,4,2],'C2':'686c9f197b542bb4'" + //
				",'C3':true,'C4':'6d586c7a527af195'},'C':{'C0':true,'C1':false,'C2':[8,0" + //
				",6],'C3':true,'C4':1342},'D':{'C0':[7,0,2],'C1':false,'C2':'75f79e0e9c421fbd'" + //
				",'C3':'58f7a6628f1c44f8','C4':false},'E':{'C0':false,'C1':[3,0,7],'C2':'79ff808e113cd43'" + //
				",'C3':[0,6,0],'C4':[0,6,1]},'F':{'C0':'4859ab4b972728a2','C1':[0,9,5],'C2':true" + //
				",'C3':[5,5,6],'C4':3039},'G':{'C0':'4fa778c8017f0db9','C1':true,'C2':false" + //
				",'C3':false,'C4':true},'H':{'C0':false,'C1':3138,'C2':10,'C3':[7,0,5],'C4':'71c8d3e97fb43cf2'}" + //
				",'I':{'C0':[1,1,4],'C1':true,'C2':[1,7,4],'C3':[8,1,0],'C4':false},'J':{'C0':'3613a4267653dfaf'" + //
				",'C1':[7,8,7],'C2':true,'C3':'74fc503d59b0e6bc','C4':[4,5,1]},'K':{'C0':[4" + //
				",3,3],'C1':[6,2,4],'C2':false,'C3':'4a01e765efc43bbb','C4':6566},'L':{'C0':[1" + //
				",8,7],'C1':true,'C2':[7,2,7],'C3':true,'C4':'42c1c1d75ce985df'},'M':{'C0':[2" + //
				",5,1],'C1':true,'C2':3599,'C3':false,'C4':'340d0f4545445763'},'N':{'C0':9810" + //
				",'C1':'1dbba6c681152e64','C2':[7,6,2],'C3':true,'C4':'322774aeb5e93ca6'}" + //
				",'O':{'C0':true,'C1':'5d1839ed1b103039','C2':false,'C3':true,'C4':'26221d8e7afc857b'}" + //
				",'P':{'C0':false,'C1':[6,5,4],'C2':'65ac8b78ebecb1b2','C3':true,'C4':'470e0594a298bf01'}" + //
				",'Q':{'C0':'184d249dfbd1a11d','C1':false,'C2':[3,7,3],'C3':true,'C4':[8" + //
				",9,0]},'R':{'C0':8747,'C1':[1,2,7],'C2':[6,4,5],'C3':5220,'C4':'7bd4b00919b2e439'}" + //
				",'S':{'C0':9472,'C1':'43e6a76eef007ff6','C2':false,'C3':'5117ebf2e62b1b7f'" + //
				",'C4':'553af864c3f8521b'},'T':{'C0':true,'C1':false,'C2':false,'C3':[6" + //
				",3,1],'C4':true},'U':{'C0':true,'C1':4469,'C2':true,'C3':8103,'C4':true}" + //
				",'V':{'C0':true,'C1':'232a8e95ef3b132','C2':false,'C3':[2,9,8],'C4':true}" + //
				",'W':{'C0':true,'C1':true,'C2':481,'C3':true,'C4':9992},'X':{'C0':false" + //
				",'C1':'3e3e03cae9153349','C2':true,'C3':'7bf4f53523ee15bd','C4':false}" + //
				",'Y':{'C0':[3,5,0],'C1':'584574384ce1d2bc','C2':true,'C3':false,'C4':'573b23b29e49d8ff'" + //
				"}}";

		String key = "PCQDydIVKG5MDmMOXdZ5v4Ri7qwAqr4okLMxHzXYO90=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abcPCQDydIVKG5MDmMOXdZ5v4Ri7qwAqr4okLMxHzXYO90=";
		String[] keys2 = null;
		check(json, key, keys2, 47);

		key = "abc.def:A|C0|73b49d8a15c83dbc|C1|4324|C2|154351a76e9eb140|C3|2d" + //
				"7a9d8786727b31|C4|false|B|C0|6|7|6|C1|7|4|2|C2|686c9f197b542bb4|C3|tru" + //
				"e|C4|6d586c7a527af195|C|C0PCQDydIVKG5MDmMOXdZ5v4Ri7qwAqr4okLMxHzXYO90=" + //
				"";
		String[] keys3 = null;
		check(json, key, keys3, 203);

		key = "abc.def:A|C0|73b49d8a15c83dbc|C1|4324|C2|154351a76e9eb140|C3|2d" + //
				"7a9d8786727b31|C4|false|B|C0|6|7|6|C1|7|4|2|C2|686c9f197b542bb4|C3|tru" + //
				"e|C4|6d586c7a527af195|C|C0|true|C1|false|C2|8|0|6|C3|true|C4|1342|D|C0" + //
				"|7|0|2|C1|false|C2|75f79e0e9c421fbd|C3|58f7a6628f1c44f8|C4|false|E|C0|" + //
				"false|C1|3|0|7|C2|PCQDydIVKG5MDmMOXdZ5v4Ri7qwAqr4okLMxHzXYO90=";
		String[] keys4 = null;
		check(json, key, keys4, 335);

		key = "abc.def:A|C0|73b49d8a15c83dbc|C1|4324|C2|154351a76e9eb140|C3|2d" + //
				"7a9d8786727b31|C4|false|B|C0|6|7|6|C1|7|4|2|C2|686c9f197b542bb4|C3|tru" + //
				"e|C4|6d586c7a527af195|C|C0|true|C1|false|C2|8|0|6|C3|true|C4|1342|D|C0" + //
				"|7|0|2|C1|false|C2|75f79e0e9c421fbd|C3|58f7a6628f1c44f8|C4|false|E|C0|" + //
				"false|C1|3|0|7|C2|79ff808e113cd43|C3|0|6|0|C4|0|6|1|F|C0|4859ab4b97272" + //
				"8a2|C1|0|9|5|C2|true|C3|5|5|6|C4|3039|G|C0|4fa778c8017f0db9|C1|true|C2" + //
				"|false|C3|false|C4|true|H|C0|false|C1|3138|C2|10|C3|7|0|5|C4|71c8d3e97" + //
				"fb43cf2|I|C0|1|1|4|C1|true|C2|1|7|4|C3|8|1|0|C4|false|J|C0|3613a426765" + //
				"3dfaf|C1|7|8|7|C2|true|C3|74fc503d59b0e6bc|C4|4|5|1|K|C0|4|3|3|C1|6|2|" + //
				"4|C2|false|C3|4a01e765efc43bbb|C4|6566|L|C0|1|8|7|C1|true|C2|7|2|7|C3|" + //
				"true|C4|42c1c1d75cePCQDydIVKG5MDmMOXdZ5v4Ri7qwAqr4okLMxHzXYO90=";
		String[] keys5 = null;
		check(json, key, keys5, 756);

	}

	@Test
	public void testT1() throws Exception {

		String json = "{'A':{'C0':[2,8,2],'C1':8214,'C2':3662,'C3':true,'C4':false}" + //
				",'B':{'C0':6754,'C1':true,'C2':true,'C3':4657,'C4':'4b061fab360dbb47'}" + //
				",'C':{'C0':'2cac37992d48d48d','C1':'2404b691dc77461','C2':'483f3b5858f089d5'" + //
				",'C3':[0,3,3],'C4':'22c441a3df4ce754'},'D':{'C0':9092,'C1':'f95b8eb1c8bc0d3'" + //
				",'C2':true,'C3':false,'C4':4264},'E':{'C0':false,'C1':[5,9,2],'C2':3429" + //
				",'C3':true,'C4':false},'F':{'C0':1013,'C1':'7316668f505918f4','C2':'6916c7637afa629e'" + //
				",'C3':[4,8,7],'C4':3045},'G':{'C0':7373,'C1':'329300b7d6a0ab99','C2':true" + //
				",'C3':[9,5,9],'C4':false},'H':{'C0':'44f490a723566425','C1':'cd54e9ab4753aa9'" + //
				",'C2':true,'C3':4696,'C4':3357},'I':{'C0':'58e141c2668e72ca','C1':false" + //
				",'C2':[1,4,8],'C3':true,'C4':6843},'J':{'C0':9525,'C1':9377,'C2':'695977f691751a9d'" + //
				",'C3':[9,1,7],'C4':'6a9e0a9384553ef4'},'K':{'C0':true,'C1':7145,'C2':true" + //
				",'C3':true,'C4':[7,6,2]},'L':{'C0':'13bfcd4a7fa3d1e9','C1':'375924f4f4fe1bf1'" + //
				",'C2':true,'C3':'76691d2032f21331','C4':[5,4,2]},'M':{'C0':[8,8,5],'C1':true" + //
				",'C2':'7b3bd3d4f5d02d88','C3':true,'C4':1694},'N':{'C0':true,'C1':false" + //
				",'C2':false,'C3':[1,2,4],'C4':'29a1675011982fb6'},'O':{'C0':'5e1d1786d00d3d60'" + //
				",'C1':[8,3,8],'C2':'2d55419e19df40c0','C3':true,'C4':false},'P':{'C0':2590" + //
				",'C1':false,'C2':true,'C3':[5,8,3],'C4':[1,0,0]},'Q':{'C0':[7,3,2],'C1':true" + //
				",'C2':1220,'C3':false,'C4':false},'R':{'C0':2804,'C1':[6,7,0],'C2':'67f88c9e207ce76f'" + //
				",'C3':9814,'C4':8453},'S':{'C0':true,'C1':6029,'C2':[8,6,1],'C3':false" + //
				",'C4':true},'T':{'C0':false,'C1':true,'C2':true,'C3':2184,'C4':'3d475cbdce3c0915'}" + //
				",'U':{'C0':'6d00b697a305d1a2','C1':[0,4,1],'C2':9853,'C3':true,'C4':'3fc0ae8214b5064'}" + //
				",'V':{'C0':6007,'C1':false,'C2':'453f8b0e453424ef','C3':'5d997ab4e78b634b'" + //
				",'C4':false},'W':{'C0':[1,3,7],'C1':[1,6,4],'C2':false,'C3':[4,1,5],'C4':'28d1176b1790292e'}" + //
				",'X':{'C0':4585,'C1':[8,7,6],'C2':2257,'C3':false,'C4':'22640f1911004d95'}" + //
				",'Y':{'C0':'379fd9066fcb1a75','C1':1926,'C2':'1cd915e737adb7b3','C3':'729acfdf1e8e615e'" + //
				",'C4':false}}";

		String key = "Y0vwo/03lZG9M8uw5Z3DQaR7HLJJGmP5zfgfSdw87h4=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|2|8|2|C1|8214|C2|3662|C3|true|C4|false|B|C0|6754|C" + //
				"1|true|C2|true|C3|4657|C4|4b061fab360dbb47|C|C0|2cac37992d48d48d|C1|24" + //
				"04b691dc77461|C2|483f3b5858f089d5|C3|0|3|3|C4|22c441a3df4ce754|D|C0|90" + //
				"92|C1|f95b8eb1c8bc0d3|C2|true|C3|false|C4|4264|E|C0|false|C1|5|9|2|CY0" + //
				"vwo/03lZG9M8uw5Z3DQaR7HLJJGmP5zfgfSdw87h4=";
		String[] keys2 = null;
		check(json, key, keys2, 315);

		key = "abc.def:A|C0|2|8|2|C1|8214|C2|3662|C3|true|C4|false|B|C0|6754|C" + //
				"1|true|C2|true|C3|4657|C4|4b061fab360dbb47|C|C0|2cac37992d48d48d|C1|24" + //
				"04b691dc77461|C2|483f3b5858f089d5|C3|0|3|3|C4|22c441a3df4ce754|D|C0|90" + //
				"92|C1|f95b8eb1c8bc0d3|C2|true|C3|false|C4|4264|E|C0|false|C1|5|9|2|C2|" + //
				"34Y0vwo/03lZG9M8uw5Z3DQaR7HLJJGmP5zfgfSdw87h4=";
		String[] keys3 = null;
		check(json, key, keys3, 319);

		key = "abc.def:A|C0|2|8|2|C1|8214|C2|3662|C3|true|C4|false|B|C0|6754|C" + //
				"1|true|C2|true|C3|4657|C4|4b061fab360dbb47|C|C0|2cac37992d48d48d|C1|24" + //
				"04b691dc77461|C2|483f3b5858f089d5|C3|0|3|3|C4|22c441a3df4ce754|D|C0|90" + //
				"92|C1|f95b8eb1c8bc0d3|C2|true|C3|false|C4|4264|E|C0|false|C1|5|9|2|C2|" + //
				"3429|C3|true|C4|false|F|C0|1013|C1|7316668f505918f4|C2|6916c7637afa629" + //
				"e|C3|4|8|7|C4|3045|G|C0|7373|C1|329300b7d6a0ab99|C2|true|C3|9|5|9|C4|f" + //
				"alse|H|C0|44f490a723566425|C1|cd54Y0vwo/03lZG9M8uw5Z3DQaR7HLJJGmP5zfgf" + //
				"Sdw87h4=";
		String[] keys4 = null;
		check(json, key, keys4, 491);

		key = "abc.def:A|C0|2|8|2|C1|8214|C2|3662|C3|true|C4|false|B|C0|6754|C" + //
				"1|true|C2|true|C3|4657|C4|4b061fab360dbb47|C|C0|2cac37992d48d48d|C1|24" + //
				"04b691dc77461|C2|483f3b5858f089d5|C3|0|3|3|C4|22c441a3df4ce754|D|C0|90" + //
				"92|C1|f95b8eb1c8bc0d3|C2|true|C3|false|C4|4264|E|C0|false|C1|5|9|2|C2|" + //
				"3429|C3|true|C4|false|F|C0|1013|C1|7316668f505918f4|C2|6916c7637afa629" + //
				"e|C3|4|8|7|C4|3045|G|C0|7373|C1|329300b7d6a0ab99|C2|true|C3|9|5|9|C4|f" + //
				"alse|H|C0|44f490a723566425|C1|cd54e9ab4753aa9|C2|true|C3|4696|C4|3357|" + //
				"I|C0|58e141c2668e72ca|C1|false|C2|1|4|8|C3|true|C4|6843|J|C0|9525|C1|9" + //
				"377|C2|695977f691751a9d|C3|9|1|7|C4|6a9e0a9384553ef4|K|C0|true|C1|7Y0v" + //
				"wo/03lZG9M8uw5Z3DQaR7HLJJGmP5zfgfSdw87h4=";
		String[] keys5 = null;
		check(json, key, keys5, 664);

		key = "abc.def:A|C0|2|8|2|C1|8214|C2|3662|C3|true|C4|false|B|C0|6754|C" + //
				"1|true|C2|true|C3|4657|C4|4b061fab360dbb47|C|C0|2cac37992d48d48d|C1|24" + //
				"04b691dc77461|C2|483f3b5858f089d5|C3|0|3|3|C4|22c441a3df4ce754|D|C0|90" + //
				"92|C1|f95b8eb1c8bc0d3|C2|true|C3|false|C4|4264|E|C0|false|C1|5|9|2|C2|" + //
				"3429|C3|true|C4|false|F|C0|1013|C1|7316668f505918f4|C2|6916c7637afa629" + //
				"e|C3|4|8|7|C4|3045|G|C0|7373|C1|329300b7d6a0ab99|C2|true|C3|9|5|9|C4|f" + //
				"alse|H|C0|44f490a723566425|C1|cd54e9ab4753aa9|C2|true|C3|4696|C4|3357|" + //
				"I|C0|58e141c2668e72ca|C1|false|C2|1|4|8|C3|true|C4|6843|J|C0|9525|C1|9" + //
				"377|C2|695977f691751a9d|C3|9|1|7|C4|6a9e0a9384553ef4|K|C0|true|C1|7145" + //
				"|C2|true|C3|true|C4|7|6|2|L|C0|13bfcd4a7fa3d1e9|C1|375924f4f4fe1bf1|C2" + //
				"|true|C3|76691d2032f21331|C4|5|4|2|M|C0|8|8|5|C1|true|C2|7b3bd3d4f5d02" + //
				"d88|C3|true|C4|1694|N|C0|true|C1|false|C2|false|C3|1|2|4|C4|29a1675011" + //
				"982fb6|O|C0|5e1d1786d00d3d60|C1|8|3|8|C2|2d55419e19df40c0|C3|true|C4|f" + //
				"alse|P|C0|2590|C1|false|C2|true|C3|5|8|3|C4|1|0|0|Q|C0|7|3|2|C1Y0vwo/0" + //
				"3lZG9M8uw5Z3DQaR7HLJJGmP5zfgfSdw87h4=";
		String[] keys6 = null;
		check(json, key, keys6, 1010);

	}

	@Test
	public void testU1() throws Exception {

		String json = "{'A':{'C0':429,'C1':[6,9,6],'C2':true,'C3':false,'C4':4671}" + //
				",'B':{'C0':[7,4,4],'C1':true,'C2':'1d32f7fda27c7962','C3':true,'C4':[6" + //
				",5,6]},'C':{'C0':2252,'C1':false,'C2':false,'C3':false,'C4':9898},'D':{'C0':false" + //
				",'C1':false,'C2':'513ca546bc3e8236','C3':8475,'C4':[4,9,6]},'E':{'C0':[9" + //
				",6,0],'C1':[0,9,1],'C2':'791818e5cb80f9c9','C3':[0,1,0],'C4':true},'F':{'C0':false" + //
				",'C1':1879,'C2':true,'C3':false,'C4':'7a3866701a285396'},'G':{'C0':'46729b837ba4ae4d'" + //
				",'C1':false,'C2':[3,1,9],'C3':'b9f792be33b0a85','C4':'3a9beb256339c16e'}" + //
				",'H':{'C0':5984,'C1':5048,'C2':true,'C3':'2cbc78fe62759d67','C4':[9,7,3]}" + //
				",'I':{'C0':'621ae22f3ce1a14b','C1':[6,8,6],'C2':false,'C3':8368,'C4':7405}" + //
				",'J':{'C0':7116,'C1':false,'C2':false,'C3':[6,8,0],'C4':true},'K':{'C0':'4cc39d6bc0637f69'" + //
				",'C1':false,'C2':'4bcefdab1344387b','C3':false,'C4':'31134a81a8d78019'}" + //
				",'L':{'C0':4146,'C1':false,'C2':'1b591b9b9b283e08','C3':'7011353fd0afedee'" + //
				",'C4':'58eb42d94dfed4e3'},'M':{'C0':false,'C1':false,'C2':true,'C3':true" + //
				",'C4':[4,1,5]},'N':{'C0':[0,6,5],'C1':true,'C2':7892,'C3':false,'C4':9167}" + //
				",'O':{'C0':[9,1,2],'C1':false,'C2':false,'C3':true,'C4':'4b1e570e4197e764'}" + //
				",'P':{'C0':'1c79be4305a7a0c0','C1':true,'C2':[5,8,6],'C3':[5,4,5],'C4':'6d35a8e117473e8'}" + //
				",'Q':{'C0':false,'C1':2075,'C2':5319,'C3':true,'C4':false},'R':{'C0':true" + //
				",'C1':false,'C2':'7abe267adb8cff70','C3':false,'C4':1537},'S':{'C0':true" + //
				",'C1':[4,6,2],'C2':525,'C3':false,'C4':false},'T':{'C0':true,'C1':false" + //
				",'C2':8751,'C3':false,'C4':1722},'U':{'C0':'2221a29e9d4ec3ee','C1':[3,0" + //
				",9],'C2':false,'C3':true,'C4':false},'V':{'C0':true,'C1':5730,'C2':true" + //
				",'C3':'595537b8faa7e557','C4':true},'W':{'C0':[3,3,3],'C1':3797,'C2':[7" + //
				",1,8],'C3':[6,2,1],'C4':[3,5,4]},'X':{'C0':'4f54204135b06243','C1':[9,2" + //
				",8],'C2':[6,0,8],'C3':true,'C4':[3,1,9]},'Y':{'C0':true,'C1':false,'C2':367" + //
				",'C3':3489,'C4':[6,2,1]}}";

		String key = "SPv30wdshJbJp+5SET840JJ1vHBOULGwah9Rv+DhjZ8=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|429|C1|6|9|6|C2|true|C3|false|C4|SPv30wdshJbJp+5SE" + //
				"T840JJ1vHBOULGwah9Rv+DhjZ8=";
		String[] keys2 = null;
		check(json, key, keys2, 90);

		key = "abc.def:A|C0|429|C1|6|9|6|C2|true|C3|false|C4|4671|B|C0|7|4|4|C" + //
				"1|true|C2|1d32f7fda27c7962|C3|true|C4|6|5|6|C|C0|2252|C1|false|C2|fals" + //
				"e|C3|false|C4|9898|D|C0|false|C1|false|C2|513ca546bc3e8236|C3|8475|C4|" + //
				"4|9|6|E|C0|9|6|0|C1|0|9|1|C2|7SPv30wdshJbJp+5SET840JJ1vHBOULGwah9Rv+Dh" + //
				"jZ8=";
		String[] keys3 = null;
		check(json, key, keys3, 277);

		key = "abc.def:A|C0|429|C1|6|9|6|C2|true|C3|false|C4|4671|B|C0|7|4|4|C" + //
				"1|true|C2|1d32f7fda27c7962|C3|true|C4|6|5|6|C|C0|2252|C1|false|C2|fals" + //
				"e|C3|false|C4|9898|D|C0|false|C1|false|C2|513ca546bc3e8236|C3|8475|C4|" + //
				"4|9|6|E|C0|9|6|0|C1|0|9|1|C2|791818e5cb80f9c9|C3|0|1|0|C4|true|F|C0|fa" + //
				"lse|C1|1879|C2|true|C3|false|C4|7a3866701a285396|G|C0|46729b837ba4ae4d" + //
				"|C1|false|C2|3|1|9|C3|b9f792be33b0a85|C4|3a9beb256339c16e|H|C0|5984|C1" + //
				"|5048|C2|true|C3|2cbc78fe62759d67|C4|9|7|3|I|C0|621ae22f3ce1a14b|C1|6|" + //
				"8|6|C2|false|C3|8368|C4|7405|J|C0|7116|C1|false|C2|false|C3|6|8|0|C4|t" + //
				"rue|K|C0|4cc39d6bc0637f69|C1|false|C2|4bcefSPv30wdshJbJp+5SET840JJ1vHB" + //
				"OULGwah9Rv+DhjZ8=";
		String[] keys4 = null;
		check(json, key, keys4, 640);

		key = "abc.def:A|C0|429|C1|6|9|6|C2|true|C3|false|C4|4671|B|C0|7|4|4|C" + //
				"1|true|C2|1d32f7fda27c7962|C3|true|C4|6|5|6|C|C0|2252|C1|false|C2|fals" + //
				"e|C3|false|C4|9898|D|C0|false|C1|false|C2|513ca546bc3e8236|C3|8475|C4|" + //
				"4|9|6|E|C0|9|6|0|C1|0|9|1|C2|791818e5cb80f9c9|C3|0|1|0|C4|true|F|C0|fa" + //
				"lse|C1|1879|C2|true|C3|false|C4|7a3866701a285396|G|C0|46729b837ba4ae4d" + //
				"|C1|false|C2|3|1|9|C3|b9f792be33b0a85|C4|3a9beb256339c16e|H|C0|5984|C1" + //
				"|5048|C2|true|C3|2cbc78fe62759d67|C4|9|7|3|I|C0|621ae22f3ce1a14b|C1|6|" + //
				"8|6|C2|false|C3|8368|C4|7405|J|C0|7116|C1|false|C2|false|C3|6|8|0|C4|t" + //
				"rue|K|C0|4cc39d6bc0637f69|C1|false|C2|4bcefdab1344387b|C3|false|C4|311" + //
				"34a81a8d78019|L|C0|4146|C1|false|C2|1b591b9b9b283e08|C3|7011353fd0afed" + //
				"ee|C4|58eb42d94dfed4e3|M|C0|false|C1|false|C2|true|C3|true|C4|4|1|5|N|" + //
				"C0|0|6|5|C1|true|C2|7892|C3|false|C4|9167|O|C0|9|1|2|C1|false|C2|false" + //
				"|C3|true|C4|4b1e570e4197e764|P|C0|1c79be4305aSPv30wdshJbJp+5SET840JJ1v" + //
				"HBOULGwah9Rv+DhjZ8=";
		String[] keys5 = null;
		check(json, key, keys5, 922);

		key = "abc.def:A|C0|429|C1|6|9|6|C2|true|C3|false|C4|4671|B|C0|7|4|4|C" + //
				"1|true|C2|1d32f7fda27c7962|C3|true|C4|6|5|6|C|C0|2252|C1|false|C2|fals" + //
				"e|C3|false|C4|9898|D|C0|false|C1|false|C2|513ca546bc3e8236|C3|8475|C4|" + //
				"4|9|6|E|C0|9|6|0|C1|0|9|1|C2|791818e5cb80f9c9|C3|0|1|0|C4|true|F|C0|fa" + //
				"lse|C1|1879|C2|true|C3|false|C4|7a3866701a285396|G|C0|46729b837ba4ae4d" + //
				"|C1|false|C2|3|1|9|C3|b9f792be33b0a85|C4|3a9beb256339c16e|H|C0|5984|C1" + //
				"|5048|C2|true|C3|2cbc78fe62759d67|C4|9|7|3|I|C0|621ae22f3ce1a14b|C1|6|" + //
				"8|6|C2|false|C3|8368|C4|7405|J|C0|7116|C1|false|C2|false|C3|6|8|0|C4|t" + //
				"rue|K|C0|4cc39d6bc0637f69|C1|false|C2|4bcefdab1344387b|C3|false|C4|311" + //
				"34a81a8d78019|L|C0|4146|C1|false|C2|1b591b9b9b283e08|C3|7011353fd0afed" + //
				"ee|C4|58eb42d94dfed4e3|M|C0|false|C1|false|C2|true|C3|true|C4|4|1|5|N|" + //
				"C0|0|6|5|C1|true|C2|7892|C3|false|C4|9167|O|C0|9|1|2|C1|false|C2|false" + //
				"|C3|true|C4|4b1e570e4197e764|P|C0|1c79be4305a7a0c0|C1|true|C2|5|8|6|C3" + //
				"|5|4|5|C4|6d35a8e117473e8|Q|C0|SPv30wdshJbJp+5SET840JJ1vHBOULGwah9Rv+D" + //
				"hjZ8=";
		String[] keys6 = null;
		check(json, key, keys6, 978);

	}

	@Test
	public void testV1() throws Exception {

		String json = "{'A':{'C0':false,'C1':false,'C2':false,'C3':false,'C4':8733}" + //
				",'B':{'C0':false,'C1':false,'C2':false,'C3':[7,9,7],'C4':'5c23490ecadf452b'}" + //
				",'C':{'C0':6298,'C1':false,'C2':[7,4,7],'C3':'11e1e5b872195693','C4':true}" + //
				",'D':{'C0':8739,'C1':[6,2,1],'C2':[0,4,8],'C3':false,'C4':'5a89e9259c3398'}" + //
				",'E':{'C0':[2,4,2],'C1':'313fe4b466c5aa36','C2':[6,9,9],'C3':true,'C4':[8" + //
				",8,7]},'F':{'C0':false,'C1':1943,'C2':true,'C3':[7,1,1],'C4':false},'G':{'C0':true" + //
				",'C1':[0,2,1],'C2':false,'C3':true,'C4':false},'H':{'C0':true,'C1':true" + //
				",'C2':[8,3,5],'C3':'5bd499bf01564977','C4':[0,8,3]},'I':{'C0':'7d7732ac907493ac'" + //
				",'C1':false,'C2':9759,'C3':'3ac433abc8edb54','C4':'5dbd1f66b0186a02'},'J':{'C0':false" + //
				",'C1':true,'C2':false,'C3':false,'C4':false},'K':{'C0':false,'C1':false" + //
				",'C2':'379cab255f2ab67b','C3':'63c93c9fd6b99bbb','C4':3726},'L':{'C0':true" + //
				",'C1':true,'C2':true,'C3':true,'C4':'47b24a733e03755b'},'M':{'C0':true" + //
				",'C1':'328ebf9d45cd30e','C2':'678198d47ce84ff8','C3':false,'C4':[7,5,4]}" + //
				",'N':{'C0':'4e9cccfc8cefe380','C1':true,'C2':'2b57620d8d0a9746','C3':true" + //
				",'C4':false},'O':{'C0':'73028b47fc600ccd','C1':false,'C2':[2,0,0],'C3':'6fd4e9a67b282f5e'" + //
				",'C4':'19dd7cc10b67b986'},'P':{'C0':false,'C1':[9,2,3],'C2':[3,3,8],'C3':4659" + //
				",'C4':[3,8,0]},'Q':{'C0':'4c542dac54f690df','C1':true,'C2':850,'C3':true" + //
				",'C4':false},'R':{'C0':3694,'C1':false,'C2':[6,7,4],'C3':false,'C4':[7" + //
				",5,4]},'S':{'C0':'674a73c3fccefb7e','C1':7836,'C2':false,'C3':false,'C4':true}" + //
				",'T':{'C0':false,'C1':'78297ef6a1d22f35','C2':4318,'C3':'65d5200cfb44e1ee'" + //
				",'C4':8358},'U':{'C0':[0,2,4],'C1':true,'C2':false,'C3':true,'C4':true}" + //
				",'V':{'C0':'2881c598993cd901','C1':false,'C2':false,'C3':1581,'C4':'7fbfcf7c86f93442'}" + //
				",'W':{'C0':'639d43730a14d24a','C1':[6,1,9],'C2':true,'C3':false,'C4':false}" + //
				",'X':{'C0':[7,3,3],'C1':true,'C2':false,'C3':'4141a8456fc55f24','C4':821}" + //
				",'Y':{'C0':true,'C1':true,'C2':'7140d7273cc763fa','C3':'6f2fcab9660f0c58'" + //
				",'C4':'5dc34ad7f09f3999'}}";

		String key = "6k+JOwNVcZq4OjW2+6/0kBnMJobwCudGH67bs47lhyU=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|8733|B|C0|fals" + //
				"e|C1|false|C2|false|C3|7|9|7|C6k+JOwNVcZq4OjW2+6/0kBnMJobwCudGH67bs47l" + //
				"hyU=";
		String[] keys2 = null;
		check(json, key, keys2, 137);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|8733|B|C0|fals" + //
				"e|C1|false|C2|false|C3|7|9|7|C4|5c23490ecadf452b|C|C0|6298|C1|false|C2" + //
				"|7|4|7|C3|11e1e5b872195693|C4|true|D|C0|8739|C1|6|2|1|C2|0|4|8|C3|fals" + //
				"e|C4|5a89e9259c3398|E|C0|2|4|2|C1|313fe4b466c5aa36|C2|6|9|9|6k+JOwNVcZ" + //
				"q4OjW2+6/0kBnMJobwCudGH67bs47lhyU=";
		String[] keys3 = null;
		check(json, key, keys3, 307);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|8733|B|C0|fals" + //
				"e|C1|false|C2|false|C3|7|9|7|C4|5c23490ecadf452b|C|C0|6298|C1|false|C2" + //
				"|7|4|7|C3|11e1e5b872195693|C4|true|D|C0|8739|C1|6|2|1|C2|0|4|8|C3|fals" + //
				"e|C4|5a89e9259c3398|E|C0|2|4|2|C1|313fe4b466c5aa36|C2|6|9|9|C3|true|C4" + //
				"|8|8|7|F|C0|false|C1|1943|C2|true|C3|7|1|1|C4|false|G|C0|true|C1|0|2|1" + //
				"|C2|false|C3|tr6k+JOwNVcZq4OjW2+6/0kBnMJobwCudGH67bs47lhyU=";
		String[] keys4 = null;
		check(json, key, keys4, 402);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|8733|B|C0|fals" + //
				"e|C1|false|C2|false|C3|7|9|7|C4|5c23490ecadf452b|C|C0|6298|C1|false|C2" + //
				"|7|4|7|C3|11e1e5b872195693|C4|true|D|C0|8739|C1|6|2|1|C2|0|4|8|C3|fals" + //
				"e|C4|5a89e9259c3398|E|C0|2|4|2|C1|313fe4b466c5aa36|C2|6|9|9|C3|true|C4" + //
				"|8|8|7|F|C0|false|C1|1943|C2|true|C3|7|1|1|C4|false|G|C0|true|C1|0|2|1" + //
				"|C2|false|C3|true|C4|false|H|C0|true|C1|true|C2|8|3|5|C3|5bd499bf01564" + //
				"977|C4|0|8|3|I|C0|7d7732ac907493ac|C1|false|C2|9759|C3|3ac433abc8edb54" + //
				"|C4|5dbd1f66b0186a02|J|C0|false|C1|true|C2|false|C3|false|C4|false|K|C" + //
				"0|false|C1|false|C2|379cab255f2ab67b|C3|63c93c9fd6b99bbb|C4|3726k+JOwN" + //
				"VcZq4OjW2+6/0kBnMJobwCudGH67bs47lhyU=";
		String[] keys5 = null;
		check(json, key, keys5, 660);

		key = "abc.def:A|C0|false|C1|false|C2|false|C3|false|C4|8733|B|C0|fals" + //
				"e|C1|false|C2|false|C3|7|9|7|C4|5c23490ecadf452b|C|C0|6298|C1|false|C2" + //
				"|7|4|7|C3|11e1e5b872195693|C4|true|D|C0|8739|C1|6|2|1|C2|0|4|8|C3|fals" + //
				"e|C4|5a89e9259c3398|E|C0|2|4|2|C1|313fe4b466c5aa36|C2|6|9|9|C3|true|C4" + //
				"|8|8|7|F|C0|false|C1|1943|C2|true|C3|7|1|1|C4|false|G|C0|true|C1|0|2|1" + //
				"|C2|false|C3|true|C4|false|H|C0|true|C1|true|C2|8|3|5|C3|5bd499bf01564" + //
				"977|C4|0|8|3|I|C0|7d7732ac907493ac|C1|false|C2|9759|C3|3ac433abc8edb54" + //
				"|C4|5dbd1f66b0186a02|J|C0|false|C1|true|C2|false|C3|false|C4|false|K|C" + //
				"0|false|C1|false|C2|379cab255f2ab67b|C3|63c93c9fd6b99bbb|C4|3726|L|C0|" + //
				"true|C1|true|C2|true|C3|true|C4|47b24a733e03755b|M|C0|true|C1|328ebf9d" + //
				"45cd30e|C2|678198d47ce84ff8|C3|false|C4|7|5|4|N|C0|4e9cccfc8cefe380|C1" + //
				"|tr6k+JOwNVcZq4OjW2+6/0kBnMJobwCudGH67bs47lhyU=";
		String[] keys6 = null;
		check(json, key, keys6, 810);

	}

	@Test
	public void testW1() throws Exception {

		String json = "{'A':{'C0':[3,7,9],'C1':true,'C2':719,'C3':[8,9,3],'C4':true}" + //
				",'B':{'C0':'3f41dcf5c1d30f13','C1':true,'C2':'32724a31c3e66b42','C3':true" + //
				",'C4':false},'C':{'C0':false,'C1':'395f8bfa85704d7e','C2':'1cd110a6df14c007'" + //
				",'C3':[3,3,7],'C4':'5b56a16ef5394537'},'D':{'C0':false,'C1':[4,8,6],'C2':'1bb944255586950d'" + //
				",'C3':3364,'C4':true},'E':{'C0':false,'C1':false,'C2':false,'C3':false" + //
				",'C4':[7,9,0]},'F':{'C0':[5,3,7],'C1':4892,'C2':[7,6,6],'C3':false,'C4':false}" + //
				",'G':{'C0':false,'C1':[0,3,7],'C2':'1ed0028747c32e5f','C3':[9,4,1],'C4':false}" + //
				",'H':{'C0':'67c29c8220a14209','C1':true,'C2':[8,4,7],'C3':[9,4,4],'C4':3702}" + //
				",'I':{'C0':[5,1,0],'C1':7868,'C2':false,'C3':7546,'C4':false},'J':{'C0':[4" + //
				",0,2],'C1':'4f63c107c9a83fd','C2':[4,4,3],'C3':9615,'C4':true},'K':{'C0':6311" + //
				",'C1':'562e1c8c965c3c1a','C2':[8,8,1],'C3':false,'C4':true},'L':{'C0':'90646b02e91bbe8'" + //
				",'C1':true,'C2':false,'C3':[8,1,0],'C4':true},'M':{'C0':'34b0a2374cb4af8'" + //
				",'C1':'1e9f6415e99cb691','C2':false,'C3':true,'C4':false},'N':{'C0':6887" + //
				",'C1':'1b394ac89d8d9a05','C2':'64c5aa006840c09b','C3':true,'C4':true},'O':{'C0':[0" + //
				",7,2],'C1':false,'C2':true,'C3':'575e2f9de4f52164','C4':[0,7,5]},'P':{'C0':[6" + //
				",8,2],'C1':false,'C2':'17c16f1905beb139','C3':[6,1,5],'C4':9118},'Q':{'C0':1824" + //
				",'C1':'501f39470edf1109','C2':[6,2,9],'C3':false,'C4':5880},'R':{'C0':8994" + //
				",'C1':[3,9,2],'C2':false,'C3':[8,7,7],'C4':'987f845169608a0'},'S':{'C0':false" + //
				",'C1':true,'C2':'1f11662fe9f6abd6','C3':false,'C4':false},'T':{'C0':98" + //
				",'C1':true,'C2':[4,1,1],'C3':'1fe38747809b6e58','C4':'7ed939c0756c24ac'}" + //
				",'U':{'C0':'5f7217a60be756a0','C1':'c9432dd4ebb46a1','C2':'2b1f2069c681aa42'" + //
				",'C3':true,'C4':true},'V':{'C0':false,'C1':false,'C2':4933,'C3':false,'C4':3707}" + //
				",'W':{'C0':false,'C1':8696,'C2':'74bbbbc4ec97211f','C3':[1,8,9],'C4':3351}" + //
				",'X':{'C0':9501,'C1':false,'C2':false,'C3':false,'C4':false},'Y':{'C0':3006" + //
				",'C1':[0,1,9],'C2':2544,'C3':true,'C4':6062}}";

		String key = "KOu2RBc8mVd406piGloUN5Qk77K7uM5Ehgd2EOyAMuk=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|3|7|9|C1|true|C2|719|C3|8|9|3|C4|true|B|C0|3f41dcf" + //
				"5c1d30f13|C1|true|C2|32724a31c3e66b42|C3|true|C4|false|C|C0|false|C1|3" + //
				"95f8bfa85704d7e|C2|1cd110a6df14c007|C3|3|3|7|C4|5b56a16ef5394537|D|C0|" + //
				"false|C1|4|8|6|C2|1bb944255586950d|C3|3364|C4|trueKOu2RBc8mVd406piGloU" + //
				"N5Qk77K7uM5Ehgd2EOyAMuk=";
		String[] keys2 = null;
		check(json, key, keys2, 297);

		key = "abc.def:A|C0|3|7|9|C1|true|C2|719|C3|8|9|3|C4|true|B|C0|3f41dcf" + //
				"5c1d30f13|C1|true|C2|32724a31c3e66b42|C3|true|C4|false|C|C0|false|C1|3" + //
				"95f8bfa85704d7e|C2|1cd110a6df14c007|C3|3|3|7|C4|5b56a16ef5394537|D|C0|" + //
				"false|C1|4|8|6|C2|1bb944255586950d|C3|3364|C4|true|E|C0|false|C1|false" + //
				"|C2|false|C3|false|C4|7|9|0|F|C0|5|3|7|C1|4892|C2|7|6|6|C3|false|C4|fa" + //
				"lse|G|C0|false|C1|0|3|7|C2|1ed0028747c32e5f|C3|9|4|1|C4|false|H|C0|67c" + //
				"29c8220a14209|C1|true|C2|8|4|7|C3|9|4|4|C4|KOu2RBc8mVd406piGloUN5Qk77K" + //
				"7uM5Ehgd2EOyAMuk=";
		String[] keys3 = null;
		check(json, key, keys3, 500);

		key = "abc.def:A|C0|3|7|9|C1|true|C2|719|C3|8|9|3|C4|true|B|C0|3f41dcf" + //
				"5c1d30f13|C1|true|C2|32724a31c3e66b42|C3|true|C4|false|C|C0|false|C1|3" + //
				"95f8bfa85704d7e|C2|1cd110a6df14c007|C3|3|3|7|C4|5b56a16ef5394537|D|C0|" + //
				"false|C1|4|8|6|C2|1bb944255586950d|C3|3364|C4|true|E|C0|false|C1|false" + //
				"|C2|false|C3|false|C4|7|9|0|F|C0|5|3|7|C1|4892|C2|7|6|6|C3|false|C4|fa" + //
				"lse|G|C0|false|C1|0|3|7|C2|1ed0028747c32e5f|C3|9|4|1|C4|false|H|C0|67c" + //
				"29c8220a14209|C1|true|C2|8|4|7|C3|9|4|4|C4|3702|I|C0|5|1|0|C1|7868|C2|" + //
				"false|C3|7546|C4|false|J|C0|4|0|2|C1|4f63c107c9a83fd|C2|4|4|3|C3|9615|" + //
				"C4|true|K|C0|6311|C1|562e1c8c965c3c1a|C2|8|8|1|C3|false|C4|true|L|C0|9" + //
				"0646b02e91bbe8|C1|true|C2|false|C3|8|1|0|C4|true|M|C0|34b0a2374cb4af8|" + //
				"C1|1e9f6415e99cb691|C2|false|C3|true|C4|false|N|C0|6887|C1|1b394ac89d8" + //
				"d9a05|C2|64c5aa006840c09b|C3|true|C4|true|O|C0|0|7|2|C1|false|CKOu2RBc" + //
				"8mVd406piGloUN5Qk77K7uM5Ehgd2EOyAMuk=";
		String[] keys4 = null;
		check(json, key, keys4, 870);

		key = "abc.def:A|C0|3|7|9|C1|true|C2|719|C3|8|9|3|C4|true|B|C0|3f41dcf" + //
				"5c1d30f13|C1|true|C2|32724a31c3e66b42|C3|true|C4|false|C|C0|false|C1|3" + //
				"95f8bfa85704d7e|C2|1cd110a6df14c007|C3|3|3|7|C4|5b56a16ef5394537|D|C0|" + //
				"false|C1|4|8|6|C2|1bb944255586950d|C3|3364|C4|true|E|C0|false|C1|false" + //
				"|C2|false|C3|false|C4|7|9|0|F|C0|5|3|7|C1|4892|C2|7|6|6|C3|false|C4|fa" + //
				"lse|G|C0|false|C1|0|3|7|C2|1ed0028747c32e5f|C3|9|4|1|C4|false|H|C0|67c" + //
				"29c8220a14209|C1|true|C2|8|4|7|C3|9|4|4|C4|3702|I|C0|5|1|0|C1|7868|C2|" + //
				"false|C3|7546|C4|false|J|C0|4|0|2|C1|4f63c107c9a83fd|C2|4|4|3|C3|9615|" + //
				"C4|true|K|C0|6311|C1|562e1c8c965c3c1a|C2|8|8|1|C3|false|C4|true|L|C0|9" + //
				"0646b02e91bbe8|C1|true|C2|false|C3|8|1|0|C4|true|M|C0|34b0a2374cb4af8|" + //
				"C1|1e9f6415e99cb691|C2|false|C3|true|C4|false|N|C0|6887|C1|1b394ac89d8" + //
				"d9a05|C2|64c5aa006840c09b|C3|true|C4|true|O|C0|0|7|2|C1|false|C2|true|" + //
				"C3|575e2f9de4f52164|C4|0|7|5|P|C0|6|8|2|C1|false|C2|17c16f1905beb139|C" + //
				"3|6|1|5|C4|9118|Q|C0|1824|CKOu2RBc8mVd406piGloUN5Qk77K7uM5Ehgd2EOyAMuk" + //
				"=";
		String[] keys5 = null;
		check(json, key, keys5, 974);

	}

	@Test
	public void testX1() throws Exception {

		String json = "{'A':{'C0':true,'C1':3565,'C2':false,'C3':'783a7d84886edf7'" + //
				",'C4':false},'B':{'C0':true,'C1':4729,'C2':false,'C3':'6d1a7d16f937fd52'" + //
				",'C4':'4759580408990a2'},'C':{'C0':false,'C1':'30727124c3c7c714','C2':'52d77debc4444d09'" + //
				",'C3':'7d5b4e380e1797ca','C4':'58f35c5e74c47b4d'},'D':{'C0':'28eb623ceff5e66'" + //
				",'C1':6678,'C2':[2,4,8],'C3':8352,'C4':7324},'E':{'C0':true,'C1':true,'C2':'a1850ae5098bc98'" + //
				",'C3':false,'C4':true},'F':{'C0':216,'C1':[5,5,0],'C2':[0,1,9],'C3':'66f7b2c75302598f'" + //
				",'C4':true},'G':{'C0':false,'C1':'71698117f0583858','C2':false,'C3':false" + //
				",'C4':1999},'H':{'C0':false,'C1':6946,'C2':1486,'C3':5740,'C4':[8,5,2]}" + //
				",'I':{'C0':'30978bb448deefe4','C1':[2,0,3],'C2':2659,'C3':[4,0,1],'C4':true}" + //
				",'J':{'C0':true,'C1':[2,3,0],'C2':'89b0adf0bdaae63','C3':[9,8,8],'C4':[2" + //
				",4,4]},'K':{'C0':1010,'C1':9727,'C2':'6f10007b6a574840','C3':'2be7608f3b03b44f'" + //
				",'C4':[2,2,5]},'L':{'C0':'2f05fbe02018c434','C1':true,'C2':false,'C3':true" + //
				",'C4':false},'M':{'C0':[8,6,8],'C1':false,'C2':false,'C3':false,'C4':[4" + //
				",8,3]},'N':{'C0':3931,'C1':true,'C2':[2,1,2],'C3':[6,4,2],'C4':false},'O':{'C0':true" + //
				",'C1':4235,'C2':true,'C3':[1,1,6],'C4':7062},'P':{'C0':true,'C1':[0,2,5]" + //
				",'C2':'5c17fb8e0efcae0','C3':true,'C4':[2,5,7]},'Q':{'C0':true,'C1':false" + //
				",'C2':[8,3,6],'C3':'7b339a7c1886e3f','C4':6027},'R':{'C0':174,'C1':true" + //
				",'C2':[5,9,2],'C3':'628c139065871af9','C4':1586},'S':{'C0':[7,2,9],'C1':9775" + //
				",'C2':3003,'C3':[1,3,0],'C4':true},'T':{'C0':'6541e121ab697aee','C1':true" + //
				",'C2':[0,8,8],'C3':true,'C4':'4614da4107fed144'},'U':{'C0':'2da4c7a88268b60e'" + //
				",'C1':1797,'C2':true,'C3':3867,'C4':'5bedc85678bcb8e7'},'V':{'C0':false" + //
				",'C1':[0,3,1],'C2':'2191fe4e33917c0f','C3':'3bfea7fbce571448','C4':false}" + //
				",'W':{'C0':'502c799f6935c5e8','C1':'5ab2662fe56ce4e7','C2':1517,'C3':false" + //
				",'C4':'4f2c0a4631880803'},'X':{'C0':[6,4,3],'C1':false,'C2':'3733ac9b809f1fba'" + //
				",'C3':[2,1,0],'C4':true},'Y':{'C0':'205405943f2a7a95','C1':true,'C2':'248df7b5e27cd14f'" + //
				",'C3':[2,3,3],'C4':'4214b0c399149b16'}}";

		String key = "J0BfNPKDmHLB7Yn9dFDJkov8hXXTB3CUmDv/nlr7Uco=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|true|C1|3565|C2|false|C3|783a7d84886edf7|C4|false|" + //
				"B|C0|true|C1|4729|C2|false|C3|6d1a7d16f937fd52|C4|4759580408990a2|C|C0" + //
				"|false|C1|30727124c3c7c714|C2|52d77debc4444d09|C3|7d5b4e380e1797ca|C4|" + //
				"58f35c5e74c47b4d|D|C0|28eb623cefJ0BfNPKDmHLB7Yn9dFDJkov8hXXTB3CUmDv/nl" + //
				"r7Uco=";
		String[] keys2 = null;
		check(json, key, keys2, 279);

		key = "abc.def:A|C0|true|C1|3565|C2|false|C3|783a7d84886edf7|C4|false|" + //
				"B|C0|true|C1|4729|C2|false|C3|6d1a7d16f937fd52|C4|4759580408990a2|C|C0" + //
				"|false|C1|30727124c3c7c714|C2|52d77debc4444d09|C3|7d5b4e380e1797ca|C4|" + //
				"58f35c5e74c47b4d|D|C0|28eb623ceff5e66|C1|6678|C2|2|4|8|C3|8352|C4|7324" + //
				"|E|C0|true|C1|true|C2|a1850ae5098bc98|C3|false|C4|true|F|C0|216|C1|5|5" + //
				"|0|C2|0|1|9|C3|66f7b2c75302598f|C4|true|G|C0|false|C1|7169811J0BfNPKDm" + //
				"HLB7Yn9dFDJkov8hXXTB3CUmDv/nlr7Uco=";
		String[] keys3 = null;
		check(json, key, keys3, 448);

		key = "abc.def:A|C0|true|C1|3565|C2|false|C3|783a7d84886edf7|C4|false|" + //
				"B|C0|true|C1|4729|C2|false|C3|6d1a7d16f937fd52|C4|4759580408990a2|C|C0" + //
				"|false|C1|30727124c3c7c714|C2|52d77debc4444d09|C3|7d5b4e380e1797ca|C4|" + //
				"58f35c5e74c47b4d|D|C0|28eb623ceff5e66|C1|6678|C2|2|4|8|C3|8352|C4|7324" + //
				"|E|C0|true|C1|true|C2|a1850ae5098bc98|C3|false|C4|true|F|C0|216|C1|5|5" + //
				"|0|C2|0|1|9|C3|66f7b2c75302598f|C4|true|G|C0|false|C1|71698117f0583858" + //
				"|C2|false|C3|false|C4|1999|H|C0|false|C1|6946|C2|1486|C3|5740|C4|8|5|2" + //
				"|I|C0|30978bb448deefe4|C1|2|0|3|C2|2659|C3|4|0|1|C4|true|J|C0|true|C1|" + //
				"2|3|0|C2|89b0adf0bdaae63|C3|9|8|8|C4|2|4|4|K|C0|1010|C1|9727|C2|6f1000" + //
				"7b6a574840|C3|2be7608f3b03b44f|C4|2|2|5|L|C0|2f05fbe02018c434|C1|true|" + //
				"C2|false|C3|true|CJ0BfNPKDmHLB7Yn9dFDJkov8hXXTB3CUmDv/nlr7Uco=";
		String[] keys4 = null;
		check(json, key, keys4, 755);

	}

	@Test
	public void testY1() throws Exception {

		String json = "{'A':{'C0':false,'C1':[7,9,3],'C2':true,'C3':[3,9,5],'C4':false}" + //
				",'B':{'C0':'1dddc9f71d6dc833','C1':'446815d5ea5d0a38','C2':[5,6,5],'C3':'30580525de22ffab'" + //
				",'C4':false},'C':{'C0':true,'C1':true,'C2':false,'C3':'6015d57ad8639922'" + //
				",'C4':[5,2,6]},'D':{'C0':[8,0,8],'C1':'3736eb96adc210ad','C2':[9,7,9],'C3':'1eaf5e19cb4ef238'" + //
				",'C4':false},'E':{'C0':false,'C1':[1,6,6],'C2':true,'C3':'5f4ecdd21f10863c'" + //
				",'C4':true},'F':{'C0':true,'C1':false,'C2':6232,'C3':true,'C4':5647},'G':{'C0':4019" + //
				",'C1':[9,5,6],'C2':'7f40e6a8022f4c42','C3':true,'C4':false},'H':{'C0':[8" + //
				",6,1],'C1':true,'C2':'79b6361858bd37cf','C3':[8,0,2],'C4':1304},'I':{'C0':true" + //
				",'C1':false,'C2':8514,'C3':6274,'C4':true},'J':{'C0':true,'C1':7779,'C2':'73838b8fca00a8e'" + //
				",'C3':[2,4,1],'C4':[8,8,7]},'K':{'C0':true,'C1':false,'C2':9664,'C3':false" + //
				",'C4':'898295e773b4592'},'L':{'C0':890,'C1':[3,7,0],'C2':true,'C3':false" + //
				",'C4':[8,4,7]},'M':{'C0':false,'C1':'27573c505079dd3a','C2':'198907f56594f5a9'" + //
				",'C3':1681,'C4':'61922240067c6706'},'N':{'C0':'32df03b5db34282c','C1':false" + //
				",'C2':true,'C3':true,'C4':'51fa807540a0a4fe'},'O':{'C0':false,'C1':722" + //
				",'C2':true,'C3':true,'C4':false},'P':{'C0':7221,'C1':false,'C2':8903,'C3':true" + //
				",'C4':true},'Q':{'C0':false,'C1':'3053bd147c7b7be9','C2':false,'C3':'12e8b1ac4b301e8f'" + //
				",'C4':[4,1,1]},'R':{'C0':6513,'C1':9176,'C2':[9,6,2],'C3':5601,'C4':'4de5be9ff06a5337'}" + //
				",'S':{'C0':[5,0,3],'C1':false,'C2':false,'C3':[4,9,7],'C4':'41ef97df1f6e723d'}" + //
				",'T':{'C0':true,'C1':'2a88786848276a3f','C2':[7,2,5],'C3':true,'C4':false}" + //
				",'U':{'C0':false,'C1':false,'C2':false,'C3':true,'C4':[3,9,5]},'V':{'C0':[0" + //
				",8,9],'C1':[0,8,5],'C2':false,'C3':4357,'C4':true},'W':{'C0':[8,6,9],'C1':true" + //
				",'C2':5634,'C3':[1,8,1],'C4':[5,5,6]},'X':{'C0':false,'C1':'51c7f4990c9d7f35'" + //
				",'C2':'3514ebc217bb1935','C3':true,'C4':true},'Y':{'C0':[8,8,9],'C1':[6" + //
				",0,4],'C2':false,'C3':'26cfa25f4359a14a','C4':true}}";

		String key = "yTMixso1tku1YQrGQUF7F+ujm+m3JnJQ5H7JaafNPwQ=";
		String[] keys1 = null;
		check(json, key, keys1, 44);

		key = "abc.def:A|C0|false|C1|7|9|3|C2|true|C3|3|9|5|C4|false|B|C0|1ddd" + //
				"c9f71d6dc833|C1|446815d5ea5d0a38|C2|5|6|5|C3|30580525de22ffab|C4|false" + //
				"|C|C0|true|C1|true|C2|false|C3|6015d57ad8639922|C4|5|2|6|D|C0|8|0|8|C1" + //
				"|3736eb96adc210ad|C2|9|7|9|C3|1eaf5e19cb4ef238|C4|false|E|C0|false|C1|" + //
				"1|6|6|C2|true|C3|5f4ecdd21f10863c|C4|true|F|C0|true|C1|false|C2|6232|C" + //
				"3|true|C4|5647|G|C0|4019|C1|9|5|6|C2|7f40e6a8022f4c42|C3|true|C4|false" + //
				"|H|C0|8|6|1|C1|true|C2|79b6361858bd37cf|C3|8|yTMixso1tku1YQrGQUF7F+ujm" + //
				"+m3JnJQ5H7JaafNPwQ=";
		String[] keys2 = null;
		check(json, key, keys2, 502);

		key = "abc.def:A|C0|false|C1|7|9|3|C2|true|C3|3|9|5|C4|false|B|C0|1ddd" + //
				"c9f71d6dc833|C1|446815d5ea5d0a38|C2|5|6|5|C3|30580525de22ffab|C4|false" + //
				"|C|C0|true|C1|true|C2|false|C3|6015d57ad8639922|C4|5|2|6|D|C0|8|0|8|C1" + //
				"|3736eb96adc210ad|C2|9|7|9|C3|1eaf5e19cb4ef238|C4|false|E|C0|false|C1|" + //
				"1|6|6|C2|true|C3|5f4ecdd21f10863c|C4|true|F|C0|true|C1|false|C2|6232|C" + //
				"3|true|C4|5647|G|C0|4019|C1|9|5|6|C2|7f40e6a8022f4c42|C3|true|C4|false" + //
				"|H|C0|8|6|1|C1|true|C2|79b6361858bd37cf|C3|8|0|2|C4|1304|I|C0|true|C1|" + //
				"false|C2|8514|C3|6274|C4|true|J|C0|true|C1|7779|C2|73838b8fca00a8e|C3|" + //
				"2|4|1|C4|8|8|7|K|C0|true|C1|false|C2|9664|C3|false|C4|898295e773b4592|" + //
				"L|C0|890|C1|3|7|0|C2|true|C3|false|C4|8|4|7|M|C0|false|C1|27573c505079" + //
				"ddyTMixso1tku1YQrGQUF7F+ujm+m3JnJQ5H7JaafNPwQ=";
		String[] keys3 = null;
		check(json, key, keys3, 739);

	}

	@Test
	public void testA2() throws Exception {

		String json = "{'A':7778,'B':false,'C':[8,5,2],'D':true,'E':[8,2,7],'F':false" + //
				",'G':'2f8739b85cf1fcfd','H':[3,0,4],'I':true,'J':true,'K':false,'L':1678" + //
				",'M':'3f56658fae74b993','N':false,'O':false,'P':'207b883d6ce9ccbb','Q':[8" + //
				",1,9],'R':'7bdb7343c9333ae3','S':'2f16b87f2ce76e58','T':false,'U':'48d4add2d23cc5fa'" + //
				",'V':1251,'W':[0,1,0],'X':897,'Y':true}";

		String key = "nG/66skhqapwxg7r3lrKOln/MBAEmWSmkoSeTTJKbpQ=";
		String[] keys1 = { "S", "V", "R", "C", "B", "Y", "Q", "K", "D", "L", "J", "O", "P", "E", "M", "W" };
		check(json, key, keys1, 44);

		key = "abc.def:48d4add2d23cc5fa|true|7778|3f56658fae74b993|true|0|1|0|" + //
				"7bdb7343c9333ae3|8|2|7|false|true|2f8739b85cf1fcfd|897";
		String[] keys2 = { "U", "D", "A", "M", "Y", "W", "R", "E", "F", "I", "G", "X" };
		check(json, key, keys2, 538);

		key = "abc.def:true|false|8|5|2|7778|3f56658fae74b993|false|false|fals" + //
				"e|7bdb7343c9333ae3|0|1|0|true|true|897|1251|false|true|false|2f16b87f2" + //
				"ce76e58|8|1|9|8|2|7";
		String[] keys3 = { "D", "O", "C", "A", "M", "T", "B", "N", "R", "W", "I", "J", "X", "V", "F", "Y", "K", "S",
				"Q", "E" };
		check(json, key, keys3, 830);

		key = "abc.def:3f56658fae74b993|2f16b87f2ce76e58|false|false|207b883d6" + //
				"ce9ccbb|8|2|7|3|0|4|true|8|1|9|false|true|false|0|1|0|8|5|2|false|897";
		String[] keys4 = { "M", "S", "K", "F", "P", "E", "H", "I", "Q", "N", "J", "B", "W", "C", "T", "X" };
		check(json, key, keys4, 949);

	}

	@Test
	public void testB2() throws Exception {

		String json = "{'A':[4,9,1],'B':[5,3,3],'C':'671c71b803903257','D':'2851681913e2def6'" + //
				",'E':true,'F':true,'G':4114,'H':1842,'I':4328,'J':true,'K':false,'L':false" + //
				",'M':'4acaef036e01f59','N':2317,'O':3270,'P':false,'Q':[2,7,4],'R':6821" + //
				",'S':false,'T':false,'U':[2,7,4],'V':[0,7,5],'W':false,'X':4391,'Y':'d71c8116919c8a3'" + //
				"}";

		String key = "5TdE8C9geOTLUHqM8JSVchosrojMV0OtMD4PKlvW4qY=";
		String[] keys1 = { "F", "I", "W", "S", "L", "N", "U", "A", "G", "Y", "J", "C", "R", "B", "E", "D", "K", "O",
				"V", "T", "Q", "H", "P", "X" };
		check(json, key, keys1, 44);

		key = "abc5/Q4r55ZQmCz3J+LElebDU5mQBGGPYFurmm0rZG68wo=";
		String[] keys2 = { "H", "Q", "J", "B", "U", "G", "S", "F", "L", "M", "O", "Y", "N", "T", "K", "W", "I", "R",
				"P", "E", "V", "X", "C", "A", "D" };
		check(json, key, keys2, 47);

		key = "abc.def:false";
		String[] keys3 = { "W" };
		check(json, key, keys3, 382);

		key = "abc.def:true|true|0|7|5";
		String[] keys4 = { "E", "F", "V" };
		check(json, key, keys4, 570);

		key = "abc.def:4328|2317|true|false|4391|2|7|4|3270|1842|671c71b803903" + //
				"257|4|9|1|6821|false|false|false|false|true|0|7|5|true";
		String[] keys5 = { "I", "N", "J", "P", "X", "Q", "O", "H", "C", "A", "R", "K", "S", "W", "T", "E", "V", "F" };
		check(json, key, keys5, 657);

		key = "abc.def:d71c8116919c8a3|4328|4|9|1|1842|false|4391|2851681913e2" + //
				"def6|671c71b803903257|false|0|7|5|false|false|false";
		String[] keys6 = { "Y", "I", "A", "H", "W", "X", "D", "C", "L", "V", "K", "P", "T" };
		check(json, key, keys6, 971);

	}

	@Test
	public void testC2() throws Exception {

		String json = "{'A':8553,'B':578,'C':true,'D':false,'E':[6,0,0],'F':false" + //
				",'G':true,'H':[5,8,4],'I':[9,1,6],'J':false,'K':[0,3,7],'L':true,'M':[0" + //
				",4,3],'N':true,'O':[1,5,9],'P':true,'Q':'2cfa02933964567e','R':[0,6,2]" + //
				",'S':4503,'T':true,'U':false,'V':4877,'W':6067,'X':'744d8c4ad184dd08','Y':'6c0892d6a3aeac58'" + //
				"}";

		String key = "FAjkeVz+hogONoxOCtaKYz6cgVps2oOsir6/nlxHYBg=";
		String[] keys1 = { "V", "E", "F", "W", "O", "A", "Q", "P", "Y", "L", "D", "H", "T", "M" };
		check(json, key, keys1, 44);

		key = "abc.def:6c0892d6a3aeac58|4503|false|578|tL56JLhxEix5ZyZFBsE+1cw" + //
				"+bIYNnRI8veZoncDFWUwU=";
		String[] keys2 = { "Y", "S", "D", "B", "C", "P", "A", "T", "H", "Q", "I", "E", "K", "U", "F", "O", "N", "R",
				"J", "G", "L", "V", "W", "M", "X" };
		check(json, key, keys2, 85);

		key = "abc.def:1|5|9|false|4503|6|0|0|578|8553|false|6c0892d6a3aeac58|" + //
				"0|4|3|false|6067|744d8c4ad184dd08|true|true|false|true|5|8|4|9|1|6|0|3" + //
				"|7|true|true|2cfa02933964567e";
		String[] keys3 = { "O", "D", "S", "E", "B", "A", "J", "Y", "M", "F", "W", "X", "P", "N", "U", "L", "H", "I",
				"K", "G", "T", "Q" };
		check(json, key, keys3, 276);

		key = "abc.def:578|744d8c4ad184dd08|false|false|true|1|5|9|true|false|" + //
				"6|0|0|2cfa02933964567e|4503|9|1|6|6c0892d6a3aeac58|true|0|3|7|6067|5|8" + //
				"|4|true|0|4|3|8553";
		String[] keys4 = { "B", "X", "F", "U", "G", "O", "P", "J", "E", "Q", "S", "I", "Y", "C", "K", "W", "H", "T",
				"M", "A" };
		check(json, key, keys4, 643);

		key = "abc.def:9|1|6|2cfa02933964567e|true|744d8c4ad184dd08|5|8|4|4877" + //
				"|6067|true|false|true|6|0|0|1|5|9";
		String[] keys5 = { "I", "Q", "T", "X", "H", "V", "W", "L", "F", "G", "E", "O" };
		check(json, key, keys5, 744);

	}

	@Test
	public void testD2() throws Exception {

		String json = "{'A':false,'B':489,'C':2870,'D':false,'E':false,'F':'5fc32105128cf569'" + //
				",'G':[9,1,3],'H':150,'I':false,'J':'36cfe458600423b5','K':3244,'L':'78d627437092c13a'" + //
				",'M':'6ff9093e14b796e6','N':[5,7,4],'O':'b3da39a5e58cdbc','P':false,'Q':true" + //
				",'R':true,'S':false,'T':true,'U':true,'V':[0,2,5],'W':true,'X':true,'Y':1988" + //
				"}";

		String key = "QS0cxHm04TN5RdKd/D99oGjIyhhHPZSwSF2lbSL0dzc=";
		String[] keys1 = { "H", "Y", "Q", "F", "P", "X", "E", "I", "C", "W", "M", "R", "D", "A", "J", "K", "V", "N",
				"B", "U", "L" };
		check(json, key, keys1, 44);

		key = "abc.def:78d627437092c13a|1988|3244|b3da39a5e58cdbc|false|true|3" + //
				"6cfe458600423b5|false|false";
		String[] keys2 = { "L", "Y", "K", "O", "E", "Q", "J", "D", "A" };
		check(json, key, keys2, 261);

		key = "abc.def:2870|9|1|3|36cfe458600423b5|5fc32105128cf569|false|true" + //
				"|3244|1988|true|false|150|b3da39a5e58cdbc";
		String[] keys3 = { "C", "G", "J", "F", "D", "T", "K", "Y", "W", "A", "H", "O" };
		check(json, key, keys3, 600);

		key = "abc.def:9|1|3|5|7|4|true|150|5fc32105128cf569|true|0|2|5";
		String[] keys4 = { "G", "N", "T", "H", "F", "R", "V" };
		check(json, key, keys4, 910);

		key = "abc.def:b3da39a5e58cdbc|5fc32105128cf569|true|0|2|5|78d62743709" + //
				"2c13a";
		String[] keys5 = { "O", "F", "X", "V", "L" };
		check(json, key, keys5, 1019);

	}

	@Test
	public void testE2() throws Exception {

		String json = "{'A':false,'B':true,'C':3804,'D':true,'E':true,'F':5225" + //
				",'G':[7,6,7],'H':[7,0,9],'I':[5,1,9],'J':true,'K':true,'L':[3,4,0],'M':[0" + //
				",8,1],'N':5713,'O':false,'P':false,'Q':[2,0,9],'R':[8,4,9],'S':false,'T':true" + //
				",'U':true,'V':true,'W':false,'X':'654e4d866ad59b3d','Y':[4,8,6]}";

		String key = "00OnXsimymCGa49F5aTJV6LI94DZz0J0i46x3K2ty7g=";
		String[] keys1 = { "F", "Y", "R", "S", "K", "B", "U", "G", "V", "W", "M", "E" };
		check(json, key, keys1, 44);

		key = "abc.def:true|false|true|5|1|9|false";
		String[] keys2 = { "D", "W", "K", "I", "A" };
		check(json, key, keys2, 169);

		key = "abc.def:0|8|1|true|3|4|0|false|false|true|true|true|5713|3804|7" + //
				"|6|7|7|0|9|2|0|9|false|5|1|9|true|true|4|8|6|5225|false";
		String[] keys3 = { "M", "D", "L", "O", "W", "E", "J", "U", "N", "C", "G", "H", "Q", "P", "I", "V", "T", "Y",
				"F", "S" };
		check(json, key, keys3, 622);

		key = "abc.def:false|7|0|9|5|1|9|true|false";
		String[] keys4 = { "P", "H", "I", "D", "O" };
		check(json, key, keys4, 1018);

	}

	@Test
	public void testF2() throws Exception {

		String json = "{'A':[7,9,9],'B':false,'C':true,'D':1882,'E':false,'F':7595" + //
				",'G':1444,'H':true,'I':true,'J':[6,6,7],'K':true,'L':[6,7,4],'M':'6363c4a63cf26c1a'" + //
				",'N':[1,4,4],'O':true,'P':8743,'Q':[8,9,0],'R':'624475f3ede217c','S':'3878a55273e61657'" + //
				",'T':'4869b62cbd623ee0','U':true,'V':5738,'W':false,'X':true,'Y':'7ff0b85fc00edd2b'" + //
				"}";

		String key = "2HMgTxDe5Uil4p0pdqO48mU4r//KZtbMh+YvicU8niE=";
		String[] keys1 = { "D", "U", "Q", "C", "R", "W", "J", "N", "B", "E", "S", "T", "M", "V", "H", "K" };
		check(json, key, keys1, 44);

		key = "abc.def:4869b62cbd623ee0|624475f3ede217c|true|5738|8743|6|6|7|t" + //
				"rue|true|1882|6363c4a63cf26c1a|7ff0b85fc00edd2b";
		String[] keys2 = { "T", "R", "K", "V", "P", "J", "C", "O", "D", "M", "Y" };
		check(json, key, keys2, 275);

		key = "abc.def:1882|1|4|4|8743|6|7|4|true|false|true|624475f3ede217c|f" + //
				"alse";
		String[] keys3 = { "D", "N", "P", "L", "C", "E", "O", "R", "W" };
		check(json, key, keys3, 732);

		key = "abc.def:1|4|4|false|false|true|5738|3878a55273e61657|6363c4a63c" + //
				"f26c1a|false|7ff0b85fc00edd2b|true|7|9|9|8|9|0|true|6|6|7|4869b62cbd62" + //
				"3ee0|624475f3ede217c|6|7|4|8743";
		String[] keys4 = { "N", "E", "B", "I", "V", "S", "M", "W", "Y", "C", "A", "Q", "U", "J", "T", "R", "L", "P" };
		check(json, key, keys4, 989);

	}

	@Test
	public void testG2() throws Exception {

		String json = "{'A':'b0d9368e840eeef','B':'41586917dc9a454a','C':'7420d272dff7ee66'" + //
				",'D':false,'E':false,'F':true,'G':'4d38c845127eb429','H':false,'I':[0,9" + //
				",9],'J':true,'K':false,'L':'2eab98e375dae4e3','M':false,'N':[9,2,8],'O':true" + //
				",'P':[7,0,3],'Q':'5c73aebbe17353b7','R':false,'S':5847,'T':5308,'U':false" + //
				",'V':'f2e6b60ea3821c6','W':[9,3,2],'X':true,'Y':false}";

		String key = "BoV5+WbkTHLqAQD4BKFx3FKw3BXxuj51j6BJVPUriWY=";
		String[] keys1 = { "H", "B", "M", "T", "F", "J", "U", "Y", "R", "O", "E", "D", "W", "S", "L", "I", "A", "V",
				"Q" };
		check(json, key, keys1, 44);

		key = "abc.def:true|false|b0d9368e840eeef|7|0|3|f2e6b60ea3821c6|415869" + //
				"17dc9a454a|true|false|5308";
		String[] keys2 = { "J", "R", "A", "P", "V", "B", "F", "M", "T" };
		check(json, key, keys2, 516);

		key = "abc.def:false|false|9|2|8|b0d9368e840eeef|false|7|0|3|2eab98e37" + //
				"5dae4e3|true|true|f2e6b60ea3821c6|5c73aebbe17353b7|9|3|2|5308|5847|tru" + //
				"e|false";
		String[] keys3 = { "E", "M", "N", "A", "K", "P", "L", "X", "F", "V", "Q", "W", "T", "S", "O", "D" };
		check(json, key, keys3, 770);

	}

	@Test
	public void testH2() throws Exception {

		String json = "{'A':[6,2,3],'B':144,'C':3287,'D':true,'E':'669b52149dfbbbf3'" + //
				",'F':false,'G':6932,'H':true,'I':[9,6,8],'J':'2c35d61b90fa6b56','K':true" + //
				",'L':4381,'M':[3,0,4],'N':5187,'O':'28fda2b5cf28a537','P':true,'Q':3598" + //
				",'R':false,'S':'2a08a366cc67bfeb','T':9858,'U':false,'V':'711f2337f0399516'" + //
				",'W':'f747cb5b30cc990','X':false,'Y':[4,6,0]}";

		String key = "g3NVtrvEqMm6ABEY+8ztsJw8C6cDpMq6g9YysvQiPEU=";
		String[] keys1 = { "V", "H", "U", "R", "S", "P", "K", "E", "M", "X" };
		check(json, key, keys1, 44);

		key = "abc.def:28fda2b5cf28a537|711f2337f0399516|true|3287|f747cb5b30c" + //
				"c990|2a08a366cc67bfeb|false|true|669b52149dfbbbf3|6|2|3|false";
		String[] keys2 = { "O", "V", "K", "C", "W", "S", "R", "P", "E", "A", "X" };
		check(json, key, keys2, 241);

		key = "abc.def:3287|6932|3|0|4|true|false|2a08a366cc67bfeb|2c35d61b90f" + //
				"a6b56|711f2337f0399516|false|9858|true|669b52149dfbbbf3|144|false|5187" + //
				"|9|6|8|3598|f747cb5b30cc990|6|2|3|4381|28fda2b5cf28a537|false";
		String[] keys3 = { "C", "G", "M", "D", "F", "S", "J", "V", "R", "T", "P", "E", "B", "X", "N", "I", "Q", "W",
				"A", "L", "O", "U" };
		check(json, key, keys3, 442);

		key = "abc.def:6932|true|2a08a366cc67bfeb|4|6|0|false|true|9|6|8|6|2|3" + //
				"|711f2337f0399516|false";
		String[] keys4 = { "G", "H", "S", "Y", "U", "K", "I", "A", "V", "X" };
		check(json, key, keys4, 539);

		key = "abc.def:28fda2b5cf28a537|3|0|4|144|false|f747cb5b30cc990|false|" + //
				"669b52149dfbbbf3|false|6|2|3|9|6|8|4381|3287|false|9858|2a08a366cc67bf" + //
				"eb|true|true|5187|3598|6932|true|2c35d61b90fa6b56|true|711f2337f039951" + //
				"6";
		String[] keys5 = { "O", "M", "B", "U", "W", "X", "E", "F", "A", "I", "L", "C", "R", "T", "S", "P", "K", "N",
				"Q", "G", "H", "J", "D", "V" };
		check(json, key, keys5, 1012);

	}

	@Test
	public void testI2() throws Exception {

		String json = "{'A':2757,'B':[1,6,7],'C':true,'D':'6d7d070af0a31129'" + //
				",'E':7182,'F':'170b2594d49eba3','G':false,'H':true,'I':'303e01f5a65dc688'" + //
				",'J':false,'K':2081,'L':true,'M':952,'N':2109,'O':true,'P':[7,3,6],'Q':'7492aba17c35fc3a'" + //
				",'R':2145,'S':true,'T':279,'U':false,'V':7405,'W':1622,'X':2085,'Y':false" + //
				"}";

		String key = "hWIahJ74NC+7VONq7stH5nZm8riSDcYK0AuHIzLpqrU=";
		String[] keys1 = { "A", "Y", "J", "D", "E", "T", "P", "N", "X", "K", "W", "M", "V", "B", "U", "F", "S", "O",
				"I" };
		check(json, key, keys1, 44);

		key = "abc.def:279|1|6|7|303e01f5a65dc688|2085|170b2594d49eba3|7|3|6|f" + //
				"alse|false|7492aba17c35fc3a|2081|1622|2145";
		String[] keys2 = { "T", "B", "I", "X", "F", "P", "Y", "U", "Q", "K", "W", "R" };
		check(json, key, keys2, 403);

		key = "abc.def:true|2145|7|3|6|1|6|7|1622|false|2081|170b2594d49eba3|3" + //
				"03e01f5a65dc688|true|279|false|2109|true|false|false";
		String[] keys3 = { "S", "R", "P", "B", "W", "G", "K", "F", "I", "O", "T", "Y", "N", "L", "U", "J" };
		check(json, key, keys3, 725);

		key = "abc.def:279|1622|7|3|6|2109|170b2594d49eba3|true|false";
		String[] keys4 = { "T", "W", "P", "N", "F", "H", "U" };
		check(json, key, keys4, 1018);

	}

	@Test
	public void testJ2() throws Exception {

		String json = "{'A':'552fa1436ba5ea9d','B':true,'C':true,'D':false,'E':[2" + //
				",6,7],'F':3193,'G':[0,7,6],'H':false,'I':5313,'J':[2,6,5],'K':[9,4,9],'L':false" + //
				",'M':false,'N':true,'O':true,'P':'31e8ad5aed496611','Q':'7783cd52a2f48f26'" + //
				",'R':false,'S':[7,6,6],'T':true,'U':[8,3,7],'V':'652042df6c4007cb','W':'1fb6b1314fb6ffdf'" + //
				",'X':1114,'Y':false}";

		String key = "abc.def:false";
		String[] keys1 = { "D" };
		check(json, key, keys1, 44);

		key = "abc.def:1fb6b1314fb6ffdf|2|6|7|true|false|1114|9|4|9|false|true" + //
				"|652042df6c4007cb|true|31e8ad5aed496611|false|0|7|6|552fa1436ba5ea9d|f" + //
				"alse|8|3|7|3193|true|true|false|2|6|5|false|7|6|6|5313|7783cd52a2f48f2" + //
				"6";
		String[] keys2 = { "W", "E", "N", "R", "X", "K", "H", "T", "V", "O", "P", "D", "G", "A", "M", "U", "F", "B",
				"C", "Y", "J", "L", "S", "I", "Q" };
		check(json, key, keys2, 465);

		key = "abc.def:5313|3193|2|6|5|false|false|false|0|7|6|true|7783cd52a2" + //
				"f48f26|true|2|6|7|652042df6c4007cb";
		String[] keys3 = { "I", "F", "J", "L", "R", "H", "G", "O", "Q", "N", "E", "V" };
		check(json, key, keys3, 582);

		key = "abc.def:true|false|false|false|true|0|7|6|1114|7|6|6|9|4|9|fals" + //
				"e|2|6|5|8|3|7|7783cd52a2f48f26|false";
		String[] keys4 = { "T", "M", "Y", "R", "C", "G", "X", "S", "K", "L", "J", "U", "Q", "D" };
		check(json, key, keys4, 688);

		key = "abc.def:8|3|7|true|true|7783cd52a2f48f26|false|2|6|7|9|4|9|1fb6" + //
				"b1314fb6ffdf|false|false";
		String[] keys5 = { "U", "C", "T", "Q", "D", "E", "K", "W", "R", "L" };
		check(json, key, keys5, 741);

		key = "abc.def:1fb6b1314fb6ffdf|2|6|7|31e8ad5aed496611|true|false|9|4|" + //
				"9|552fa1436ba5ea9d|false|false|652042df6c4007cb|1114|2|6|5|false|0|7|6" + //
				"|true|7|6|6|8|3|7|5313|true|false|3193|7783cd52a2f48f26|true|true";
		String[] keys6 = { "W", "E", "P", "T", "H", "K", "A", "M", "R", "V", "X", "J", "L", "G", "O", "S", "U", "I",
				"N", "D", "F", "Q", "B", "C" };
		check(json, key, keys6, 1021);

	}

	@Test
	public void testK2() throws Exception {

		String json = "{'A':true,'B':true,'C':true,'D':[7,8,6],'E':352,'F':[7" + //
				",8,6],'G':3254,'H':false,'I':true,'J':[1,9,1],'K':9909,'L':[2,4,6],'M':[4" + //
				",7,5],'N':true,'O':3862,'P':false,'Q':true,'R':false,'S':false,'T':false" + //
				",'U':false,'V':344,'W':5683,'X':false,'Y':[4,2,6]}";

		String key = "0gHizQHGJjOOuAoqk6dEHyQ1r19M6qqLYWa+GOsgYOY=";
		String[] keys1 = { "E", "G", "S", "L", "Q", "M", "K", "N", "R", "P", "Y", "W", "H", "U", "J", "D", "O", "X",
				"F", "C" };
		check(json, key, keys1, 44);

		key = "abc.def:4|2|6|false|true|4|7|5|true|false|true|false|false|1|9|" + //
				"1|7|8|6|3254|2|4|6|7|8|6|false|true|true|3862|352|5683";
		String[] keys2 = { "Y", "T", "N", "M", "B", "R", "C", "U", "P", "J", "D", "G", "L", "F", "H", "Q", "A", "O",
				"E", "W" };
		check(json, key, keys2, 303);

		key = "abc.def:false|5683|4|2|6|false|7|8|6|352|true|false|true|9909|7" + //
				"|8|6|3862|true|true|false|344|false|false|2|4|6|true|1|9|1|4|7|5|true|" + //
				"false";
		String[] keys3 = { "U", "W", "Y", "H", "F", "E", "B", "R", "A", "K", "D", "O", "Q", "C", "T", "V", "P", "S",
				"L", "I", "J", "M", "N", "X" };
		check(json, key, keys3, 471);

		key = "abc.def:344|true|5683|false|352|true|1|9|1";
		String[] keys4 = { "V", "I", "W", "X", "E", "B", "J" };
		check(json, key, keys4, 717);

	}

	@Test
	public void testL2() throws Exception {

		String json = "{'A':[0,6,1],'B':141,'C':true,'D':[7,0,0],'E':2643,'F':false" + //
				",'G':true,'H':9276,'I':true,'J':false,'K':true,'L':'68d29dfd940888ac','M':'467cd88aff9a7824'" + //
				",'N':false,'O':false,'P':false,'Q':false,'R':4123,'S':6734,'T':[8,3,1]" + //
				",'U':true,'V':3419,'W':597,'X':false,'Y':4828}";

		String key = "bGVlfEiZaq7rX6DYGIqRH91H4rMmF5QKCmYtzS5Lwpg=";
		String[] keys1 = { "N", "Q", "Y", "D", "P", "V", "O", "J", "U", "X", "F", "A", "R", "I", "M", "C", "S", "K",
				"L", "E", "B", "G", "H", "W" };
		check(json, key, keys1, 44);

		key = "abc.def:true|true|false|false|true|141|4123|true|0|6|1";
		String[] keys2 = { "U", "I", "O", "X", "G", "B", "R", "K", "A" };
		check(json, key, keys2, 459);

		key = "abc.def:true|467cd88aff9a7824|8|3|1|false|false|4828|4123|68d29" + //
				"dfd940888ac";
		String[] keys3 = { "U", "M", "T", "F", "Q", "Y", "R", "L" };
		check(json, key, keys3, 893);

		key = "abc.def:68d29dfd940888ac|true|true|0|6|1|141|false|4123|true|tr" + //
				"ue|4828|3419|2643|467cd88aff9a7824|false|false|true|597|6734|7|0|0|fal" + //
				"se|8|3|1|9276|false|false";
		String[] keys4 = { "L", "G", "U", "A", "B", "P", "R", "C", "K", "Y", "V", "E", "M", "N", "X", "I", "W", "S",
				"D", "Q", "T", "H", "J", "F" };
		check(json, key, keys4, 954);

	}

	@Test
	public void testM2() throws Exception {

		String json = "{'A':true,'B':true,'C':[2,1,7],'D':true,'E':false,'F':false" + //
				",'G':'8295ae7c8e62c4c','H':true,'I':false,'J':[9,9,0],'K':2983,'L':4061" + //
				",'M':[6,8,4],'N':1181,'O':true,'P':true,'Q':[1,0,4],'R':1388,'S':1644,'T':2146" + //
				",'U':6061,'V':3049,'W':false,'X':'16282b3dfdcf447e','Y':false}";

		String key = "ehDAl1LZI6Gpk+CKKa0Wexmhw1H6uazSynmkNOmq/Ws=";
		String[] keys1 = { "J", "I", "P", "R", "S", "A", "G", "T", "M", "V", "K", "L", "U", "Q", "O", "B", "C", "W",
				"E", "Y", "H", "F" };
		check(json, key, keys1, 44);

		key = "abc.def:true|3049|true|4061|1388|false|2|1|7|16282b3dfdcf447e|t" + //
				"rue";
		String[] keys2 = { "D", "V", "P", "L", "R", "W", "C", "X", "H" };
		check(json, key, keys2, 231);

		key = "abc.def:2983|true|8295ae7c8e62c4c|2146|true|false|false|true|fa" + //
				"lse|true|1181|1388|false|true|1644|true|16282b3dfdcf447e|2|1|7";
		String[] keys3 = { "K", "O", "G", "T", "B", "W", "I", "P", "E", "D", "N", "R", "F", "H", "S", "A", "X", "C" };
		check(json, key, keys3, 680);

		key = "abc.def:1181|8295ae7c8e62c4c|false|6061|true|true|2|1|7|2146|fa" + //
				"lse|4061|false|16282b3dfdcf447e|true|6|8|4|9|9|0|3049|1644|1388|2983|t" + //
				"rue";
		String[] keys4 = { "N", "G", "Y", "U", "A", "H", "C", "T", "I", "L", "W", "X", "O", "M", "J", "V", "S", "R",
				"K", "D" };
		check(json, key, keys4, 804);

	}

	@Test
	public void testN2() throws Exception {

		String json = "{'A':'677c6a8dfcc0e9b4','B':4580,'C':false,'D':'4e94f1cb98221b48'" + //
				",'E':'5da0cb0d997bce86','F':false,'G':9236,'H':[5,2,1],'I':'5b9ab25291b6a7d2'" + //
				",'J':'21363d802907d757','K':true,'L':true,'M':[2,0,7],'N':6015,'O':8328" + //
				",'P':[5,5,6],'Q':true,'R':false,'S':[1,5,3],'T':false,'U':false,'V':'780ad8b8a3f0665b'" + //
				",'W':5836,'X':7814,'Y':false}";

		String key = "QHwRQroSv0dfR1SgNyDMrwH4OGWpW01B/S79y1BN1IU=";
		String[] keys1 = { "R", "P", "V", "G", "E", "U", "Y", "L", "C", "H", "I", "F", "Q", "O", "T", "D", "X" };
		check(json, key, keys1, 44);

		key = "abc.def:true|false|5da0cb0d997bce86|780ad8b8a3f0665b|9236|5|5|6" + //
				"|5836|7814|1|5|3|2|0|7|21363d802907d757|5b9ab25291b6a7d2|false|true|4e" + //
				"94f1cbFtLSsB8TVMJpF07Or/9LJ1hIc8ZnfLuFkOXzNB7ONzs=";
		String[] keys2 = { "L", "C", "E", "V", "G", "P", "W", "X", "S", "M", "J", "I", "T", "K", "D", "Q", "Y", "R",
				"A", "U", "F" };
		check(json, key, keys2, 183);

		key = "abc.def:5|5|6|5da0cb0d997bce86|4580|780ad8b8a3f0665b|false|fals" + //
				"e|5b9ab25291b6a7d2|9236|false|21363d802907d757|677c6a8dfcc0e9b4|4e94f1" + //
				"cb98221b48|5|2|1|true|7814|false|true|2|0|7|false|5836|true|8328|1|5|3" + //
				"|false";
		String[] keys3 = { "P", "E", "B", "V", "F", "U", "I", "G", "Y", "J", "A", "D", "H", "Q", "X", "C", "K", "M",
				"R", "W", "L", "O", "S", "T" };
		check(json, key, keys3, 432);

		key = "abc.def:780ad8b8a3f0665b|4e94f1cb98221b48|5|5|6|2|0|7|false|tru" + //
				"e";
		String[] keys4 = { "V", "D", "P", "M", "F", "L" };
		check(json, key, keys4, 440);

		key = "abc.def:1|5|3|9236|7814|false|21363d802907d757|2|0|7|677c6a8dfc" + //
				"c0e9b4|5da0cb0d997bce86|false|5836|false|5|5|6|4580|true|true|false";
		String[] keys5 = { "S", "G", "X", "C", "J", "M", "A", "E", "T", "W", "R", "P", "B", "L", "Q", "Y" };
		check(json, key, keys5, 615);

		key = "abc.def:1|5|3|false|780ad8b8a3f0665b|false|false|false|true|tru" + //
				"e|false|5b9ab25291b6a7d2|5|5|6|false|7814";
		String[] keys6 = { "S", "Y", "V", "F", "C", "R", "Q", "K", "U", "I", "P", "T", "X" };
		check(json, key, keys6, 740);

	}

	@Test
	public void testO2() throws Exception {

		String json = "{'A':true,'B':[4,6,5],'C':[2,4,7],'D':[3,7,2],'E':[6,6" + //
				",4],'F':'3c4aebb7a3efda3d','G':3041,'H':false,'I':'6365e278a4d23d9b','J':false" + //
				",'K':false,'L':true,'M':false,'N':'1295ece6edb085b1','O':false,'P':'15bd4a2820489e3f'" + //
				",'Q':true,'R':true,'S':true,'T':false,'U':true,'V':[7,6,9],'W':true,'X':'1f94584640f1a372'" + //
				",'Y':'1b2ef65573a3c017'}";

		String key = "XE/ZxvXfHRXdqRH7bo5qRwqN5qXoFz9ucXQVe78tSTA=";
		String[] keys1 = { "U", "F", "C", "S", "B", "T", "W", "M", "R", "N", "P", "O", "H", "D", "G", "J" };
		check(json, key, keys1, 44);

		key = "abc.def:false|true|1f94584640f1a372|trueOPUDvQi36RUOOJFN2jkapm6" + //
				"MWb+Uv5cpnmuLXqGtGWE=";
		String[] keys2 = { "H", "R", "X", "L", "I", "N", "D", "O", "T", "C", "E", "M", "Y", "P", "A", "V", "J", "W",
				"B", "F" };
		check(json, key, keys2, 84);

		key = "abc.def:3c4aebb7a3efda3d|6|6|4|3041|3|7|2|15bd4a2820489e3f|fals" + //
				"e|1f94584640f1a372|true|false|true|true|false|true|true|false|1b2ef655" + //
				"73a3c017|6365e278a4d23d9b|false|4|6|5|false|2|4|7|true|7|6|9|1295ece6e" + //
				"db085b1";
		String[] keys3 = { "F", "E", "G", "D", "P", "K", "X", "S", "H", "W", "L", "O", "A", "U", "M", "Y", "I", "T",
				"B", "J", "C", "Q", "V", "N" };
		check(json, key, keys3, 415);

		key = "abc.def:6|6|4|false|3041|1295ece6edb085b1|6365e278a4d23d9b|3|7|" + //
				"2|false";
		String[] keys4 = { "E", "M", "G", "N", "I", "D", "T" };
		check(json, key, keys4, 492);

		key = "abc.def:true|6|6|4|false|false|1f94584640f1a372|4|6|5|false|15b" + //
				"d4a2820489e3f|6365e278a4d23d9b|3|7|2|7|6|9|true|false|3c4aebb7a3efda3d" + //
				"|3041|1b2ef65573a3c017|false|false|true|true";
		String[] keys5 = { "U", "E", "T", "M", "X", "B", "K", "P", "I", "D", "V", "A", "J", "F", "G", "Y", "H", "O",
				"R", "Q" };
		check(json, key, keys5, 871);

	}

	@Test
	public void testP2() throws Exception {

		String json = "{'A':false,'B':[4,9,6],'C':[1,9,9],'D':9713,'E':[6,4,5]" + //
				",'F':[2,0,6],'G':false,'H':'6d0f69c245fa9063','I':false,'J':true,'K':4824" + //
				",'L':[8,6,7],'M':[8,8,6],'N':9905,'O':[1,6,1],'P':false,'Q':false,'R':false" + //
				",'S':[4,0,0],'T':[8,5,7],'U':'5367edd9ce69ba2d','V':9305,'W':false,'X':'5e7f0631232cd0a9'" + //
				",'Y':true}";

		String key = "W/1Fq4PTnABBsTkBa2P5MrfdWHOhnF/B4p83ddbaMqE=";
		String[] keys1 = { "L", "A", "V", "G", "I", "O", "H", "S", "R", "U", "T", "M", "F", "C" };
		check(json, key, keys1, 44);

		key = "abc.def:5367edd9ce69ba2d|4|9|6|4|0|0|false|4824|false|false|930" + //
				"5|8|5|7|6|4|5|true|5e7f0631232cd0a9|9905|false|true|1|9|9|6d0f69c245fa" + //
				"9063|9713|false|false|8|6|7|2|0|6|1|6|1";
		String[] keys2 = { "U", "B", "S", "Q", "K", "A", "G", "V", "T", "E", "J", "X", "N", "W", "Y", "C", "H", "D",
				"I", "R", "L", "F", "O" };
		check(json, key, keys2, 471);

		key = "abc.def:5e7f0631232cd0a9|1|6|1|8|6|7|5367edd9ce69ba2d|false|fal" + //
				"se|false|6|4|5|1|9|9|9713|true|9305|false|false|4|9|6|6d0f69c245fa9063" + //
				"|2|0|6|9905";
		String[] keys3 = { "X", "O", "L", "U", "A", "Q", "G", "E", "C", "D", "Y", "V", "W", "I", "B", "H", "F", "N" };
		check(json, key, keys3, 602);

	}

	@Test
	public void testQ2() throws Exception {

		String json = "{'A':'956b749a7287942','B':false,'C':[5,9,4],'D':490,'E':[1" + //
				",5,2],'F':[4,2,3],'G':'5fb711b262935abc','H':false,'I':9990,'J':8773,'K':'3cafe98e1743ac42'" + //
				",'L':true,'M':5119,'N':true,'O':[7,6,1],'P':'2bff9636fd2d66b7','Q':'2a4c8ecb495c90fc'" + //
				",'R':'331cc78069cecf54','S':true,'T':true,'U':'7f853a833d95dce0','V':false" + //
				",'W':3841,'X':2615,'Y':[8,0,9]}";

		String key = "3TAXJA8jwV5e5pCGHhdSTYouoNt5Lyf5bVTj2NaMCwg=";
		String[] keys1 = { "I", "U", "V", "B", "X", "Q", "K", "N", "M", "A", "F", "O", "T", "H", "J", "P", "D", "L",
				"W", "E" };
		check(json, key, keys1, 44);

		key = "abc.def:4|2|3|true|true|331cc78069cecf54";
		String[] keys2 = { "F", "L", "N", "R" };
		check(json, key, keys2, 207);

		key = "abc.def:1|5|2|true|5119|9990";
		String[] keys3 = { "E", "N", "M", "I" };
		check(json, key, keys3, 648);

	}

	@Test
	public void testR2() throws Exception {

		String json = "{'A':false,'B':[8,7,5],'C':3908,'D':'20c45cf3e9d35278'" + //
				",'E':false,'F':[5,8,6],'G':[8,0,7],'H':[8,4,5],'I':4379,'J':'cbfc144245716f3'" + //
				",'K':2258,'L':[6,7,8],'M':true,'N':false,'O':[7,1,3],'P':'4df58a6c10da62cf'" + //
				",'Q':false,'R':[0,6,3],'S':[9,0,3],'T':8910,'U':'43afdadc84513340','V':398" + //
				",'W':9871,'X':true,'Y':[9,7,0]}";

		String key = "tTA9WsY9wYwfjS9teTb3Wh1bot9oNBiZx78g8vrljC8=";
		String[] keys1 = { "E", "R", "L", "N", "I", "D", "M", "Y", "V", "G", "X", "O", "J", "B" };
		check(json, key, keys1, 44);

		key = "abc.def:5|8|6|6|7|8|3908|8|7|5|8|4|5|8910|4df58a6c10da62cf|20c4" + //
				"5cf3e9d35278|43afdadc84513340|false";
		String[] keys2 = { "F", "L", "C", "B", "H", "T", "P", "D", "U", "A" };
		check(json, key, keys2, 186);

		key = "abc.def:8|7|5|9|7|0|true|43afdadc84513340|20c45cf3e9d35278|6|7|" + //
				"8|false";
		String[] keys3 = { "B", "Y", "M", "U", "D", "L", "E" };
		check(json, key, keys3, 390);

		key = "abc.def:7|1|3|43afdadc84513340|3908|false|20c45cf3e9d35278|8|0|" + //
				"7|8|4|5|false|8910|cbfc144245716f3|true|9|7|0|false";
		String[] keys4 = { "O", "U", "C", "Q", "D", "G", "H", "N", "T", "J", "M", "Y", "E" };
		check(json, key, keys4, 868);

		key = "abc.def:true|4df58a6c10da62cf|3908|8|0|7|false|false|false|2258" + //
				"|false";
		String[] keys5 = { "M", "P", "C", "G", "Q", "E", "N", "K", "A" };
		check(json, key, keys5, 980);

	}

	@Test
	public void testS2() throws Exception {

		String json = "{'A':true,'B':6582,'C':2485,'D':true,'E':false,'F':false" + //
				",'G':4125,'H':false,'I':false,'J':true,'K':false,'L':true,'M':false,'N':7906" + //
				",'O':true,'P':false,'Q':'4885f20d123d1868','R':false,'S':[9,7,1],'T':'36b050d2a2a19454'" + //
				",'U':true,'V':'2ee23694e905a8f8','W':'20e458642bca4cee','X':[2,9,7],'Y':'cd706fa3138b657'" + //
				"}";

		String key = "qnpJPpEYSdl+hoU89mGpkxZfeKf9bSEF8VFMS9y4tsM=";
		String[] keys1 = { "U", "X", "D", "E", "Y", "I", "T", "M", "H", "C", "N", "G", "P", "J", "S", "W" };
		check(json, key, keys1, 44);

		key = "abc.def:false|false|cd706fa3138b657|true|false|9|7|1|true|false" + //
				"|2|9|7|36b050d2a2a19454|false|2485|true|7906|false|false|true|true|fal" + //
				"se|4885f20d123d1868|true";
		String[] keys2 = { "E", "I", "Y", "J", "F", "S", "A", "P", "X", "T", "H", "C", "L", "N", "M", "R", "O", "U",
				"K", "Q", "D" };
		check(json, key, keys2, 393);

		key = "abc.def:36b050d2a2a19454|9|7|1|true|false|false|false|20e458642" + //
				"bca4cee|false|2ee23694e905a8f8|false|2|9|7|4885f20d123d1868|true|true|" + //
				"2485|6582|7906|true|cd706fa3138b657|true|true";
		String[] keys3 = { "T", "S", "A", "I", "R", "F", "W", "K", "V", "H", "X", "Q", "D", "U", "C", "B", "N", "O",
				"Y", "L", "J" };
		check(json, key, keys3, 395);

		key = "abc.def:false|2485|cd706fa3138b657|false|false|6582|20e458642bc" + //
				"a4cee|4885f20d123d1868|false";
		String[] keys4 = { "I", "C", "Y", "R", "H", "B", "W", "Q", "P" };
		check(json, key, keys4, 702);

		key = "abc.def:false|false|6582|false|true|4125|false|4885f20d123d1868" + //
				"|false|9|7|1|true|true|2ee23694e905a8f8|true|36b050d2a2a19454|true|cd7" + //
				"06fa3138b657";
		String[] keys5 = { "E", "P", "B", "K", "D", "G", "R", "Q", "F", "S", "J", "O", "V", "L", "T", "U", "Y" };
		check(json, key, keys5, 708);

		key = "abc.def:true|false|false|cd706fa3138b657|true|4125|2|9|7|36b050" + //
				"d2a2a19454|4885f20d123d1868|true|6582|2485|20e458642bca4cee|false|fals" + //
				"e|false|false|2ee23694e905a8f8|true|9|7|1|false|7906";
		String[] keys6 = { "D", "I", "H", "Y", "O", "G", "X", "T", "Q", "L", "B", "C", "W", "K", "M", "P", "F", "V",
				"U", "S", "E", "N" };
		check(json, key, keys6, 1002);

	}

	@Test
	public void testT2() throws Exception {

		String json = "{'A':9078,'B':7113,'C':'5a406524784daa30','D':true,'E':'6d5ad497628bfb17'" + //
				",'F':3962,'G':false,'H':[1,5,5],'I':7786,'J':3008,'K':false,'L':true,'M':6629" + //
				",'N':[2,0,1],'O':true,'P':6721,'Q':false,'R':6036,'S':[7,7,2],'T':6507" + //
				",'U':true,'V':[2,9,3],'W':'3c1bb2983b0a2a06','X':'51af67a99fb2d516','Y':'2ca3b5a9cd629c50'" + //
				"}";

		String key = "abc.def:3008|9078";
		String[] keys1 = { "J", "A" };
		check(json, key, keys1, 44);

		key = "abc.def:5a406524784daa30|51af67a99fb2d516|false|6036|6507|false" + //
				"|true|90a1gwmEoGm5ir8eFXlPjshqo/ZmkWnIqLtRTlW6S48bQ=";
		String[] keys2 = { "C", "X", "G", "R", "T", "Q", "U", "A", "W", "V", "P", "S", "H", "B", "I", "K", "E", "Y",
				"M" };
		check(json, key, keys2, 115);

		key = "abc.def:7113|false|5a406524784daa30|true|51af67a99fb2d516|6d5ad" + //
				"497628bfb17|3008|9078|6721|6036|2|9|3|true|3962|true|false|6629|true|2" + //
				"ca3b5a9cd629c50|6507|1|5|5|7|7|2";
		String[] keys3 = { "B", "G", "C", "O", "X", "E", "J", "A", "P", "R", "V", "L", "F", "D", "Q", "M", "U", "Y",
				"T", "H", "S" };
		check(json, key, keys3, 467);

		key = "abc.def:6d5ad497628bfb17|2ca3b5a9cd629c50|7786|7|7|2|9078|true|" + //
				"2|0|1|5a406524784daa30|true|7113|6721|false";
		String[] keys4 = { "E", "Y", "I", "S", "A", "O", "N", "C", "L", "B", "P", "Q" };
		check(json, key, keys4, 665);

	}

	@Test
	public void testU2() throws Exception {

		String json = "{'A':8730,'B':'669a7385b8801a19','C':'6cf7a5a9e8aaac72'" + //
				",'D':true,'E':5305,'F':8390,'G':'49078e68a510b323','H':'50194a9adcec0d5f'" + //
				",'I':'4ad5a0597fdfcefb','J':'2baa408bbc8da787','K':'6799f7829792f865','L':false" + //
				",'M':false,'N':9821,'O':false,'P':6522,'Q':false,'R':[4,7,5],'S':[8,5,7]" + //
				",'T':'789e7d6c1885b4e','U':[2,9,1],'V':6091,'W':8753,'X':[5,1,2],'Y':'430a3b4ebab633ac'" + //
				"}";

		String key = "IV3y9+WgPGMfsRqGbGfXKos6jPr8jl7LFDqtybZr3pw=";
		String[] keys1 = { "Y", "U", "O", "A", "S", "D", "M", "V", "I", "H", "G" };
		check(json, key, keys1, 44);

		key = "abc.def:8753|50194a9adcec0d5f|false|789e7d6c1885b4e|5305|9821|4" + //
				"9078e68a510b323|false|669a7385b8801a19|6cf7a5a9e8aaac72|430a3b4ebab633" + //
				"ac|6091|5|1|2|2baa408bbc8da787|4|7|5";
		String[] keys2 = { "W", "H", "M", "T", "E", "N", "G", "O", "B", "C", "Y", "V", "X", "J", "R" };
		check(json, key, keys2, 219);

		key = "abc.def:9821|430a3b4ebab633ac|6799f7829792f865|4ad5a0597fdfcefb" + //
				"|false|8|5|7|6cf7a5a9e8aaac72|669a7385b8801a19|false|789e7d6c1885b4e|5" + //
				"305|4|7|5|8753|8730|8390|50194a9adcec0d5f|2baa408bbc8da787|6522|49078e" + //
				"68a510b323|true|2|9|1|false|6091";
		String[] keys3 = { "N", "Y", "K", "I", "Q", "S", "C", "B", "O", "T", "E", "R", "W", "A", "F", "H", "J", "P",
				"G", "D", "U", "L", "V" };
		check(json, key, keys3, 542);

		key = "abc.def:50194a9adcec0d5f|true|5305|8753|8|5|7|false|false|669a7" + //
				"385b8801a19|6799f7829792f865";
		String[] keys4 = { "H", "D", "E", "W", "S", "O", "M", "B", "K" };
		check(json, key, keys4, 572);

		key = "abc.def:false|9821|6cf7a5a9e8aaac72|8753|8390|789e7d6c1885b4e|4" + //
				"9078e68a510b323|8|5|7|true|5305|false|false|2baa408bbc8da787|6522|5019" + //
				"4a9adcec0d5f";
		String[] keys5 = { "L", "N", "C", "W", "F", "T", "G", "S", "D", "E", "M", "Q", "J", "P", "H" };
		check(json, key, keys5, 852);

	}

	@Test
	public void testV2() throws Exception {

		String json = "{'A':false,'B':true,'C':7646,'D':false,'E':7431,'F':true" + //
				",'G':[0,9,4],'H':false,'I':[0,1,5],'J':'3cba07bf014abdda','K':'3af2f31dc5aa5453'" + //
				",'L':2595,'M':false,'N':'6e6ec0f0566a5dfe','O':[6,3,9],'P':[7,3,6],'Q':3875" + //
				",'R':[8,6,1],'S':[8,2,9],'T':true,'U':true,'V':[8,5,2],'W':'21df5ea6047f7e79'" + //
				",'X':[1,8,0],'Y':'4e64cfef186680b1'}";

		String key = "LsvN6MQJn4sF2Tibfl8mwSZzw6s6KE99Ay+7Pui0Kl4=";
		String[] keys1 = { "F", "L", "H", "R", "C", "Y", "X", "J", "E", "P", "K", "A", "T", "B", "O", "S", "M", "V",
				"N", "Q", "U", "I", "G", "W" };
		check(json, key, keys1, 44);

		key = "abc.def:3875|true|2595|7|3|6|7646|0|9|4|true|false|true|4e64cfe" + //
				"f186680b1|6e6ec0f0566a5dfe|false|0|1|5|8|2|9|false";
		String[] keys2 = { "Q", "F", "L", "P", "C", "G", "B", "A", "T", "Y", "N", "H", "I", "S", "D" };
		check(json, key, keys2, 475);

		key = "abc.def:21df5ea6047f7e79|true|7431|true|4e64cfef186680b1";
		String[] keys3 = { "W", "U", "E", "F", "Y" };
		check(json, key, keys3, 878);

	}

	@Test
	public void testW2() throws Exception {

		String json = "{'A':'405a02dd5a4cd638','B':true,'C':7146,'D':'7bfc2cd5369c6b77'" + //
				",'E':true,'F':6867,'G':true,'H':false,'I':false,'J':[2,7,5],'K':true,'L':3710" + //
				",'M':[1,2,0],'N':true,'O':'50709dc89de981af','P':[6,4,8],'Q':1285,'R':815" + //
				",'S':[7,8,1],'T':[2,1,0],'U':false,'V':true,'W':'4d62156262938ed9','X':[2" + //
				",0,3],'Y':true}";

		String key = "/MrZMQX/AGvkS/Ee0a9ic7Q3l5rb9XXxG2Nl2ruHSas=";
		String[] keys1 = { "W", "K", "U", "B", "C", "L", "R", "H", "O", "A", "Q", "P", "Y", "M", "S", "I", "E", "F",
				"T" };
		check(json, key, keys1, 44);

		key = "abc.def:50709dc89de981af|6867|true|false|1|2|0|true|true|1285|4" + //
				"05a02dd5a4cd638|4d62156262938ed9|7bfc2cd5369c6b77|2|1|0|6|4|8|815|2|7|" + //
				"5|3710";
		String[] keys2 = { "O", "F", "V", "H", "M", "G", "K", "Q", "A", "W", "D", "T", "P", "R", "J", "L" };
		check(json, key, keys2, 161);

		key = "abc.def:4d62156262938ed9|815|6867|1|2|0";
		String[] keys3 = { "W", "R", "F", "M" };
		check(json, key, keys3, 576);

		key = "abc.def:50709dc89de981af|true|true|4d62156262938ed9|1|2|0";
		String[] keys4 = { "O", "E", "K", "W", "M" };
		check(json, key, keys4, 958);

	}

	@Test
	public void testX2() throws Exception {

		String json = "{'A':false,'B':[2,7,9],'C':true,'D':true,'E':true,'F':'d3db37a8160b993'" + //
				",'G':[0,6,1],'H':false,'I':4598,'J':true,'K':false,'L':[9,4,2],'M':4792" + //
				",'N':true,'O':true,'P':'4bb9a068e1f4e745','Q':7183,'R':true,'S':false,'T':false" + //
				",'U':[9,9,0],'V':false,'W':'5073e82731c2274c','X':7646,'Y':false}";

		String key = "ekfSnsdJpV0PhS6hRZoLqYs0w7HFzTEYgH4SrtPHWgU=";
		String[] keys1 = { "P", "K", "Q", "O", "S", "I", "J", "N", "R", "H", "C", "G", "U", "L" };
		check(json, key, keys1, 44);

		key = "abc.def:0|6|1|false|true|5073e82731c2274c|false";
		String[] keys2 = { "G", "H", "N", "W", "T" };
		check(json, key, keys2, 375);

		key = "abc.def:false|d3db37a8160b993|0|6|1|9|9|0|9|4|2|4bb9a068e1f4e74" + //
				"5|2|7|9|false|true|true|5073e82731c2274c|true|4792|7646|false|true|fal" + //
				"se|true|4598|7183|false";
		String[] keys3 = { "T", "F", "G", "U", "L", "P", "B", "K", "E", "C", "W", "D", "M", "X", "S", "R", "H", "J",
				"I", "Q", "A" };
		check(json, key, keys3, 556);

		key = "abc.def:0|6|1|9|9|0|7646|d3db37a8160b993|4792|false";
		String[] keys4 = { "G", "U", "X", "F", "M", "K" };
		check(json, key, keys4, 910);

	}

	@Test
	public void testY2() throws Exception {

		String json = "{'A':false,'B':false,'C':[6,6,0],'D':6647,'E':'4820f1193e9a7c92'" + //
				",'F':7879,'G':true,'H':[6,6,6],'I':'16f317fb6d496fe3','J':4580,'K':false" + //
				",'L':[0,0,5],'M':2427,'N':false,'O':2575,'P':7762,'Q':[9,9,0],'R':8395" + //
				",'S':true,'T':false,'U':'32f09dcdbcad4a6f','V':true,'W':true,'X':true,'Y':false" + //
				"}";

		String key = "Y4OKYw3VdQnsmId/WxwN8UeAKVcwINc9xkp49kVzye8=";
		String[] keys1 = { "B", "U", "L", "F", "S", "C", "M", "J", "W", "K", "V", "Q", "X", "Y", "G", "O", "A", "I" };
		check(json, key, keys1, 44);

		key = "abc.def:false|true|0|0|5|true|4820f1193e9a7c92|true|false|6|6|0" + //
				"|7879|6|6|6";
		String[] keys2 = { "Y", "V", "L", "X", "E", "S", "T", "C", "F", "H" };
		check(json, key, keys2, 451);

		key = "abc.def:8395|7762|7879|false|true|false|false|9|9|0|6|6|6|32f09" + //
				"dcdbcad4a6f|false|6|6|0|16f317fb6d496fe3|true|4580|2427|true|true|6647" + //
				"|0|0|5|2575";
		String[] keys3 = { "R", "P", "F", "N", "S", "T", "K", "Q", "H", "U", "Y", "C", "I", "G", "J", "M", "V", "W",
				"D", "L", "O" };
		check(json, key, keys3, 465);

		key = "abc.def:6|6|0|32f09dcdbcad4a6f|2575|true|6647|4820f1193e9a7c92|" + //
				"4580|true|false|2427";
		String[] keys4 = { "C", "U", "O", "W", "D", "E", "J", "G", "Y", "M" };
		check(json, key, keys4, 839);

		key = "abc.def:6|6|6|6|6|0|6647|false|true|false|32f09dcdbcad4a6f|4580" + //
				"|2575|false|16f317fb6d496fe3|7762|false|4820f1193e9a7c92|7879|true|9|9" + //
				"|0|2427|false";
		String[] keys5 = { "H", "C", "D", "N", "G", "B", "U", "J", "O", "A", "I", "P", "T", "E", "F", "X", "Q", "M",
				"Y" };
		check(json, key, keys5, 845);

		key = "abc.def:6647|false|4580|true|2427|16f317fb6d496fe3|9|9|0|false|" + //
				"7762|0|0|5|4820f1193e9a7c92|false|32f09dcdbcad4a6f|true|false|true|257" + //
				"5|true|false|true|false|6|6|0|6|6|6|7879";
		String[] keys6 = { "D", "Y", "J", "S", "M", "I", "Q", "T", "P", "L", "E", "K", "U", "V", "N", "X", "O", "W",
				"B", "G", "A", "C", "H", "F" };
		check(json, key, keys6, 959);

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