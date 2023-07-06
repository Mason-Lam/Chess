package Chess;

import java.util.Arrays;

import Chess.Computer.BoardStorage;

public class ChessBoard {
	

	private final PieceSet[][] attacks;
	
	private final int[] kingPos;
	private final PieceSet[] pieces;
	
	private final ChessPiece[] board;
	private final boolean[][] castling;
	
	private String fenString;
	private int turn;
	private int enPassant;
	private int promotingPawn;
	
	public ChessBoard () {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}

	public ChessBoard (String fen) {
		turn = Constants.BLACK;
		fenString = fen;
		
		attacks = new PieceSet[2][1];
		attacks[Constants.BLACK] = new PieceSet[64];
		attacks[Constants.WHITE] = new PieceSet[64];

		pieces = new PieceSet[2];
		pieces[Constants.BLACK] = new PieceSet();
		pieces[Constants.WHITE] = new PieceSet();
		
		kingPos = new int[2];
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
		
		if (!castling[Constants.WHITE][0] && !castling[Constants.WHITE][1]) fen += " -";
		if (castling[Constants.WHITE][1]) fen += " K";
		if (castling[Constants.WHITE][0]) fen += "Q";
		
		if (!castling[Constants.BLACK][0] && !castling[Constants.BLACK][1]) fen += " -";
		if (castling[Constants.BLACK][1]) fen += "k";
		if (castling[Constants.BLACK][0]) fen += "q";
		
		if (enPassant == Constants.EMPTY) fen += " -";
		else {
			final int passant = (turn == Constants.BLACK) ? enPassant + 8 : enPassant - 8;
			fen += " " + Constants.indexToSquare(getColumn(passant), 8 - getRow(passant));
		}
		
		return fen;
	}

