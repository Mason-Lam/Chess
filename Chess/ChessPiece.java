package Chess;

import static Chess.Constants.PositionConstants.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.BoardUtil.*;

/**
 * Class representing an individual chess piece.
 */
public class ChessPiece {
	private static final ChessPiece emptySquare = new ChessPiece(PieceType.EMPTY, PieceColor.COLORLESS, EMPTY, null, EMPTY);

	private PieceType type;
	private int pos;
	
	private final PieceColor color;
	private final int pieceID;
	
	/**
	 * Constructs a new Chess Piece.
	 * @param type Represents the type of piece, 
	 * <ul>
	 * 	<li>EMPTY : -1</li>
	 * 	<li>PAWN : 0</li>
	 * 	<li>KNIGHT : 1</li>
	 * 	<li>BISHOP : 2</li>
	 * 	<li>ROOK : 3</li>
	 * 	<li>QUEEN : 4</li>
	 * 	<li>KING : 5</li>
	 * </ul>
	 * @param color Color of the piece; BLACK : 0, WHITE : 1.
	 * @param pos Position of the piece, 0 to 63.
	 * @param board ChessBoard object the piece occupies.
	 * @param pieceID Unique identifier for the chess piece.
	 */
	public ChessPiece(PieceType type, PieceColor color, int pos, ChessBoard board, int pieceID) {
		this.type = type;
		this.pos = pos;
		this.color = color;
		this.pieceID = pieceID;
	}

	/**
	 * Change the type of the piece.
	 * @param newPos The new position of the piece.
	 */
	public void setType(PieceType newType) {
		type = newType;
	}

	/**
	 * Gets the type of the piece.
	 * @return An enum object representing the type of the piece.
	 */
	public PieceType getType() {
		return type;
	}

	public PieceColor getColor() {
		return color;
	}

	/**
	 * Change the position of the piece.
	 * @param newPos The new position of the piece.
	 */
	public void setPos(int newPos) {
		pos = newPos;
	}

	/**
	 * Gets the position of the piece.
	 * @return An integer from 0 to 63 representing position.
	 */
	public int getPos() {
		return pos;
	}
	
	/**
	 * Checks whether or not a square is empty.
	 * @return True if the square is empty, false otherwise.
	 */
	public boolean isEmpty() {
		return type == PieceType.EMPTY;
	}

	/**
	 * Checks whether or not a piece is a pawn.
	 * @return True if the piece is a pawn, false otherwise.
	 */
	public boolean isPawn() {
		return type == PieceType.PAWN;
	}

	/**
	 * Checks whether or not a piece is a knight.
	 * @return True if the piece is a knight, false otherwise.
	 */
	public boolean isKnight() {
		return type == PieceType.KNIGHT;
	}

	/**
	 * Checks whether or not a piece is a bishop.
	 * @return True if the piece is a bishop, false otherwise.
	 */
	public boolean isBishop() {
		return type == PieceType.BISHOP;
	}

	/**
	 * Checks whether or not a piece is a rook.
	 * @return True if the piece is a rook, false otherwise.
	 */
	public boolean isRook() {
		return type == PieceType.ROOK;
	}

	/**
	 * Checks whether or not a piece is a queen.
	 * @return True if the piece is a queen, false otherwise.
	 */
	public boolean isQueen() {
		return type == PieceType.QUEEN;
	}

	/**
	 * Checks whether or not a piece is a king.
	 * @return True if the piece is a king, false otherwise.
	 */
	public boolean isKing() {
		return type == PieceType.KING;
	}

	/**
	 * Checks whether or not a piece is a diagonal attacker.
	 * @return True if the piece is a diagonal attacker, false otherwise.
	 */
	public boolean isDiagonalAttacker() {
		return (isBishop() || isQueen());
	}

	/**
	 * Checks whether or not a piece is a straight line attacker.
	 * @return True if the piece is a straight line attacker, false otherwise.
	 */
	public boolean isLineAttacker() {
		return (isRook() || isQueen());
	}

	@Override
	public String toString() {
		return Character.toString(pieceToChar(this));
	}
	
	@Override
	public boolean equals(Object anObject) {
		if (this == anObject) return true;
		if (anObject instanceof ChessPiece) {
			final ChessPiece piece = (ChessPiece) anObject;
			return ((piece.type == type) && (piece.color == color) && (piece.pos == pos) && (piece.pieceID == pieceID));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return pieceID;
	}
	
	/**
	 * Creates a new Empty Chess Piece.
	 * @return Empty square.
	 */
	public static ChessPiece empty() {
		return emptySquare;
	}
}
