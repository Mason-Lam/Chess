package Chess;

public class Tests {
	public static class Test {
		private final Computer computer;
		public final String fen;
		public final int depth;
		public final int nodes;
		
		public Test(int depth, int nodes, String fen) {
			computer = new ChessBoard(fen).getComputer();
			this.depth = depth;
			this.nodes = nodes;
			this.fen = fen;
		}
		
		public void runTest() {
			final long prevTime = System.currentTimeMillis();
			final int totalMoves = computer.totalMoves(depth);
			System.out.println(fen);
			System.out.println("Total Possible Moves: " + totalMoves);
			if (totalMoves == nodes) {
				System.out.println("TEST PASSED");
			}
			else {
				System.out.println("TEST FAILED, Expected: " + nodes);
			}
			System.out.println(System.currentTimeMillis() - prevTime + "\n");
		}
	}
	
	public static final Test test1 = new Test(1, 8, "r6r/1b2k1bq/8/8/7B/8/8/R3K2R b KQ - 3 2");
	
	public static final Test test2 = new Test(1, 8, "8/8/8/2k5/2pP4/8/B7/4K3 b - d3 0 3");
	
	public static final Test test3 = new Test(1, 19, "r1bqkbnr/pppppppp/n7/8/8/P7/1PPPPPPP/RNBQKBNR w KQkq - 2 2");
	
	public static final Test test4 = new Test(1, 5, "r3k2r/p1pp1pb1/bn2Qnp1/2qPN3/1p2P3/2N5/PPPBBPPP/R3K2R b KQkq - 3 2");
	
	public static final Test test5 = new Test(1, 44, "2kr3r/p1ppqpb1/bn2Qnp1/3PN3/1p2P3/2N5/PPPBBPPP/R3K2R b KQ - 3 2");
	
	public static final Test test6 = new Test(1, 39, "rnb2k1r/pp1Pbppp/2p5/q7/2B5/8/PPPQNnPP/RNB1K2R w KQ - 3 9");
	
	public static final Test test7 = new Test(1, 9, "2r5/3pk3/8/2P5/8/2K5/8/8 w - - 5 4");
	
	public static final Test test8 = new Test(3, 62379, "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
	
	public static final Test test9 = new Test(3, 89890, "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10");
	
	public static final Test test10 = new Test(6, 1134888, "3k4/3p4/8/K1P4r/8/8/8/8 b - - 0 1");
	
	public static final Test test11 = new Test(6, 1015133, "8/8/4k3/8/2p5/8/B2P2K1/8 w - - 0 1");
	
	public static final Test test12 = new Test(6, 1440467, "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1");
	
	public static final Test test13 = new Test(6, 661072, "5k2/8/8/8/8/8/8/4K2R w K - 0 1");
	
	public static final Test test14 = new Test(6, 803711, "3k4/8/8/8/8/8/8/R3K3 w Q - 0 1");
	
	public static final Test test15 = new Test(4, 1274206, "r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1");
	
	public static final Test test16 = new Test(4, 1720476, "r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1");
	
	public static final Test test17 = new Test(6, 3821001, "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1");
	
	public static final Test test18 = new Test(5, 1004658, "8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1");
	
	public static final Test test19 = new Test(6, 217342, "4k3/1P6/8/8/8/8/K7/8 w - - 0 1");
	
	public static final Test test20 = new Test(6, 92683, "8/P1k5/K7/8/8/8/8/8 w - - 0 1");
	
	public static final Test test21 = new Test(6, 2217, "K1k5/8/P7/8/8/8/8/8 w - - 0 1");
	
	public static final Test test22 = new Test(7, 567584, "8/k1P5/8/1K6/8/8/8/8 w - - 0 1");
	
	public static final Test test23 = new Test(4, 23527, "8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1");
	
	
	public static void runTests() {
		System.out.println("Test 1:");
		test1.runTest();
		System.out.println("Test 2:");
		test2.runTest();
		System.out.println("Test 3:");
		test3.runTest();
		System.out.println("Test 4:");
		test4.runTest();
		System.out.println("Test 5:");
		test5.runTest();
		System.out.println("Test 6:");
		test6.runTest();
		System.out.println("Test 7:");
		test7.runTest();
		System.out.println("Test 8:");
		test8.runTest();
		System.out.println("Test 9:");
		test9.runTest();
		System.out.println("Test 10:");
		test10.runTest();
		System.out.println("Test 11:");
		test11.runTest();
		System.out.println("Test 12:");
		test12.runTest();
		System.out.println("Test 13:");
		test13.runTest();
		System.out.println("Test 14:");
		test14.runTest();
		System.out.println("Test 15:"); //Repeat?
		test15.runTest();
		System.out.println("Test 16:"); //Repeat?
		test16.runTest();
		System.out.println("Test 17:");
		test17.runTest();
		System.out.println("Test 18:");
		test18.runTest();
		System.out.println("Test 19:");
		test19.runTest();
		System.out.println("Test 20:");
		test20.runTest();
		System.out.println("Test 21:");
		test21.runTest();
		System.out.println("Test 22:");
		test22.runTest();
		System.out.println("Test 23:");
		test23.runTest();
	}
}
