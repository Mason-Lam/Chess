package Chess;

import java.util.ArrayList;
import java.util.Arrays;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.Constants.EvaluateConstants.*;

/**
 * Class representing a ChessBoard.
 */
public class ChessBoard {

	public static class BoardStorage {
		public final int enPassant;
		public final int halfMove;
		private final boolean[] castling;

		public BoardStorage(int enPassant, int halfMove, boolean[] castling) {
			this.enPassant = enPassant;
			this.halfMove = halfMove;
			this.castling = new boolean[2];
			this.castling[0] = castling[0];
			this.castling[1] = castling[1];
		}

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

	private int turn;
	private int enPassant;
	private int promotingPawn;
	public int halfMove;
	public int fullMove;
	
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
		turn = BLACK;
		halfMove = 0;
		fullMove = 1;
		
		attacks = new PieceSet[2][];
		attacks[BLACK] = new PieceSet[64];
		attacks[WHITE] = new PieceSet[64];

		pieces = new PieceSet[2];
		pieces[BLACK] = new PieceSet();
		pieces[WHITE] = new PieceSet();
		
		kingPos = new int[2];
		pieceCount = new int[2][5];
		pieceCount[0] = new int[5];
		pieceCount[1] = new int[5];
		Arrays.fill(pieceCount[0], 0);
		Arrays.fill(pieceCount[1], 0);
		board = new ChessPiece[64];
		castling = new boolean[2][2]; //Black: Queenside, Kingside, White: Queenside, Kingside
		Arrays.fill(castling[BLACK], false);
		Arrays.fill(castling[WHITE], false);
		enPassant = -1;
		promotingPawn = -1;
		fen_to_board(fen);
		hardAttackUpdate();
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
		
		fen = (turn == WHITE) ? fen + " w" : fen + " b";	//Store whose turn it is.

		boolean whiteCanCastle = true;

		//If white can't castle at all, add a "-".
		if (!castling[WHITE][0] && !castling[WHITE][1]) {
			fen += " -";
			whiteCanCastle = false;
		}

		//Add capital letters based on white's castling ability.
		if (castling[WHITE][1]) fen += " K";
		if (castling[WHITE][0]) fen += "Q";
		
		//If Black can't castle at all but white can, add a "-".
		if (!castling[BLACK][0] && !castling[BLACK][1] && whiteCanCastle) fen += " -";

		//Add lowercase letter based on black's castling ability.
		if (castling[BLACK][1]) fen += "k";
		if (castling[BLACK][0]) fen += "q";
		
		//Handle enPassant
		if (enPassant == EMPTY) fen += " -";
		else {
			final int passant = enPassant + getPawnDirection(turn);
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
					turn = WHITE;
				}
				
				//Castling.
				else if (letter == 'K') {
					castling[WHITE][1] = true;
				}
				else if (letter == 'Q') {
					castling[WHITE][0] = true;
				}
				else if (letter == 'k') {
					castling[BLACK][1] = true;
				}
				else if (letter == 'q') {
					castling[BLACK][0] = true;
				}
				
				//enPassant; it's garbage but I can't be bothered to clean it up.
				else if ((int) letter <= 104 && (int) letter >= 97 && Character.isDigit(fen.charAt(index + 1))) {
					enPassant = squareToIndex(Character.toString(letter) + fen.charAt(index + 1));
					enPassant = (turn == BLACK) ? enPassant - 8 : enPassant + 8;
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
				for (int j = 0; j < pieceValue; j++) {
					board[pos] = ChessPiece.empty();
					pos ++;
				}
				continue;
			}

			//Create and store the piece.
			final ChessPiece piece = charToPiece(letter, pos, this, pieceIDs);
			board[pos] = piece;
			pieces[piece.color].add(piece);
			
			//Keep track of the piece.
			if (piece.isKing()) kingPos[piece.color] = pos;
			else pieceCount[piece.color][piece.getType()] ++;