	private void fen_to_board(String fen) {
		final int[] pieceIDs = new int[] {0, 0};
		int index = 0;
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
				
				//Halfmove clock
				//SKIPPED
				
				//Full move number
				//SKIPPED
				
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
			
			if (piece.type == Constants.KING) 
				kingPos[piece.color] = index;
			
			index++;
		}
	}
	
	public void make_move(Move move, boolean permanent) {
		long prevTime = System.currentTimeMillis();
		final ChessPiece piece = board[move.getStart()];
		final PieceSet[] updatePieces = softAttackUpdate(move);
		for (final PieceSet pieces : updatePieces) {
			for (final ChessPiece attacker : pieces) {
				pieceAttacks(attacker.pos, true);
			}
		}
		pieceAttacks(move.getStart(), true);

		if (move.getType() == Move.Type.ATTACK) {
			pieceAttacks(move.getFinish(), true);
			updatePosition(board[move.getFinish()], move.getFinish(), true);
		}
		
		board[move.getStart()] = ChessPiece.empty();

		int passant = -1;
		if (piece.type == Constants.PAWN) {
			if (move.isSpecial()) {
				pieceAttacks(enPassant, true);
				updatePosition(board[enPassant], enPassant, true);
			}
			if (getDistanceVert(move.getStart(), move.getFinish()) == 2) {
				passant = move.getFinish();
			}
			if (getRow(move.getFinish()) == Constants.PROMOTION_LINE[turn]) {
				promotingPawn = move.getFinish();
			}
		}
		
		if (piece.type == Constants.ROOK) {
			if (move.getStart() == Constants.ROOK_POSITIONS[piece.color][0])
				castling[piece.color][0] = false;
			if (move.getStart() == Constants.ROOK_POSITIONS[piece.color][1])
				castling[piece.color][1] = false;
		}
		
		if (piece.type == Constants.KING) {
			Arrays.fill(castling[piece.color], false);
			if (move.isSpecial()) {
				if (move.getFinish() > move.getStart()) {
					updatePosition(board[Constants.ROOK_POSITIONS[turn][1]], move.getFinish() - 1, false);
					board[Constants.ROOK_POSITIONS[turn][1]] = ChessPiece.empty();
				}
				else {
					updatePosition(board[Constants.ROOK_POSITIONS[turn][0]], move.getFinish() + 1, false);
					board[Constants.ROOK_POSITIONS[turn][0]] = ChessPiece.empty();
				}
			}
		}
		
		updatePosition(piece, move.getFinish(), false);
		pieceAttacks(move.getFinish(), false);

		for (final PieceSet pieces : updatePieces) {
			for (final ChessPiece attacker : pieces) {
				pieceAttacks(attacker.pos, false);
			}
		}

		enPassant = passant;
		if (!is_promote()) next_turn();
		if (permanent) fenString = board_to_fen();
		ChessGame.timeMakeMove += System.currentTimeMillis() - prevTime;
	}

	public void undoMove(Move move, ChessPiece capturedPiece, Computer.BoardStorage store) {
		long prevTime = System.currentTimeMillis();
		if (!is_promote()) next_turn();

		fenString = store.fenString;
		castling[turn] = store.getCastling();
		enPassant = store.enPassant;

		final ChessPiece piece = board[move.getFinish()];
		final PieceSet[] updatePieces = softAttackUpdate(move);
		for (final PieceSet pieces : updatePieces) {
			for (final ChessPiece attacker : pieces) {
				pieceAttacks(attacker.pos, true);
			}
		}
		pieceAttacks(move.getFinish(), true);

		if (piece.type == Constants.KING) {
			if (move.isSpecial()) {
				if (move.getFinish() > move.getStart()) {
					updatePosition(board[move.getFinish() - 1], Constants.ROOK_POSITIONS[turn][1], false);
					board[move.getFinish() - 1] = ChessPiece.empty();
				}
				else {
					updatePosition(board[move.getFinish() + 1], Constants.ROOK_POSITIONS[turn][0], false);
					board[move.getFinish() + 1] = ChessPiece.empty();
				}
			}
		}

		if (piece.type == Constants.PAWN && move.isSpecial()) {
			updatePosition(capturedPiece, enPassant, false);
			pieceAttacks(enPassant, false);
		}

		board[move.getFinish()] = ChessPiece.empty();
		updatePosition(piece, move.getStart(), false);

		if (move.getType() == Move.Type.ATTACK) {
			updatePosition(capturedPiece, move.getFinish(), false);
			pieceAttacks(move.getFinish(), false);
		}

		for (final PieceSet pieces : updatePieces) {
			for (final ChessPiece attacker : pieces) {
				pieceAttacks(attacker.pos, false);
			}
		}
		pieceAttacks(move.getStart(), false);
		
		promotingPawn = -1;
		ChessGame.timeUndoMove += System.currentTimeMillis() - prevTime;
	}
	
	private void updatePosition(ChessPiece piece, int newPos, boolean remove) {
		if (remove) {
			pieces[piece.color].remove(piece);
			board[newPos] = ChessPiece.empty();
			return;
		}
		board[newPos] = piece;
		piece.setPos(newPos);
		pieces[piece.color].add(piece);
		
		if (piece.type == Constants.KING)
			kingPos[piece.color] = newPos;
	}
	
	private void hardAttackUpdate() {
		for (int color = 0; color < 2; color ++) {
			for (int j = 0;  j < attacks[color].length; j++) {
				attacks[color][j] = new PieceSet();
			}
			for (final ChessPiece piece : pieces[color]) {
				pieceAttacks(piece.pos, false);
			}
		}
	}
	
	private PieceSet[] softAttackUpdate(Move move) {
		final boolean passant = move.isSpecial() && (board[move.getStart()].type == Constants.PAWN || board[move.getFinish()].type == Constants.PAWN);
		final PieceSet[] pieces = new PieceSet[2];
		pieces[0] = new PieceSet();
		pieces[1] = new PieceSet();
		for (int color = 0; color < 2; color ++) {
			for (final ChessPiece attacker : attacks[color][move.getStart()]) {
				if (attacker.type == Constants.BISHOP || attacker.type == Constants.ROOK || attacker.type == Constants.QUEEN) {
					if (attacker.pos != move.getFinish()) {
						pieces[color].add(attacker);
					}
				}
			}

			for (final ChessPiece attacker : attacks[color][move.getFinish()]) {
				if (attacker.type == Constants.BISHOP || attacker.type == Constants.ROOK || attacker.type == Constants.QUEEN) {
					if (attacker.pos != move.getStart()) {
						pieces[color].add(attacker);
					}
				}
			}

			if (passant) {
				for (final ChessPiece attacker : attacks[color][enPassant]) {
					if (attacker.type == Constants.BISHOP || attacker.type == Constants.ROOK || attacker.type == Constants.QUEEN) {
						if (attacker.pos != move.getFinish() && attacker.pos != move.getStart()) {
							pieces[color].add(attacker);
						}
					}
				}
			}
		}

		return pieces;
	}
	
	private void pieceAttacks(int pos, boolean remove) {
		final ChessPiece piece = board[pos];
		if (piece.type == Constants.PAWN) pawnAttacks(pos, remove); 
		else if (piece.type == Constants.KNIGHT) knightAttacks(pos, remove); 
		else if (piece.type == Constants.BISHOP) bishopAttacks(pos, remove);
		else if (piece.type == Constants.ROOK) rookAttacks(pos, remove); 
		else if (piece.type == Constants.QUEEN) queenAttacks(pos, remove); 
		else if (piece.type == Constants.KING) kingAttacks(pos, remove);
	}
	
	private void pawnAttacks(int pos, boolean remove) {
		final ChessPiece piece = board[pos];
		for (int i = 0; i < 2; i++) {
			int newPos = pos + Constants.PAWN_DIAGONALS[piece.color][i];
			if (!onBoard(newPos) || !onDiagonal(pos,newPos)) continue;

			if (remove) {
				attacks[piece.color][newPos].remove(piece);
				continue;
			}
			attacks[piece.color][newPos].add(piece);
		}
	}
	
	private void knightAttacks(int pos, boolean remove) {
		final ChessPiece piece = board[pos];
		for (int i = 0; i < Constants.KNIGHT_MOVES.length; i++) {
			int newPos = pos + Constants.KNIGHT_MOVES[i];
			if (!onBoard(newPos) || !onL(pos, newPos)) continue;

			if (remove) {
				attacks[piece.color][newPos].remove(piece);
				continue;
			}
			attacks[piece.color][newPos].add(piece);
		}
	}
	
	private void bishopAttacks(int pos, boolean remove) {
		final ChessPiece piece = board[pos];
		for (int i = 0; i < Constants.DIAGONALS.length; i++) {
			int newPos = pos;
			while (true) {
				newPos += Constants.DIAGONALS[i];
				if (!onBoard(newPos) || !onDiagonal(pos, newPos)) break;
				
				if (remove) {
					attacks[piece.color][newPos].remove(piece);
					continue;
				}
				attacks[piece.color][newPos].add(piece);

				if (!board[newPos].isEmpty()) break;
			}
		}
	}
	
	private void rookAttacks(int pos, boolean remove) {
		final ChessPiece piece = board[pos];
		for (int i = 0; i < Constants.STRAIGHT.length; i++) {
			int newPos = pos;
			while (true) {
				newPos += Constants.STRAIGHT[i];
				if (!onBoard(newPos) || (!onColumn(pos, newPos) && i < 2) || (!onRow(pos, newPos) && i >= 2)) break;
				
				if (remove) {
					attacks[piece.color][newPos].remove(piece);
					continue;
				}
				attacks[piece.color][newPos].add(piece);

				if (!board[newPos].isEmpty()) break;
			}
		}
	}
	
	private void queenAttacks(int pos, boolean remove) {
		rookAttacks(pos, remove);
		bishopAttacks(pos, remove);
	}
	
	private void kingAttacks(int pos, boolean remove) {
		final ChessPiece piece = board[pos];
		for (int i = 0; i < Constants.KING_MOVES.length; i++) {
			int newPos = pos + Constants.KING_MOVES[i];
			if (!onBoard(newPos) || getDistance(pos, newPos) > 2) continue;

			if (remove) {
				attacks[piece.color][newPos].remove(piece);
				continue;
			}
			attacks[piece.color][newPos].add(piece);
		}
	}
	
	public boolean blocksLine(int attacker, int target, int defender) {
		return onSameLine(attacker, target, defender) && getDistance(defender, target) < getDistance(attacker, target) &&
				getDistance(defender, attacker) < getDistance (target, attacker);
	}
	
	public boolean onSameLine(int pos1, int pos2, int pos3) {
		return (onColumn(pos1, pos2) && onColumn(pos1, pos3)) || (onRow(pos1, pos2) && onRow(pos1, pos3));
	}
	
	public boolean onLine(int pos1, int pos2) {
		return onRow(pos1, pos2) || onColumn(pos1, pos2);
	}
	
	public boolean onL(int pos1, int pos2) {
		return getDistance(pos1, pos2) == 3;
	}
	
	public boolean blocksDiagonal(int attacker, int target, int defender) {
		return onSameDiagonal(attacker, target, defender) && getDistance(defender, target) < getDistance(attacker, target) &&
				getDistance(defender, attacker) < getDistance (target, attacker);
	}
	
	public boolean onSameDiagonal(int pos1, int pos2, int pos3) {
		return onDiagonal(pos1, pos2) && onDiagonal(pos2, pos3) && onDiagonal(pos1, pos3);
	}
	
	public boolean onDiagonal(int pos1, int pos2) {
		return getDistanceVert(pos1, pos2) == getDistanceHor(pos1, pos2);
	}
	
	public boolean onColumn(int pos1, int pos2) {
		return getColumn(pos1) == getColumn(pos2);
	}
	
	public boolean onRow(int pos1, int pos2) {
		return getRow(pos1) == getRow(pos2);
	}
	
	public boolean onBoard(int pos) {
		return (pos >= 0 && pos <= 63);
	}
	
	public int getDistance(int pos1, int pos2) {
		return getDistanceHor(pos1, pos2) + getDistanceVert(pos1, pos2);
	}
	
	public int getDistanceHor(int pos1, int pos2) {
		return Math.abs(getColumn(pos1) - getColumn(pos2));
	}
	
	public int getDistanceVert(int pos1, int pos2) {
		return Math.abs(getRow(pos1) - getRow(pos2));
	}
	
	public int getRow(int pos) {
		return pos / 8;
	}
	
	public int getColumn(int pos) {
		return pos % 8;
	}
	
	public boolean doubleCheck(int color) {
		return numAttacks(kingPos[color], color) >= 2;
	}
	
	public boolean isChecked(int color) {
		return isAttacked(kingPos[color], color);
	}
	
	public boolean isAttacked(int pos, int color) {
		return numAttacks(pos,color) >= 1;
	}
	
	public int numAttacks(int pos, int color) {
		return attacks[next(color)][pos].size();
	}
	
	public PieceSet getPieces(int color) {
		return pieces[color];
	}
	
	public ChessPiece getPiece(int pos) {
		return board[pos];
	}

	public PieceSet getAttackers(int pos, int color) {
		return attacks[next(color)][pos];
	}

	public int getKingPos(int color) {
		return kingPos[color];
	}
	
	public void promote(byte type) {
		board[promotingPawn].type = type;
		updatePosition(board[promotingPawn], promotingPawn, false);
		pieceAttacks(promotingPawn, false);
		next_turn();
		promotingPawn = -1;
		fenString = board_to_fen();
	}

	public void unPromote(int pos, BoardStorage store) {
		next_turn();
		promotingPawn = pos;

		final ChessPiece piece = board[pos];

		pieceAttacks(promotingPawn, true);
		updatePosition(piece, promotingPawn, true);
		piece.type = Constants.PAWN;
		updatePosition(piece, promotingPawn, false);

		fenString = store.fenString;
	}
	
	public int isWinner() {
		if (hasInsufficientMaterial()) return Constants.DRAW;
		for(ChessPiece piece : pieces[turn]) {
			final MoveList moves = piece.piece_moves(Constants.ALL_MOVES);
			//System.out.println(moves.size());
			if(moves.size() > 0) {
				return Constants.PROGRESS;
			}
		}
		return isChecked(turn) ? Constants.WIN : Constants.DRAW;
	}
	
	private boolean hasInsufficientMaterial() {
		for (int color = 0; color < 2; color++) {
			final PieceSet colorPieces = pieces[color];
			if (colorPieces.size() > 3) return false;
			
			if (colorPieces.size() == 3) {
				int knightCount = 0;
				for (ChessPiece piece : colorPieces) {
					if (piece.type == Constants.KNIGHT) knightCount ++;
				}
				if (knightCount != 2) return false;
			}
			
			if (colorPieces.size() == 2) {
				for (ChessPiece piece : colorPieces) {
					if (piece.type != Constants.KNIGHT && piece.type != Constants.BISHOP && piece.type != Constants.KING) return false; 
				}
			}
		}
		return true;
	}
	
	public boolean is_promote() {
		return promotingPawn != -1;
	}
	
	public int next(int currTurn) {
		return (currTurn + 1) % 2;
	}
	
	public void next_turn() {
		turn = next(turn);
	}

	public String getFenString() {
		return fenString;
	}

	public int getEnPassant() {
		return enPassant;
	}

	public int getTurn() {
		return turn;
	}

	public boolean[] getCastling(int turn) {
		return castling[turn];
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

	public boolean debug() {
		boolean valid = true;
		for (int color = 0; color < 2; color++) {
			var colorAttacks = attacks[color];
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					int attacks = colorAttacks[i * 8 + j].size();
					if (attacks < 0) valid = false;
					//System.out.print(attacks + " ");
				}
				//System.out.println();
			}
			//System.out.println();
		}
		// System.out.println(check);
		// System.out.println(fenString);
		//if (!valid) System.out.println("ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR");
		return !valid;
	}
	
	public Computer getComputer() {
		return new Computer(this);
	}
}