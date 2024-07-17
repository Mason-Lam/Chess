package Chess;

import java.util.ArrayList;
import java.util.Arrays;

import Chess.Computer.BoardStorage;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.Constants.EvaluateConstants.*;

public class ChessBoard {

	private final PieceSet[][] attacks;
	
	private final int[] kingPos;
	private final int[][] pieceCount;
	private final PieceSet[] pieces;
	
	private final ChessPiece[] board;
	private final boolean[][] castling;

	private int turn;
	private int enPassant;
	private int promotingPawn;
	public int halfMove;
	public int fullMove;
	
	public ChessBoard () {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}

	public ChessBoard (String fen) {
		turn = BLACK;
		halfMove = 0;
		fullMove = 1;
		
		attacks = new PieceSet[2][1];
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
		//System.out.println(board_to_fen());
	}
	
	public String getFenString() {
		String fen = "";
		for (int i = 0; i < 8; i++) {
			int emptySpaces = 0;
			for (int j = 0; j < 8; j++) {
				int pos = i * 8 + j;
				if (board[pos].isEmpty()) {
					emptySpaces++;
					continue;
				}
				else if (emptySpaces > 0) {
					fen += Integer.valueOf(emptySpaces);
					emptySpaces = 0;
				}
				fen += pieceToChar(board[pos]);
			}
			if (emptySpaces > 0) fen += Integer.valueOf(emptySpaces);
			if (i != 8) fen += "/";
		}
		
		fen = (turn == WHITE) ? fen + " w" : fen + " b";
		boolean whiteCanCastle = true;
		if (!castling[WHITE][0] && !castling[WHITE][1]) {
			fen += " -";
			whiteCanCastle = false;
		}
		if (castling[WHITE][1]) fen += " K";
		if (castling[WHITE][0]) fen += "Q";
		
		if (!castling[BLACK][0] && !castling[BLACK][1] && whiteCanCastle) fen += " -";
		if (castling[BLACK][1]) fen += "k";
		if (castling[BLACK][0]) fen += "q";
		
		if (enPassant == EMPTY) fen += " -";
		else {
			final int passant = (turn == BLACK) ? enPassant + 8 : enPassant - 8;
			fen += " " + indexToSquare(getColumn(passant), 8 - getRow(passant));
		}

		fen += " " + halfMove;
		fen += " " + fullMove;
		
		return fen;
	}

