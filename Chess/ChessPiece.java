package Chess;

import java.util.ArrayList;
import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.ChessBoard.*;

/**
 * Class representing an individual chess piece
 */
public class ChessPiece {
	public byte type;
	public int pos;
	private boolean updatingCopy;
	private ChessPiece pinPiece;
	private ArrayList<Move> movesCopy;
	
	public final byte color;
	public final int pieceID;
	private final ChessBoard board;
	
	/**
	 * Constructs a new Chess Piece
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
	 * @param color Color of the piece; BLACK : 0, WHITE : 1
	 * @param pos Position of the piece, 0-63
	 * @param board ChessBoard object the piece occupies
	 * @param pieceID Unique identifier for the chess piece
	 */
	public ChessPiece(byte type, byte color, int pos, ChessBoard board, int pieceID) {
		this.type = type;
		this.pos = pos;
		this.color = color;
		this.board = board;
		this.pieceID = pieceID;

		updatingCopy = false;
		movesCopy = !isEmpty() ? new ArrayList<Move>(MAX_MOVES[type]) : null;
		pinPiece = null;
	}

	/**
	 * Updates the squares on the board that the piece is attacking
	 * @param remove Boolean determining if the piece is attacking more or less squares
	 */
	public void pieceAttacks(boolean remove) {
		reset();									//Piece has moved or been captured, thus reset the stored copy of moves
		switch(type) {
			case (PAWN): pawnAttacks(remove);
				break;
			case (KNIGHT): knightAttacks(remove);
				break;
			case (KING): kingAttacks(remove);
				break;
			default: slidingAttacks(remove);
				break;
		}
	}
	
	/**
	 * Updates the squares on the board that the pawn is attacking 
	 * @param remove Boolean determining if the pawn is attacking more or less squares
	 */
	private void pawnAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();

		// Checks the two diagonals next to the pawn
		for (int i = 0; i < 2; i++) {
			if (getDistFromEdge(PAWN_DIAGONALS[color][i], pos) < 1) continue;
			final int newPos = pos + PAWN_DIAGONALS[color][i];

			board.modifyAttacks(this, newPos, remove);
			if (!remove && board.getPiece(newPos).color == ChessBoard.next(color)) movesCopy.add(new Move(pos, newPos, false));
		}

		//Update the pawns move copy with the squares in front of it
		final int direction = ChessBoard.getPawnDirection(color);
		if (!remove && board.getPiece(pos + direction).isEmpty()) {
			movesCopy.add(new Move(pos, pos + direction, false));
			if (getRow(pos) == PAWN_STARTS[color]) {
				if (board.getPiece(pos + direction * 2).isEmpty()) movesCopy.add(new Move(pos, pos + direction * 2, false));
			}
		}

