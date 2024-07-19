package Chess;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;

public class BoardUtil {
	public static int squareToIndex(String square) {
		final int ascii = (int) square.charAt(0);
		return (ascii - 97) +  (8 - Character.getNumericValue(square.charAt(1))) * 8;
	}

	public static String indexToSquare(int column, int row) {
		return Character.toString(COLUMNS[column]) + row;
	}
	
	public static ChessPiece charToPiece(char letter, int pos, ChessBoard board, int[] pieceIDs) {
		final byte color = Character.isLowerCase(letter) ? BLACK : WHITE;
		final char[] pieces = PIECES[color];
		for (byte i = 0; i < pieces.length; i++) {
			if (letter == pieces[i]) {
				pieceIDs[color] ++;
				return new ChessPiece(i, color, pos, board, pieceIDs[color] - 1);
			}
		}
		return null;
	}
	
	public static char pieceToChar(ChessPiece piece) {
		return PIECES[piece.color][piece.getType()];
	}
	
    public static int next(int currTurn) {
		return (currTurn + 1) % 2;
	}
    
    public static int getDistFromEdge(int direction, int pos) {
		switch (direction) {
			case(9): return distFromEdge[pos][5];
			case(-9): return distFromEdge[pos][4];
			case(7): return distFromEdge[pos][7];
			case(-7): return distFromEdge[pos][6];
			case(8): return distFromEdge[pos][1];
			case(-8): return distFromEdge[pos][0];
			case(1): return distFromEdge[pos][3];
			case(-1): return distFromEdge[pos][2];
			default:
				new Exception("Invalid direction");
				return -1;
		} 
	}

	public static int getPawnDirection(int color) {
		return color == WHITE ? -8 : 8;
	}

	public static int getDirection(int startingPos, int endPos) {
		return onDiagonal(startingPos, endPos) ? getDiagonalDirection(startingPos, endPos) : getHorizontalDirection(startingPos, endPos);
	}

	public static int getDiagonalDirection(int startingPos, int endPos) {
		int direction = Math.abs(startingPos - endPos) % 7 == 0 ? 7 : 9;
		if (startingPos - endPos > 0) direction *= -1;
		return direction;
	}

	public static int getHorizontalDirection(int startingPos, int endPos) {
		int direction = onColumn(startingPos, endPos) ? 8 : 1;
		if (startingPos - endPos > 0) direction *= -1;
		return direction;
	}

	public static boolean onPawn(ChessPiece pawn, int moveSquare) {
		final int offset = moveSquare - pawn.getPos();
		final int direction = (pawn.color == WHITE) ? -1 : 1;
		if (offset == 8 * direction || (getRow(pawn.getPos()) == PAWN_STARTS[pawn.color] && offset == 16 * direction)) return true;
		if (!onDiagonal(pawn.getPos(), moveSquare)) return false;
		return (offset == 7 * direction || offset == 9 * direction);
	}
	
	public static boolean blocksLine(int attacker, int target, int defender) {
		return onSameLine(attacker, target, defender) && getDistance(defender, target) < getDistance(attacker, target) &&
				getDistance(defender, attacker) < getDistance (target, attacker);
	}
	
	public static boolean onSameLine(int pos1, int pos2, int pos3) {
		return (onColumn(pos1, pos2) && onColumn(pos1, pos3)) || (onRow(pos1, pos2) && onRow(pos1, pos3));
	}

	public static boolean hasPawnMoved(int pos, int color) {
		return getRow(pos) != PAWN_STARTS[color];
	}
	
	public static boolean onLine(int pos1, int pos2) {
		return onRow(pos1, pos2) || onColumn(pos1, pos2);
	}
	
	public static boolean onL(int pos1, int pos2) {
		final int distanceVert = getDistanceVert(pos1, pos2);
		final int distanceHor = getDistanceHor(pos1, pos2);
		return (distanceVert == 1 && distanceHor == 2) || (distanceVert == 2 && distanceHor == 1);
	}
	
	public static boolean blocksDiagonal(int attacker, int target, int defender) {
		return onSameDiagonal(attacker, target, defender) && getDistance(defender, target) < getDistance(attacker, target) &&
				getDistance(defender, attacker) < getDistance (target, attacker);
	}
	
	public static boolean onSameDiagonal(int pos1, int pos2, int pos3) {
		return onDiagonal(pos1, pos2) && onDiagonal(pos2, pos3) && onDiagonal(pos1, pos3);
	}
	
	public static boolean onDiagonal(int pos1, int pos2) {
		return getDistanceVert(pos1, pos2) == getDistanceHor(pos1, pos2);
	}
	
	public static boolean onColumn(int pos1, int pos2) {
		return getColumn(pos1) == getColumn(pos2);
	}
	
	public static boolean onRow(int pos1, int pos2) {
		return getRow(pos1) == getRow(pos2);
	}
	
	public static boolean onBoard(int pos) {
		return (pos >= 0 && pos <= 63);
	}
	
	public static int getDistance(int pos1, int pos2) {
		return getDistanceHor(pos1, pos2) + getDistanceVert(pos1, pos2);
	}
	
	public static int getDistanceHor(int pos1, int pos2) {
		return Math.abs(getColumn(pos1) - getColumn(pos2));
	}
	
	public static int getDistanceVert(int pos1, int pos2) {
		return Math.abs(getRow(pos1) - getRow(pos2));
	}
	
	public static int getRow(int pos) {
		return pos / 8;
	}
	
	public static int getColumn(int pos) {
		return pos % 8;
	}
}
