package Chess;

import java.util.ArrayList;
import java.util.Arrays;

import Chess.Constants.DirectionConstants.Direction;
import Chess.Constants.PieceConstants.PieceColor;
import Chess.Constants.PieceConstants.PieceType;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PositionConstants.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.Constants.EvaluateConstants.*;
import static Chess.BoardUtil.*;

/**
 * Class representing a ChessBoard.
 */
public class ChessBoard {

	/**
	 * Class to store data that's lost when a move is made: halfmove, enPassant, and castling.
	 */
	public static class BoardStorage {
		public final int enPassant;
		public final int halfMove;
		private final boolean[] castling;

		/**
		 * Creates a new BoardStorage object.
		 * @param enPassant The position of the enPassant pawn.
		 * @param halfMove The halfmove count.
		 * @param castling The castling ability of both sides.
		 */
		public BoardStorage(int enPassant, int halfMove, boolean[] castling) {
			this.enPassant = enPassant;
			this.halfMove = halfMove;
			this.castling = new boolean[2];
			this.castling[QUEENSIDE] = castling[QUEENSIDE];
			this.castling[KINGSIDE] = castling[KINGSIDE];
		}

		/**
		 * Gets the castling ability of the color, copies it to not modify the original.
		 * @return A boolean array storing the ability to castle.
		 */
		public boolean[] getCastling() {
			final boolean[] castle = new boolean[2];
			castle[0] = castling[0];
			castle[1] = castling[1];
			return castle;
		}
	}

	/** 2d PieceSet array storing all pieces attacking a square, 0 refers to BLACK attackers, 1 for WHITE ATTACKERS.*/
	private final PieceSet[][] attacks;		
	
	/** int array storing the position of the kings, 0 refers to BLACK, 1 for WHITE.*/
	private final int[] kingPos;
	
	/** 2d int array storing a count of all types pieces, 0 refers to BLACK, 1 for WHITE.*/
	private final int[][] pieceCount;

	/** PieceSet array storing all pieces, 0 refers to BLACK, 1 for WHITE.*/
	private final PieceSet[] pieces;
	
	/** ChessPiece array representing the board.*/
	private final ChessPiece[] board;

	/** 2d boolean array storing castling ability of both sides, 0 refers to BLACK, 1 for WHITE; 0 refers to Queenside, 1 to Kingside*/
	private final boolean[][] castling;

	/** Variable used to store the piece that is currently attacking the king. */
	private ChessPiece kingAttacker = ChessPiece.empty();

	private PieceColor turn;
	private int promotingPawn;
	private int halfMove;
	private int fullMove;

	private final ZobristHashing hashing;

	public final Bitboard bitboard;


	
	/**
	 * Creates a new Chessboard object with the default starting position.
	 */
	public ChessBoard () {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}

	/**
	 * Creates a new Chessboard object with a specified starting position.
	 * @param fen String that specifies the starting position using FEN {@link https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation}
	 */
	public ChessBoard (String fen) {
		turn = PieceColor.BLACK;
		halfMove = 0;
		fullMove = 1;
		
		attacks = new PieceSet[2][];
		attacks[PieceColor.BLACK.arrayIndex] = new PieceSet[64];
		attacks[PieceColor.WHITE.arrayIndex] = new PieceSet[64];

		pieces = new PieceSet[2];
		pieces[PieceColor.BLACK.arrayIndex] = new PieceSet();
		pieces[PieceColor.WHITE.arrayIndex] = new PieceSet();
		
		kingPos = new int[2];
		pieceCount = new int[2][5];
		pieceCount[PieceColor.BLACK.arrayIndex] = new int[5];
		pieceCount[PieceColor.WHITE.arrayIndex] = new int[5];
		Arrays.fill(pieceCount[0], 0);
		Arrays.fill(pieceCount[1], 0);
		board = new ChessPiece[64];
		castling = new boolean[2][2]; //Black: Queenside, Kingside, White: Queenside, Kingside
		Arrays.fill(castling[PieceColor.BLACK.arrayIndex], false);
		Arrays.fill(castling[PieceColor.WHITE.arrayIndex], false);
		promotingPawn = EMPTY;

		bitboard = new Bitboard(fen);
		fen_to_board(fen);
		hardAttackUpdate();
		
		hashing = new ZobristHashing(this);
	}
	
