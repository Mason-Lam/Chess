package Chess;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;

/**
 * Utility class to organize commonly repeated board calculations.
 */
public class BoardUtil {

	/**
	 * Converts a square represented as a string to an index from 0 to 63.
	 * @param square The square to convert.
	 * @return The index of the square from 0 to 63.
	 */
	public static int squareToIndex(String square) {
		final int ascii = (int) square.charAt(0);
		return (ascii - 97) +  (8 - Character.getNumericValue(square.charAt(1))) * 8;
	}

	/**
	 * Converts a row and column to a string square.
	 * @param column The column of the square.
	 * @param row The row of the square.
	 * @return The square represented as a string.
	 */
	public static String indexToSquare(int column, int row) {
		return Character.toString(COLUMNS[column]) + row;
	}
	
	/**
	 * Converts a letter to a ChessPiece.
	 * @param letter The letter being converted.
	 * @param pos The position of the piece.
	 * @param board The board the piece is on.
	 * @param pieceIDs int array used to assign unique IDs to pieces.
	 * @return A new ChessPiece object.
	 */
	public static ChessPiece charToPiece(char letter, int pos, ChessBoard board, int[] pieceIDs) {
		final byte color = Character.isLowerCase(letter) ? BLACK : WHITE;
		final char[] pieces = PIECES[color];
		for (byte i = 0; i < pieces.length; i++) {
			if (letter == pieces[i]) {
				pieceIDs[color] ++;
				return new ChessPiece(i, color, pos, board, pieceIDs[color] - 1);
			}
		}
		throw new IllegalArgumentException("Invalid piece character");
	}
	
	/**
	 * Converts a ChessPiece to a character.
	 * @param piece The piece being converted.
	 * @return A character representing the chess piece.
	 */
	public static char pieceToChar(ChessPiece piece) {
		return PIECES[piece.color][piece.getType()];
	}

	/**
	 * Flips the color given.
	 * @param color The current color.
	 * @return The opposite color.
	 */
    public static int next(int color) {
		return (color + 1) & 1;
	}
    
	/**
	 * Returns the number of squares between the given position and the edge of the board heading in a specific direction.
	 * @param direction The direction to travel in.
	 * @param pos The position of the square.
	 * @return The distance between the square and the edge of the board.
	 */
    public static int getDistFromEdge(int direction, int pos) {
		switch (direction) {
			case(9): return distFromEdge[pos][5];	//Southeast
			case(-9): return distFromEdge[pos][4];	//Northwest
			case(7): return distFromEdge[pos][7];	//Southwest
			case(-7): return distFromEdge[pos][6]; //Northeast
			case(8): return distFromEdge[pos][1];  //South
			case(-8): return distFromEdge[pos][0];  //North
			case(1): return distFromEdge[pos][3];   //East
			case(-1): return distFromEdge[pos][2];  //West
			default:
				throw new IllegalArgumentException("Invalid direction");
		}
	}

	/**
	 * Returns the direction pawns move in based on color.
	 * @param color The color of the pawn.
	 * @return The direction the pawns move.
	 */
	public static int getPawnDirection(int color) {
		return color == WHITE ? -8 : 8;
	}

	/**
	 * Returns the direction to travel to get from one point to another.
	 * @param startingPos The starting point. 
	 * @param endPos The point to travel towards.
	 * @return The direction to travel.
	 */
	public static int getDirection(int startingPos, int endPos) {
		return onDiagonal(startingPos, endPos) ? getDiagonalDirection(startingPos, endPos) : getHorizontalDirection(startingPos, endPos);
	}

	/**
	 * Returns the direction to travel to get from one point to another, precondition: the points are on the same diagonal.
	 * @param startingPos The starting point. 
	 * @param endPos The point to travel towards.
	 * @return The direction to travel.
	 */
	public static int getDiagonalDirection(int startingPos, int endPos) {
		final int offset = startingPos - endPos;
		final int direction = (offset) % 9 == 0 ? 9 : 7;
		return direction * (offset > 0 ? -1 : 1);
	}

	/**
	 * Returns the direction to travel to get from one point to another, precondition: the points are on the same line.
	 * @param startingPos The starting point. 
	 * @param endPos The point to travel towards.
	 * @return The direction to travel.
	 */
	public static int getHorizontalDirection(int startingPos, int endPos) {
		final int direction = onColumn(startingPos, endPos) ? 8 : 1;
		return direction * (startingPos - endPos > 0 ? -1 : 1);
	}

	public static boolean hasPawnMoved(int pos, int color) {
		return getRow(pos) != PAWN_STARTS[color];
	}

