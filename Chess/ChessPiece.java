package Chess;

import java.util.ArrayList;
import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PositionConstants.*;
import static Chess.Constants.DirectionConstants.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.BoardUtil.*;

/**
 * Class representing an individual chess piece.
 */
public class ChessPiece {
	private static final ChessPiece emptySquare = new ChessPiece(EMPTY, EMPTY, EMPTY, null, EMPTY);

	private byte type;
	private int pos;
	private boolean updatingCopy;
	private ChessPiece pinPiece;
	public ArrayList<Integer> movesCopy;
	
	public final byte color;
	public final int pieceID;
	private final ChessBoard board;
	
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
	public ChessPiece(byte type, byte color, int pos, ChessBoard board, int pieceID) {
		this.type = type;
		this.pos = pos;
		this.color = color;
		this.board = board;
		this.pieceID = pieceID;

		updatingCopy = false;
		movesCopy = !isEmpty() ? new ArrayList<Integer>(MAX_MOVES[type]) : null;
		pinPiece = null;
	}

	/**
	 * Updates the squares on the board that the piece is attacking.
	 * @param remove Boolean determining if the piece is attacking more or less squares.
	 */
	public void pieceAttacks(boolean remove) {
		resetMoveCopy();									//Piece has moved or been captured, thus reset the stored copy of moves.
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
	 * Updates the squares on the board that the pawn is attacking.
	 * @param remove Boolean determining if the pawn is attacking more or less squares.
	 */
	private void pawnAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();

		// Checks the two diagonals next to the pawn.
		for (final int direction : PAWN_ATTACK_DIRECTIONS[color]) {
			if (getNumSquaresFromEdge(direction, pos) < 1) continue;
			final int newPos = pos + direction;

			board.modifyAttacks(this, newPos, remove);
			if (!remove && board.getPiece(newPos).color == next(color)) movesCopy.add(newPos);
		}

		//Update the pawns move copy with the squares in front of it.
		final int direction = getPawnDirection(color);
		if (!remove && board.getPiece(pos + direction).isEmpty()) {
			movesCopy.add(pos + direction);
			if (getRow(pos) == PAWN_STARTING_ROW[color]) {
				if (board.getPiece(pos + direction * 2).isEmpty()) movesCopy.add(pos + direction * 2);
			}
		}

		ChessGame.timePawnAttack += System.currentTimeMillis() - prevTime;
	}
	
	/**
	 * Updates the squares on the board that the knight is attacking.
	 * @param remove Boolean determining if the knight is attacking more or less squares.
	 */
	private void knightAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();

		//Checks all squares a knight can attack.
		for (final int direction : KNIGHT_DIRECTIONS) {
			final int newPos = pos + direction;
			if (!onBoard(newPos) || !onL(pos, newPos)) continue;		//Makes sure the knight doesn't skip rows or columns.

			board.modifyAttacks(this, newPos, remove);
			if (board.getPiece(newPos).color != color && !remove) movesCopy.add(newPos);
		}

		ChessGame.timeKnightAttack += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Updates the squares on the board that the bishop, rook, or queen is attacking.
	 * @param remove Boolean determining if the bishop, rook, or queen is attacking more or less squares.
	 */
	private void slidingAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();

		final int[] directions = isQueen() ? DIRECTIONS : (isBishop() ? DIAGONAL_DIRECTIONS : STRAIGHT_DIRECTIONS);	//Directions the piece can attack in.
		// Adds or removes attacks in each direction.
		for (final int direction : directions) {
			if (remove) {
				removeAttacksSliding(direction);
				continue;
			}
			addAttacksSliding(direction);
		}

		ChessGame.timeSlidingAttack += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Same as {@link ChessPiece#addAttacks(int, int, int)} except it stores the moves in the copy.
	 * @param direction Direction to move towards.
	 */
	private void addAttacksSliding(int direction) {
		final int distance = getNumSquaresFromEdge(direction, pos);
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = pos + direction * i;
			board.addAttacker(this, newPos);
			if (board.getPiece(newPos).color != color) movesCopy.add(newPos);

			if (!board.getPiece(newPos).isEmpty()) break;
		}
	}