	/**
	 * Convert the current board position to a FEN String.
	 * @return A FEN String representing the board position.
	 */
	public String getFenString() {
		String fen = "";
		//Handle each pieces position and the empty spaces.
		for (int row = 0; row < 8; row++) {
			int emptySpaces = 0;		//Number of empty spaces in between pieces.
			for (int column = 0; column < 8; column++) {
				final int pos = row * 8 + column;		//Convert row, column to position.

				if (board[pos].isEmpty()) {
					emptySpaces++;
					continue;
				}

				else if (emptySpaces > 0) {
					fen += emptySpaces;
					emptySpaces = 0;
				}
				
				fen += pieceToChar(board[pos]);
			}

			if (emptySpaces > 0) fen += emptySpaces;

			if (row != 7) fen += "/";
		}
		
		fen = (turn == PieceColor.WHITE) ? fen + " w" : fen + " b";	//Store whose turn it is.

		boolean whiteCanCastle = true;

		//If white can't castle at all, add a "-".
		if (!castling[PieceColor.WHITE.arrayIndex][QUEENSIDE] && !castling[PieceColor.WHITE.arrayIndex][KINGSIDE]) {
			fen += " -";
			whiteCanCastle = false;
		}

		//Add capital letters based on white's castling ability.
		if (castling[PieceColor.WHITE.arrayIndex][KINGSIDE]) fen += " K";
		if (castling[PieceColor.WHITE.arrayIndex][QUEENSIDE]) fen += "Q";
		
		//If Black can't castle at all but white can, add a "-".
		if (!castling[PieceColor.BLACK.arrayIndex][QUEENSIDE] && !castling[PieceColor.BLACK.arrayIndex][KINGSIDE] && whiteCanCastle) fen += " -";

		//Add lowercase letter based on black's castling ability.
		if (castling[PieceColor.BLACK.arrayIndex][KINGSIDE]) fen += "k";
		if (castling[PieceColor.BLACK.arrayIndex][QUEENSIDE]) fen += "q";
		
		//Handle enPassant
		if (bitboard.getEnPassant() == EMPTY) fen += " -";
		else {
			final int passant = bitboard.getEnPassant() + getPawnDirection(turn).rawArrayValue;
			fen += " " + indexToSquare(getColumn(passant), 8 - getRow(passant));
		}

		//Add halfMove and fullMove
		fen += " " + halfMove;
		fen += " " + fullMove;
		
		return fen;
	}

	/**
	 * Sets the board position using a FEN String.
	 * @param fen The FEN String for the board position to be based on.
	 */
	private void fen_to_board(String fen) {
		final int[] pieceIDs = new int[] {0, 0};	//Piece IDs to be used for discount Hash map.
		int pos = 0;
		boolean reachedHalfMove = false;
		//Iterate over each character in the FEN String.
		for (int index = 0; index < fen.length(); index++) {
			final char letter = fen.charAt(index);
			if (letter == ' ') continue;
			
			//Turn, Castling, enPassant.
			if (pos > 63) {
				
				//Turn.
				if (letter == 'w') {
					turn = PieceColor.WHITE;
				}
				
				//Castling.
				else if (letter == 'K') {
					castling[PieceColor.WHITE.arrayIndex][KINGSIDE] = true;
				}
				else if (letter == 'Q') {
					castling[PieceColor.WHITE.arrayIndex][QUEENSIDE] = true;
				}
				else if (letter == 'k') {
					castling[PieceColor.BLACK.arrayIndex][KINGSIDE] = true;
				}
				else if (letter == 'q') {
					castling[PieceColor.BLACK.arrayIndex][QUEENSIDE] = true;
				}
				
				//enPassant; it's garbage but I can't be bothered to clean it up.
				else if ((int) letter <= 104 && (int) letter >= 97 && Character.isDigit(fen.charAt(index + 1))) {
					int enPassant = squareToIndex(Character.toString(letter) + fen.charAt(index + 1));
					enPassant = (turn == PieceColor.BLACK) ? enPassant - 8 : enPassant + 8;
					bitboard.setEnPassant(enPassant);
					index++;
				}

				//Handle move counts, also garbage.
				else if (Character.isDigit(letter)) {
					int number = Character.getNumericValue(letter);
					int digit = index + 1;
					while (digit < fen.length()) {
						if (!Character.isDigit(fen.charAt(digit))) break;
						number = number * 10 + Character.getNumericValue(fen.charAt(digit));
						digit ++;
					}
					if (reachedHalfMove) fullMove = number;
					else {
						halfMove = number;
						reachedHalfMove = true;
					}
				}
				
				continue;
			}
			
			if (letter == '/') continue;

			//Filling board with pieces.
			final int pieceValue = Character.getNumericValue(letter);
			//Empty squares.
			if (pieceValue <= 8 && pieceValue > 0) {
				for (int square = 0; square < pieceValue; square++) {
					board[pos] = ChessPiece.empty();
					pos ++;
				}
				continue;
			}

			//Create and store the piece.
			final ChessPiece piece = charToPiece(letter, pos, this, pieceIDs);
			board[pos] = piece;
			pieces[piece.color.arrayIndex].add(piece);
			
			//Keep track of the piece.
			if (piece.isKing()) kingPos[piece.color.arrayIndex] = pos;
			else pieceCount[piece.color.arrayIndex][piece.getType().arrayIndex] ++;

			pos++;
		}
	}
	
