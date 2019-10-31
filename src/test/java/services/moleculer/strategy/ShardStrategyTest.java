/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.strategy;

import java.util.HashSet;

import org.junit.Test;

import io.datatree.Tree;
import services.moleculer.context.Context;
import services.moleculer.service.LocalActionEndpoint;

public class ShardStrategyTest extends StrategyTest {

	@Override
	public Strategy<LocalActionEndpoint> createStrategy(boolean preferLocal) throws Exception {
		ShardStrategyFactory f = new ShardStrategyFactory(preferLocal);
		f.setShardKey("key");
		// f.setRingSize(100);
		f.setCacheSize(1024);
		f.started(br);
		return f.create();
	}

	// --- TEST METHODS ---

	@Test
	public void testSharding() throws Exception {

		ShardStrategy<LocalActionEndpoint> s = (ShardStrategy<LocalActionEndpoint>) createStrategy(false);
		for (int i = 0; i <= 9; i++) {
			s.addEndpoint(createEndpoint(br, "node" + i, "e" + i));
		}

		Tree params = new Tree();
		params.put("key", "sfasdlfjlkjfkljrqweriru2314qe");
		HashSet<String> set = collect(s, params, 100);
		assertEquals(1, set.size());
		String nodeID = set.iterator().next();
		
		params.remove("key");
		params.put("wrong", "fdsfsdfsdfsadfsdf");
		set = collect(s, params, 100);
		assertTrue(set.size() > 3);
		
		params.put("key", "sfasdlfjlkjfkljrqweriru2314qe");
		set = collect(s, params, 10);
		assertEquals(1, set.size());
		assertEquals(nodeID, set.iterator().next());

		params.put("key", "sfasdlfjl");
		set = collect(s, params, 10);
		assertEquals(1, set.size());
		assertNotSame(nodeID, set.iterator().next());
		
		assertEquals(3486326916L, s.getHash("0"));
		assertEquals(724397302L, s.getHash("4a6b07269c41b0"));
		assertEquals(1203819753L, s.getHash("94d60e4d4d2ac04a6b0726a757c"));
		assertEquals(1290134576L, s.getHash("df4115740c94904a6b0726af8b294d60e4d604d4"));
		assertEquals(3078036978L, s.getHash("129ac1c9ae2518"));
		assertEquals(2115014930L, s.getHash("1741723c1c26200"));
		assertEquals(1791083853L, s.getHash("1be822aeb135f804a6b07272f569"));
		assertEquals(194521309L, s.getHash("208ed3212ac1b104a6b07273dfe494d60e4e7c980"));
		assertEquals(1049237500L, s.getHash("25358393acc748"));
		assertEquals(3344970316L, s.getHash("29dc34062850110"));
		assertEquals(1059795840L, s.getHash("2e82e478a3ca4204a6b07276d393"));
		assertEquals(2268826568L, s.getHash("332994eb20d55d04a6b07277655d94d60e4eed472"));
		assertEquals(3498678610L, s.getHash("37d0455d9ed85c"));
		assertEquals(2817756008L, s.getHash("3c76f5d01ec4e90"));
		assertEquals(2997157689L, s.getHash("411da6429f104a04a6b072791d2b"));
		assertEquals(2571292884L, s.getHash("45c456b5200a9204a6b07279a40694d60e4f351c4"));
		assertEquals(1466883034L, s.getHash("4a6b0727a30f40"));
		assertEquals(4045131562L, s.getHash("4f11b79a2551920"));
		assertEquals(138243266L, s.getHash("53b8680ca935e204a6b0727b3898"));
		assertEquals(352636368L, s.getHash("585f187f2e65cc04a6b0727bc0aa94d60e4f7889e"));
		assertEquals(2273849769L, s.getHash("5d05c8f1b57e18"));
		assertEquals(588847511L, s.getHash("61ac79643c54ce0"));
		assertEquals(2319755670L, s.getHash("665329d6c4629e04a6b0727d553c"));
		assertEquals(2018050882L, s.getHash("6af9da494efdf304a6b0727deaab94d60e4fbdca0"));
		assertEquals(3263977327L, s.getHash("6fa08abbdadef8"));
		assertEquals(398434408L, s.getHash("74473b2e6511dc0"));
		assertEquals(886540594L, s.getHash("78edeba0f0941004a6b0727f7317"));
		assertEquals(2833093651L, s.getHash("7d949c137e97b804a6b0727ffd9794d60e5000278"));
		assertEquals(3675093615L, s.getHash("823b4c860d6c58"));
		assertEquals(3313150130L, s.getHash("86e1fcf89ca0f50"));
		assertEquals(492206794L, s.getHash("8b88ad6b2dfd3e04a6b072819497"));
		assertEquals(3760893786L, s.getHash("902f5dddbfffdd04a6b072821ca994d60e50447e6"));
		assertEquals(774442496L, s.getHash("94d60e505449c0"));
		assertEquals(2776987465L, s.getHash("997cbec2e6954c0"));
		assertEquals(1601659566L, s.getHash("9e236f357b9a2e04a6b07283b272"));
		assertEquals(3430860961L, s.getHash("a2ca1fa81256da04a6b07284394d94d60e5087c52"));
		assertEquals(561474753L, s.getHash("a770d01aaa56d4"));
		assertEquals(555389561L, s.getHash("ac17808d40a1570"));
		assertEquals(3133264599L, s.getHash("b0be30ffd8f3d204a6b07285c082"));
		assertEquals(502772168L, s.getHash("b564e17273938404a6b0728649cb94d60e50c9ae0"));
		assertEquals(1848953068L, s.getHash("ba0b91e50ee498"));
		assertEquals(2871289633L, s.getHash("beb24257a8bbdf0"));
		assertEquals(1434351094L, s.getHash("c358f2ca45b2b204a6b07287cd5b"));
		assertEquals(3379603680L, s.getHash("c7ffa33ce3b23604a6b07288509194d60e510aada"));
		assertEquals(1158327753L, s.getHash("cca653af8430c0"));
		assertEquals(3603063043L, s.getHash("d14d04222227cb0"));
		assertEquals(1767770729L, s.getHash("d5f3b494c2ce7404a6b07289d7c5"));
		assertEquals(455531586L, s.getHash("da9a650765cfe004a6b0728a672294d60e514d7fc"));
		assertEquals(3123302400L, s.getHash("df41157a0b4190"));
		assertEquals(2096814677L, s.getHash("e3e7c5ecad69610"));
		assertEquals(1752104883L, s.getHash("e88e765f51772604a6b0728be97a"));
		assertEquals(327655376L, s.getHash("ed3526d1f7797304a6b0728c78d794d60e518fb66"));
		assertEquals(2701341810L, s.getHash("f1dbd744a3b0fc"));
		assertEquals(820593198L, s.getHash("f68287b74a97b60"));
		assertEquals(1743913596L, s.getHash("fb293829f33d8404a6b0728e088c"));
		assertEquals(2224362458L, s.getHash("ffcfe89ca476c604a6b0728ebb2494d60e51d8000"));
		assertEquals(517707929L, s.getHash("10476990f589040"));
		assertEquals(2448939331L, s.getHash("1091d49820488800"));
		assertEquals(1948853711L, s.getHash("10dc3f9f4b20f3c04a6b072905bdc"));
		assertEquals(2088765863L, s.getHash("1126aaa6763f8bb04a6b07290eda794d60e521ec50"));
		assertEquals(1005982381L, s.getHash("117115ada16bcb0"));
		assertEquals(1953516898L, s.getHash("11bb80b4d03f3a10"));
		assertEquals(735126713L, s.getHash("1205ebbbfc3ebd004a6b07293ba75"));
		assertEquals(3764942190L, s.getHash("125056c327a940304a6b07294462c94d60e5289610"));
		assertEquals(3265688126L, s.getHash("129ac1ca539a2c0"));
		assertEquals(2603066263L, s.getHash("12e52cd17eeed1c0"));
		assertEquals(3647966497L, s.getHash("132f97d8aa6272404a6b07295f578"));
		assertEquals(760845854L, s.getHash("137a02dfd5fad2c04a6b072967c5394d60e52d0260"));
		assertEquals(1497570554L, s.getHash("13c46de707ac168"));
		assertEquals(1906782356L, s.getHash("140ed8ee3377f290"));
		assertEquals(363667900L, s.getHash("145943f55f3a16e04a6b07299774c"));
		assertEquals(2792937459L, s.getHash("14a3aefc8b0c54304a6b07299f94c94d60e533f9e2"));
		assertEquals(1340417761L, s.getHash("14ee1a03b7568d8"));
		assertEquals(3030368299L, s.getHash("1538850ae33f5850"));
		assertEquals(1417319658L, s.getHash("1582f0120f432b204a6b0729b8f14"));
		assertEquals(403872944L, s.getHash("15cd5b193b7ef3a04a6b0729c185e94d60e5383a74"));
		assertEquals(362880290L, s.getHash("1617c62067c624c"));
		assertEquals(1512282808L, s.getHash("1662312794187ac0"));
		assertEquals(1337590107L, s.getHash("16ac9c2ec075c0004a6b0729db571"));
		assertEquals(2294218872L, s.getHash("16f70735ed07bc104a6b0729e39de94d60e53c7d74"));
		assertEquals(188405640L, s.getHash("1741723d19ab510"));
		assertEquals(795173332L, s.getHash("178bdd4445eba960"));
		assertEquals(1272205773L, s.getHash("17d6484b728bad404a6b0729fc5ee"));
		assertEquals(3845773986L, s.getHash("1820b3529f42b2f04a6b072a0492494d60e5409c02"));
		assertEquals(4023863213L, s.getHash("186b1e59cc10fa0"));
		assertEquals(3551407294L, s.getHash("18b58960f8a2cbe0"));
		assertEquals(3894737157L, s.getHash("18fff46826cb9c604a6b072a20e4f"));
		assertEquals(1407156978L, s.getHash("194a5f6f53c905c04a6b072a29a0694d60e5453b58"));
		assertEquals(2108634166L, s.getHash("1994ca7685549e8"));
		assertEquals(610075097L, s.getHash("19df357db25f0470"));
		assertEquals(2132423929L, s.getHash("1a29a084e0840aa04a6b072a51b81"));
		assertEquals(3311734549L, s.getHash("1a740b8c0db198b04a6b072a59d8094d60e54b44ba"));
		assertEquals(2392290243L, s.getHash("1abe769340e22b0"));
		assertEquals(4073617810L, s.getHash("1b08e19a6f691170"));
		assertEquals(3372201925L, s.getHash("1b534ca19d6967204a6b072a87407"));
		assertEquals(3357340148L, s.getHash("1b9db7a8cb2d8d104a6b072a8ffbe94d60e55206c6"));
		assertEquals(85270405L, s.getHash("1be822aff8d01e0"));
		assertEquals(3304207601L, s.getHash("1c328db726235700"));
		assertEquals(828887894L, s.getHash("1c7cf8be53b913804a6b072aa724b"));
		assertEquals(3600310098L, s.getHash("1cc763c5816d56204a6b072aaef6e94d60e555e626"));
		
		// for (int i = 0; i < 100; i++) {
		// String key = Long.toHexString(System.nanoTime() * i);
		// for (int n = 0; n < i % 4; n++) {
	    // key += Long.toHexString(System.nanoTime() * n);
		// }
		// long hash = s.getHash(key);
		// System.out.println("assertEquals(" + hash + "L, s.getHash(\"" + key + "\"));");
		// }
	}

	protected HashSet<String> collect(ShardStrategy<LocalActionEndpoint> s, Tree params, int cycles) {
		HashSet<String> nodeIDs = new HashSet<String>();
		for (int i = 0; i < cycles; i++) {
			Context ctx = createContext(params);
			LocalActionEndpoint ep = s.getEndpoint(ctx, null);
			nodeIDs.add(ep.getNodeID());
		}
		return nodeIDs;
	}

	protected Context createContext(Tree params) {
		return new Context(null, null, null, "id1", "name", params, 1, null, "id1", null, null, "nodeID");
	}

}