	/**
	 * Returns whether or not a piece is attacking another piece.
	 * @param attacker The attacking piece.
	 * @param target The target piece.
	 * @param blocker The blocking piece.
	 * @return True if the blocking piece is in between the target and attacker, false otherwise.
	 */
	public static boolean blocksDiagonal(int attacker, int target, int blocker) {
		return onSameDiagonal(attacker, target, blocker) && getDistance(blocker, target) < getDistance(attacker, target) &&
				getDistance(blocker, attacker) < getDistance (target, attacker);
	}

	/**
	 * Returns whether or not a piece is attacking another piece.
	 * @param attacker The attacking piece.
	 * @param target The target piece.
	 * @param blocker The blocking piece.
	 * @return True if the blocking piece is in between the target and attacker, false otherwise.
	 */
	public static boolean blocksLine(int attacker, int target, int blocker) {
		return onSameLine(attacker, target, blocker) && getDistance(blocker, target) < getDistance(attacker, target) &&
				getDistance(blocker, attacker) < getDistance (target, attacker);
	}
	
	/**
	 * Returns whether or not three positions are on the same diagonal.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @param pos3 The third position.
	 * @return True if the positions are on the same diagonal, false otherwise.
	 */
	public static boolean onSameDiagonal(int pos1, int pos2, int pos3) {
		return onDiagonal(pos1, pos2) && onDiagonal(pos2, pos3) && onDiagonal(pos1, pos3);
	}

	/**
	 * Returns whether or not three positions are on the same line.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @param pos3 The third position.
	 * @return True if the positions are on the same line, false otherwise.
	 */
	public static boolean onSameLine(int pos1, int pos2, int pos3) {
		return (onColumn(pos1, pos2) && onColumn(pos1, pos3)) || (onRow(pos1, pos2) && onRow(pos1, pos3));
	}

	/**
	 * Returns whether or not two positions are on a line.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @return True if the positions are on a line, false otherwise.
	 */
	public static boolean onLine(int pos1, int pos2) {
		return onRow(pos1, pos2) || onColumn(pos1, pos2);
	}
	
	/**
	 * Returns whether or not two positions are on an L shape.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @return True if the positions are a knight move away from each other, false otherwise.
	 */
	public static boolean onL(int pos1, int pos2) {
		final int verticalDistance = getRowDistance(pos1, pos2);
		final int horizontalDistance = getColumnDistance(pos1, pos2);
		return (verticalDistance == 1 && horizontalDistance == 2) || (verticalDistance == 2 && horizontalDistance == 1);
	}
	
	/**
	 * Returns whether or not two positions are on the same diagonal.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @return True if the positions are on the same diagonal, false otherwise.
	 */
	public static boolean onDiagonal(int pos1, int pos2) {
		return getRowDistance(pos1, pos2) == getColumnDistance(pos1, pos2);
	}
	
	/**
	 * Returns whether or not two positions are on the same column.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @return True if the positions are on the same column, false otherwise.
	 */
	public static boolean onColumn(int pos1, int pos2) {
		return getColumn(pos1) == getColumn(pos2);
	}
	
	/**
	 * Returns whether or not two positions are on the same row.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @return True if the positions are on the same row, false otherwise.
	 */
	public static boolean onRow(int pos1, int pos2) {
		return getRow(pos1) == getRow(pos2);
	}

	/**
	 * Returns whether or not a position is on the board.
	 * @param pos The position to check.
	 * @return True if the position is on the board, false otherwise.
	 */
	public static boolean onBoard(int pos) {
		return (pos >= 0 && pos <= 63);
	}

	/**
	 * Returns the distance between two positions.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @return The number of squares between the two positions, no diagonals.
	 */
	public static int getDistance(int pos1, int pos2) {
		return getColumnDistance(pos1, pos2) + getRowDistance(pos1, pos2);
	}
	
	/**
	 * Returns the horizontal distance between two positions.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @return The number of columns in between the two positions.
	 */
	public static int getColumnDistance(int pos1, int pos2) {
		return Math.abs(getColumn(pos1) - getColumn(pos2));
	}
	
	/**
	 * Returns the vertical distance between two positions.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @return The number of rows in between the two positions.
	 */
	public static int getRowDistance(int pos1, int pos2) {
		return Math.abs(getRow(pos1) - getRow(pos2));
	}
	
	/**
	 * Returns the row of the given position.
	 * @param pos The position.
	 * @return The row of the position.
	 */
	public static int getRow(int pos) {
		return pos / 8;
	}
	
	/**
	 * Returns the column of the given position.
	 * @param pos The position.
	 * @return The column of the position.
	 */
	public static int getColumn(int pos) {
		return pos & 7;
	}
}