	private void fen_to_board(String fen) {
		final int[] pieceIDs = new int[] {0, 0};
		int index = 0;
		boolean reachedHalfMove = false;
		for (int i = 0; i < fen.length(); i++) {
			char letter = fen.charAt(i);
			if (letter == ' ') continue;
			
			//Turn, Castling, en Passant
			if (index > 63) {
				
				//Turn
				if (letter == 'w') {
					turn = WHITE;
				}
				
				//Castling
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
				
				else if ((int) letter <= 104 && (int) letter >= 97 && Character.isDigit(fen.charAt(i + 1))) {
					enPassant = squareToIndex(Character.toString(letter) + fen.charAt(i + 1));
					enPassant = (turn == BLACK) ? enPassant - 8 : enPassant + 8;
					i++;
				}

				else if (Character.isDigit(letter)) {
					int number = Character.getNumericValue(letter);
					int digit = i + 1;
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
			
			//Filling board with pieces
			if (letter == '/') continue;
			final int pieceValue = Character.getNumericValue(letter);
			if (pieceValue < 10 && pieceValue > 0) {
				for (int j = 0; j < pieceValue; j++) {
					board[index] = ChessPiece.empty();
					index ++;
				}
				continue;
			}
			final ChessPiece piece = charToPiece(letter, index, this, pieceIDs);
			board[index] = piece;
			pieces[piece.color].add(piece);
			
			switch (piece.getType()) {
				case PAWN: pieceCount[piece.color][PAWN] += 1;
					break;
				case KNIGHT: pieceCount[piece.color][KNIGHT] += 1;
					break;
				case BISHOP: pieceCount[piece.color][BISHOP] += 1;
					break;
				case ROOK: pieceCount[piece.color][ROOK] += 1;
					break;
				case QUEEN: pieceCount[piece.color][QUEEN] += 1;
					break;
				case KING: kingPos[piece.color] = index;
					break;
			}
			index++;
		}
	}
	
	public void make_move(Move move, boolean permanent) {
		long prevTime = System.currentTimeMillis();
		final ChessPiece piece = board[move.start];
		final boolean isAttack = !board[move.finish].isEmpty();
		kingAttacker = null;
		int castle = EMPTY;
		piece.pieceAttacks(true);

		if (isAttack) {
			halfMove = EMPTY;
			board[move.finish].pieceAttacks(true);
			updatePosition(board[move.finish], move.finish, true);
		}

		int passant = EMPTY;
		if (piece.isPawn()) {
			halfMove = EMPTY;
			if (move.SPECIAL) {
				board[enPassant].pieceAttacks(true);
				updatePosition(board[enPassant], enPassant, true);
			}
			if (getDistanceVert(move.start, move.finish) == 2) {
				passant = move.finish;
			}
			if (getRow(move.finish) == PROMOTION_LINE[turn]) {
				promotingPawn = move.finish;
			}
		}
		
		if (piece.isRook()) {
			if (move.start == ROOK_POSITIONS[piece.color][0])
				castling[piece.color][0] = false;
			if (move.start == ROOK_POSITIONS[piece.color][1])
				castling[piece.color][1] = false;
		}
		
		if (piece.isKing()) {
			Arrays.fill(castling[piece.color], false);
			if (move.SPECIAL) {
				if (move.finish > move.start) {
					castle = ROOK_POSITIONS[turn][1];
					board[castle].pieceAttacks(true);
					updatePosition(board[castle], move.finish - 1, false);
					castle = move.finish - 1;
					board[ROOK_POSITIONS[turn][1]] = ChessPiece.empty();
				}
				else {
					castle = ROOK_POSITIONS[turn][0];
					board[castle].pieceAttacks(true);
					updatePosition(board[castle], move.finish + 1, false);
					castle = move.finish + 1;
					board[ROOK_POSITIONS[turn][0]] = ChessPiece.empty();
				}
			}
		}
		board[move.start] = ChessPiece.empty();
		
		updatePosition(piece, move.finish, false);
		resetPieces(move, isAttack, false);
		if (promotingPawn == EMPTY) piece.pieceAttacks(false);
		if (castle != EMPTY) {
			board[castle].pieceAttacks(false);
		}

		enPassant = passant;
		if (!is_promote()) {
			halfMove ++;
			if (turn == BLACK) fullMove ++;
			next_turn();
		}
		ChessGame.timeMakeMove += System.currentTimeMillis() - prevTime;
	}

	public void undoMove(Move move, ChessPiece capturedPiece, Computer.BoardStorage store) {
		long prevTime = System.currentTimeMillis();
		if (!is_promote()) {
			halfMove = store.halfMove;
			if (turn == WHITE) fullMove --;
			next_turn();
		}

		castling[turn] = store.getCastling();
		enPassant = store.enPassant;
		kingAttacker = null;
		final Move invertedMove = move.invert();
		final boolean isAttack = !capturedPiece.isEmpty() && !move.SPECIAL;

		final ChessPiece piece = board[invertedMove.start];
		int castle = EMPTY;
		piece.pieceAttacks(true);

		if (isCastle(invertedMove)) {
			if (invertedMove.start > invertedMove.finish) {
				castle = invertedMove.start - 1;
				board[castle].pieceAttacks(true);
				updatePosition(board[castle], ROOK_POSITIONS[turn][1], false);
				castle = ROOK_POSITIONS[turn][1];
				board[invertedMove.start - 1] = ChessPiece.empty();
			}
			else {
				castle = invertedMove.start + 1;
				board[castle].pieceAttacks(true);
				updatePosition(board[castle], ROOK_POSITIONS[turn][0], false);
				castle = ROOK_POSITIONS[turn][0];
				board[invertedMove.start + 1] = ChessPiece.empty();
			}
		}

		board[invertedMove.start] = ChessPiece.empty();
		updatePosition(piece, invertedMove.finish, false);

		if (!capturedPiece.isEmpty() && !move.SPECIAL) updatePosition(capturedPiece, capturedPiece.getPos(), false);

		resetPieces(invertedMove, isAttack, true);

		if (!capturedPiece.isEmpty() && move.SPECIAL) updatePosition(capturedPiece, capturedPiece.getPos(), false);
		
		piece.pieceAttacks(false);

		if (!capturedPiece.isEmpty()) capturedPiece.pieceAttacks(false);
		
		if (castle != -1) board[castle].pieceAttacks(false);
		
		promotingPawn = -1;
		ChessGame.timeUndoMove += System.currentTimeMillis() - prevTime;
	}

	//Not a big deal
	public void promote(byte type) {
		long prevTime = System.currentTimeMillis();
		board[promotingPawn].setType(type);
		updatePosition(board[promotingPawn], promotingPawn, false);
		pieceCount[turn][type] += 1;
		pieceCount[turn][PAWN] -= 1;
		board[promotingPawn].pieceAttacks(false);
		halfMove ++;
		if (turn == BLACK) fullMove ++;
		next_turn();
		promotingPawn = -1;
	}

	//Not a big deal
	public void unPromote(int pos, BoardStorage store) {
		long prevTime = System.currentTimeMillis();
		halfMove --;
		if (turn == WHITE) fullMove --;
		next_turn();
		promotingPawn = pos;

		final ChessPiece piece = board[pos];

		piece.pieceAttacks(true);
		updatePosition(piece, promotingPawn, true);
		piece.setType(PAWN);;
		updatePosition(piece, promotingPawn, false);

		halfMove = store.halfMove;
	}
	
	//Not big deal
	private void updatePosition(ChessPiece piece, int newPos, boolean remove) {
		long prevTime = System.currentTimeMillis();
		if (remove) {
			if (pieces[piece.color].remove(piece)) {
				pieceCount[piece.color][piece.getType()] -= 1;
			}
			board[newPos] = ChessPiece.empty();
			return;
		}
		board[newPos] = piece;
		piece.setPos(newPos);
		if (pieces[piece.color].add(piece)) {
			pieceCount[piece.color][piece.getType()] += 1;
		}
		
		if (piece.isKing()) kingPos[piece.color] = newPos;
	}

	private void resetPieces(Move move, boolean isAttack, boolean undoMove) {
		final long prevTime = System.currentTimeMillis();
		if (isCastle(move)) {
			final boolean kingSide = (move.finish > move.start && !undoMove) || (move.start > move.finish && undoMove);
			final int startingRookPos = kingSide ? ROOK_POSITIONS[turn][1] : ROOK_POSITIONS[turn][0];
			final int castledRookPos = kingSide ? ROOK_POSITIONS[turn][1] - 2 : ROOK_POSITIONS[turn][0] + 3;
			pawnReset(startingRookPos, next(turn));
			pawnReset(castledRookPos, next(turn));
			for (int color = 0; color < 2; color ++) {
				final PieceSet startingRookAttacks = kingSide ? attacks[turn][ROOK_POSITIONS[turn][1]] : attacks[turn][ROOK_POSITIONS[turn][0]];
				final PieceSet castledRookAttacks = kingSide ? attacks[turn][ROOK_POSITIONS[turn][1] - 2] : attacks[turn][ROOK_POSITIONS[turn][0] + 3];
				for (final ChessPiece piece : startingRookAttacks) {
					// if (piece.pos == move.start || piece.pos == move.finish) continue;
					pieceReset(piece, undoMove ? 1 : 0, startingRookPos, false, undoMove);
				}
				for (final ChessPiece piece : castledRookAttacks) {
					// if (piece.pos == move.start || piece.pos == move.finish) continue;
					pieceReset(piece, undoMove ? 0 : 1, castledRookPos, false, undoMove);
				}
			}
			// for (int color = 0; color < 2; color ++) {
			// 	final PieceSet coloredPieces = pieces[color];
			// 	for (final ChessPiece piece : coloredPieces) {
			// 		piece.reset();
			// 	}
			// }
		}

		int[] squares = getSquares(move);
		for (int index = 0; index < squares.length; index ++) {
			final int pos = squares[index];
			for (int color = 0; color < 2; color++) {
				final PieceSet attacks = getAttacks(pos, color);
				for (final ChessPiece piece : attacks) {
					// if (piece.pos == move.start || piece.pos == move.finish) continue;
					long prevTime2 = System.currentTimeMillis();
					piece.softAttack(pos, index, isAttack, undoMove);
					ChessGame.timeSoftAttack += System.currentTimeMillis() - prevTime2;
					pieceReset(piece, index, pos, isAttack, undoMove);
				}
				pawnReset(pos, color);
			}
		}
		ChessGame.timePieceUpdate += System.currentTimeMillis() - prevTime;
	}

	//Solid
	private void pieceReset(ChessPiece piece, int index, int square, boolean isAttack, boolean undoMove) {
		// if (!piece.hasCopy()) return;
		switch (piece.getType()) {
			case PAWN:
				if (index == 2) {
					if (piece.color == turn) piece.reset();
					break;
				}
				if (isAttack && ((index == 0 && undoMove) || (index == 1 && !undoMove))) {
					piece.reset();
					break;
				}

				if (piece.color != turn) piece.reset();
				break;
			case KNIGHT:
			case KING:
				if (index == 2) {
					if (piece.color != turn) piece.updateCopy(undoMove, square);
					//undoMove: empty to black; black to empty
					break;
				}
				if (isAttack && ((index == 0 && undoMove) || (index == 1 && !undoMove))) {
					final boolean remove = undoMove ? piece.color != turn : piece.color == turn;
					//undoMove: white to black; black to white
					piece.updateCopy(remove, square);
					break;
				}

				/**
				 * undoMove:
				 *   index == 0: White to empty
				 *   index == 1: empty to white
				 * !undoMove:
				 * 	 index == 0: White to empty
				 *   index == 1: empty to white
				 */
				if (piece.color == turn) piece.updateCopy(index == 1, square);
				break;
			default:
				piece.reset();
				break;
		}
	}

	//Meh
	private void pawnReset(int square, int color) {
		final int direction = color == WHITE ? 8 : - 8;
		final int size = Math.abs(getRow(square) - PAWN_STARTS[color]) == 2 ? 3 : 2;
		for (int i = 1; i < size; i++) {
			final int newPos = square + direction * i;
			if (!onBoard(newPos)) return;
			final ChessPiece piece = board[newPos];
			if (piece.isPawn()) piece.reset();
			else if (piece.isEmpty()) continue;
			return;
		}
	}

	//Probably good
	private int[] getSquares(Move move) {
		if (isPassant(move)) return new int[] {move.start, move.finish, enPassant};
		return new int[] {move.start, move.finish};
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