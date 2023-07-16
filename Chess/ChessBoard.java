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
			
			switch (piece.type) {
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
		kingAttacker = null;
		int castle = EMPTY;
		piece.pieceAttacks(true);

		if (move.type == Move.Type.ATTACK) {
			halfMove = -1;
			board[move.finish].pieceAttacks(true);
			updatePosition(board[move.finish], move.finish, true);
		}

		int passant = -1;
		if (piece.isPawn()) {
			halfMove = -1;
			if (move.type == Move.Type.SPECIAL) {
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
			if (move.type == Move.Type.SPECIAL) {
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
		piece.pieceAttacks(false);
		softAttackUpdate(move, false);
		if (castle != -1) {
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

		if (isPassant(invertedMove)) {
			updatePosition(capturedPiece, enPassant, false);
			capturedPiece.pieceAttacks(false);
		}

		board[invertedMove.start] = ChessPiece.empty();
		updatePosition(piece, invertedMove.finish, false);

		if (invertedMove.type == Move.Type.ATTACK) {
			updatePosition(capturedPiece, invertedMove.start, false);
			capturedPiece.pieceAttacks(false);
		}

		piece.pieceAttacks(false);
		softAttackUpdate(invertedMove, true);
		if (castle != -1) {
			board[castle].pieceAttacks(false);
		}
		
		promotingPawn = -1;
		ChessGame.timeUndoMove += System.currentTimeMillis() - prevTime;
	}

	public void promote(byte type) {
		board[promotingPawn].type = type;
		updatePosition(board[promotingPawn], promotingPawn, false);
		pieceCount[turn][type] += 1;
		pieceCount[turn][PAWN] -= 1;
		board[promotingPawn].pieceAttacks(false);
		halfMove ++;
		if (turn == BLACK) fullMove ++;
		next_turn();
		promotingPawn = -1;
	}

	public void unPromote(int pos, BoardStorage store) {
		halfMove --;
		if (turn == WHITE) fullMove --;
		next_turn();
		promotingPawn = pos;

		final ChessPiece piece = board[pos];

		piece.pieceAttacks(true);
		updatePosition(piece, promotingPawn, true);
		piece.type = PAWN;
		updatePosition(piece, promotingPawn, false);

		halfMove = store.halfMove;
	}
	
	private void updatePosition(ChessPiece piece, int newPos, boolean remove) {
		long prevTime = System.currentTimeMillis();
		if (remove) {
			if (pieces[piece.color].remove(piece)) {
				pieceCount[piece.color][piece.type] -= 1;
			}
			board[newPos] = ChessPiece.empty();
			return;
		}
		board[newPos] = piece;
		piece.setPos(newPos);
		if (pieces[piece.color].add(piece)) {
			pieceCount[piece.color][piece.type] += 1;
		}
		
		if (piece.isKing())
			kingPos[piece.color] = newPos;
	}
	
	private void softAttackUpdate(Move move, boolean undoMove) {
		for (int color = 0; color < 2; color++) {
			for (final ChessPiece piece : pieces[color]) {
				long prevTime = System.currentTimeMillis();
				piece.softAttack(move, undoMove);
				//ChessGame.timeDebug += System.currentTimeMillis() - prevTime;
			}
		}
	}

	private void hardAttackUpdate() {
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
		return (board[move.start].isKing() || board[move.finish].isKing()) && move.type == Move.Type.SPECIAL;
	}

	public boolean isPassant(Move move) {
		return (board[move.start].isPawn() || board[move.finish].isPawn()) && move.type == Move.Type.SPECIAL;
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
		return numAttacks(piece.pos, piece.color) >= 1;
	}

	public boolean is_promote() {
		return promotingPawn != -1;
	}

	public boolean[] getCastling(int turn) {
		return castling[turn];
	}
	
	public int numAttacks(int pos, int color) {
		return attacks[next(color)][pos].size();
	}

	public int getKingPos(int color) {
		return kingPos[color];
	}
	
	public int next(int currTurn) {
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
		return attacks[next(piece.color)][piece.pos];
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

	public static int getEdge(int direction, int pos) {
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

	public static int getDiagonalOffset(int startingPos, int endPos) {
		int direction = Math.abs(startingPos - endPos) % 7 == 0 ? 7 : 9;
		if (startingPos - endPos > 0) direction *= -1;
		return direction;
	}

	public static int getHorizontalOffset(int startingPos, int endPos) {
		int direction = onColumn(startingPos, endPos) ? 8 : 1;
		if (startingPos - endPos > 0) direction *= -1;
		return direction;
	}
	
	public static boolean blocksLine(int attacker, int target, int defender) {
		return onSameLine(attacker, target, defender) && getDistance(defender, target) < getDistance(attacker, target) &&
				getDistance(defender, attacker) < getDistance (target, attacker);
	}
	
	public static boolean onSameLine(int pos1, int pos2, int pos3) {
		return (onColumn(pos1, pos2) && onColumn(pos1, pos3)) || (onRow(pos1, pos2) && onRow(pos1, pos3));
	}
	
	public static boolean onLine(int pos1, int pos2) {
		return onRow(pos1, pos2) || onColumn(pos1, pos2);
	}
	
	public static boolean onL(int pos1, int pos2) {
		return getDistance(pos1, pos2) == 3;
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