			pos++;
		}
	}
	
	/**
	 * Make a move on the board.
	 * @param move The move to be made.
	 */
	public void makeMove(Move move) {
		long prevTime = System.currentTimeMillis();

		final ChessPiece movingPiece = board[move.start];		
		final boolean isAttack = !board[move.finish].isEmpty();
		kingAttacker = null;
		int castle = EMPTY;
		movingPiece.pieceAttacks(true);		//Update the squares the moving piece currently attacks.

		//Handle the captured piece.
		if (isAttack) {
			halfMove = EMPTY;
			board[move.finish].pieceAttacks(true);		//Update the squares the capture piece used to attack.
			updatePosition(board[move.finish], move.finish, true);	//Remove the captured piece from the board.
		}

		int newEnPassant = EMPTY;
		//Handles pawn moves.
		if (movingPiece.isPawn()) {
			halfMove = EMPTY;
			//Captures enPassant.
			if (move.SPECIAL) {
				board[enPassant].pieceAttacks(true);		//Update the squares the enPassant pawn used to attack.
				updatePosition(board[enPassant], enPassant, true);	//Remove the enPassant pawn from the board.
			}
			//Pawn moves two squares forward.
			if (getDistanceVert(move.start, move.finish) == 2) {
				newEnPassant = move.finish;
			}
			//Pawn is promoting.
			if (getRow(move.finish) == PROMOTION_LINE[turn]) {
				promotingPawn = move.finish;
			}
		}
		
		//Removes castling rights when a rook moves.
		if (movingPiece.isRook()) {
			//Queenside
			if (move.start == ROOK_POSITIONS[movingPiece.color][0])
				castling[movingPiece.color][0] = false;
			//Kingside
			if (move.start == ROOK_POSITIONS[movingPiece.color][1])
				castling[movingPiece.color][1] = false;
		}
		
		//Handles king moves.
		if (movingPiece.isKing()) {
			Arrays.fill(castling[movingPiece.color], false);		//King can no longer castle.
			//Handle castling.
			if (move.SPECIAL) {
				//Kingside.
				if (move.finish > move.start) {
					castle = ROOK_POSITIONS[turn][1];			//Store the rook.
					board[castle].pieceAttacks(true);	//Update the squares the rook currently attacks.
					updatePosition(board[castle], move.finish - 1, false);	//Move the rook to the new position.
					board[castle] = ChessPiece.empty();		//Empty the square the rook used to occupy.
					castle = move.finish - 1;		//Set the new rook position.
				}
				//Queenside.
				else {
					castle = ROOK_POSITIONS[turn][0];			//Store the rook.
					board[castle].pieceAttacks(true);	//Update the squares the rook currently attacks.
					updatePosition(board[castle], move.finish + 1, false);	//Move the rook to the new position.
					board[castle] = ChessPiece.empty();	//Empty the square the rook used to occupy.
					castle = move.finish + 1;	//Set the new rook position.
				}
			}
		}
		board[move.start] = ChessPiece.empty();	//Empty the square the moving piece used to occupy.
		
		updatePosition(movingPiece, move.finish, false);		//Move the moving piece to the new position.
		resetPieces(move, isAttack, false);		//Reset the move copies of pieces affected by this new position.

		pawnReset(move);

		if (promotingPawn == EMPTY) movingPiece.pieceAttacks(false);		//Update the squares the moving piece attacks in its new position.
		if (castle != EMPTY) board[castle].pieceAttacks(false);	//Update the squares the castled rook attacks in its new position.

		enPassant = newEnPassant;			//Store the pawn that moved two squares.

		//Move on to the next turn if a promotion isn't happenning.
		if (!is_promote()) {
			halfMove ++;
			if (turn == BLACK) fullMove ++;
			next_turn();
		}
		
		ChessGame.timeMakeMove += System.currentTimeMillis() - prevTime;
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
			if (turn == WHITE) fullMove --;
			next_turn();
		}

		//Reset castling data and enPassant data.
		castling[turn] = store.getCastling();
		enPassant = store.enPassant;

		kingAttacker = null;
		final Move invertedMove = move.invert();
		final boolean isAttack = !capturedPiece.isEmpty() && !move.SPECIAL;

		final ChessPiece movingPiece = board[invertedMove.start];
		int castle = EMPTY;
		movingPiece.pieceAttacks(true);		//Update the squares the moving piece currently attacks.

		//Check for castling
		if (isCastle(invertedMove)) {
			//Kingside
			if (invertedMove.start > invertedMove.finish) {
				castle = invertedMove.start - 1;			//Store the rook position.
				board[castle].pieceAttacks(true);		//Update the squares the rook currently attacks.
				updatePosition(board[castle], ROOK_POSITIONS[turn][1], false);		//Move the rook to the new position.
				board[castle] = ChessPiece.empty();			//Empty the square the rook used to occupy.
				castle = ROOK_POSITIONS[turn][1];			//Set the new rook position.
			}
			//Queenside
			else {
				castle = invertedMove.start + 1;			//Store the rook position.
				board[castle].pieceAttacks(true);		//Update the squares the rook currently attacks.
				updatePosition(board[castle], ROOK_POSITIONS[turn][0], false);		//Move the rook to the new position.
				castle = ROOK_POSITIONS[turn][0];				//Empty the square the rook used to occupy.
				board[invertedMove.start + 1] = ChessPiece.empty();		//Set the new rook position.
			}
		}

		board[invertedMove.start] = ChessPiece.empty();		//Empty the square the piece used to occupy.
		updatePosition(movingPiece, invertedMove.finish, false);			//Move the moving piece to the new position.

		//Add the captured piece back onto the board.
		if (!capturedPiece.isEmpty() && !move.SPECIAL) updatePosition(capturedPiece, capturedPiece.getPos(), false);

		resetPieces(invertedMove, isAttack, true);

		//Finnicky thing happens when you undo enPassant before calling softAttack on pieces.
		if (!capturedPiece.isEmpty() && move.SPECIAL) updatePosition(capturedPiece, capturedPiece.getPos(), false);

		pawnReset(move.invert());
		
		movingPiece.pieceAttacks(false);		//Update the squares the moving piece attacks in its new position.

		if (!capturedPiece.isEmpty()) capturedPiece.pieceAttacks(false);		//Update the squares the captured piece now attacks.
		
		if (castle != EMPTY) board[castle].pieceAttacks(false);			//Update the squares the castled rook now attacks.
		
		promotingPawn = EMPTY;

		ChessGame.timeUndoMove += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Updates the board and tracking variables with a piece removal or movement.
	 * @param piece The piece either being removed or added to the board.
	 * @param pos The position of the piece being added or removed from.
	 * @param remove Whether or not the piece is getting removed or added.
	 */
	private void updatePosition(ChessPiece piece, int pos, boolean remove) {
		//Remove the piece from the board and updates the tracking variables.
		if (remove) {
			pieces[piece.color].remove(piece);
			pieceCount[piece.color][piece.getType()] -= 1;
			board[pos] = ChessPiece.empty();
			return;
		}

		//Add the piece to the board and update the tracking variables.
		board[pos] = piece;
		piece.setPos(pos);
		if (pieces[piece.color].add(piece)) pieceCount[piece.color][piece.getType()] += 1;
		
		//Update the king position variable if the king moves.
		if (piece.isKing()) kingPos[piece.color] = pos;
	}

	/**
	 * Promotes the pawn.
	 * @param type The new type of the promoted piece.
	 */
	public void promote(byte type) {
		final ChessPiece promotingPiece = board[promotingPawn];
		
		//Add the promoted piece to the board.
		promotingPiece.setType(type);
		updatePosition(promotingPiece, promotingPawn, false);

		//Update tracking variables.
		pieceCount[turn][type] += 1;
		pieceCount[turn][PAWN] -= 1;

		promotingPiece.pieceAttacks(false);	//Update the squares the promoted piece now attacks.

		//Next turn.
		halfMove ++;
		if (turn == BLACK) fullMove ++;
		next_turn();

		promotingPawn = EMPTY;
	}

	/**
	 * Unpromotes a piece.
	 * @param pos The position of the promoted piece.
	 */
	public void unPromote(int pos) {
		final ChessPiece unpromotingPiece = board[pos];

		//Backup a turn.
		halfMove --;
		if (turn == WHITE) fullMove --;
		next_turn();

		//Update tracking variables.
		pieceCount[turn][unpromotingPiece.getType()] --;
		pieceCount[turn][PAWN] ++;

		//Update the squares the unpromoting piece used to attack.
		unpromotingPiece.pieceAttacks(true);

		//Reset the piece to a pawn.
		unpromotingPiece.setType(PAWN);
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

		if (isCastle) {
			//Can't be bothered to handle castling because it happens at max twice in a game.
			for (int color = 0; color < 2; color ++) {
				final PieceSet coloredPieces = pieces[color];
				for (final ChessPiece piece : coloredPieces) {
					piece.resetMoveCopy();
				}
			}
		}

		int[] modifiedSquares = isPassant(move) ? new int[] {move.start, move.finish, enPassant} : new int[] {move.start, move.finish};
		//Check each square that the move affects.
		for (int movePart = 0; movePart < modifiedSquares.length; movePart ++) {

			//Go through black and white pieces potentially affected.
			final int pos = modifiedSquares[movePart];
			for (int color = 0; color < 2; color++) {

				//Check each piece attacking the square.
				final PieceSet attacks = getAttacks(pos, color);
				for (final ChessPiece piece : attacks) {
					long prevTime2 = System.currentTimeMillis();

					//Update straight line and diagonal attackers.
					piece.softAttack(pos, movePart, isAttack, undoMove);

					ChessGame.timeSoftAttack += System.currentTimeMillis() - prevTime2;

					//Reset the moves copy in pieces affected by the move.
					if (!isCastle) pieceReset(piece, pos, movePart, isAttack, undoMove);
				}
			}
		}
		ChessGame.timePieceUpdate += System.currentTimeMillis() - prevTime;
	}

	/**
	 * Resets a piece's move copy list based on a move made.
	 * @param piece The chess piece being updated, prereq is that it's currently attacking the square.
	 * @param square Position of the modified square; starting position or end position of a move.
	 * @param movePart Integer representing if the square paramater is the starting position or end position of the move.
	 * @param isAttack If the move resulted in a capture of another piece.
	 * @param undoMove Whether or not the move is being undone.
	 */
	private void pieceReset(ChessPiece piece, int square, int movePart, boolean isAttack, boolean undoMove) {
		//Comments made from white's perspective
		switch (piece.getType()) {
			case PAWN:
				if (movePart == EN_PASSANT) {
					/**
					 * Square goes from black to empty (normal move) or empty to black (undo move),
					 * only white pawn's change either attacking another the black piece or losing a capture possibility.
					*/
					if (piece.color == turn) piece.updateCopy(!undoMove, square);
					break;
				}
				if (movePart == START) {
					
					/**
					 * Square goes from white to black, covers the case where it's an capture and undo move.
					 * Black pawns will lose their attack and white pawns will be able to attack the square.
					 */
					if (undoMove && isAttack) {
						piece.updateCopy(piece.color != turn, square);
						break;
					}
					/**
					 * Square goes from white to empty for both normal and undo moves,
					 * black pawns will then no longer be able to attack the square.
					 */
					if (piece.color != turn) piece.updateCopy(true, square);
					break;
				}
				if (movePart == END) {
					/**
					 * Square goes from empty to white for undoing a move or a non capturing normal move,
					 * black pawns will then be able to target the square.
					 */
					if (undoMove || !isAttack) {
						if (piece.color != turn) piece.updateCopy(false, square);
						break;
					}

					/**
					 * Square goes from black to white for a capture normal move,
					 * white pawns will no longer be able to attack the square, but black pawns will.
					 */
					piece.updateCopy(piece.color == turn, square);
				}
				break;
			case KNIGHT:
			case KING:
				if (movePart == EN_PASSANT) {
					/**
					 * Square goes from black to empty (normal move) or empty to black (undo move),
					 * white pieces can still move to the square,
					 * black pieces will gain an attack when the move is normal and lose an attack during an undo move.
					 */
					if (piece.color != turn) piece.updateCopy(undoMove, square);
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
					piece.updateCopy(undoMove ? piece.color != turn : piece.color == turn, square);
					break;
				}

				/**
				 * Square goes from empty to white or white to empty, black pieces do nothing,
				 * white pieces will lose a move if the square goes from empty to white (END),
				 * white pieces will gain a move if the square goes from white to empty (START).
				 */
				if (piece.color == turn) piece.updateCopy(movePart == END, square);
				break;
			default:
				//Slidy pieces.
				piece.resetMoveCopy();
				break;
		}
	}

	/**
	 * Removes or adds straight line moves to a pawn's move copy based on a move made.
	 * @param move The move being made, make sure it is uninverted when undoing a move.
	 */
	private void pawnReset(Move move) {
		final int[] squares = isPassant(move) ? new int[] {move.start, move.finish, enPassant} : new int[] {move.start, move.finish};

		//Update both black and white pawns.
		for (int color = 0; color < 2; color++) {
			final int direction = getPawnDirection(color);

			//Check every square involved in the move made.
			for (final int pos : squares) {
				final boolean isEmpty = board[pos].isEmpty();		//Checks if the square is empty, if so the pawn can make the move forward.
				
				//Go one square ahead of the involved square.
				final int oneSquareAhead = pos - direction;
				if (!onBoard(oneSquareAhead)) continue;
				final ChessPiece pieceOneSquareAhead = board[oneSquareAhead];

				//Check if it's a pawn that would be influneced by the move.
				if (pieceOneSquareAhead.isPawn() && pieceOneSquareAhead.color == color) {
					pieceOneSquareAhead.updateCopy(!isEmpty, pos);
					//Check for double move forward.
					if (getRow(oneSquareAhead) == PAWN_STARTS[color]) {
						//Check the square behind the involved square.
						final int oneSquareBehind = pos + direction;
						if (board[oneSquareBehind].isEmpty()) pieceOneSquareAhead.updateCopy(!isEmpty, oneSquareBehind);
					}
					continue;
				}
				else if (!pieceOneSquareAhead.isEmpty()) continue;

				//Go two squares ahead if the first is empty.
				final int twoSquaresAhead = oneSquareAhead - direction;
				if (!onBoard(twoSquaresAhead)) continue;
				final ChessPiece pieceTwoSquaresAhead = board[twoSquaresAhead];

				//Check if it's a pawn that would be influenced by the move.
				if (pieceTwoSquaresAhead.isPawn() && pieceTwoSquaresAhead.color == color && getRow(twoSquaresAhead) == PAWN_STARTS[color]) {
					pieceTwoSquaresAhead.updateCopy(!isEmpty, pos);
				}
			}
		}
	}

	//Good
	private void hardAttackUpdate() {
		long prevTime = System.currentTimeMillis();
		for (int color = 0; color < 2; color ++) {
			for (int j = 0;  j < attacks[color].length; j++) {
				attacks[color][j] = new PieceSet();
			}
			for (final ChessPiece piece : pieces[color]) {
				piece.pieceAttacks(false);
			}
		}
	}

	public int isWinner() {
		if (halfMove >= 50) return DRAW;
		if (hasInsufficientMaterial()) return DRAW;
		for(ChessPiece piece : pieces[turn]) {
			final ArrayList<Move> moves = new ArrayList<Move>();
			piece.pieceMoves(moves);
			if(moves.size() > 0) {
				return PROGRESS;
			}
		}
		return isChecked(turn) ? WIN : DRAW;
	}
	
	private boolean hasInsufficientMaterial() {
		for (int color = 0; color < 2; color++) {
			if (pieceCount[color][PAWN] > 0 || pieceCount[color][KNIGHT] > 2 
				|| pieceCount[color][BISHOP] > 1 || pieceCount[color][ROOK] > 0 
				|| pieceCount[color][QUEEN] > 0) return false;
		}
		return true;
	}

	public void modifyAttacks(ChessPiece piece, int pos, boolean remove) {
		if (remove) {
			removeAttacker(piece, pos);
			return;
		}
		addAttacker(piece, pos);
	}

	public void addAttacker(ChessPiece piece, int pos) {
		attacks[piece.color][pos].add(piece);
	}

	public void removeAttacker(ChessPiece piece, int pos) {
		attacks[piece.color][pos].remove(piece);
	}

	public void next_turn() {
		turn = next(turn);
	}

	public boolean isCastle(Move move) {
		return (board[move.start].isKing() || board[move.finish].isKing()) && move.SPECIAL;
	}

	public boolean isPassant(Move move) {
		return (board[move.start].isPawn() || board[move.finish].isPawn()) && move.SPECIAL;
	}
	
	public boolean doubleCheck(int color) {
		return numAttacks(kingPos[color], color) >= 2;
	}
	
	public boolean isChecked(int color) {
		return isAttacked(board[kingPos[color]]);
	}
	
	public boolean isAttacked(int pos, int color) {
		return numAttacks(pos,color) >= 1;
	}

	public boolean isAttacked(ChessPiece piece) {
		return numAttacks(piece.getPos(), piece.color) >= 1;
	}

	public boolean is_promote() {
		return promotingPawn != -1;
	}

	public boolean clearPath(int startPos, int endPos) {
		final int direction = getDirection(startPos, endPos);
		int pos = startPos + direction;
		while (pos != endPos) {
			if (!getPiece(pos).isEmpty()) return false;
			pos += direction;
		}
		return true;
	}

	public boolean canCastle(boolean kingSide, int color) {
		if (isChecked(color)) return false;
		if (!castling[turn][kingSide ? 1 : 0]) return false;

		final int rookPos = ROOK_POSITIONS[color][kingSide ? 1 : 0];
		if (!getPiece(rookPos).isRook()) return false;

		final int distance = Math.abs(rookPos - KING_POSITIONS[color]);
		for (int i = 1; i < distance; i++) {
			if (!getPiece(kingSide ? KING_POSITIONS[color] + i : KING_POSITIONS[color] - i).isEmpty()) return false;
		}

		for (int i = 1; i < 3; i++) {
			if (isAttacked(kingSide ? KING_POSITIONS[color] + i : KING_POSITIONS[color] - i, color)) return false;
		}

		return true;
	}

	public boolean[] getCastlingPotential(int turn) {
		return castling[turn];
	}
	
	public int numAttacks(int pos, int color) {
		return attacks[next(color)][pos].size();
	}

	public int getKingPos(int color) {
		return kingPos[color];
	}
	
	public static int next(int currTurn) {
		return (currTurn + 1) % 2;
	}

	public int getEnPassant() {
		return enPassant;
	}

	public int getTurn() {
		return turn;
	}

	public ChessPiece getPiece(int pos) {
		return board[pos];
	}

	private ChessPiece kingAttacker = null;
	public ChessPiece getKingAttacker(int color) {
		if (kingAttacker == null) {
			for (final ChessPiece piece : getAttackers(board[kingPos[color]])) kingAttacker = piece;
		}
		return kingAttacker;
	}
	
	public PieceSet getPieces(int color) {
		return pieces[color];
	}

	public PieceSet getAttackers(ChessPiece piece) {
		return attacks[next(piece.color)][piece.getPos()];
	}

	public PieceSet getAttacks(int pos, int color)  {
		return attacks[color][pos];
	}

	public Computer getComputer() {
		return new Computer(this);
	}

	public BoardStorage copyData() {
		return new BoardStorage(getEnPassant(), halfMove, getCastlingPotential(getTurn()));
	}
	
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