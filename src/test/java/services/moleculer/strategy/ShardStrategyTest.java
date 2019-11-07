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

		// Precalculated values
		ShardStrategyFactory f = new ShardStrategyFactory();

		assertEquals(3486326916L, (long) f.hash.apply("0"));
		assertEquals(724397302L, (long) f.hash.apply("4a6b07269c41b0"));
		assertEquals(1203819753L, (long) f.hash.apply("94d60e4d4d2ac04a6b0726a757c"));
		assertEquals(1290134576L, (long) f.hash.apply("df4115740c94904a6b0726af8b294d60e4d604d4"));
		assertEquals(3078036978L, (long) f.hash.apply("129ac1c9ae2518"));
		assertEquals(2115014930L, (long) f.hash.apply("1741723c1c26200"));
		assertEquals(1791083853L, (long) f.hash.apply("1be822aeb135f804a6b07272f569"));
		assertEquals(194521309L, (long) f.hash.apply("208ed3212ac1b104a6b07273dfe494d60e4e7c980"));
		assertEquals(1049237500L, (long) f.hash.apply("25358393acc748"));
		assertEquals(3344970316L, (long) f.hash.apply("29dc34062850110"));
		assertEquals(1059795840L, (long) f.hash.apply("2e82e478a3ca4204a6b07276d393"));
		assertEquals(2268826568L, (long) f.hash.apply("332994eb20d55d04a6b07277655d94d60e4eed472"));
		assertEquals(3498678610L, (long) f.hash.apply("37d0455d9ed85c"));
		assertEquals(2817756008L, (long) f.hash.apply("3c76f5d01ec4e90"));
		assertEquals(2997157689L, (long) f.hash.apply("411da6429f104a04a6b072791d2b"));
		assertEquals(2571292884L, (long) f.hash.apply("45c456b5200a9204a6b07279a40694d60e4f351c4"));
		assertEquals(1466883034L, (long) f.hash.apply("4a6b0727a30f40"));
		assertEquals(4045131562L, (long) f.hash.apply("4f11b79a2551920"));
		assertEquals(138243266L, (long) f.hash.apply("53b8680ca935e204a6b0727b3898"));
		assertEquals(352636368L, (long) f.hash.apply("585f187f2e65cc04a6b0727bc0aa94d60e4f7889e"));
		assertEquals(2273849769L, (long) f.hash.apply("5d05c8f1b57e18"));
		assertEquals(588847511L, (long) f.hash.apply("61ac79643c54ce0"));
		assertEquals(2319755670L, (long) f.hash.apply("665329d6c4629e04a6b0727d553c"));
		assertEquals(2018050882L, (long) f.hash.apply("6af9da494efdf304a6b0727deaab94d60e4fbdca0"));
		assertEquals(3263977327L, (long) f.hash.apply("6fa08abbdadef8"));
		assertEquals(398434408L, (long) f.hash.apply("74473b2e6511dc0"));
		assertEquals(886540594L, (long) f.hash.apply("78edeba0f0941004a6b0727f7317"));
		assertEquals(2833093651L, (long) f.hash.apply("7d949c137e97b804a6b0727ffd9794d60e5000278"));
		assertEquals(3675093615L, (long) f.hash.apply("823b4c860d6c58"));
		assertEquals(3313150130L, (long) f.hash.apply("86e1fcf89ca0f50"));
		assertEquals(492206794L, (long) f.hash.apply("8b88ad6b2dfd3e04a6b072819497"));
		assertEquals(3760893786L, (long) f.hash.apply("902f5dddbfffdd04a6b072821ca994d60e50447e6"));
		assertEquals(774442496L, (long) f.hash.apply("94d60e505449c0"));
		assertEquals(2776987465L, (long) f.hash.apply("997cbec2e6954c0"));
		assertEquals(1601659566L, (long) f.hash.apply("9e236f357b9a2e04a6b07283b272"));
		assertEquals(3430860961L, (long) f.hash.apply("a2ca1fa81256da04a6b07284394d94d60e5087c52"));
		assertEquals(561474753L, (long) f.hash.apply("a770d01aaa56d4"));
		assertEquals(555389561L, (long) f.hash.apply("ac17808d40a1570"));
		assertEquals(3133264599L, (long) f.hash.apply("b0be30ffd8f3d204a6b07285c082"));
		assertEquals(502772168L, (long) f.hash.apply("b564e17273938404a6b0728649cb94d60e50c9ae0"));
		assertEquals(1848953068L, (long) f.hash.apply("ba0b91e50ee498"));
		assertEquals(2871289633L, (long) f.hash.apply("beb24257a8bbdf0"));
		assertEquals(1434351094L, (long) f.hash.apply("c358f2ca45b2b204a6b07287cd5b"));
		assertEquals(3379603680L, (long) f.hash.apply("c7ffa33ce3b23604a6b07288509194d60e510aada"));
		assertEquals(1158327753L, (long) f.hash.apply("cca653af8430c0"));
		assertEquals(3603063043L, (long) f.hash.apply("d14d04222227cb0"));
		assertEquals(1767770729L, (long) f.hash.apply("d5f3b494c2ce7404a6b07289d7c5"));
		assertEquals(455531586L, (long) f.hash.apply("da9a650765cfe004a6b0728a672294d60e514d7fc"));
		assertEquals(3123302400L, (long) f.hash.apply("df41157a0b4190"));
		assertEquals(2096814677L, (long) f.hash.apply("e3e7c5ecad69610"));
		assertEquals(1752104883L, (long) f.hash.apply("e88e765f51772604a6b0728be97a"));
		assertEquals(327655376L, (long) f.hash.apply("ed3526d1f7797304a6b0728c78d794d60e518fb66"));
		assertEquals(2701341810L, (long) f.hash.apply("f1dbd744a3b0fc"));
		assertEquals(820593198L, (long) f.hash.apply("f68287b74a97b60"));
		assertEquals(1743913596L, (long) f.hash.apply("fb293829f33d8404a6b0728e088c"));
		assertEquals(2224362458L, (long) f.hash.apply("ffcfe89ca476c604a6b0728ebb2494d60e51d8000"));
		assertEquals(517707929L, (long) f.hash.apply("10476990f589040"));
		assertEquals(2448939331L, (long) f.hash.apply("1091d49820488800"));
		assertEquals(1948853711L, (long) f.hash.apply("10dc3f9f4b20f3c04a6b072905bdc"));
		assertEquals(2088765863L, (long) f.hash.apply("1126aaa6763f8bb04a6b07290eda794d60e521ec50"));
		assertEquals(1005982381L, (long) f.hash.apply("117115ada16bcb0"));
		assertEquals(1953516898L, (long) f.hash.apply("11bb80b4d03f3a10"));
		assertEquals(735126713L, (long) f.hash.apply("1205ebbbfc3ebd004a6b07293ba75"));
		assertEquals(3764942190L, (long) f.hash.apply("125056c327a940304a6b07294462c94d60e5289610"));
		assertEquals(3265688126L, (long) f.hash.apply("129ac1ca539a2c0"));
		assertEquals(2603066263L, (long) f.hash.apply("12e52cd17eeed1c0"));
		assertEquals(3647966497L, (long) f.hash.apply("132f97d8aa6272404a6b07295f578"));
		assertEquals(760845854L, (long) f.hash.apply("137a02dfd5fad2c04a6b072967c5394d60e52d0260"));
		assertEquals(1497570554L, (long) f.hash.apply("13c46de707ac168"));
		assertEquals(1906782356L, (long) f.hash.apply("140ed8ee3377f290"));
		assertEquals(363667900L, (long) f.hash.apply("145943f55f3a16e04a6b07299774c"));
		assertEquals(2792937459L, (long) f.hash.apply("14a3aefc8b0c54304a6b07299f94c94d60e533f9e2"));
		assertEquals(1340417761L, (long) f.hash.apply("14ee1a03b7568d8"));
		assertEquals(3030368299L, (long) f.hash.apply("1538850ae33f5850"));
		assertEquals(1417319658L, (long) f.hash.apply("1582f0120f432b204a6b0729b8f14"));
		assertEquals(403872944L, (long) f.hash.apply("15cd5b193b7ef3a04a6b0729c185e94d60e5383a74"));
		assertEquals(362880290L, (long) f.hash.apply("1617c62067c624c"));
		assertEquals(1512282808L, (long) f.hash.apply("1662312794187ac0"));
		assertEquals(1337590107L, (long) f.hash.apply("16ac9c2ec075c0004a6b0729db571"));
		assertEquals(2294218872L, (long) f.hash.apply("16f70735ed07bc104a6b0729e39de94d60e53c7d74"));
		assertEquals(188405640L, (long) f.hash.apply("1741723d19ab510"));
		assertEquals(795173332L, (long) f.hash.apply("178bdd4445eba960"));
		assertEquals(1272205773L, (long) f.hash.apply("17d6484b728bad404a6b0729fc5ee"));
		assertEquals(3845773986L, (long) f.hash.apply("1820b3529f42b2f04a6b072a0492494d60e5409c02"));
		assertEquals(4023863213L, (long) f.hash.apply("186b1e59cc10fa0"));
		assertEquals(3551407294L, (long) f.hash.apply("18b58960f8a2cbe0"));
		assertEquals(3894737157L, (long) f.hash.apply("18fff46826cb9c604a6b072a20e4f"));
		assertEquals(1407156978L, (long) f.hash.apply("194a5f6f53c905c04a6b072a29a0694d60e5453b58"));
		assertEquals(2108634166L, (long) f.hash.apply("1994ca7685549e8"));
		assertEquals(610075097L, (long) f.hash.apply("19df357db25f0470"));
		assertEquals(2132423929L, (long) f.hash.apply("1a29a084e0840aa04a6b072a51b81"));
		assertEquals(3311734549L, (long) f.hash.apply("1a740b8c0db198b04a6b072a59d8094d60e54b44ba"));
		assertEquals(2392290243L, (long) f.hash.apply("1abe769340e22b0"));
		assertEquals(4073617810L, (long) f.hash.apply("1b08e19a6f691170"));
		assertEquals(3372201925L, (long) f.hash.apply("1b534ca19d6967204a6b072a87407"));
		assertEquals(3357340148L, (long) f.hash.apply("1b9db7a8cb2d8d104a6b072a8ffbe94d60e55206c6"));
		assertEquals(85270405L, (long) f.hash.apply("1be822aff8d01e0"));
		assertEquals(3304207601L, (long) f.hash.apply("1c328db726235700"));
		assertEquals(828887894L, (long) f.hash.apply("1c7cf8be53b913804a6b072aa724b"));
		assertEquals(3600310098L, (long) f.hash.apply("1cc763c5816d56204a6b072aaef6e94d60e555e626"));
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