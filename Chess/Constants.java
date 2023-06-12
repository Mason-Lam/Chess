package Chess;

public class Constants {
	public static final boolean CHECKS = true;
	
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
	public static final int BLACK = 0;
	public static final int WHITE = 1;
	
	//OutcomeConstants
	public static final int DRAW = 0;
	public static final int WIN = 1;
	public static final int PROGRESS = 2;
	
	//Move Constants
	public static final int[][] ROOK_POSITIONS = new int[][] {
		new int[] {0, 7},
		new int[] {56, 63}
	};
	
	public static final int[] PROMOTION_LINE = new int[] {
		7, 0
	};
	
	public static final int[] PAWN_STARTS = new int[] {
		1, 6
	};
	
	public static final boolean[] ALL_MOVES = new boolean[] {
		true, true, true	//Moves, Attacks, Special
	};
	
	public static final boolean[] ATTACKS_ONLY = new boolean[] {
		false, true, false
	};
	
	public static final boolean[] MOVES_ONLY = new boolean[] {
		true, false, false
	};
	
	public static final int[] KING_MOVES = new int[] {
		8, -8, 1, -1, 7, 9, -7, -9
	};
	
	public static final int[] KNIGHT_MOVES = new int[] {
		-15, -17, 15, 17, 10, -10, 6, -6
	};
	
	public static final int[] STRAIGHT = new int[] {
		8, -8, 1, -1
	};
	
	public static final int[] DIAGONALS = new int[] {
		7, 9, -7, -9
	};
	
	public static final int[][] PAWN_DIAGONALS = new int[][] {
		new int[] {DIAGONALS[0], DIAGONALS[1]},
		new int[] {DIAGONALS[2], DIAGONALS[3]}
	};
	
	public static final char[][] PIECES = new char[][] {
		new char[] {'p','n','b','r','q','k'}, //Black
		new char[] {'P','N','B','R','Q','K'} // White
	};

	public static int squareToIndex(String square) {
		int ascii = (int) square.charAt(0);
		return (ascii - 97) +  (8 - Character.getNumericValue(square.charAt(1))) * 8;
	}

	public static String indexToSquare(int column, int row) {
		return Character.toString(COLUMNS[column]) + row;
	}
	
	public static ChessPiece charToPiece(char letter) {
		char[] pieces = PIECES[1];
		byte color = WHITE;
		if (Character.isLowerCase(letter)) {
			pieces = PIECES[0];
			color = BLACK;
		}
		for (byte i = 0; i < pieces.length; i++) {
			if (letter == pieces[i]) {
				return new ChessPiece(i,color);
			}
		}
		return null;
	}
	
	public static char pieceToChar(ChessPiece piece) {
		return PIECES [piece.color][piece.type];
	}
}
