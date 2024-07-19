package Chess;

public class Constants {

	public static class MoveConstants {
		public static final int HALF_MOVE_TIMER = 50;

		public static final boolean shouldCopyOptimize = true;

		public static final int START = 0;

		public static final int END = 1;

		public static final int EN_PASSANT = 2;

		public static final int[] MAX_MOVES = new int[] {
			4,
			8,
			13,
			14,
			27,
			9,
			256
		};

		public static final boolean CHECKS = true;
		
		public static final int[][] distFromEdge = new int[64][8];

		//Move Constants
		public static final int[][] ROOK_POSITIONS = new int[][] {
			new int[] {0, 7},
			new int[] {56, 63}
		};

		public static final int[] KING_POSITIONS = new int[] {
			4, 60
		};
		
		public static final int[] PROMOTION_LINE = new int[] {
			7, 0
		};
		
		public static final int[] PAWN_STARTS = new int[] {
			1, 6
		};
		
		public static final int[] DIRECTIONS = new int[] {
			8, -8, 1, -1, 7, 9, -7, -9
		};

		public static final int[] STRAIGHT_DIRECTIONS = new int[] {
			8, -8, 1, -1
		};

		public static final int[] DIAGONAL_DIRECTIONS = new int[] {
			7, 9, -7, -9
		};
		
		public static final int[] KNIGHT_MOVES = new int[] {
			-15, -17, 15, 17, 10, -10, 6, -6
		};
		
		public static final int[][] PAWN_DIAGONALS = new int[][] {
			new int[] {7, 9},
			new int[] {-7, -9}
		};

		static {
			for (int row = 0; row < 8; row++) {
				for(int column = 0; column < 8; column++) {
					final int northDist = row;
					final int southDist = 7 - row;
					final int westDist = column;
					final int eastDist = 7 - column;
					final int[] data = new int[] {
						northDist,
						southDist,
						westDist,
						eastDist,
						Math.min(northDist, westDist),
						Math.min(southDist, eastDist),
						Math.min(northDist, eastDist),
						Math.min(southDist, westDist)
					};
					distFromEdge[row * 8 + column] = data;
				}
			}
		}
	}

	public static class PieceConstants {
		//Pieces
		public static final byte EMPTY = -1;
		public static final byte PAWN = 0;
		public static final byte KNIGHT = 1;
		public static final byte BISHOP = 2;
		public static final byte ROOK = 3;
		public static final byte QUEEN = 4;
		public static final byte KING = 5;
		
		public static final char[] COLUMNS = new char[] {
			'a',
			'b',
			'c',
			'd',
			'e',
			'f',
			'g',
			'h'
		};
		
		public static final byte[] PROMOTION_PIECES = new byte[] {
			1,
			2,
			3,
			4
		};
		
		//Teams
		public static final byte BLACK = 0;
		public static final byte WHITE = 1;

		public static final char[][] PIECES = new char[][] {
			new char[] {'p','n','b','r','q','k'}, //Black
			new char[] {'P','N','B','R','Q','K'} // White
		};
	}

	public static class EvaluateConstants {
		//OutcomeConstants
		public static final int DRAW = 0;
		public static final int WIN = 1;
		public static final int CONTINUE = 2;
	}
}