		ChessGame.timePawnAttack += System.currentTimeMillis() - prevTime;
	}
	
	/**
	 * Updates the squares on the board that the knight is attacking 
	 * @param remove Boolean determining if the knight is attacking more or less squares
	 */
	private void knightAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();

		//Checks all squares a knight can attack
		for (int i = 0; i < KNIGHT_MOVES.length; i++) {
			final int newPos = pos + KNIGHT_MOVES[i];
			if (!onBoard(newPos) || !onL(pos, newPos)) continue;		//Makes sure the knight doesn't skip rows or columns

			board.modifyAttacks(this, newPos, remove);
			if (board.getPiece(newPos).color != color && !remove) movesCopy.add(new Move(pos, newPos, false));
		}

		ChessGame.timeKnightAttack += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Updates the squares on the board that the bishop, rook, or queen is attacking 
	 * @param remove Boolean determining if the bishop, rook, or queen is attacking more or less squares
	 */
	private void slidingAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();

		final int[] directions = isQueen() ? DIRECTIONS : (isBishop() ? DIAGONAL_DIRECTIONS : STRAIGHT_DIRECTIONS);	//Directions the piece can attack in
		// Adds or removes attacks in each direction
		for (final int direction : directions) {
			if (remove) {
				removeAttacks(direction, pos);
				continue;
			}
			addAttacksSliding(direction);
		}

		ChessGame.timeSlidingAttack += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Same as {@link ChessPiece#addAttacks(int, int, int)} except it stores the moves in the copy.
	 * @param direction Direction to move towards
	 */
	private void addAttacksSliding(int direction) {
		final int distance = getDistFromEdge(direction, pos);
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = pos + direction * i;
			board.addAttacker(this, newPos);
			if (board.getPiece(newPos).color != color) movesCopy.add(new Move(pos, newPos, false));

			if (!board.getPiece(newPos).isEmpty()) break;
		}
	}
	
	/**
	 * Updates the squares on the board that the king is attacking 
	 * @param remove Boolean determining if the king is attacking more or less squares
	 */
	private void kingAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();

		//Adds or removes attacks one square in each direction
		for (final int direction : DIRECTIONS) {
			if (getDistFromEdge(direction, pos) < 1) continue;
			final int newPos = pos + direction;

			board.modifyAttacks(this, newPos, remove);
			if (board.getPiece(newPos).color != color && !remove) movesCopy.add(new Move(pos, newPos, false));
		}

		ChessGame.timeKingAttack += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Updates the attacks of a bishop, rook, or queen aloong a diagonal or line
	 * @param square Position of the modified square; starting position or end position of a move
	 * @param index Integer representing if the square paramater is the starting position or end position of the move
	 * @param isAttack If the move resulted in a capture of another piece
	 * @param undoMove Whether or not the move is being undone
	 */
	public void softAttack(int square, int index, boolean isAttack, boolean undoMove) {
		//Need to combine this so its called only once and takes a Move as a paramater

		// if (pos == move.start || pos == move.finish) return;
		if (isPawn() || isKnight() || isKing()) return;

		// if (board.isCastle(move) && (Math.abs(pos - move.finish) == 1 || Math.abs(pos - move.start) == 1)) return;

		if (index == START) {
			final int direction = getDirection(pos, square);
			/** 
			 * If the move is a capture and we're undoing it, then the square will be replaced by the captured piece
			 * and thus no attacks are added.
			 * If the move is normal then the starting square will always be empty and attacks must be added. 
			*/
			if (!(isAttack && undoMove)) addAttacks(direction, square);
			return;
		}

		if (index == END) {
			final int direction = getDirection(pos, square);
			/**
			 * If the move is a capture and normal, then the square would've already been occupied and thus no attacks
			 * need to be removed.
			 * If the move is an undo or not a capture, then the square was empty and thus attacks will be removed.
			 */
			if (!(isAttack && !undoMove)) removeAttacks(direction, square);
			return;
		}

		//Runs for the enPassant square
		final int direction = getDirection(pos, square);
		//If undoing the move, the En Passant square is now occupied 
		if (undoMove) {
			removeAttacks(direction, square);
		}
		else {
			addAttacks(direction, square);
		}
	}
	
	/**
	 * Adds attacks along a direction until the edge of the board is reached or a piece blocks
	 * @param direction Represents the direction to add attacks along
	 * @param startingPos Position to start adding attacks, non inclusive
	 */
	private void addAttacks(int direction, int startingPos) {
		addAttacks(direction, startingPos, getDistFromEdge(direction, startingPos));
	}

	/**
	 * Removes attacks along a direction until the edge of the board is reached or a piece blocks
	 * @param direction Represents the direction to remove attacks along
	 * @param startingPos Position to start removing attacks, non inclusive
	 */
	private void removeAttacks(int direction, int startingPos) {
		removeAttacks(direction, startingPos, getDistFromEdge(direction, startingPos));
	}

	/**
	 * Adds attacks along a direction across a certain distance, interrupted by a piece block
	 * @param direction Represents the direction to add attacks along
	 * @param startingPos Position to start adding attacks, non inclusive
	 * @param distance Distance to add attacks along
	 */
	private void addAttacks(int direction, int startingPos, int distance) {
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = startingPos + direction * i;
			board.addAttacker(this, newPos);
			if (!board.getPiece(newPos).isEmpty()) break;
		}
	}

	/**
	 * Remove attacks along a direction across a certain distance, interrupted by a piece block
	 * @param direction Represents the direction to remove attacks along
	 * @param startingPos Position to start removing attacks, non inclusive
	 * @param distance Distance to remove attacks along
	 */
	private void removeAttacks(int direction, int startingPos, int distance) {
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = startingPos + direction * i;
			board.removeAttacker(this, newPos);
			if (!board.getPiece(newPos).isEmpty()) break;
		}
	}

	/**
	 * Adds all possible moves a piece has to an ArrayList
	 * @param moves ArrayList to be modified
	 */
	public void pieceMoves(ArrayList<Move> moves) {
		pieceMoves(moves, false);
	}

	/**
	 * Adds all specified moves a piece has to an ArrayList
	 * @param moves ArrayList to be modified
	 * @param attacksOnly Whether or not captures only should be returned
	 */
	public void pieceMoves(ArrayList<Move> moves, boolean attacksOnly) {
		long prevTime = System.currentTimeMillis();

		updatingCopy = movesCopy.isEmpty() || !shouldCopyOptimize;	//If the copy of moves is empty need to regenerate moves.

		if (!isKing() && board.doubleCheck(color)) return; //If the king is double checked, then the king is the only piece that can move.

		long prevTime2 = System.currentTimeMillis();
		switch (type) {
			case PAWN: 
				pawnMoves(moves, attacksOnly);

				ChessGame.timePawnGen += System.currentTimeMillis() - prevTime2;

				break;
			case KNIGHT: 
				knightMoves(moves, attacksOnly);

				ChessGame.timeKnightGen += System.currentTimeMillis() - prevTime2;

				break;
			case KING:
				kingMoves(moves, attacksOnly);

				ChessGame.timeKingGen += System.currentTimeMillis() - prevTime;

				break;
			default: 
				slidingMoves(moves, attacksOnly);

				ChessGame.timeSlidingGen += System.currentTimeMillis() - prevTime2;

				break;
		}
		pinPiece = null;

		ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
	}
	
	private void pawnMoves(ArrayList<Move> moves, boolean attacksOnly) {
		if (board.getEnPassant() != EMPTY) {
			if (Math.abs(board.getEnPassant() - pos) == 1 && getDistance(board.getEnPassant(), pos) == 1) {
				final int newPos = (color == WHITE) ? board.getEnPassant() - 8 : board.getEnPassant() + 8;
				if (board.getPiece(newPos).isEmpty()) addMove(moves, new Move(pos, newPos, true), attacksOnly);
			}
		}

		if (!updatingCopy) {
			pinPiece = getPin();
			if (board.isChecked(color)) {
				checkMoves(moves, attacksOnly);
				return;
			}

			if (pinPiece.isEmpty()) {
				copy(moves, attacksOnly);
				return;
			}

			if (ChessBoard.onColumn(pinPiece.pos, pos)) {
				copyPawn(moves, attacksOnly);
				return;
			}

			if (ChessBoard.onRow(pinPiece.pos, pos)) {
				return;
			}

			checkPawn(moves);
			return;
		}
		
		final int direction = (color == WHITE) ? -8 : 8;
		if (board.getPiece(pos + direction).isEmpty()) {
			addMove(moves, new Move(pos, pos + direction, false), attacksOnly);
			if (getRow(pos) == PAWN_STARTS[color]) {
				if (board.getPiece(pos + direction * 2).isEmpty()) addMove(moves, new Move(pos, pos + direction * 2, false), attacksOnly);
			}
		}
		//Attacks
		for (int i = 0; i < 2; i++) {
			if (getDistFromEdge(PAWN_DIAGONALS[color][i], pos) < 1) continue;
			final int newPos = pos + PAWN_DIAGONALS[color][i];
			if (board.getPiece(newPos).color == ChessBoard.next(color)) addMove(moves, new Move(pos, newPos, false), attacksOnly);
		}
	}
	
	private void knightMoves(ArrayList<Move> moves, boolean attacksOnly) {
		pinPiece = getPin();
		if (!pinPiece.isEmpty()) return;

		if (!updatingCopy) {
			if (!board.isChecked(color)) {
				copy(moves, attacksOnly);
				return;
			}
			checkMoves(moves, attacksOnly);
			return;
		}

		for (int i = 0; i < KNIGHT_MOVES.length; i++) {
			final int newPos = pos + KNIGHT_MOVES[i];
			if (!onBoard(newPos) || !onL(pos, newPos)) continue;
			
			if (board.getPiece(newPos).color == color) continue;
			addMove(moves, new Move(pos, newPos, false), attacksOnly);
		}
	}
	
	private void kingMoves(ArrayList<Move> moves, boolean attacksOnly) {
		if (castleCheck(true)) {
			addMove(moves, new Move(pos, ROOK_POSITIONS[color][1] - 1, true), attacksOnly);
		}

		if (castleCheck(false)) {
			addMove(moves, new Move(pos, ROOK_POSITIONS[color][0] + 2, true), attacksOnly);
		}

		if (!updatingCopy) {
			if (!board.isChecked(color)) {
				copyKing(moves, attacksOnly);
				return;
			}
			checkMoves(moves, attacksOnly);
			return;
		}

		for (int i = 0; i < DIRECTIONS.length; i++) {
			if (getDistFromEdge(DIRECTIONS[i], pos) < 1) continue;
			final int newPos = pos + DIRECTIONS[i];
			if(board.getPiece(newPos).color == color) continue;

			addMove(moves, new Move(pos, newPos, false), attacksOnly);
		}
	}

	private boolean castleCheck(boolean kingSide) {
		if (board.isChecked(color)) return false;
		if (!board.getCastling(color)[kingSide ? 1 : 0]) return false;

		final int rookPos = ROOK_POSITIONS[color][kingSide ? 1 : 0];
		if (!board.getPiece(rookPos).isRook()) return false;

		final int distance = Math.abs(rookPos - pos);
		for (int i = 1; i < distance; i++) {
			if (!board.getPiece(kingSide ? pos + i : pos - i).isEmpty()) return false;
		}

		for (int i = 1; i < 3; i++) {
			if (board.isAttacked(kingSide ? pos + i : pos - i, color)) return false;
		}

		return true;
	}

	private void slidingMoves(ArrayList<Move> moves, boolean attacksOnly) {
		if (!updatingCopy) {
			pinPiece = getPin();
			if (!board.isChecked(color) && pinPiece.isEmpty()) {
				copy(moves, attacksOnly);
				return;
			}
			checkMoves(moves, attacksOnly);
			return;
		}

		final int startingIndex = isBishop() ? 4 : 0;
		final int endIndex = isRook() ? 4 : 8;
		for (int i = startingIndex; i < endIndex; i++) {
			for (int j = 1; j < getDistFromEdge(DIRECTIONS[i], pos) + 1; j++) {
				final int newPos = pos + DIRECTIONS[i] * j;
				final ChessPiece piece = board.getPiece(newPos);

				if (piece.color == color) break;
				
				addMove(moves, new Move(pos, newPos, false), attacksOnly);

				if (!piece.isEmpty()) break;
			}
		}
	}

	private void addMove(ArrayList<Move> moves, Move move, boolean attacksOnly) {
		long prevTime = System.currentTimeMillis();
		if (updatingCopy && !move.SPECIAL) {
			movesCopy.add(move);
		}
		if (attacksOnly) {
			if (board.getPiece(move.finish).isEmpty() && !board.isPassant(move)) return;
		}
		if (!CHECKS) {
			moves.add(move);
			return;
		}
		if (validMove(move)) {
			ChessGame.timeValidMove += System.currentTimeMillis() - prevTime;
			prevTime = System.currentTimeMillis();
			moves.add(move);
			ChessGame.timeValidPart += System.currentTimeMillis() - prevTime;
			return;
		}
		ChessGame.timeValidMove += System.currentTimeMillis() - prevTime;
	}

	private boolean validMove(Move move) {
		final ChessPiece piece = board.getPiece(move.start);
		if (piece.isKing()) {
			return kingCheck(move);
		}
		if (board.doubleCheck(piece.color)) return false;
		if (board.isChecked(piece.color)) {
			if (!stopsCheck(move)) {
				return false;
			}
		}
		return !isPinned(move);
	}
	
	private boolean kingCheck(Move move) {
		if (board.isAttacked(move.finish, color)) return false;
		if (!board.isChecked(color)) return true;
		for (final ChessPiece attacker : board.getAttackers(this)) {
			if (move.finish == attacker.pos) return true;
			if (attacker.isQueen() || attacker.isBishop()) {
				if (onSameDiagonal(move.start, move.finish, attacker.pos)) return false;
			}
			if (attacker.isQueen() || attacker.isRook()) {
				if (onSameLine(move.start, move.finish, attacker.pos)) return false;
			}
		}
		return true;
	}
	
	private boolean stopsCheck(Move move) {
		final int king = board.getKingPos(color);
		final ChessPiece attacker = board.getKingAttacker(color);
		if (board.isPassant(move) && board.getEnPassant() == attacker.pos) return true;
		if (attacker.isPawn() || attacker.isKnight()) return move.finish == attacker.pos;
		if (move.finish == attacker.pos) return true;
		
		if (onDiagonal(king, attacker.pos)) {
			return blocksDiagonal(attacker.pos, king, move.finish);
		}
		return blocksLine(attacker.pos, king, move.finish);
	}
	
	private boolean isPinned(Move move) {
		pinPiece = getPin();
		final int king = board.getKingPos(color);

		if (board.isPassant(move)) {
			final PieceSet attackers = new PieceSet();
			final int enPassant = board.getEnPassant();
			if ((onDiagonal(king, enPassant) || onLine(king, enPassant)) && board.isAttacked(enPassant, color)) {
				attackers.addAll(board.getAttacks(enPassant, ChessBoard.next(color)));
			}
			if (!pinPiece.isEmpty()) {
				attackers.add(pinPiece);
			}

			for (final ChessPiece piece : attackers) {
				if (piece.isPawn() || piece.isKnight() || piece.isKing()) continue;

				if (piece.isBishop() || piece.isQueen()) {
					final int pinnedPos = onDiagonal(piece.pos, board.getEnPassant()) ? board.getEnPassant() : move.start;
					if (blocksDiagonal(piece.pos, king, pinnedPos)) {
						if (onSameDiagonal(move.finish, king, piece.pos) && pinnedPos == move.start) return false;

						final int direction = getDiagonalDirection(piece.pos, pinnedPos);

						for (int j = 1; j < getDistanceVert(pinnedPos, king); j++) {
							if (!board.getPiece(pinnedPos + direction * j).isEmpty()) return false;
						}
						return true;
					}
				}

				if (piece.isRook() || piece.isQueen()) {
					if (blocksLine(piece.pos, king, move.start)) {
						if (onSameLine(move.finish, king, piece.pos)) return false;

						final int direction = getHorizontalDirection(piece.pos, move.start);

						for (int j = 1; j < getDistance(move.start, king); j++) {
							final int newPos = move.start + direction * j;
							if (!board.getPiece(newPos).isEmpty() && newPos != board.getEnPassant()) return false;
						}
						return true;
					}
				}
			}
			return false;
		}
		else {
			if (pinPiece.isEmpty()) return false;
			if (onDiagonal(pos, king)) return !onSameDiagonal(move.finish, king, pinPiece.pos);
			if (onLine(pos, king)) return !onSameLine(move.finish, king, pinPiece.pos);
			new Exception("Invalid Attacker");
			return true;
		}
	}

	private ChessPiece getPin() {
		if (pinPiece != null) return pinPiece;
		final int king = board.getKingPos(color);

		final boolean pinnedPiece = (onDiagonal(king, pos) || onLine(king, pos)) && board.isAttacked(this);
		if (!pinnedPiece) return empty();
		final PieceSet attackers = board.getAttackers(this);
		for (final ChessPiece attacker : attackers) {
			if (attacker.isPawn() || attacker.isKnight() || attacker.isKing()) continue;

			if (blocksDiagonal(attacker.pos, king, pos)) {

				final int direction = getDiagonalDirection(pos, king);

				for (int j = 1; j < getDistanceVert(pos, king); j++) {
					if (!board.getPiece(pos + direction * j).isEmpty()) return empty();
				}
				return attacker;
			}

			if (blocksLine(attacker.pos, king, pos)) {

				final int direction = getHorizontalDirection(pos, king);

				for (int j = 1; j < getDistance(pos, king); j++) {
					if (!board.getPiece(pos + direction * j).isEmpty()) return empty();
				}
				return attacker;
			}
		}
		return empty();
	}

	private void checkMoves(ArrayList<Move> moves, boolean attacksOnly) {
		for (final Move move : movesCopy) {
			ChessGame.copyCount ++;
			addMove(moves, move, attacksOnly);
		}
	}

	private void checkPawn(ArrayList<Move> moves) {
		for (final Move move : movesCopy) {
			ChessGame.copyCount ++;
			if (move.finish == pinPiece.pos) {
				moves.add(move);
				return;
			}
		}
	}

	private void copy(ArrayList<Move> moves, boolean attacksOnly) {
		for (final Move move : movesCopy) {
			ChessGame.copyCount ++;
			if (attacksOnly) {
				if (board.getPiece(move.finish).isEmpty() && !board.isPassant(move)) continue;
			}
			moves.add(move);
		}
	}

	private void copyPawn(ArrayList<Move> moves, boolean attacksOnly) {
		if (attacksOnly) return;
		for (final Move move : movesCopy) {
			ChessGame.copyCount ++;
			if (ChessBoard.onColumn(move.start, move.finish)) moves.add(move);
		}
	}

	private void copyKing(ArrayList<Move> moves, boolean attacksOnly) {
		for (final Move move : movesCopy) {
			ChessGame.copyCount ++;
			if (attacksOnly) {
				if (board.getPiece(move.finish).isEmpty() && !board.isPassant(move)) continue;
			}
			if (CHECKS && board.isAttacked(move.finish, color)) continue;
			moves.add(move);
		}
	}

	public void updateCopy(boolean remove, int square) {
		final Move move = new Move(pos, square, false);
		if (remove) {
			movesCopy.remove(move);
			return;
		}
		movesCopy.add(move);
	}

	public void reset() {
		movesCopy = new ArrayList<Move>(MAX_MOVES[type]);
	}

	public void setPos(int newPos) {
		pos = newPos;
	}
	
	public boolean isEmpty() {
		return type == EMPTY;
	}

	public boolean isPawn() {
		return type == PAWN;
	}

	public boolean isKnight() {
		return type == KNIGHT;
	}

	public boolean isBishop() {
		return type == BISHOP;
	}

	public boolean isRook() {
		return type == ROOK;
	}

	public boolean isQueen() {
		return type == QUEEN;
	}

	public boolean isKing() {
		return type == KING;
	}

	public boolean isDiagonal() {
		return (isBishop() || isQueen());
	}

	public boolean isLine() {
		return (isRook() || isQueen());
	}

	public boolean hasCopy() {
		return movesCopy.size() > 0;
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
	
	public static ChessPiece empty() {
		return new ChessPiece(EMPTY, EMPTY, EMPTY, null, EMPTY);
	}
}
