package Chess;

import java.util.ArrayList;

import Chess.Constants.DirectionConstants.Direction;
import Chess.Constants.PieceConstants.PieceColor;
import Chess.Constants.PieceConstants.PieceType;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.PIECE_COLORS;
import static Chess.Constants.PositionConstants.*;
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
		public final boolean isChecked;
		private final boolean[] castling;

		/**
		 * Creates a new BoardStorage object.
		 * @param enPassant The position of the enPassant pawn.
		 * @param halfMove The halfmove count.
		 * @param castling The castling ability of both sides.
		 */
		public BoardStorage(int enPassant, int halfMove, boolean isChecked, boolean[] castling) {
			this.enPassant = enPassant;
			this.halfMove = halfMove;
			this.isChecked = isChecked;
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

	private PieceColor turn;
	private int promotingPawn;
	private int halfMove;
	private int fullMove;

	private final ZobristHashing hashing;

	private final Bitboard bitboard;
	
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
		
		promotingPawn = EMPTY;

		bitboard = new Bitboard(fen);
		fen_to_board(fen);
		
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

				if (getPiece(pos).isEmpty()) {
					emptySpaces++;
					continue;
				}

				else if (emptySpaces > 0) {
					fen += emptySpaces;
					emptySpaces = 0;
				}
				
				fen += pieceToChar(getPiece(pos));
			}

			if (emptySpaces > 0) fen += emptySpaces;

			if (row != 7) fen += "/";
		}
		
		fen = (turn == PieceColor.WHITE) ? fen + " w" : fen + " b";	//Store whose turn it is.

		boolean whiteCanCastle = true;

		//If white can't castle at all, add a "-".
		if (!bitboard.getCastlingRights(PieceColor.WHITE, KINGSIDE) && !bitboard.getCastlingRights(PieceColor.WHITE, QUEENSIDE)) {
			fen += " -";
			whiteCanCastle = false;
		}

		//Add capital letters based on white's castling ability.
		if (bitboard.getCastlingRights(PieceColor.WHITE, KINGSIDE)) fen += " K";
		if (bitboard.getCastlingRights(PieceColor.WHITE, QUEENSIDE)) fen += "Q";
		
		//If Black can't castle at all but white can, add a "-".
		if (!bitboard.getCastlingRights(PieceColor.BLACK, QUEENSIDE) && !bitboard.getCastlingRights(PieceColor.BLACK, KINGSIDE) && whiteCanCastle) fen += " -";

		//Add lowercase letter based on black's castling ability.
		if (bitboard.getCastlingRights(PieceColor.BLACK, KINGSIDE)) fen += "k";
		if (bitboard.getCastlingRights(PieceColor.BLACK, QUEENSIDE)) fen += "q";

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
		final String[] splitFen = fen.split(" ");
		turn = splitFen[1].equals("w") ? PieceColor.WHITE : PieceColor.BLACK;
		bitboard.setCastlingRights(PieceColor.WHITE, KINGSIDE, splitFen[2].contains("K"));
		bitboard.setCastlingRights(PieceColor.WHITE, QUEENSIDE, splitFen[2].contains("Q"));
		bitboard.setCastlingRights(PieceColor.BLACK, KINGSIDE, splitFen[2].contains("k"));
		bitboard.setCastlingRights(PieceColor.BLACK, QUEENSIDE, splitFen[2].contains("q"));
		if (splitFen[3].equals("-")) {
			bitboard.setEnPassant(EMPTY);
		}
		else {
			int enPassant = squareToIndex(splitFen[3]);
			enPassant = (turn == PieceColor.BLACK) ? enPassant - 8 : enPassant + 8;
			bitboard.setEnPassant(enPassant);
		}
		halfMove = Integer.parseInt(splitFen[4]);
		fullMove = Integer.parseInt(splitFen[5]);

		bitboard.updateCheck(turn);
	}

	/**
	 * Make a move on the board.
	 * @param move The move to be made.
	 */
	public void makeMove(Move move) {
		final ChessPiece movingPiece = getPiece(move.getStart());

		final boolean isAttack = bitboard.isOccupied(move.getFinish());

		//Handle the captured piece.
		if (isAttack) {
			halfMove = EMPTY;
			updatePosition(getPiece(move.getFinish()), move.getFinish(), true);	//Remove the captured piece from the board.
		}

		int newEnPassant = EMPTY;
		//Handles pawn moves.
		if (movingPiece.isPawn()) {
			newEnPassant = handlePawnSpecialBehavior(move);
		}
		
		//Removes castling rights when a rook moves.
		if (movingPiece.isRook()) {
			updateCastlingOnRookMove(move.getStart(), movingPiece.getColor());
		}
		
		//Handles king moves.
		if (movingPiece.isKing()) {
			bitboard.clearCastlingRights(movingPiece.getColor());             //King can no longer castle.
			hashing.setCastlingRights(turn, new boolean[] {false, false});
			//Handle castling.
			if (move.isSpecial()) {
				makeCastleMove(move);
			}
		}

		hashing.flipPiece(move.getStart(), movingPiece);
		bitboard.clearPiece(move.getStart());
		
		updatePosition(movingPiece, move.getFinish(), false);		//Move the moving piece to the new position.

		bitboard.setEnPassant(newEnPassant);
		hashing.setEnPassantFile(newEnPassant != EMPTY ? getColumn(newEnPassant) : newEnPassant);

		//Move on to the next turn if a promotion isn't happenning.
		if (!is_promote()) {
			halfMove ++;
			if (turn == PieceColor.BLACK) fullMove ++;
			next_turn();
		}
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
			updatePosition(getPiece(bitboard.getEnPassant()), bitboard.getEnPassant(), true);	//Remove the enPassant pawn from the board.
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
			bitboard.setCastlingRights(color, QUEENSIDE, false);
			hashing.setCastlingRights(color, QUEENSIDE, false);
		}
		//Kingside
		if (startingPos == ROOK_POSITIONS[color.arrayIndex][KINGSIDE]) {
			bitboard.setCastlingRights(color, KINGSIDE, false);
			hashing.setCastlingRights(color, KINGSIDE, false);
		}
	}

	/**
	 * Helper function that handles making a castle move.
	 * @param move The move being made by the king.
	 * @return Position of the castled rook.
	 */
	private void makeCastleMove(Move move) {
		final int side = move.getFinish() > move.getStart() ? KINGSIDE : QUEENSIDE;
		final int currentRookPos = ROOK_POSITIONS[turn.arrayIndex][side];
		final ChessPiece castledRook = getPiece(currentRookPos);
		bitboard.clearPiece(currentRookPos);
		final int newRookPos = move.getFinish() + (side == KINGSIDE ? Direction.LEFT : Direction.RIGHT).rawArrayValue;
		updatePosition(castledRook, newRookPos, false);

		hashing.flipPiece(currentRookPos, castledRook);
	}

	/**
	 * Undo a move on the board.
	 * @param move The original move that was made, uninverted.
	 * @param capturedPiece The piece that was captured, empty if no piece was captured.
	 * @param store Data that's lost when a move is made: halfmove, enPassant, and castling.
	 */
	public void undoMove(Move move, ChessPiece capturedPiece, BoardStorage store) {

		//Back up a turn if a promotion isn't happenning.
		if (!is_promote()) {
			halfMove = store.halfMove;
			if (turn == PieceColor.WHITE) fullMove --;
			next_turn();
		}
		bitboard.setCheck(store.isChecked);

		//Reset castling data and enPassant data.
		bitboard.setCastlingRights(turn, store.getCastling());
		bitboard.setEnPassant(store.enPassant);

		hashing.setCastlingRights(turn, bitboard.getCastlingRights(turn));
		hashing.setEnPassantFile(bitboard.getEnPassant() != EMPTY ? getColumn(bitboard.getEnPassant()) : bitboard.getEnPassant());

		final Move invertedMove = move.invert();

		final ChessPiece movingPiece = getPiece(invertedMove.getStart());

		//Check for castling
		if (movingPiece.isKing() && move.isSpecial()) {
			undoCastleMove(invertedMove);
		}

		bitboard.clearPiece(invertedMove.getStart());
		hashing.flipPiece(invertedMove.getStart(), movingPiece);
		updatePosition(movingPiece, invertedMove.getFinish(), false);			//Move the moving piece to the new position.

		//Add the captured piece back onto the board.
		// if (!capturedPiece.isEmpty() && !move.isSpecial()) updatePosition(capturedPiece, capturedPiece.getPos(), false);

		//Finnicky thing happens when you undo enPassant before calling softAttack on pieces.
		if (!capturedPiece.isEmpty()) updatePosition(capturedPiece, capturedPiece.getPos(), false);
		
		promotingPawn = EMPTY;
	}

	/**
	 * Helper function that handles undoing a castle move.
	 * @param invertedMove The move being made by the king.
	 * @return Position of the castled rook.
	 */
	private void undoCastleMove(Move invertedMove) {
		final int side = invertedMove.getStart() > invertedMove.getFinish() ? KINGSIDE : QUEENSIDE;
		final int castledRookPos = invertedMove.getStart() + (side == KINGSIDE ? Direction.LEFT : Direction.RIGHT).rawArrayValue;
		final ChessPiece castledRook = getPiece(castledRookPos);
		bitboard.clearPiece(castledRookPos);
		updatePosition(castledRook, ROOK_POSITIONS[turn.arrayIndex][side], false);		//Move the rook to the new position.
		hashing.flipPiece(castledRookPos, castledRook);
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
			bitboard.clearPiece(pos);
			return;
		}

		//Add the piece to the board and update the tracking variables.
		bitboard.setPiece(pos, piece);
		piece.setPos(pos);
	}

	/**
	 * Promotes the pawn.
	 * @param type The new type of the promoted piece.
	 */
	public void promote(PieceType type) {
		final ChessPiece promotingPiece = getPiece(promotingPawn);
		
		//Add the promoted piece to the board.
		hashing.flipPiece(promotingPawn, promotingPiece);
		promotingPiece.setType(type);
		updatePosition(promotingPiece, promotingPawn, false);

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
		final ChessPiece unpromotingPiece = getPiece(pos);
		hashing.flipPiece(pos, unpromotingPiece);
		bitboard.clearPiece(pos);

		//Backup a turn.
		halfMove --;
		if (turn == PieceColor.WHITE) fullMove --;
		next_turn();

		//Reset the piece to a pawn.
		unpromotingPiece.setType(PieceType.PAWN);
		updatePosition(unpromotingPiece, pos, false);

		promotingPawn = pos;
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
		for(final ChessPiece piece : getPieces(turn)) {
			final ArrayList<Move> moves = new ArrayList<Move>();
			bitboard.generatePieceMoves(moves, piece.getPos(), false);
			if(moves.size() > 0) {
				return CONTINUE;
			}
		}
		//If the king is in check, it's checkmate, if not draw. 
		return bitboard.isChecked() ? WIN : DRAW;
	}
	
	/**
	 * Check if the game is over by insufficient material resulting in a draw.
	 * @return Whether or not the game is a draw.
	 */
	private boolean hasInsufficientMaterial() {
		//Check each side to see if there's enough pieces.
		for (final PieceColor color : PIECE_COLORS) {
			if (bitboard.getPieceCount(color, PieceType.PAWN) > 0 || bitboard.getPieceCount(color, PieceType.KNIGHT) > 2
				|| bitboard.getPieceCount(color, PieceType.BISHOP) > 1 || bitboard.getPieceCount(color, PieceType.ROOK) > 0
				|| bitboard.getPieceCount(color, PieceType.QUEEN) > 0) return false;
		}
		return true;
	}

	/**
	 * Moves on to the next turn.
	 */
	public void next_turn() {
		turn = flipColor(turn);
		bitboard.updateCheck(turn);
		hashing.toggleSideToMove();
	}

	/**
	 * Returns whether or not a king can castle in the future.
	 * @param color The color of the king.
	 * @return True if the king can castle in the future, false if it can't.
	 */
	public boolean[] getCastlingPotential(PieceColor color) {
		return bitboard.getCastlingRights(color);
	}

	/**
	 * Checks if a move is a capture through enPassant.
	 * @param move The move being played.
	 * @return True if the move is a capture through enPassant, false if it isn't.
	 */
	public boolean isEnPassant(Move move) {
		return move.isSpecial() && (getPiece(move.getStart()).isPawn() || getPiece(move.getFinish()).isPawn());
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
		return bitboard.getPiece(pos);
	}

	/**
	 * Returns all pieces of a specific color.
	 * @param color The color of the pieces.
	 * @return A piece set object, use enhanced for loop.
	 */
	public PieceSet getPieces(PieceColor color) {
		return bitboard.getPieces(color);
	}

	/**
	 * Returns a copy of data that's lost when a move is made.
	 * @return A BoardStorage object containing enPassant, halfMove, and castling potential.
	 */
	public BoardStorage copyData() {
		return new BoardStorage(getEnPassant(), halfMove, bitboard.isChecked(), getCastlingPotential(getTurn()));
	}

	/**
	 * Returns a computer that can analyze the board.
	 * @return A computer object that can evaluate the best move or find the max amount of possible moves.
	 */
	public Computer getComputer() {
		return new Computer(this);
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
	 * Debugging tool to display the attacks of each piece, checks for negative attacks.
	 */
	public void displayAttacks(PieceSet[][] temp) {
		boolean valid = true;
		for (int color = 0; color < 2; color++) {
			final var colorAttacks = temp[color];
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

	public Bitboard getBitboard() {
		return bitboard;
	}

	public long hash() {
		return hashing.getHash();
	}
}