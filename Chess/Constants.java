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

	}

	public static class DirectionConstants {
		//Directions written from perspective of white player.

		public static final int UP = -8;
		public static final int DOWN = 8;

		public static final int LEFT = -1;
		public static final int RIGHT = 1;

		public static final int UPLEFT = UP + LEFT;
		public static final int UPRIGHT = UP + RIGHT;

		public static final int DOWNLEFT = DOWN + LEFT;
		public static final int DOWNRIGHT = DOWN + RIGHT;

		public static final int[] STRAIGHT_DIRECTIONS = new int [] {
			DOWN, UP, RIGHT, LEFT
		};

		public static final int[] DIAGONAL_DIRECTIONS = new int[] {
			DOWNLEFT, DOWNRIGHT, UPRIGHT, UPLEFT
		};

		public static final int[] DIRECTIONS = new int[] {
			DOWN, UP, RIGHT, LEFT, DOWNLEFT, DOWNRIGHT, UPRIGHT, UPLEFT
		};

		public static final int[] KNIGHT_DIRECTIONS = new int[] {
			UP + UP + RIGHT,
			UP + UP + LEFT,
			DOWN + DOWN + LEFT,
			DOWN + DOWN + RIGHT,
			RIGHT + RIGHT + DOWN,
			LEFT + LEFT + UP,
			LEFT + LEFT + DOWN,
			RIGHT + RIGHT + UP
		};

		public static final int[][] PAWN_ATTACK_DIRECTIONS = new int[][] {
			new int[] {DOWNLEFT, DOWNRIGHT},		//BLACK
			new int[] {UPRIGHT, UPLEFT}				//WHITE
		};

		public static final int[][] NUM_SQUARES_FROM_EDGE = new int[64][8];

		static {
			for (int row = 0; row < 8; row++) {
				for(int column = 0; column < 8; column++) {
					final int upDist = row;
					final int downDist = 7 - row;
					final int leftDist = column;
					final int rightDist = 7 - column;
					final int upLeftDist = Math.min(upDist, leftDist);
					final int downRightDist = Math.min(downDist, rightDist);
					final int upRightDist = Math.min(upDist, rightDist);
					final int downLeftDist = Math.min(downDist, leftDist);

					final int[] NUM_SQUARES_FROM_EDGE_BY_DIRECTION = new int[] {
						upDist,
						downDist,
						leftDist,
						rightDist,
						upLeftDist,
						downRightDist,
						upRightDist,
						downLeftDist
					};
					NUM_SQUARES_FROM_EDGE[row * 8 + column] = NUM_SQUARES_FROM_EDGE_BY_DIRECTION;
				}
			}
		}
	}

	public static class PositionConstants {
		//Grid has (0,0) at the top left with right being the positive x-axis and down being the positive y-axis.
		public static final int ORIGIN = 0;

		public static final int QUEENSIDE = 0;
		public static final int KINGSIDE = 1;

		public static final int BLACK_QUEENSIDE_ROOK_POS = ORIGIN;
		public static final int BLACK_KINGSIDE_ROOK_POS = ORIGIN + DirectionConstants.RIGHT * 7;

		public static final int WHITE_QUEENSIDE_ROOK_POS = ORIGIN + DirectionConstants.DOWN * 7;
		public static final int WHITE_KINGSIDE_ROOK_POS = WHITE_QUEENSIDE_ROOK_POS + DirectionConstants.RIGHT * 7;

		public static final int BLACK_KING_POS = ORIGIN + DirectionConstants.RIGHT * 4;
		public static final int WHITE_KING_POS = BLACK_KING_POS + DirectionConstants.DOWN * 7;

		public static final int BLACK_PROMOTION_ROW = 7;
		public static final int WHITE_PROMOTION_ROW = 0;

		public static final int BLACK_PAWN_STARTING_ROW = 1;
		public static final int WHITE_PAWN_STARTING_ROW = 6;

		public static final int[][] ROOK_POSITIONS = new int[][] {
			new int[] {BLACK_QUEENSIDE_ROOK_POS, BLACK_KINGSIDE_ROOK_POS},
			new int[] {WHITE_QUEENSIDE_ROOK_POS, WHITE_KINGSIDE_ROOK_POS}
		};
		
		public static final int[] PROMOTION_ROW = new int[] {
			BLACK_PROMOTION_ROW, WHITE_PROMOTION_ROW
		};
		
		public static final int[] PAWN_STARTING_ROW = new int[] {
			BLACK_PAWN_STARTING_ROW, WHITE_PAWN_STARTING_ROW
		};
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
