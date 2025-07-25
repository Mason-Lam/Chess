package Chess;

import java.util.ArrayList;

public class Tests {
	
	private static final ArrayList<Test> tests = new ArrayList<Test>();
	
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
			tests.add(this);
		}
		
		public boolean runTest(boolean verbose) {
			if (verbose) System.out.println(fen);
			final long prevTime = System.currentTimeMillis();
			for (int i = 1; i < depth; i++) {
				if (verbose) System.out.println("Depth: " + i + ", Total Possible Moves: " + computer.totalMoves(i));
			}
			final int totalMoves = computer.totalMoves(depth);
			if (verbose) System.out.println("Depth: " + depth + ", Total Possible Moves: " + totalMoves);
			if (totalMoves == nodes) {
				if (verbose) System.out.println("TEST PASSED");
			}
			else {
				if (verbose) System.out.println("TEST FAILED, Expected: " + nodes);
			}
			if (verbose) System.out.println(System.currentTimeMillis() - prevTime + "\n");
			return totalMoves == nodes;
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

	public static final Test test24 = new Test(6, 936530, "3k4/1p6/8/K1P4r/8/8/8/8 b - - 0 1");

	public static final Test test25 = new Test(7, 625511, "8/8/P6k/8/8/7K/8/8 w - - 0 1");

	public static final Test test26 = new Test(4, 232252, "3Q4/8/8/8/6q1/8/P4K2/k7 w - - 0 1");

	public static void runTestsShallow(boolean verbose, boolean displayTimeStats) {
		resetTimeStats();
		long prevTime = System.currentTimeMillis();
		System.out.println("----------------------------------------");
		ArrayList<Integer> failedTests = new ArrayList<>();
		for (int i = 0; i < tests.size(); i++) {
			if (verbose) System.out.println("Test " + (i + 1) + ":");
			if(!tests.get(i).runTest(verbose)) failedTests.add(i + 1);
		}
		if (!failedTests.isEmpty()) {
			for (int testNum : failedTests) {
				System.out.println("Test " + testNum + " failed.");
			}
		}
		else System.out.println("ALL TESTS PASSED!");
		if (displayTimeStats) {
			System.out.println(System.currentTimeMillis() - prevTime);
			displayTimeStats();
		}
		System.out.println("----------------------------------------");
	}
	
	public static void runTestsDeep(boolean verbose, boolean displayTimeStats) {
		runTestsShallow(false, false);
		runTestsShallow(verbose, displayTimeStats);
	}

	public static void timeCheckBoard(ChessBoard board, int depth) {
		System.out.println("----------------------------------------");
		System.out.println(board.getFenString());
		resetTimeStats();
		long prevTime;
		Computer computer = board.getComputer();
		for (int i = 1; i < depth + 1; i++) {
			prevTime = System.currentTimeMillis();
			System.out.println("Depth: " + i + ", Total Possible Moves: " + computer.totalMoves(i));
			System.out.println("Time Taken: " + (System.currentTimeMillis() - prevTime));	
		}
		displayTimeStats();
		System.out.println("----------------------------------------");
	}

	public static long timeMoveGen = 0;
	public static long timePawnGen = 0;
	public static long timeKnightGen = 0;
	public static long timeBishopGen = 0;
	public static long timeRookGen = 0;
	public static long timeQueenGen = 0;
	public static long timeKingGen = 0;
	public static long timeValidMove = 0;
	public static long timeValidPart = 0;
	public static long timeMakeMove = 0;
	public static long timeUndoMove = 0;
	public static long timeAttack = 0;
	public static long timePawnAttack = 0;
	public static long timeKnightAttack = 0;
	public static long timeBishopAttack = 0;
	public static long timeRookAttack = 0;
	public static long timeQueenAttack = 0;
	public static long timeKingAttack = 0;
	public static long timePieceUpdate = 0;
	public static long timeMisc = 0;
	public static long timeDebug = 0;
	public static long copyCount = 0;

	private static void displayTimeStats() {
		System.out.println("Move Generation: " + timeMoveGen);
		System.out.println("Pawn Move: " + timePawnGen);
		System.out.println("Knight Move: " + timeKnightGen);
		System.out.println("Bishop Move: " + timeBishopGen);
		System.out.println("Rook Move: " + timeRookGen);
		System.out.println("Queen Move: " + timeQueenGen);
		System.out.println("King Move: " + timeKingGen);
		System.out.println("Valid Move: " + timeValidMove);
		System.out.println("Valid Part: " + timeValidPart);
		System.out.println("Make Move: " + timeMakeMove);
		System.out.println("Undo Move: " + timeUndoMove);
		System.out.println("Pawn Attack: " + timePawnAttack);
		System.out.println("Knight Attack: " + timeKnightAttack);
		System.out.println("Bishop Attack: " + timeBishopAttack);
		System.out.println("Rook Attack: " + timeRookAttack);
		System.out.println("Queen Attack: " + timeQueenAttack);
		System.out.println("King Attack: " + timeKingAttack);
		System.out.println("Piece Update: " + timePieceUpdate);
		System.out.println("Misc: " + timeMisc);
		System.out.println("Debug: " + timeDebug);
		System.out.println("Moves Copied: " + copyCount);
	}

	private static void resetTimeStats() {
		timeMoveGen = 0;
		timePawnGen = 0;
		timeKnightGen = 0;
		timeBishopGen = 0;
		timeRookGen = 0;
		timeQueenGen = 0;
		timeKingGen = 0;
		timeValidMove = 0;
		timeValidPart = 0;
		timeMakeMove = 0;
		timeUndoMove = 0;
		timePawnAttack = 0;
		timeKnightAttack = 0;
		timeBishopAttack = 0;
		timeRookAttack = 0;
		timeQueenAttack = 0;
		timeKingAttack = 0;
		timePieceUpdate = 0;
		timeMisc = 0;
		timeDebug = 0;
	}
}