	private void removeAttacksSliding(int direction) {
		final int distance = getNumSquaresFromEdge(direction, pos);
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = pos + direction * i;
			board.removeAttacker(this, newPos);

			if (!board.getPiece(newPos).isEmpty()) break;
		}
	}
	
	/**
	 * Updates the squares on the board that the king is attacking.
	 * @param remove Boolean determining if the king is attacking more or less squares.
	 */
	private void kingAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();

		//Adds or removes attacks one square in each direction.
		for (final int direction : DIRECTIONS) {
			if (getNumSquaresFromEdge(direction, pos) < 1) continue;
			final int newPos = pos + direction;

			board.modifyAttacks(this, newPos, remove);
			if (board.getPiece(newPos).color != color && !remove) movesCopy.add(newPos);
		}

		ChessGame.timeKingAttack += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Update a bishop, rook, or queen's attacks based on a move made, precondition: the piece is attacking a square involved in a move.
	 * @param square The square that the piece is guaranteed to have been attacking.
	 * @param movePart What part of the move is the square, START: 0, END: 1, EN_PASSANT: 2.
	 * @param move The move made.
	 * @param isAttack True if the move is a capture, false if not.
	 * @param undoMove True if the move is an undo move, false if a normal move.
	 * @return True if the piece is fully updated, false if not.
	 */
	public boolean softAttack(int square, int movePart, Move move, boolean isAttack, boolean undoMove) {
		//Comment this.
		if (isPawn() || isKnight() || isKing()) return false;

		final int enPassantDirection = board.isEnPassant(move) ? getDirection(pos, board.getEnPassant()) : 0;

		//Handles the case where a rook or queen is on the same column as the enPassant square.
		if (enPassantDirection == 8 || enPassantDirection == -8 && isLineAttacker()) {
			final int enPassant = board.getEnPassant();
			final boolean enPassantIsCloser = getNumSquaresFromEdge(enPassantDirection, enPassant) > getNumSquaresFromEdge(enPassantDirection, undoMove ? move.start : move.finish);

			//The enPassant square is closer to the attacking piece than the end square of the move.
			if (enPassantIsCloser) {
				//The square goes from empty to filled meaning remove attacks.
				if (undoMove) removeAttacks(enPassantDirection, enPassant, 1);
				//The square goes from filled to empty meaning add attacks.
				else addAttacks(enPassantDirection, enPassant, 1);
				return true;
			}
			//The square goes from empty to filled meaning add attacks.
			if (undoMove) addAttacks(enPassantDirection, move.start, 1);
			//The square goes from filled to empty meaning remove attacks.
			else removeAttacks(enPassantDirection, move.finish, 1);
			return true;
		}

		final int startDirection = getDirection(pos, move.start);
		final int finishDirection = getDirection(pos, move.finish);
		
		//Each square is attacked independently of each other.
		if (startDirection != finishDirection || (startDirection == 0 && finishDirection == 0)) {
			switch (movePart) {
				case START:
					/** 
					 * If the move is a capture and we're undoing it, then the square will be replaced by the captured piece
					 * and thus no attacks are added.
					 * If the move is normal then the starting square will always be empty and attacks must be added. 
					*/
					if (!(isAttack && undoMove)) {
						addAttacks(startDirection, square);
						if (board.isEnPassant(move) && !undoMove) {
							pieceReset(move.start, START, isAttack, undoMove);
							return true;
						}
					}
					return false;
				case END:
					/**
					 * If the move is a capture and normal, then the square would've already been occupied and thus no attacks
					 * need to be removed.
					 * If the move is an undo or not a capture, then the square was empty and thus attacks will be removed.
					 */
					if (!(isAttack && !undoMove)) {
						removeAttacks(finishDirection, square);

					}
					return false;
				case EN_PASSANT:
					//If undoing the move, the En Passant square is now occupied .
					if (undoMove) {
						removeAttacks(enPassantDirection, square);
					}
					else {
						addAttacks(enPassantDirection, square);
					}
					return false;
				default:
					throw new IllegalArgumentException("Invalid move part: " + movePart);
			}
		}
		
		/**
		 * Run when a the start and end of a move are on the same path of attack.
		 */
		final int attackDirection = startDirection;
		final boolean startIsCloser = getNumSquaresFromEdge(startDirection, move.start) > getNumSquaresFromEdge(startDirection, move.finish);
		//If the start of the move is closer, then you only have to add attacks.
		if (startIsCloser) {
			/** 
			 * If the move is a capture and we're undoing it, then the square will be replaced by the captured piece
			 * and thus no attacks are added.
			 * If the move is normal then the starting square will always be empty and attacks must be added. 
			*/
			if (!(isAttack && undoMove)) {
				addAttacks(attackDirection, move.start);
			}
			pieceReset(move.start, START, isAttack, undoMove);
			return true;
		}

		//If the end of the move is closer, then you only have to remove attacks.
		final int distance = getNumSquaresFromEdge(attackDirection, move.finish) - getNumSquaresFromEdge(attackDirection, move.start);
		/**
		 * If the move is a capture and normal, then the square would've already been occupied and thus no attacks
		 * need to be removed.
		 * If the move is an undo or not a capture, then the square was empty and thus attacks will be removed.
		 */
		if (!(isAttack && !undoMove)) {
			removeAttacks(attackDirection, move.finish, board.isCastle(move) ? 1 : distance);
		}
		pieceReset(move.finish, END, isAttack, undoMove);
		return true;
	}

	/**
	 * Updates a piece's move copy list based on a move made.
	 * @param square Position of the modified square; starting position or end position of a move.
	 * @param movePart Integer representing if the square paramater is the starting position or end position of the move.
	 * @param isAttack If the move resulted in a capture of another piece.
	 * @param undoMove Whether or not the move is being undone.
	 */
	public void pieceReset(int square, int movePart, boolean isAttack, boolean undoMove) {
		final int turn = board.getTurn();
		//Comments made from white's perspective
		switch (type) {
			case PAWN:
				if (movePart == EN_PASSANT) {
					/**
					 * Square goes from black to empty (normal move) or empty to black (undo move),
					 * only white pawn's change either attacking another the black piece or losing a capture possibility.
					*/
					if (color == turn) updateCopy(!undoMove, square);
					break;
				}
				if (movePart == START) {
					
					/**
					 * Square goes from white to black, covers the case where it's an capture and undo move.
					 * Black pawns will lose their attack and white pawns will be able to attack the square.
					 */
					if (undoMove && isAttack) {
						updateCopy(color != turn, square);
						break;
					}
					/**
					 * Square goes from white to empty for both normal and undo moves,
					 * black pawns will then no longer be able to attack the square.
					 */
					if (color != turn) updateCopy(true, square);
					break;
				}
				if (movePart == END) {
					/**
					 * Square goes from empty to white for undoing a move or a non capturing normal move,
					 * black pawns will then be able to target the square.
					 */
					if (undoMove || !isAttack) {
						if (color != turn) updateCopy(false, square);
						break;
					}

					/**
					 * Square goes from black to white for a capture normal move,
					 * white pawns will no longer be able to attack the square, but black pawns will.
					 */
					updateCopy(color == turn, square);
				}
				break;
			case KNIGHT:
			case BISHOP:
			case ROOK:
			case QUEEN:
			case KING:
				if (movePart == EN_PASSANT) {
					/**
					 * Square goes from black to empty (normal move) or empty to black (undo move),
					 * white pieces can still move to the square,
					 * black pieces will gain an attack when the move is normal and lose an attack during an undo move.
					 */
					if (color != turn) updateCopy(undoMove, square);
					break;
				}

				/**
				 * Square goes from black to white or white to black.
				 */
				if (isAttack && ((movePart == START && undoMove) || (movePart == END && !undoMove))) {
					/**
					 * If the square goes from black to white (normal move), white pieces lose a move and black pieces gain one.
					 * If the square goes from white to black (undo move) black pieces lose a move and white pieces gain one.
					 */
					updateCopy(undoMove ? color != turn : color == turn, square);
					break;
				}

				/**
				 * Square goes from empty to white or white to empty, black pieces do nothing,
				 * white pieces will lose a move if the square goes from empty to white (END),
				 * white pieces will gain a move if the square goes from white to empty (START).
				 */
				if (color == turn) updateCopy(movePart == END, square);
				break;
			default:
				throw new IllegalArgumentException("Invalid piece type: " + getType());
		}
	}

	/**
	 * Adds attacks along a direction until the edge of the board is reached or a piece blocks.
	 * @param direction Represents the direction to add attacks along.
	 * @param startingPos Position to start adding attacks, non inclusive.
	 */
	private void addAttacks(int direction, int startingPos) {
		addAttacks(direction, startingPos, getNumSquaresFromEdge(direction, startingPos));
	}

	/**
	 * Removes attacks along a direction until the edge of the board is reached or a piece blocks.
	 * @param direction Represents the direction to remove attacks along.
	 * @param startingPos Position to start removing attacks, non inclusive.
	 */
	private void removeAttacks(int direction, int startingPos) {
		removeAttacks(direction, startingPos, getNumSquaresFromEdge(direction, startingPos));
	}

	/**
	 * Adds attacks along a direction across a certain distance, interrupted by a piece block.
	 * @param direction Represents the direction to add attacks along.
	 * @param startingPos Position to start adding attacks, non inclusive.
	 * @param distance Distance to add attacks along.
	 */
	private void addAttacks(int direction, int startingPos, int distance) {
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = startingPos + direction * i;
			if (!board.addAttacker(this, newPos)) throw new IllegalArgumentException();
			final ChessPiece piece = board.getPiece(newPos);

			if (piece.color != color) {
				movesCopy.add(newPos);
			}
			if (!piece.isEmpty()) break;
		}
	}

	/**
	 * Remove attacks along a direction across a certain distance, interrupted by a piece block.
	 * @param direction Represents the direction to remove attacks along.
	 * @param startingPos Position to start removing attacks, non inclusive.
	 * @param distance Distance to remove attacks along.
	 */
	private void removeAttacks(int direction, int startingPos, int distance) {
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = startingPos + direction * i;
			if (!board.removeAttacker(this, newPos)) throw new IllegalArgumentException();
			final ChessPiece piece = board.getPiece(newPos);

			if (piece.color != color) movesCopy.remove((Integer) newPos);
			if (!piece.isEmpty()) break;
		}
	}

	/**
	 * Adds all possible moves a piece has to an ArrayList.
	 * @param moves ArrayList to be modified.
	 */
	public void pieceMoves(ArrayList<Move> moves) {
		pieceMoves(moves, false);
	}

	/**
	 * Adds all specified moves a piece has to an ArrayList.
	 * @param moves ArrayList to be modified.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	public void pieceMoves(ArrayList<Move> moves, boolean attacksOnly) {
		long prevTime = System.currentTimeMillis();

		updatingCopy = movesCopy.isEmpty() || !shouldCopyOptimize;	//If the copy of moves is empty need to regenerate moves.

		if (!isKing() && board.doubleCheck(color)) return; //If the king is double checked, then the king is the only piece that can move.

		long prevTime2 = System.currentTimeMillis();
		pinPiece = getPin();

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
	
	/**
	 * Adds all specified moves a pawn has to an ArrayList.
	 * @param moves ArrayList to be modified.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	private void pawnMoves(ArrayList<Move> moves, boolean attacksOnly) {
		//Checks for possiblity of EnPassant.
		if (board.getEnPassant() != EMPTY) {
			final int enPassantPos = board.getEnPassant();
			if (onRow(enPassantPos, pos) && Math.abs(enPassantPos - pos) == 1) {
				final int newPos = enPassantPos + getPawnDirection(color);
				addMove(moves, new Move(pos, newPos, true), attacksOnly);
			}
		}

		//Skips regenerating moves if a stored copy is available.
		if (!updatingCopy) {
			copyPawnMoves(moves, attacksOnly);
			return;
		}
		
		//Move pawns forward.
		final int direction = getPawnDirection(color);
		if (board.getPiece(pos + direction).isEmpty()) {
			addMove(moves, new Move(pos, pos + direction, false), attacksOnly);
			if (!hasPawnMoved(pos, color)) {
				if (board.getPiece(pos + direction * 2).isEmpty()) addMove(moves, new Move(pos, pos + direction * 2, false), attacksOnly);
			}
		}

		//Attacks.
		for (int i = 0; i < 2; i++) {
			if (getNumSquaresFromEdge(PAWN_ATTACK_DIRECTIONS[color][i], pos) < 1) continue;	// Checks if the pawn is at the edge of the board
			final int newPos = pos + PAWN_ATTACK_DIRECTIONS[color][i];
			if (board.getPiece(newPos).color == next(color)) addMove(moves, new Move(pos, newPos, false), attacksOnly);
		}
	}

	/**
	 * Uses the stored copy of pawn moves and adds them to an ArrayList.
	 * @param moves ArrayList to be modified.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	private void copyPawnMoves(ArrayList<Move> moves, boolean attacksOnly) {
		//Runs if the king is in check.
		if (board.isChecked(color)) {
			copyMovesInCheck(moves, attacksOnly);
			return;
		}

		//Runs if no piece pins the pawn to the king.
		if (pinPiece.isEmpty()) {
			copyMoves(moves, attacksOnly);
			return;
		}

		//Runs if the pawn is pinned.
		copyPawnMovesPinned(moves, attacksOnly);
	}

	/**
	 * Uses the stored copy of pawn moves and adds them to an ArrayList; run when pawn is pinned.
	 * @param moves ArrayList to be modified.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	private void copyPawnMovesPinned(ArrayList<Move> moves, boolean attacksOnly) {
		//Runs if the pinning piece piece is on the same row.
		if (onRow(pinPiece.pos, pos)) {
			//The pawn has no legal moves in this case.
			return;
		}

		//Runs if the pinning piece piece is on the same column.
		if (onColumn(pinPiece.pos, pos)) {
			if (attacksOnly) return;
			for (final Integer finish : movesCopy) {
				ChessGame.copyCount ++;
				if (onColumn(pos, finish)) moves.add(new Move(pos, finish));
			}
			return;
		}

		//Runs if the pinning piece is on the same diagonal.
		for (final Integer finish : movesCopy) {
			ChessGame.copyCount ++;
			//If a pawn is pinned on a diagonal, its only legal move would be to capture the piece.
			if (finish == pinPiece.pos) {
				moves.add(new Move(pos, finish));
				return;
			}
		}
	}
	
	/**
	 * Adds all specified moves a knight has to an ArrayList.
	 * @param moves ArrayList to be modified.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	private void knightMoves(ArrayList<Move> moves, boolean attacksOnly) {
		//If a knight is pinned it has no legal moves.
		if (!pinPiece.isEmpty()) return;

		//Skips regenerating moves if a stored copy is available.
		if (!updatingCopy) {
			//Runs if the king is in check
			if (board.isChecked(color)) {
				copyMovesInCheck(moves, attacksOnly);
				return;
			}

			copyMoves(moves, attacksOnly);
			return;
		}

		//Iterates over every L shape.
		for (int i = 0; i < KNIGHT_DIRECTIONS.length; i++) {
			final int newPos = pos + KNIGHT_DIRECTIONS[i];
			if (!onBoard(newPos) || !onL(pos, newPos)) continue;
			
			if (board.getPiece(newPos).color == color) continue;
			addMove(moves, new Move(pos, newPos, false), attacksOnly);
		}
	}
	
	/**
	 * Adds all specified moves the king has to an ArrayList.
	 * @param moves ArrayList to be modified.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	private void kingMoves(ArrayList<Move> moves, boolean attacksOnly) {
		//Check for castling king side/shorter side.
		if (board.canCastle(KINGSIDE, color)) {
			addMove(moves, new Move(pos, ROOK_POSITIONS[color][KINGSIDE] - 1, true), attacksOnly);
		}

		//Check for castling queen side/longer side.
		if (board.canCastle(QUEENSIDE, color)) {
			addMove(moves, new Move(pos, ROOK_POSITIONS[color][QUEENSIDE] + 2, true), attacksOnly);
		}

		//Skips regenerating moves if a stored copy is available.
		if (!updatingCopy) {
			copyMovesInCheck(moves, attacksOnly);
			return;
		}

		//Iterates over each square that's 1 away from the king.
		for (int i = 0; i < DIRECTIONS.length; i++) {
			if (getNumSquaresFromEdge(DIRECTIONS[i], pos) < 1) continue;
			final int newPos = pos + DIRECTIONS[i];
			if(board.getPiece(newPos).color == color) continue;

			addMove(moves, new Move(pos, newPos, false), attacksOnly);
		}
	}


	/**
	 * Adds all specified moves the bishop, rook, or queen has to an ArrayList.
	 * @param moves ArrayList to be modified.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	private void slidingMoves(ArrayList<Move> moves, boolean attacksOnly) {
		//Skips regenerating moves if a stored copy is available.
		if (!updatingCopy) {
			if (board.isChecked(color) || !pinPiece.isEmpty()) {
				copyMovesInCheck(moves, attacksOnly);
				return;
			}
			copyMoves(moves, attacksOnly);
			return;
		}

		final int startingIndex = isBishop() ? 4 : 0;	//If a bishop, ignore the first 4 directions which are straight.
		final int endIndex = isRook() ? 4 : 8;			//If a rook, ignore the last 4 directions which are diagonal.
		//Iterate over all sliding directions.
		for (int i = startingIndex; i < endIndex; i++) {
			//Iterate to the edge of the board.
			for (int j = 1; j < getNumSquaresFromEdge(DIRECTIONS[i], pos) + 1; j++) {
				final int newPos = pos + DIRECTIONS[i] * j;
				final ChessPiece piece = board.getPiece(newPos);

				if (piece.color == color) break;
				
				addMove(moves, new Move(pos, newPos, false), attacksOnly);

				if (!piece.isEmpty()) break;
			}
		}
	}

	/**
	 * Uses the stored copy of moves and adds them to an ArrayList.
	 * @param moves ArrayList to be modified.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	private void copyMoves(ArrayList<Move> moves, boolean attacksOnly) {
		for (final Integer finish : movesCopy) {
			ChessGame.copyCount ++;
			if (attacksOnly && board.getPiece(finish).isEmpty()) continue;
			moves.add(new Move(pos, finish));
		}
	}

	/**
	 * Uses the stored copy of moves and adds them to an ArrayList; run when the king is in check.
	 * @param moves ArrayList to be modified.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	private void copyMovesInCheck(ArrayList<Move> moves, boolean attacksOnly) {
		for (final Integer finish : movesCopy) {
			ChessGame.copyCount ++;
			addMove(moves, new Move(pos, finish), attacksOnly);
		}
	}

	/**
	 * Verifies the legality of a move and adds it to an ArrayList.
	 * @param moves ArrayList to be modified.
	 * @param move The potential move a piece can make.
	 * @param attacksOnly Whether or not captures only should be returned.
	 */
	private void addMove(ArrayList<Move> moves, Move move, boolean attacksOnly) {

		long prevTime = System.currentTimeMillis();

		if (updatingCopy && !move.SPECIAL) movesCopy.add(move.finish);		//Fill copy with moves to be reused later.

		if (attacksOnly && board.getPiece(move.finish).isEmpty() && !board.isEnPassant(move)) return;		//Checks for attacks only.

		//Adds the move if it is legal.
		if (!CHECKS || isLegalMove(move)) {

			ChessGame.timeValidMove += System.currentTimeMillis() - prevTime;
			prevTime = System.currentTimeMillis();

			moves.add(move);

			ChessGame.timeValidPart += System.currentTimeMillis() - prevTime;

			return;
		}
		ChessGame.timeValidMove += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Checks if a move is legal.
	 * @param move The potential move a piece can make.
	 * @return Whether or not the move is legal.
	 */
	private boolean isLegalMove(Move move) {
		final ChessPiece piece = board.getPiece(move.start);

		if (piece.isKing()) return isLegalKingMove(move);	//Seperate case for king moves.

		if (board.doubleCheck(piece.color)) return false;	//Only king can move in double check.

		//If the king is in check and the move does not stop the check, the move is illegal.
		if (board.isChecked(piece.color) && !stopsCheck(move)) return false;

		return !sacrificesKing(move);		//Checks if the move would sacrifice the king.
	}
	
	/**
	 * Checks if a king move is legal.
	 * @param move The potential move the king can make.
	 * @return Whether or not the move is legal.
	 */
	private boolean isLegalKingMove(Move move) {
		if (board.isAttacked(move.finish, color)) return false;		//If the square is attacked, the king cannot move there.

		if (!board.isChecked(color)) return true;		//If the king is not in check, any square that is not attacked is legal.

		//Iterates over each attacking the king.
		for (final ChessPiece attacker : board.getAttackers(this)) {
			if (move.finish == attacker.pos) return true;	//If the king captures the attacking piece, it's legal.

			//Checks if the king would still be in check on the same diagonal.
			if (attacker.isDiagonalAttacker()) {
				if (onSameDiagonal(move.start, move.finish, attacker.pos)) return false;
			}

			//Checks if the king would still be in check on the same line.
			if (attacker.isLineAttacker()) {
				if (onSameLine(move.start, move.finish, attacker.pos)) return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if a move protects the king from check.
	 * @param move The potential move a piece can make.
	 * @return Whether or not the move stops the check.
	 */
	private boolean stopsCheck(Move move) {
		final int king = board.getKingPos(color);
		final ChessPiece attacker = board.getKingAttacker();
		
		if (board.getEnPassant() == attacker.pos && board.isEnPassant(move)) return true;	//Pawn captures enPassant to remove attacker.

		if (attacker.isPawn() || attacker.isKnight()) return move.finish == attacker.pos;	//If the king is attacked by a knight or pawn, they must be captured.

		if (move.finish == attacker.pos) return true;	//If the attacking piece is captured, the king will no longer be in check.
		
		//If the king is checked by a bishop or queen on a diagonal, it must be blocked.
		if (onDiagonal(king, attacker.pos)) {
			return blocksDiagonal(attacker.pos, king, move.finish);
		}
		//If the king is checked by a rook or queen along a line, it must be blocked.
		return blocksLine(attacker.pos, king, move.finish);
	}
	
	/**
	 * Checks if a move leaves the king open to capture.
	 * @param move The potential move a piece can make.
	 * @return Whether or not the move sacrifices the king.
	 */
	private boolean sacrificesKing(Move move) {
		final int king = board.getKingPos(color);

		//Runs if the move is enPassant; if the pawn is pinned then the enPassant doesn't matter and use normal test case.
		if (pinPiece.isEmpty() && board.isEnPassant(move)) {
			final int enPassant = board.getEnPassant();
			//Check if the enPassant pawn potentially blocks an attack on the king.
			if ((onDiagonal(king, enPassant) || onLine(king, enPassant))) {
				final PieceSet attackers = board.getAttackers(enPassant, color);
				//Iterate over all pieces attacking the enPassant pawn.
				for (final ChessPiece piece : attackers) {
					if (piece.isPawn() || piece.isKnight() || piece.isKing()) continue;
	
					// Check if the enPassant pawn is on the path between the attacker and king.
					if (blocksDiagonal(piece.pos, king, enPassant)) {
						// If there is a clear path between enPassant pawn and king, the move is illegal.
						return board.clearPath(enPassant, king);
					}

					// Check if the enPassant pawn is on the path between the attacker and king.
					if (blocksLine(piece.pos, king, move.start)) {
						// If there is a clear path between enPassant pawn and king, the move is illegal.
						return board.clearPath(move.start, king);
					}
				}
			}
			//Check if a rook or queen blocks enPassant move.
			if (onLine(king, move.start)) {
				final PieceSet attackers = board.getAttackers(move.start, color);
				for (final ChessPiece piece : attackers) {
					if (piece.isPawn() || piece.isKnight() || piece.isBishop() || piece.isKing()) continue;

					// Check if the pawn is on the path between the attacker and king.
					if (blocksLine(piece.pos, king, move.start)) {
						// If there is a clear path between enPassant pawn and king, the move is illegal.
						return board.clearPath(enPassant, king);
					}
				}
			}
			return false;
		}

		//Normal run.
		if (pinPiece.isEmpty()) return false;	//If the piece isn't pinned it can't sacrifice the king.

		//If the piece is pinning the piece on a diagonal, the move is legal if the piece moves to the same diagonal.
		if (onDiagonal(pos, king)) return !onSameDiagonal(move.finish, king, pinPiece.pos);

		//If the piece is pinning the piece along a line, the move is legal if the piece moves to the same line.
		return !onSameLine(move.finish, king, pinPiece.pos);
	}

	/**
	 * Finds the chess piece that pins this piece to the king.
	 * @return ChessPiece pinning the piece to the king; returns empty if there is none.
	 */
	private ChessPiece getPin() {
		//Only check for a pin once when generating moves and a king can't be pinned.
		if (isKing() || pinPiece != null) return pinPiece;
		
		final int king = board.getKingPos(color);

		//If not aligned with the king or not attacked, then the piece can't be pinned.
		if (!(onDiagonal(king, pos) || onLine(king, pos)) || !board.isAttacked(this)) return empty();

		final PieceSet attackers = board.getAttackers(this);
		//Iterate over every attacking piece.
		for (final ChessPiece attacker : attackers) {
			if (attacker.isPawn() || attacker.isKnight() || attacker.isKing()) continue;	//Can't be pinned by these pieces.

			if (blocksDiagonal(attacker.pos, king, pos) || blocksLine(attacker.pos, king, pos)) {
				return board.clearPath(pos, king) ? attacker : empty();
			}
		}
		return empty();
	}

	/**
	 * Update the move copy list.
	 * @param remove Whether or not to remove or add the move.
	 * @param square The end spot of a new move.
	 */
	public void updateCopy(boolean remove, int square) {
		if (remove) {
			movesCopy.remove((Integer) square);
			return;
		}
		movesCopy.add(square);
	}

	/**
	 * Reset the move copy list, use when a piece moves or one of its moves becomes illegal.
	 */
	public void resetMoveCopy() {
		movesCopy = new ArrayList<Integer>(MAX_MOVES[type]);
	}

	/**
	 * Change the type of the piece.
	 * @param newPos The new position of the piece.
	 */
	public void setType(byte newType) {
		type = newType;
	}

	/**
	 * Gets the type of the piece.
	 * @return A byte from -1 to 5 representing the type of the piece.
	 */
	public byte getType() {
		return type;
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
		return type == EMPTY;
	}

	/**
	 * Checks whether or not a piece is a pawn.
	 * @return True if the piece is a pawn, false otherwise.
	 */
	public boolean isPawn() {
		return type == PAWN;
	}

	/**
	 * Checks whether or not a piece is a knight.
	 * @return True if the piece is a knight, false otherwise.
	 */
	public boolean isKnight() {
		return type == KNIGHT;
	}

	/**
	 * Checks whether or not a piece is a bishop.
	 * @return True if the piece is a bishop, false otherwise.
	 */
	public boolean isBishop() {
		return type == BISHOP;
	}

	/**
	 * Checks whether or not a piece is a rook.
	 * @return True if the piece is a rook, false otherwise.
	 */
	public boolean isRook() {
		return type == ROOK;
	}

	/**
	 * Checks whether or not a piece is a queen.
	 * @return True if the piece is a queen, false otherwise.
	 */
	public boolean isQueen() {
		return type == QUEEN;
	}

	/**
	 * Checks whether or not a piece is a king.
	 * @return True if the piece is a king, false otherwise.
	 */
	public boolean isKing() {
		return type == KING;
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
	public boolean equals(Object anObject) {
		if (this == anObject) return true;
		if (anObject instanceof ChessPiece) {
			final ChessPiece piece = (ChessPiece) anObject;
			return ((piece.type == type) && (piece.color == color) && (piece.pos == pos) && (piece.pieceID == pieceID));
		}
		return false;
	}
	
	/**
	 * Creates a new Empty Chess Piece.
	 * @return Empty square.
	 */
	public static ChessPiece empty() {
		return emptySquare;
	}
}