	/**
	 * Make a move on the board.
	 * @param move The move to be made.
	 */
	public void makeMove(Move move) {
		long prevTime = System.currentTimeMillis();

		final ChessPiece movingPiece = board[move.getStart()];

		final boolean isAttack = !board[move.getFinish()].isEmpty();
		kingAttacker = ChessPiece.empty();
		int castledRookPos = EMPTY;
		movingPiece.pieceAttacks(true);		//Update the squares the moving piece currently attacks.

		//Handle the captured piece.
		if (isAttack) {
			halfMove = EMPTY;
			board[move.getFinish()].pieceAttacks(true);		//Update the squares the capture piece used to attack.
			updatePosition(board[move.getFinish()], move.getFinish(), true);	//Remove the captured piece from the board.
		}

		int newEnPassant = EMPTY;
		//Handles pawn moves.
		if (movingPiece.isPawn()) {
			newEnPassant = handlePawnSpecialBehavior(move);
		}
		
		//Removes castling rights when a rook moves.
		if (movingPiece.isRook()) {
			updateCastlingOnRookMove(move.getStart(), movingPiece.color);
		}
		
		//Handles king moves.
		if (movingPiece.isKing()) {
			Arrays.fill(castling[movingPiece.color.arrayIndex], false);		//King can no longer castle.
			hashing.setCastlingRights(turn, new boolean[] {false, false});
			//Handle castling.
			if (move.isSpecial()) {
				castledRookPos = makeCastleMove(move);
			}
		}

		hashing.flipPiece(move.getStart(), movingPiece);
		board[move.getStart()] = ChessPiece.empty();	//Empty the square the moving piece used to occupy.
		
		updatePosition(movingPiece, move.getFinish(), false);		//Move the moving piece to the new position.
		resetPieces(move, isAttack, false);		//Reset the move copies of pieces affected by this new position.

		pawnReset(move, isAttack);

		if (promotingPawn == EMPTY) movingPiece.pieceAttacks(false);		//Update the squares the moving piece attacks in its new position.
		if (castledRookPos != EMPTY) board[castledRookPos].pieceAttacks(false);	//Update the squares the castled rook attacks in its new position.

		bitboard.setEnPassant(newEnPassant);
		hashing.setEnPassantFile(newEnPassant != EMPTY ? getColumn(newEnPassant) : newEnPassant);

		//Move on to the next turn if a promotion isn't happenning.
		if (!is_promote()) {
			halfMove ++;
			if (turn == PieceColor.BLACK) fullMove ++;
			next_turn();
		}
		
		Tests.timeMakeMove += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Helper function that handles necessary behavior when a pawn moves: promoting and enPassant.
	 * @param move A move made by a pawn.
	 * @return Integer representing enPassant position.
	 */
	private int handlePawnSpecialBehavior(Move move) {
		halfMove = EMPTY;
		//Captures enPassant.
		if (move.isSpecial()) {
			board[bitboard.getEnPassant()].pieceAttacks(true);		//Update the squares the enPassant pawn used to attack.
			updatePosition(board[bitboard.getEnPassant()], bitboard.getEnPassant(), true);	//Remove the enPassant pawn from the board.
		}
		//Pawn moves two squares forward.
		if (getRowDistance(move.getStart(), move.getFinish()) == 2) {
			return move.getFinish();
		}
		//Pawn is promoting.
		if (getRow(move.getFinish()) == PROMOTION_ROW[turn.arrayIndex]) {
			promotingPawn = move.getFinish();
		}
		return EMPTY;
	}

	/**
	 * Helper function that updatesCastling when a rook moves.
	 * @param startingPos Position where the rook moved from.
	 * @param color The color of the rook that moved.
	 */
	private void updateCastlingOnRookMove(int startingPos, PieceColor color) {
		//Queenside
		if (startingPos == ROOK_POSITIONS[color.arrayIndex][QUEENSIDE]) {
			castling[color.arrayIndex][QUEENSIDE] = false;
			hashing.setCastlingRights(color, QUEENSIDE, false);
		}
		//Kingside
		if (startingPos == ROOK_POSITIONS[color.arrayIndex][KINGSIDE]) {
			castling[color.arrayIndex][KINGSIDE] = false;
			hashing.setCastlingRights(color, KINGSIDE, false);
		}
	}

	/**
	 * Helper function that handles making a castle move.
	 * @param move The move being made by the king.
	 * @return Position of the castled rook.
	 */
	private int makeCastleMove(Move move) {
		final int side = move.getFinish() > move.getStart() ? KINGSIDE : QUEENSIDE;
		final int currentRookPos = ROOK_POSITIONS[turn.arrayIndex][side];
		board[currentRookPos].pieceAttacks(true);
		final int newRookPos = move.getFinish() + (side == KINGSIDE ? Direction.LEFT : Direction.RIGHT).rawArrayValue; 
		updatePosition(board[currentRookPos], newRookPos, false);

		board[currentRookPos] = ChessPiece.empty();
		hashing.flipPiece(currentRookPos, board[newRookPos]);

		return newRookPos;
	}

	/**
	 * Undo a move on the board.
	 * @param move The original move that was made, uninverted.
	 * @param capturedPiece The piece that was captured, empty if no piece was captured.
	 * @param store Data that's lost when a move is made: halfmove, enPassant, and castling.
	 */
	public void undoMove(Move move, ChessPiece capturedPiece, BoardStorage store) {
		long prevTime = System.currentTimeMillis();

		//Back up a turn if a promotion isn't happenning.
		if (!is_promote()) {
			halfMove = store.halfMove;
			if (turn == PieceColor.WHITE) fullMove --;
			next_turn();
		}

		//Reset castling data and enPassant data.
		castling[turn.arrayIndex] = store.getCastling();
		bitboard.setEnPassant(store.enPassant);

		hashing.setCastlingRights(turn, castling[turn.arrayIndex]);
		hashing.setEnPassantFile(bitboard.getEnPassant() != EMPTY ? getColumn(bitboard.getEnPassant()) : bitboard.getEnPassant());

		kingAttacker = ChessPiece.empty();
		final Move invertedMove = move.invert();
		final boolean isAttack = !capturedPiece.isEmpty() && !move.isSpecial();

		final ChessPiece movingPiece = board[invertedMove.getStart()];
		int castledRookPos = EMPTY;
		movingPiece.pieceAttacks(true);		//Update the squares the moving piece currently attacks.

		//Check for castling
		if (isCastle(invertedMove)) {
			castledRookPos = undoCastleMove(invertedMove);
		}

		board[invertedMove.getStart()] = ChessPiece.empty();		//Empty the square the piece used to occupy.
		hashing.flipPiece(invertedMove.getStart(), movingPiece);
		updatePosition(movingPiece, invertedMove.getFinish(), false);			//Move the moving piece to the new position.

		//Add the captured piece back onto the board.
		// if (!capturedPiece.isEmpty() && !move.isSpecial()) updatePosition(capturedPiece, capturedPiece.getPos(), false);

		resetPieces(invertedMove, isAttack, true);

		//Finnicky thing happens when you undo enPassant before calling softAttack on pieces.
		if (!capturedPiece.isEmpty()) updatePosition(capturedPiece, capturedPiece.getPos(), false);

		pawnReset(move, isAttack);
		
		movingPiece.pieceAttacks(false);		//Update the squares the moving piece attacks in its new position.

		if (!capturedPiece.isEmpty()) capturedPiece.pieceAttacks(false);		//Update the squares the captured piece now attacks.
		
		if (castledRookPos != EMPTY) board[castledRookPos].pieceAttacks(false);			//Update the squares the castled rook now attacks.
		
		promotingPawn = EMPTY;

		Tests.timeUndoMove += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Helper function that handles undoing a castle move.
	 * @param invertedMove The move being made by the king.
	 * @return Position of the castled rook.
	 */
	private int undoCastleMove(Move invertedMove) {
		final int side = invertedMove.getStart() > invertedMove.getFinish() ? KINGSIDE : QUEENSIDE;
		final int castledRookPos = invertedMove.getStart() + (side == KINGSIDE ? Direction.LEFT : Direction.RIGHT).rawArrayValue;
		final ChessPiece castledRook = board[castledRookPos];
		castledRook.pieceAttacks(true);		//Update the squares the rook currently attacks.
		updatePosition(castledRook, ROOK_POSITIONS[turn.arrayIndex][side], false);		//Move the rook to the new position.
		board[castledRookPos] = ChessPiece.empty();			//Empty the square the rook used to occupy.
		hashing.flipPiece(castledRookPos, castledRook);
		return ROOK_POSITIONS[turn.arrayIndex][side];
	}

	/**
	 * Updates the board and tracking variables with a piece removal or movement.
	 * @param piece The piece either being removed or added to the board.
	 * @param pos The position of the piece being added or removed from.
	 * @param remove Whether or not the piece is getting removed or added.
	 */
	private void updatePosition(ChessPiece piece, int pos, boolean remove) {
		hashing.flipPiece(pos, piece);

		//Remove the piece from the board and updates the tracking variables.
		if (remove) {
			pieces[piece.color.arrayIndex].remove(piece);
			pieceCount[piece.color.arrayIndex][piece.getType().arrayIndex] -= 1;
			board[pos] = ChessPiece.empty();
			return;
		}

		//Add the piece to the board and update the tracking variables.
		board[pos] = piece;
		piece.setPos(pos);
		if (pieces[piece.color.arrayIndex].add(piece)) pieceCount[piece.color.arrayIndex][piece.getType().arrayIndex] += 1;
		
		//Update the king position variable if the king moves.
		if (piece.isKing()) kingPos[piece.color.arrayIndex] = pos;
	}

	/**
	 * Promotes the pawn.
	 * @param type The new type of the promoted piece.
	 */
	public void promote(PieceType type) {
		final ChessPiece promotingPiece = board[promotingPawn];
		
		//Add the promoted piece to the board.
		hashing.flipPiece(promotingPawn, promotingPiece);
		promotingPiece.setType(type);
		updatePosition(promotingPiece, promotingPawn, false);

		//Update tracking variables.
		pieceCount[turn.arrayIndex][type.arrayIndex] += 1;
		pieceCount[turn.arrayIndex][PieceType.PAWN.arrayIndex] -= 1;

		promotingPiece.pieceAttacks(false);	//Update the squares the promoted piece now attacks.

		//Next turn.
		halfMove ++;
		if (turn == PieceColor.BLACK) fullMove ++;
		next_turn();

		promotingPawn = EMPTY;

	}

	/**
	 * Unpromotes a piece.
	 * @param pos The position of the promoted piece.
	 */
	public void unPromote(int pos) {
		final ChessPiece unpromotingPiece = board[pos];
		hashing.flipPiece(pos, unpromotingPiece);

		//Backup a turn.
		halfMove --;
		if (turn == PieceColor.WHITE) fullMove --;
		next_turn();

		//Update tracking variables.
		pieceCount[turn.arrayIndex][unpromotingPiece.getType().arrayIndex] --;
		pieceCount[turn.arrayIndex][PieceType.PAWN.arrayIndex] ++;

		//Update the squares the unpromoting piece used to attack.
		unpromotingPiece.pieceAttacks(true);

		//Reset the piece to a pawn.
		unpromotingPiece.setType(PieceType.PAWN);
		updatePosition(board[pos], pos, false);

		promotingPawn = pos;
	}

	/**
	 * Reset the moves copy and updates squares attacked by pieces affected by a move.
	 * @param move The Move being made.
	 * @param isAttack	Whether or not the move is a capture.
	 * @param undoMove	Whether or not the move is being undone.
	 */
	private void resetPieces(Move move, boolean isAttack, boolean undoMove) {
		final long prevTime = System.currentTimeMillis();

		final boolean isCastle = isCastle(move);

		final int[] modifiedSquares = isEnPassant(move) ? new int[] {move.getStart(), move.getFinish(), bitboard.getEnPassant()} : new int[] {move.getStart(), move.getFinish()};
		//Check each square that the move affects.
		for (final PieceColor color : PIECE_COLORS) {
			//Go through black and white pieces potentially affected.
			final PieceSet softAttackPieces = new PieceSet();
			for (int movePart = 0; movePart < modifiedSquares.length; movePart++) {
				final int pos = modifiedSquares[movePart];
				//Check each piece attacking the square.
				final PieceSet attacks = getAttackers(pos, color);
				for (final ChessPiece piece : attacks) {
					long prevTime2 = System.currentTimeMillis();

					//Update straight line and diagonal attackers.
					if (!softAttackPieces.contains(piece)) {
						final boolean updated = piece.softAttack(pos, movePart, move, isAttack, undoMove);
						if (updated) {
							softAttackPieces.add(piece);
							continue;
						}
					}
					else continue;

					Tests.timeSoftAttack += System.currentTimeMillis() - prevTime2;

					//Reset the moves copy in pieces affected by the move.
					if (!isCastle) piece.pieceReset(pos, movePart, isAttack, undoMove);

				}
			}
		}

		if (isCastle) {
			//Can't be bothered to handle castling because it happens at max twice in a game.
			//Nope u have to fix this.
			for (int color = 0; color < 2; color ++) {
				final PieceSet coloredPieces = pieces[color];
				for (final ChessPiece piece : coloredPieces) {
					piece.resetMoveCopy();
					piece.pieceMoves(new ArrayList<Move>(MAX_MOVES[piece.getType().arrayIndex]));
				}
			}
		}

		Tests.timePieceUpdate += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Removes or adds straight line moves to a pawn's move copy based on a move made.
	 * @param move The move being made, make sure it is uninverted when undoing a move.
	 */
	private void pawnReset(Move move, boolean isAttack) {
		final int[] squares;
		if (isAttack) {
			squares = isEnPassant(move) ? new int[] {move.getStart(), bitboard.getEnPassant()} : new int[] {move.getStart()};
		}
		else {
			squares = isEnPassant(move) ? new int[] {move.getStart(), move.getFinish(), bitboard.getEnPassant()} : new int[] {move.getStart(), move.getFinish()};
		}

		//Update both black and white pawns.
		for (final PieceColor color : PIECE_COLORS) {
			final Direction pawnDirection = getPawnDirection(color);

			//Check every square involved in the move made.
			for (final int pos : squares) {
				final boolean isEmpty = board[pos].isEmpty();		//Checks if the square is empty, if so the pawn can make the move forward.
				
				//Go one square ahead of the involved square.
				final int oneSquareAhead = pos - pawnDirection.rawArrayValue;
				if (!onBoard(oneSquareAhead) || move.contains(oneSquareAhead)) continue;
				final ChessPiece pieceOneSquareAhead = board[oneSquareAhead];

				//Check if it's a pawn that would be influneced by the move.
				if (pieceOneSquareAhead.isPawn() && pieceOneSquareAhead.color == color) {
					pieceOneSquareAhead.updateCopy(!isEmpty, pos);
					//Check for double move forward.
					if (getRow(oneSquareAhead) == PAWN_STARTING_ROW[color.arrayIndex]) {
						//Check the square behind the involved square.
						final int oneSquareBehind = pos + pawnDirection.rawArrayValue;
						if (board[oneSquareBehind].isEmpty() && !move.contains(oneSquareBehind)) pieceOneSquareAhead.updateCopy(!isEmpty, oneSquareBehind);
					}
					continue;
				}
				else if (!pieceOneSquareAhead.isEmpty()) continue;

				//Go two squares ahead if the first is empty.
				final int twoSquaresAhead = oneSquareAhead - pawnDirection.rawArrayValue;
				if (!onBoard(twoSquaresAhead) || move.contains(twoSquaresAhead)) continue;
				final ChessPiece pieceTwoSquaresAhead = board[twoSquaresAhead];

				//Check if it's a pawn that would be influenced by the move.
				if (pieceTwoSquaresAhead.isPawn() && pieceTwoSquaresAhead.color == color && getRow(twoSquaresAhead) == PAWN_STARTING_ROW[color.arrayIndex]) {
					pieceTwoSquaresAhead.updateCopy(!isEmpty, pos);
				}
			}
		}
	}

	/**
	 * Initialize and store attacks each piece has, use when first loading a position.
	 */
	private void hardAttackUpdate() {
		//Iterate over each color.
		for (int color = 0; color < 2; color ++) {
			//Reset the attacks currently stored.
			for (int square = 0;  square < attacks[color].length; square++) {
				attacks[color][square] = new PieceSet();
			}

			//Generate attacks for each of the pieces.
			for (final ChessPiece piece : pieces[color]) {
				piece.pieceAttacks(false);
			}
		}
	}

	/**
	 * Check if the game is over.
	 * @return The current state of the game, CONTINUE: 2, WIN: 1, DRAW: 0.
	 */
	public int isWinner() {
		//Half move timer, look it up.
		if (halfMove >= HALF_MOVE_TIMER) return DRAW;

		//If neither side has enough pieces to secure checkmate, game ends in a draw.
		if (hasInsufficientMaterial()) return DRAW;

		//Check to see if any piece has a legal move.
		for(final ChessPiece piece : pieces[turn.arrayIndex]) {
			final ArrayList<Move> moves = new ArrayList<Move>();
			piece.pieceMoves(moves);
			if(moves.size() > 0) {
				return CONTINUE;
			}
		}
		//If the king is in check, it's checkmate, if not draw. 
		return isChecked(turn) ? WIN : DRAW;
	}
	
	/**
	 * Check if the game is over by insufficient material resulting in a draw.
	 * @return Whether or not the game is a draw.
	 */
	private boolean hasInsufficientMaterial() {
		//Check each side to see if there's enough pieces.
		for (int color = 0; color < 2; color++) {
			if (pieceCount[color][PieceType.PAWN.arrayIndex] > 0 || pieceCount[color][PieceType.KNIGHT.arrayIndex] > 2 
				|| pieceCount[color][PieceType.BISHOP.arrayIndex] > 1 || pieceCount[color][PieceType.ROOK.arrayIndex] > 0 
				|| pieceCount[color][PieceType.QUEEN.arrayIndex] > 0) return false;
		}
		return true;
	}

	/**
	 * Modify the attack storage by adding or removing an attacker of a square.
	 * @param piece The piece being added or removed as an attacker.
	 * @param pos The square being attacked.
	 * @param remove Whether or not to remove or add an attacker. 
	 */
	public void modifyAttacks(ChessPiece piece, int pos, boolean remove) {
		if (remove) {
			removeAttacker(piece, pos);
			return;
		}
		addAttacker(piece, pos);
	}

	/**
	 * Add an attacker to the attack storage of a square.
	 * @param piece The piece being added as an attacker.
	 * @param pos The square being attacked.
	 */
	public boolean addAttacker(ChessPiece piece, int pos) {
		return attacks[piece.color.arrayIndex][pos].add(piece);
	}

	/**
	 * Remove an attacker from the attack storage of a square.
	 * @param piece The piece being removed as an attacker.
	 * @param pos The square being attacked.
	 */
	public boolean removeAttacker(ChessPiece piece, int pos) {
		return attacks[piece.color.arrayIndex][pos].remove(piece);
	}

	/**
	 * Moves on to the next turn.
	 */
	public void next_turn() {
		turn = flipColor(turn);
		hashing.toggleSideToMove();
	}

	/**
	 * Returns whether or not a king can castle in the future.
	 * @param color The color of the king.
	 * @return True if the king can castle in the future, false if it can't.
	 */
	public boolean[] getCastlingPotential(PieceColor color) {
		return castling[color.arrayIndex];
	}

	/**
	 * Returns the king's position on the board.
	 * @param color The color of the king.
	 * @return The king's position from 0 to 63.
	 */
	public int getKingPos(PieceColor color) {
		return kingPos[color.arrayIndex];
	}

	/**
	 * Checks if a move castles the king.
	 * @param move The move being played.
	 * @return True if the move is a castle, false if it isn't.
	 */
	public boolean isCastle(Move move) {
		return move.isSpecial() && (move.contains(getKingPos(turn)));
	}

	/**
	 * Checks if a move is a capture through enPassant.
	 * @param move The move being played.
	 * @return True if the move is a capture through enPassant, false if it isn't.
	 */
	public boolean isEnPassant(Move move) {
		return move.isSpecial() && (board[move.getStart()].isPawn() || board[move.getFinish()].isPawn());
	}
	
	/**
	 * Checks if a king is in double check; two pieces attacking.
	 * @param color The color of the king being attacked.
	 * @return True if the king is in double check, false if it isn't.
	 */
	public boolean doubleCheck(PieceColor color) {
		return numAttacks(kingPos[color.arrayIndex], color) >= 2;
	}
	
	/**
	 * Checks if a king is in check; at least one piece attacking.
	 * @param color The color of the king being attacked.
	 * @return True if the king is in check, false if it isn't.
	 */
	public boolean isChecked(PieceColor color) {
		return isAttacked(board[kingPos[color.arrayIndex]]);
	}
	
	/**
	 * Checks if a square is attacked; at least one piece attacking.
	 * @param pos The position of the square being attacked.
	 * @param color The color of pieces that are attacking the square.
	 * @return True if the square is attacked, false if it isn't.
	 */
	public boolean isAttacked(int pos, PieceColor color) {
		return numAttacks(pos,color) >= 1;
	}

	/**
	 * Checks if a piece is attacked; at least one piece attacking.
	 * @param piece The ChessPiece that is being attacked.
	 * @return True if the piece is attacked, false if it isn't.
	 */
	public boolean isAttacked(ChessPiece piece) {
		return numAttacks(piece.getPos(), piece.color) >= 1;
	}

	/**
	 * Returns the number of pieces attacking a specific square.
	 * @param pos The position of the square.
	 * @param color The color of the square (If white is inputted, the method returns the number of black attackers).
	 * @return The number of attackers.
	 */
	private int numAttacks(int pos, PieceColor color) {
		return getAttackers(pos, color).size();
	}

	/**
	 * Returns the pieces attacking another piece.
	 * @param piece The ChessPiece being attacked.
	 * @return The Chess pieces attacking.
	 */
	public PieceSet getAttackers(ChessPiece piece) {
		return getAttackers(piece.getPos(), piece.color);
	}

	/**
	 * Returns the pieces attacking a specific square.
	 * @param pos The position of the square.
	 * @param color The color of the square (If white is inputted, the method returns the number of black attackers).
	 * @return The Chess pieces attacking.
	 */
	public PieceSet getAttackers(int pos, PieceColor color)  {
		return attacks[flipColor(color).arrayIndex][pos];
	}

	/**
	 * Checks if a promotion is ongoing.
	 * @return True if a promotion is happening, false if it isn't.
	 */
	public boolean is_promote() {
		return promotingPawn != EMPTY;
	}

	/**
	 * Returns the square the enPassant pawn occupies.
	 * @return The enPassant pawn, returns EMPTY:-1 if there is none.
	 */
	public int getEnPassant() {
		return bitboard.getEnPassant();
	}

	/**
	 * Returns whose turn it is on the board.
	 * @return BLACK: 0, WHITE: 1.
	 */
	public PieceColor getTurn() {
		return turn;
	}

	/**
	 * Returns a ChessPiece on the board.
	 * @param pos A position on the board.
	 * @return The ChessPiece, includes empty squares.
	 */
	public ChessPiece getPiece(int pos) {
		return board[pos];
	}

	/**
	 * Returns all pieces of a specific color.
	 * @param color The color of the pieces.
	 * @return A piece set object, use enhanced for loop.
	 */
	public PieceSet getPieces(PieceColor color) {
		return pieces[color.arrayIndex];
	}

	/**
	 * Returns a copy of data that's lost when a move is made.
	 * @return A BoardStorage object containing enPassant, halfMove, and castling potential.
	 */
	public BoardStorage copyData() {
		return new BoardStorage(getEnPassant(), halfMove, getCastlingPotential(getTurn()));
	}

	/**
	 * Returns a computer that can analyze the board.
	 * @return A computer object that can evaluate the best move or find the max amount of possible moves.
	 */
	public Computer getComputer() {
		return new Computer(this);
	}

	/**
	 * Returns the piece that is currently attacking the king.
	 * @return The ChessPiece attacking the king.
	 */
	public ChessPiece getKingAttacker() {
		if (kingAttacker.isEmpty()) {
			for (final ChessPiece piece : getAttackers(board[kingPos[turn.arrayIndex]])) kingAttacker = piece;
		}
		return kingAttacker;
	}

	/**
	 * Returns whether a clear path exists between two points meaning all empty spaces in between.
	 * @param pos1 The first position.
	 * @param pos2 The second position.
	 * @return True if a clear path exists, false if it doesn't.
	 */
	public boolean clearPath(int pos1, int pos2) {
		final Direction direction = getDirection(pos1, pos2);
		if (direction == null) {
			return false;
		}

		int pos = pos1 + direction.rawArrayValue;
		while (pos != pos2) {
			if (!getPiece(pos).isEmpty()) return false;	//Piece blocks the path.
			pos += direction.rawArrayValue;
		}
		return true;
	}

	/**
	 * Returns whether or not the king can castle queenside or kingside.
	 * @param side Whether or not to check for kingside or queenside.
	 * @param color The color of the king.
	 * @return True if the king can castle, false if it can't.
	 */
	public boolean canCastle(int side, PieceColor color) {
		//If the king is checked, it can't castle.
		if (isChecked(color)) return false;

		//If the king or associated rook have already moved, it can't castle.
		if (!castling[turn.arrayIndex][side]) return false;

		//If the piece on the side is not a rook, it can't castle.
		final int rookPos = ROOK_POSITIONS[color.arrayIndex][side];
		if (!getPiece(rookPos).isRook()) return false;
		
		//There must be a clear path between the king and rook.
		if (!clearPath(getKingPos(color), rookPos)) return false;

		//Each square must not be attacked, except on the queenside with square next to the rook.
		for (int distance = 1; distance < 3; distance++) {
			if (isAttacked(side == KINGSIDE ? getKingPos(color) + distance : getKingPos(color) - distance, color)) return false;
		}

		return true;
	}
	
	/**
	 * Debugging tool to display the attacks of each piece, checks for negative attacks.
	 */
	public void displayAttacks() {
		boolean valid = true;
		for (int color = 0; color < 2; color++) {
			final var colorAttacks = attacks[color];
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					int attacks = colorAttacks[i * 8 + j].size();
					if (attacks < 0) valid = false;
					System.out.print(attacks + " ");
				}
				System.out.println();
			}
			System.out.println();
		}
		System.out.println(isChecked(turn));
		System.out.println(getFenString());
		if (!valid) System.out.println("ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof Move) {
			final ChessBoard aBoard = (ChessBoard) o;
			return aBoard.hashing.getHash() == hashing.getHash();
		}
		return false;
	}

	public long hash() {
		return hashing.getHash();
	}
}