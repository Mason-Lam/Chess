package Chess;

import java.util.ArrayList;
import java.util.Arrays;

import Chess.Computer.BoardStorage;

public class ChessBoard {
	

	private final PieceSet[][] attacks;
	
	private final int[] kingPos;
	private final int[][] pieceCount;
	private final PieceSet[] pieces;
	
	private final ChessPiece[] board;
	private final boolean[][] castling;
	
	private String fenString;
	private int turn;
	private int enPassant;
	private int promotingPawn;
	public int halfMove;
	public int fullMove;
	
	public ChessBoard () {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}

	public ChessBoard (String fen) {
		turn = Constants.BLACK;
		fenString = fen;
		halfMove = 0;
		fullMove = 1;
		
		attacks = new PieceSet[2][1];
		attacks[Constants.BLACK] = new PieceSet[64];
		attacks[Constants.WHITE] = new PieceSet[64];

		pieces = new PieceSet[2];
		pieces[Constants.BLACK] = new PieceSet();
		pieces[Constants.WHITE] = new PieceSet();
		
		kingPos = new int[2];
		pieceCount = new int[2][5];
		pieceCount[0] = new int[5];
		pieceCount[1] = new int[5];
		Arrays.fill(pieceCount[0], 0);
		Arrays.fill(pieceCount[1], 0);
		board = new ChessPiece[64];
		castling = new boolean[2][2]; //Black: Queenside, Kingside, White: Queenside, Kingside
		Arrays.fill(castling[Constants.BLACK], false);
		Arrays.fill(castling[Constants.WHITE], false);
		enPassant = -1;
		promotingPawn = -1;
		fen_to_board(fen);
		hardAttackUpdate();
		//System.out.println(board_to_fen());
	}
	
