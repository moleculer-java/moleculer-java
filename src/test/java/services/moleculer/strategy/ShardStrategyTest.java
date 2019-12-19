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
		f.started(br);
		return f.create();
	}

	// --- TEST METHODS ---

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testSharding() throws Exception {

		ShardStrategy<LocalActionEndpoint> s = (ShardStrategy<LocalActionEndpoint>) createStrategy(false);
		for (int i = 0; i <= 9; i++) {
			s.addEndpoint(createEndpoint(br, "node" + i, "e", "e" + i));
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

		// Precalculated values (Node.js compatibility tests)
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

		f = new ShardStrategyFactory(false);
		f.setShardKey("key");
		f.setRingSize(3001);
		f.setVnodes(7);
		f.setCacheSize(1024);
		f.started(br);
		s = (ShardStrategy) f.create();
		for (int i = 0; i <= 8; i++) {
			s.addEndpoint(createEndpoint(br, "node" + i, "e", "e" + i));
		}

		// nodes: 8 (node0...node7)
		// vnodes: 7
		// ringSize: 3001
		assertNodeLinkedToKey(s, "node4", "70f83f5064f4d");
		assertNodeLinkedToKey(s, "node6", "e1f07ea102414e1f07ea104fd0");
		assertNodeLinkedToKey(s, "node0", "152e8bdf1ae353");
		assertNodeLinkedToKey(s, "node8", "1c3e0fd4269a6c1c3e0fd426bc70");
		assertNodeLinkedToKey(s, "node0", "234d93c934316d234d93c9349ec3");
		assertNodeLinkedToKey(s, "node2", "2a5d17be439d5e2a5d17be43d7ae2a5d17be43f4dc");
		assertNodeLinkedToKey(s, "node5", "316c9bb353eef0");
		assertNodeLinkedToKey(s, "node3", "387c1fa8667440387c1fa866c200");
		assertNodeLinkedToKey(s, "node3", "3f8ba39d79c0cb");
		assertNodeLinkedToKey(s, "node1", "469b27928e1156469b27928e6660469b27928f1c9a");
		assertNodeLinkedToKey(s, "node7", "4daaab87a576564daaab87a5e149");
		assertNodeLinkedToKey(s, "node7", "54ba2f7cbd0bf4");
		assertNodeLinkedToKey(s, "node6", "5bc9b371d70ac9");
		assertNodeLinkedToKey(s, "node5", "62d93766f16ac862d93766f1f2d862d93766f236e0");
		assertNodeLinkedToKey(s, "node6", "69e8bb5c0e0d11");
		assertNodeLinkedToKey(s, "node0", "70f83f512ad640");
		assertNodeLinkedToKey(s, "node0", "7807c3471b4ca7");
		assertNodeLinkedToKey(s, "node0", "7f17473c4a91fa7f17473c4b56c87f17473c4bae40");
		assertNodeLinkedToKey(s, "node2", "8626cb317880998626cb3179c3d2");
		assertNodeLinkedToKey(s, "node4", "8d364f26a8e2408d364f26a98c548d364f26aa4eb4");
		assertNodeLinkedToKey(s, "node0", "9445d31bd97a85");
		assertNodeLinkedToKey(s, "node6", "9b555711e7c24a");
		assertNodeLinkedToKey(s, "node5", "a264db0733382f");
		assertNodeLinkedToKey(s, "node0", "a9745efc715838");
		assertNodeLinkedToKey(s, "node5", "b083e2f1dd5c80");
		assertNodeLinkedToKey(s, "node3", "b79366e725e138b79366e726fd7eb79366e7279b6c");
		assertNodeLinkedToKey(s, "node7", "bea2eadc89a4cfbea2eadc8aecd1");
		assertNodeLinkedToKey(s, "node6", "c5b26ed1d43518c5b26ed1d54538c5b26ed1d5ef4c");
		assertNodeLinkedToKey(s, "node3", "ccc1f2c71e6b81ccc1f2c71f3ee3");
		assertNodeLinkedToKey(s, "node2", "d3d176bc67833cd3d176bc68825a");
		assertNodeLinkedToKey(s, "node0", "dae0fab1b4d1c5");
		assertNodeLinkedToKey(s, "node8", "e1f07ea6ffa3c0e1f07ea700dac0");
		assertNodeLinkedToKey(s, "node6", "e900029c4f1ebbe900029c50375ce900029c50ffcf");
		assertNodeLinkedToKey(s, "node7", "f00f86919e3fd6");
		assertNodeLinkedToKey(s, "node3", "f71f0a86ec89cd");
		assertNodeLinkedToKey(s, "node6", "fe2e8e7c3dbb68fe2e8e7c3eed8c");
		assertNodeLinkedToKey(s, "node0", "1053e127193339c1053e1271946e411053e1271954f00");
		assertNodeLinkedToKey(s, "node3", "10c4d9666e7c772");
		assertNodeLinkedToKey(s, "node4", "1135d1a5c3bd5851135d1a5c3d50b41135d1a5c3e0e38");
		assertNodeLinkedToKey(s, "node3", "11a6c9e51948a78");
		assertNodeLinkedToKey(s, "node0", "1217c2246ec82d4");
		assertNodeLinkedToKey(s, "node3", "1288ba63c456230");
		assertNodeLinkedToKey(s, "node5", "12f9b2a31b8068212f9b2a31b9dca712f9b2a31baadc6");
		assertNodeLinkedToKey(s, "node0", "136aaae27154870136aaae27186a3c136aaae27194038");
		assertNodeLinkedToKey(s, "node8", "13dba321c778255");
		assertNodeLinkedToKey(s, "node8", "144c9b611f41f52144c9b611f64e26144c9b611f76590");
		assertNodeLinkedToKey(s, "node6", "14bd93a0761790d14bd93a0764263914bd93a076c6705");
		assertNodeLinkedToKey(s, "node7", "152e8bdfcd94d30");
		assertNodeLinkedToKey(s, "node4", "159f841f244a9f6");
		assertNodeLinkedToKey(s, "node6", "16107c5e7b215d016107c5e7b4387e");
		assertNodeLinkedToKey(s, "node8", "1681749dd18e1c41681749dd1ad16c");
		assertNodeLinkedToKey(s, "node6", "16f26cdd2881dbc");
		assertNodeLinkedToKey(s, "node6", "1763651c7f5a32b1763651c7f7e6db1763651c7f8e867");
		assertNodeLinkedToKey(s, "node8", "17d45d5bd610116");
		assertNodeLinkedToKey(s, "node3", "1845559b2c9c1a61845559b2cbd82e");
		assertNodeLinkedToKey(s, "node3", "18b64dda839c760");
		assertNodeLinkedToKey(s, "node8", "19274619daaa2b719274619daccce8");
		assertNodeLinkedToKey(s, "node6", "19983e59318ff4a");
		assertNodeLinkedToKey(s, "node6", "1a09369895e39651a0936989614e0f");
		assertNodeLinkedToKey(s, "node4", "1a7a2ed7edbc45c");
		assertNodeLinkedToKey(s, "node6", "1aeb271744e63fe1aeb2717450b4d6");
		assertNodeLinkedToKey(s, "node1", "1b5c1f569c5aa321b5c1f569c7b9701b5c1f569c9320a");
		assertNodeLinkedToKey(s, "node4", "1bcd1795f447385");
		assertNodeLinkedToKey(s, "node2", "1c3e0fd54ba5800");
		assertNodeLinkedToKey(s, "node6", "1caf0814a3ae8ef1caf0814a3d60a7");
		assertNodeLinkedToKey(s, "node7", "1d200053fc7d8081d200053fca5978");
		assertNodeLinkedToKey(s, "node7", "1d90f8935427b6a1d90f89354506921d90f8935464c26");
		assertNodeLinkedToKey(s, "node7", "1e01f0d2ac4119c1e01f0d2ac653e01e01f0d2ac89624");
		assertNodeLinkedToKey(s, "node0", "1e72e9120455401");
		assertNodeLinkedToKey(s, "node4", "1ee3e1515f55c781ee3e1515f859d2");
		assertNodeLinkedToKey(s, "node1", "1f54d990b7d2a301f54d990b8493c61f54d990b8d56a7");
		assertNodeLinkedToKey(s, "node7", "1fc5d1d01124910");
		assertNodeLinkedToKey(s, "node1", "2036ca0f695003c2036ca0f697c5b4");
		assertNodeLinkedToKey(s, "node3", "20a7c24ec18db46");
		assertNodeLinkedToKey(s, "node7", "2118ba8e1a1c3da2118ba8e1a49cc2");
		assertNodeLinkedToKey(s, "node7", "2189b2cd7a238bc2189b2cd7a5d4042189b2cd7a74554");
		assertNodeLinkedToKey(s, "node3", "21faab0cd32764f21faab0cd35056921faab0cd367b95");
		assertNodeLinkedToKey(s, "node7", "226ba34c2d7a6d4");
		assertNodeLinkedToKey(s, "node8", "22dc9b8b8874bfd22dc9b8b88a4bc522dc9b8b88d4b8d");
		assertNodeLinkedToKey(s, "node6", "234d93cae1bc1f0234d93cae1e6a40");
		assertNodeLinkedToKey(s, "node5", "23be8c0a3a59ed9");
		assertNodeLinkedToKey(s, "node0", "242f8449934c9f8");
		assertNodeLinkedToKey(s, "node6", "24a07c88ebec8ed24a07c88ec3e811");
		assertNodeLinkedToKey(s, "node0", "251174c845d4ac8251174c84601570");
		assertNodeLinkedToKey(s, "node2", "25826d07a58d00b25826d07a5c7166");
		assertNodeLinkedToKey(s, "node8", "25f36546fef728a");
		assertNodeLinkedToKey(s, "node5", "26645d865824b46");
		assertNodeLinkedToKey(s, "node4", "26d555c5b12da78");
		assertNodeLinkedToKey(s, "node6", "27464e050a463f027464e050a7c4e827464e050a97564");
		assertNodeLinkedToKey(s, "node2", "27b7464463dbd0e27b74644640bac2");
		assertNodeLinkedToKey(s, "node8", "28283e83bd531d2");
		assertNodeLinkedToKey(s, "node2", "289936c316b17c8289936c316e95e8289936c317054f8");
		assertNodeLinkedToKey(s, "node3", "290a2f02705f821");
		assertNodeLinkedToKey(s, "node7", "297b2741c9bb12c297b2741c9ed08a");
		assertNodeLinkedToKey(s, "node4", "29ec1f8123fe64e");
		assertNodeLinkedToKey(s, "node5", "2a5d17c07d98120");
		assertNodeLinkedToKey(s, "node6", "2ace0fffd7243b8");
		assertNodeLinkedToKey(s, "node6", "2b3f083f3145b18");
		assertNodeLinkedToKey(s, "node0", "2bb0007e8af96a82bb0007e8b358d02bb0007e8b5b229");
		assertNodeLinkedToKey(s, "node0", "2c20f8bde5af9902c20f8bde5ec570");

		// for (int i = 0; i < 100; i++) {
		// String key = Long.toHexString(System.nanoTime() * (i + 1));
		// for (int n = 0; n < System.nanoTime() % 3; n++) {
		// key += Long.toHexString(System.nanoTime() * (i + 1));
		// }
		// params.put("key", key);
		// LocalActionEndpoint ep = s.getEndpoint(createContext(params), null);
		// System.out.println("assertNodeLinkedToKey(s, \"" + ep.getNodeID() +
		// "\", \"" + params.get("key", "") + "\");");
		// }
	}

	protected void assertNodeLinkedToKey(ShardStrategy<LocalActionEndpoint> s, String nodeID, String key) {
		Tree params = new Tree().put("key", key);
		LocalActionEndpoint ep = s.getEndpoint(createContext(params), null);
		assertEquals(nodeID, ep.getNodeID());
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