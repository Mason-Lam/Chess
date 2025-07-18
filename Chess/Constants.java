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
		enum Direction {
			UP(-8),
			DOWN(8),
			LEFT(-1),
			RIGHT(1),
			UPLEFT(sumDirections(UP, LEFT)),
			UPRIGHT(sumDirections(UP, RIGHT)),
			DOWNLEFT(sumDirections(DOWN, LEFT)),
			DOWNRIGHT(sumDirections(DOWN, RIGHT));


			public final int rawArrayValue;
			private Direction(int rawArrayValue) {
				this.rawArrayValue = rawArrayValue;
			}

			public static int sumDirections(Direction... directions) {
				int sum = 0;
				for (Direction direction : directions) {
					sum += direction.rawArrayValue;
				}
				return sum;
			}
		}

		public static final Direction[] STRAIGHT_DIRECTIONS = new Direction [] {
			Direction.DOWN, Direction.UP, Direction.RIGHT, Direction.LEFT
		};

		public static final Direction[] DIAGONAL_DIRECTIONS = new Direction[] {
			Direction.DOWNLEFT, Direction.DOWNRIGHT, Direction.UPRIGHT, Direction.UPLEFT
		};

		public static final Direction[] ALL_DIRECTIONS = Direction.values();

		public static final int[] KNIGHT_DIRECTIONS = new int[] {
			Direction.sumDirections(Direction.UP, Direction.UP, Direction.RIGHT),
			Direction.sumDirections(Direction.UP, Direction.UP, Direction.LEFT),
			Direction.sumDirections(Direction.DOWN, Direction.DOWN, Direction.LEFT),
			Direction.sumDirections(Direction.DOWN, Direction.DOWN, Direction.RIGHT),
			Direction.sumDirections(Direction.RIGHT, Direction.RIGHT, Direction.DOWN),
			Direction.sumDirections(Direction.LEFT, Direction.LEFT, Direction.UP),
			Direction.sumDirections(Direction.LEFT, Direction.LEFT, Direction.DOWN),
			Direction.sumDirections(Direction.RIGHT, Direction.RIGHT, Direction.UP),
		};

		public static final Direction[][] PAWN_ATTACK_DIRECTIONS = new Direction[][] {
			new Direction[] {Direction.DOWNLEFT, Direction.DOWNRIGHT},		//BLACK
			new Direction[] {Direction.UPRIGHT, Direction.UPLEFT}				//WHITE
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
		public static final int BLACK_KINGSIDE_ROOK_POS = ORIGIN + DirectionConstants.Direction.RIGHT.rawArrayValue * 7;

		public static final int WHITE_QUEENSIDE_ROOK_POS = ORIGIN + DirectionConstants.Direction.DOWN.rawArrayValue * 7;
		public static final int WHITE_KINGSIDE_ROOK_POS = WHITE_QUEENSIDE_ROOK_POS + DirectionConstants.Direction.RIGHT.rawArrayValue * 7;

		public static final int BLACK_KING_POS = ORIGIN + DirectionConstants.Direction.RIGHT.rawArrayValue * 4;
		public static final int WHITE_KING_POS = BLACK_KING_POS + DirectionConstants.Direction.DOWN.rawArrayValue * 7;

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