	private String board_to_fen() {
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
				fen += Constants.pieceToChar(board[pos]);
			}
			if (emptySpaces > 0) fen += Integer.valueOf(emptySpaces);
			if (i != 8) fen += "/";
		}
		
		fen = (turn == Constants.WHITE) ? fen + " w" : fen + " b";
		boolean whiteCanCastle = true;
		if (!castling[Constants.WHITE][0] && !castling[Constants.WHITE][1]) {
			fen += " -";
			whiteCanCastle = false;
		}
		if (castling[Constants.WHITE][1]) fen += " K";
		if (castling[Constants.WHITE][0]) fen += "Q";
		
		if (!castling[Constants.BLACK][0] && !castling[Constants.BLACK][1] && whiteCanCastle) fen += " -";
		if (castling[Constants.BLACK][1]) fen += "k";
		if (castling[Constants.BLACK][0]) fen += "q";
		
		if (enPassant == Constants.EMPTY) fen += " -";
		else {
			final int passant = (turn == Constants.BLACK) ? enPassant + 8 : enPassant - 8;
			fen += " " + Constants.indexToSquare(getColumn(passant), 8 - getRow(passant));
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
					turn = Constants.WHITE;
				}
				
				//Castling
				else if (letter == 'K') {
					castling[Constants.WHITE][1] = true;
				}
				else if (letter == 'Q') {
					castling[Constants.WHITE][0] = true;
				}
				else if (letter == 'k') {
					castling[Constants.BLACK][1] = true;
				}
				else if (letter == 'q') {
					castling[Constants.BLACK][0] = true;
				}
				
				else if ((int) letter <= 104 && (int) letter >= 97 && Character.isDigit(fen.charAt(i + 1))) {
					enPassant = Constants.squareToIndex(Character.toString(letter) + fen.charAt(i + 1));
					enPassant = (turn == Constants.BLACK) ? enPassant - 8 : enPassant + 8;
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
			final ChessPiece piece = Constants.charToPiece(letter, index, this, pieceIDs);
			board[index] = piece;
			pieces[piece.color].add(piece);
			
			switch (piece.type) {
				case Constants.PAWN: pieceCount[piece.color][Constants.PAWN] += 1;
					break;
				case Constants.KNIGHT: pieceCount[piece.color][Constants.KNIGHT] += 1;
					break;
				case Constants.BISHOP: pieceCount[piece.color][Constants.BISHOP] += 1;
					break;
				case Constants.ROOK: pieceCount[piece.color][Constants.ROOK] += 1;
					break;
				case Constants.QUEEN: pieceCount[piece.color][Constants.QUEEN] += 1;
					break;
				case Constants.KING: kingPos[piece.color] = index;
					break;
			}
			index++;
		}
	}
	
	public void make_move(Move move, boolean permanent) {
		long prevTime = System.currentTimeMillis();
		final ChessPiece piece = board[move.start];
		int castle = Constants.EMPTY;
		piece.pieceAttacks(true);

		if (move.type == Move.Type.ATTACK) {
			halfMove = -1;
			board[move.finish].pieceAttacks(true);
			updatePosition(board[move.finish], move.finish, true);
		}

		int passant = -1;
		if (piece.type == Constants.PAWN) {
			halfMove = -1;
			if (move.isSpecial()) {
				board[enPassant].pieceAttacks(true);
				updatePosition(board[enPassant], enPassant, true);
			}
			if (getDistanceVert(move.start, move.finish) == 2) {
				passant = move.finish;
			}
			if (getRow(move.finish) == Constants.PROMOTION_LINE[turn]) {
				promotingPawn = move.finish;
			}
		}
		
		if (piece.type == Constants.ROOK) {
			if (move.start == Constants.ROOK_POSITIONS[piece.color][0])
				castling[piece.color][0] = false;
			if (move.start == Constants.ROOK_POSITIONS[piece.color][1])
				castling[piece.color][1] = false;
		}
		
		if (piece.type == Constants.KING) {
			Arrays.fill(castling[piece.color], false);
			if (move.isSpecial()) {
				if (move.finish > move.start) {
					castle = Constants.ROOK_POSITIONS[turn][1];
					board[castle].pieceAttacks(true);
					updatePosition(board[castle], move.finish - 1, false);
					castle = move.finish - 1;
					board[Constants.ROOK_POSITIONS[turn][1]] = ChessPiece.empty();
				}
				else {
					castle = Constants.ROOK_POSITIONS[turn][0];
					board[castle].pieceAttacks(true);
					updatePosition(board[castle], move.finish + 1, false);
					castle = move.finish + 1;
					board[Constants.ROOK_POSITIONS[turn][0]] = ChessPiece.empty();
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
			if (turn == Constants.BLACK) fullMove ++;
			next_turn();
		}
		if (permanent) fenString = board_to_fen();
		ChessGame.timeMakeMove += System.currentTimeMillis() - prevTime;
	}

	public void undoMove(Move move, ChessPiece capturedPiece, Computer.BoardStorage store) {
		long prevTime = System.currentTimeMillis();
		if (!is_promote()) {
			halfMove = store.halfMove;
			if (turn == Constants.WHITE) fullMove --;
			next_turn();
		}

		fenString = store.fenString;
		castling[turn] = store.getCastling();
		enPassant = store.enPassant;

		final ChessPiece piece = board[move.finish];
		int castle = Constants.EMPTY;
		piece.pieceAttacks(true);

		if (piece.type == Constants.KING) {
			if (move.isSpecial()) {
				if (move.finish > move.start) {
					castle = move.finish - 1;
					board[castle].pieceAttacks(true);
					updatePosition(board[castle], Constants.ROOK_POSITIONS[turn][1], false);
					castle = Constants.ROOK_POSITIONS[turn][1];
					board[move.finish - 1] = ChessPiece.empty();
				}
				else {
					castle = move.finish + 1;
					board[castle].pieceAttacks(true);
					updatePosition(board[castle], Constants.ROOK_POSITIONS[turn][0], false);
					castle = Constants.ROOK_POSITIONS[turn][0];
					board[move.finish + 1] = ChessPiece.empty();
				}
			}
		}

		if (piece.type == Constants.PAWN && move.isSpecial()) {
			updatePosition(capturedPiece, enPassant, false);
			capturedPiece.pieceAttacks(false);
		}

		board[move.finish] = ChessPiece.empty();
		updatePosition(piece, move.start, false);

		if (move.type == Move.Type.ATTACK) {
			updatePosition(capturedPiece, move.finish, false);
			capturedPiece.pieceAttacks(false);
		}

		piece.pieceAttacks(false);
		softAttackUpdate(move, true);
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
		pieceCount[turn][Constants.PAWN] -= 1;
		board[promotingPawn].pieceAttacks(false);
		halfMove ++;
		if (turn == Constants.BLACK) fullMove ++;
		next_turn();
		promotingPawn = -1;
		fenString = board_to_fen();
	}

	public void unPromote(int pos, BoardStorage store) {
		halfMove --;
		if (turn == Constants.WHITE) fullMove --;
		next_turn();
		promotingPawn = pos;

		final ChessPiece piece = board[pos];

		piece.pieceAttacks(true);
		updatePosition(piece, promotingPawn, true);
		piece.type = Constants.PAWN;
		updatePosition(piece, promotingPawn, false);

		fenString = store.fenString;
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
		
		if (piece.type == Constants.KING)
			kingPos[piece.color] = newPos;
	}
	
	private void softAttackUpdate(Move move, boolean undoMove) {
		for (int color = 0; color < 2; color++) {
			for (final ChessPiece piece : pieces[color]) {
				long prevTime = System.currentTimeMillis();
				piece.softPieceUpdate(move, undoMove);
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
		if (halfMove >= 50) return Constants.DRAW;
		if (hasInsufficientMaterial()) return Constants.DRAW;
		for(ChessPiece piece : pieces[turn]) {
			final ArrayList<Move> moves = new ArrayList<Move>();
			piece.pieceMoves(moves);
			if(moves.size() > 0) {
				return Constants.PROGRESS;
			}
		}
		return isChecked(turn) ? Constants.WIN : Constants.DRAW;
	}
	
	private boolean hasInsufficientMaterial() {
		for (int color = 0; color < 2; color++) {
			if (pieceCount[color][Constants.PAWN] > 0 || pieceCount[color][Constants.KNIGHT] > 2 
				|| pieceCount[color][Constants.BISHOP] > 1 || pieceCount[color][Constants.ROOK] > 0 
				|| pieceCount[color][Constants.QUEEN] > 0) return false;
		}
		return true;
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

	public String getFenString() {
		return fenString;
	}

	public ChessPiece getPiece(int pos) {
		return board[pos];
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
		System.out.println(fenString);
		if (!valid) System.out.println("ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR");
	}

	public static int getEdge(int direction, int pos) {
		switch (direction) {
			case(9): return Constants.distFromEdge[pos][5];
			case(-9): return Constants.distFromEdge[pos][4];
			case(7): return Constants.distFromEdge[pos][7];
			case(-7): return Constants.distFromEdge[pos][6];
			case(8): return Constants.distFromEdge[pos][1];
			case(-8): return Constants.distFromEdge[pos][0];
			case(1): return Constants.distFromEdge[pos][3];
			case(-1): return Constants.distFromEdge[pos][2];
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