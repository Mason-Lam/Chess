package Chess;

import java.util.Arrays;
import java.util.HashSet;

import Chess.Computer.BoardStorage;

public class ChessBoard {
	

	private final HashSet<ChessPiece>[][] attacks;
	
	private final int[] kingPos;
	private final HashSet<ChessPiece>[] pieces;
	
	private final ChessPiece[] board;
	private final boolean[][] castling;
	
	private String fenString;
	private int turn;
	private int enPassant;
	private int promotingPawn;
	private boolean check;
	private boolean GAME_OVER;
	
	public ChessBoard () {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}
	
	@SuppressWarnings("unchecked")
	public ChessBoard (String fen) {
		turn = Constants.BLACK;
		fenString = fen;
		
		attacks = new HashSet[2][1];
		attacks[Constants.BLACK] = new HashSet[64];
		attacks[Constants.WHITE] = new HashSet[64];

		pieces = new HashSet[2];
		pieces[Constants.BLACK] = new HashSet<ChessPiece>();
		pieces[Constants.WHITE] = new HashSet<ChessPiece>();
		
		kingPos = new int[2];
		board = new ChessPiece[64];
		castling = new boolean[2][2]; //Black: Queenside, Kingside, White: Queenside, Kingside
		Arrays.fill(castling[Constants.BLACK], false);
		Arrays.fill(castling[Constants.WHITE], false);
		enPassant = -1;
		promotingPawn = -1;
		fen_to_board(fen);
		hardAttackUpdate();
		check = isChecked(turn);
		GAME_OVER = false;
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
			final ChessPiece piece = Constants.charToPiece(letter, index);
			board[index] = piece;
			pieces[piece.color].add(piece);
			
			if (piece.type == Constants.KING) 
				kingPos[piece.color] = index;
			
			index++;
		}
	}
	
	public void make_move(Move move, boolean permanent) {
		final ChessPiece piece = board[move.start];
		final HashSet<ChessPiece> updatePieces = softAttackUpdate(move);
		for (ChessPiece i : updatePieces) {
			pieceAttacks(i.pos, true);
		}
		pieceAttacks(move.start, true);

		if (move.type == Move.Type.ATTACK) {
			pieceAttacks(move.finish, true);
			updatePosition(board[move.finish], move.finish, true);
		}
		
		board[move.start] = ChessPiece.empty();

		int passant = -1;
		if (piece.type == Constants.PAWN) {
			if (move.isSpecial()) {
				pieceAttacks(enPassant, true);
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
					updatePosition(board[Constants.ROOK_POSITIONS[turn][1]], move.finish - 1, false);
					board[Constants.ROOK_POSITIONS[turn][1]] = ChessPiece.empty();
				}
				else {
					updatePosition(board[Constants.ROOK_POSITIONS[turn][0]], move.finish + 1, false);
					board[Constants.ROOK_POSITIONS[turn][0]] = ChessPiece.empty();
				}
			}
		}
		
		updatePosition(piece, move.finish, false);
		pieceAttacks(move.finish, false);

		for (ChessPiece i : updatePieces) {
			pieceAttacks(i.pos, false);
		}

		check = isChecked(next(turn));
		enPassant = passant;
		if (!is_promote()) next_turn();
		if (permanent) fenString = board_to_fen();
	}

	public void undoMove(Move move, ChessPiece capturedPiece, Computer.BoardStorage store) {
		if (!is_promote()) next_turn();

		fenString = store.fenString;
		castling[turn] = store.getCastling();
		enPassant = store.enPassant;
		GAME_OVER = false;

		final ChessPiece piece = board[move.finish];
		final HashSet<ChessPiece> updatePieces = softAttackUpdate(move);
		for (ChessPiece i : updatePieces) {
			pieceAttacks(i.pos, true);
		}
		pieceAttacks(move.finish, true);

		if (piece.type == Constants.KING) {
			if (move.isSpecial()) {
				if (move.finish > move.start) {
					updatePosition(board[move.finish - 1], Constants.ROOK_POSITIONS[turn][1], false);
					board[move.finish - 1] = ChessPiece.empty();
				}
				else {
					updatePosition(board[move.finish + 1], Constants.ROOK_POSITIONS[turn][0], false);
					board[move.finish + 1] = ChessPiece.empty();
				}
			}
		}

		if (piece.type == Constants.PAWN && move.isSpecial()) {
			updatePosition(capturedPiece, enPassant, false);
			pieceAttacks(enPassant, false);
		}

		board[move.finish] = ChessPiece.empty();
		updatePosition(piece, move.start, false);

		if (move.type == Move.Type.ATTACK) {
			updatePosition(capturedPiece, move.finish, false);
			pieceAttacks(move.finish, false);
		}

		for (ChessPiece i : updatePieces) {
			pieceAttacks(i.pos, false);
		}
		pieceAttacks(move.start, false);
		
		check = isChecked(turn);
		promotingPawn = -1;
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
	
	public void piece_moves(int pos, boolean[] types, HashSet<Move> moves) {
		if (is_promote() || GAME_OVER) return;

		if (board[pos].type == Constants.KING) {
			king(pos, board[pos].color, types, moves);
			return;
		}

		if (doubleCheck(board[pos].color)) return;

		if (board[pos].type == Constants.PAWN) pawn(pos, board[pos].color, types, moves);
		else if (board[pos].type == Constants.KNIGHT) knight(pos, board[pos].color, types, moves);
		else if (board[pos].type == Constants.BISHOP) bishop(pos,board[pos].color, types, moves);
		else if (board[pos].type == Constants.ROOK) rook(pos, board[pos].color, types, moves);
		else if (board[pos].type == Constants.QUEEN) queen(pos,board[pos].color, types, moves);
	}
	
	private void pawn(int pos, int color, boolean[] types, HashSet<Move> moves) {
		//Moves
		if (types[0]) {
			int newPos = pos;
			for (int i = 0; i < 2; i++) {
				newPos = (color == Constants.WHITE) ? newPos - 8 : newPos + 8;
				if (!onBoard(newPos)) break;
				
				if (board[newPos].isEmpty()) addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
				else break;
				
				if (getRow(pos) != Constants.PAWN_STARTS[color]) break;
			}
		}
		//Attacks
		if (types[1]) {
			for (int i = 0; i < 2; i++) {
				int newPos = pos + Constants.PAWN_DIAGONALS[color][i];
				if (!onBoard(newPos) || !onDiagonal(pos,newPos)) continue;
				if (!board[newPos].isEmpty() && board[newPos].color != color) 
					addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
			}
		}
		//EnPassant
		if (types[2]) {
			if (enPassant == -1) return;
			if (Math.abs(enPassant - pos) != 1 || getDistance(enPassant, pos) != 1) return;
			int newPos = color == Constants.WHITE ? enPassant - 8 : enPassant + 8;
			if (board[newPos].isEmpty()) addMove(moves, new Move(pos, newPos, Move.Type.SPECIAL));
		}
	}
	
	private void knight(int pos, int color, boolean[] types, HashSet<Move> moves) {
		for (int i = 0; i < Constants.KNIGHT_MOVES.length; i++) {
			int newPos = pos + Constants.KNIGHT_MOVES[i];
			if (!onBoard(newPos) || !onL(pos, newPos)) continue;
			
			if (board[newPos].isEmpty()) { 
				if (types[0])
					addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
			}
			else if (board[newPos].color != color && types[1]) 
				addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
		}
	}
	
	private void bishop(int pos, int color, boolean[] types, HashSet<Move> moves) {
		for (int i = 0; i < Constants.DIAGONALS.length; i++) {
			int newPos = pos;
			while (true) {
				newPos += Constants.DIAGONALS[i];
				if (!onBoard(newPos) || !onDiagonal(pos, newPos)) break;
				

				if (board[newPos].color == color) break;
				if (!board[newPos].isEmpty()) {
					if (types[1])
						addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
					break;
				}
				if (types[0])
					addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
			}
		}
	}
	
	private void rook(int pos, int color, boolean[] types, HashSet<Move> moves) {
		for (int i = 0; i < Constants.STRAIGHT.length; i++) {
			int newPos = pos;
			while (true) {
				newPos += Constants.STRAIGHT[i];
				if (!onBoard(newPos) || (!onColumn(pos, newPos) && i < 2) || (!onRow(pos, newPos) && i >= 2)) break;
				
				if (board[newPos].color == color) break;
				if (!board[newPos].isEmpty()) {
					if (types[1])
						addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
					break;
				}
				if (types[0])
					addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
			}
		}
	}
	
	private void queen(int pos, int color, boolean[] types, HashSet<Move> moves) {
		rook(pos, color, types, moves);
		bishop(pos, color, types, moves);
	}
	
	private void king(int pos, int color, boolean[] types, HashSet<Move> moves) {
		if (types[0] || types[1]) {
			for (int i = 0; i < Constants.KING_MOVES.length; i++) {
				int newPos = pos + Constants.KING_MOVES[i];
				if (!onBoard(newPos) || getDistance(pos, newPos) > 2) continue;
				if (board[newPos].isEmpty()) {
					if (types[0])
						addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
				}
				else if (board[newPos].color != color && types[1]) 
					addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
			}
		}
		//Castling
		if (types[2] && !isChecked(color)) {
			boolean canCastle = true;
			//Queenside
			if (castling[color][0]) {
				for (int i = 1; i < 4; i++) {
					if (!(board[pos - i].isEmpty() && (!isAttacked(pos - i, color) || i == 3))) {
						canCastle = false;
						break;
					}	
				}
				if (canCastle && board[Constants.ROOK_POSITIONS[color][0]].type == Constants.ROOK)
					addMove(moves, new Move(pos, Constants.ROOK_POSITIONS[color][0] + 2, Move.Type.SPECIAL));
			}
			
			//Kingside
			canCastle = true;
			if (castling[color][1]) {
				for (int i = 1; i < 3; i++) {
					if (!(board[pos + i].isEmpty() && !isAttacked(pos + i, color))) {
						canCastle = false;
						break;
					}	
				}
				if (canCastle && board[Constants.ROOK_POSITIONS[color][1]].type == Constants.ROOK)
					addMove(moves, new Move(pos, Constants.ROOK_POSITIONS[color][1] - 1, Move.Type.SPECIAL));
			}
		}
	}
	
	private void addMove(HashSet<Move> moves, Move move) {
		if (validMove(move) || !Constants.CHECKS) {
			moves.add(move);
		}
	}
	
	private boolean validMove(Move move) {
		final ChessPiece piece = board[move.start];
		if (piece.type == Constants.KING) return kingCheck(move);
		if (doubleCheck(turn)) return false;
		if (isChecked(turn)) {
			if (stopsCheck(move)) {
				return !isPinned(move);
			}
			return false;
		}
		return !isPinned(move);
	}
	
	private boolean kingCheck(Move move) {
		if (isAttacked(move.finish, turn)) return false;
		if (!check) return true;
		for (final ChessPiece attacker : attacks[next(turn)][move.start]) {
			if (attacker.type == Constants.QUEEN || attacker.type == Constants.BISHOP) {
				if (onSameDiagonal(move.start, move.finish, attacker.pos) && move.finish != attacker.pos) return false;
			}
			if (attacker.type == Constants.QUEEN || attacker.type == Constants.ROOK) {
				if (onSameLine(move.start, move.finish, attacker.pos) && move.finish != attacker.pos) return false;
			}
		}
		return true;
	}
	
	private boolean stopsCheck(Move move) {
		ChessPiece attacker = ChessPiece.empty();
		for (final ChessPiece piece : attacks[next(turn)][kingPos[turn]]) attacker = piece;
		if (attacker.type == Constants.PAWN && move.isSpecial() && enPassant == attacker.pos) return true;
		if (attacker.type == Constants.PAWN || attacker.type == Constants.KNIGHT) return move.finish == attacker.pos;
		if (move.finish == attacker.pos) return true;
		
		final int king = kingPos[turn];
		if (onDiagonal(king, attacker.pos)) {
			return blocksDiagonal(attacker.pos, king, move.finish);
		}
		return blocksLine(attacker.pos, king, move.finish);
	}
	
	private boolean isPinned(Move move) {
		final int king = kingPos[board[move.start].color];
		if (!onDiagonal(king, move.start) && !onLine(king, move.start)) return false;

		final boolean passant = (move.isSpecial() && board[move.start].type == Constants.PAWN);
		final HashSet<ChessPiece> attackers;
		if (passant) {
			if (!isAttacked(move.start, turn) && !isAttacked(enPassant, turn)) return false;
			attackers = new HashSet<ChessPiece>() {
				{
					addAll(attacks[next(turn)][move.start]);
					addAll(attacks[next(turn)][enPassant]);
				}
			};
		}
		else {
			if (!isAttacked(move.start, turn)) return false;
			attackers = attacks[next(turn)][move.start];
		}

		for (final ChessPiece piece : attackers) {
			if (piece.type == Constants.PAWN || piece.type == Constants.KNIGHT || piece.type == Constants.KING) continue;

			if (piece.type == Constants.BISHOP || piece.type == Constants.QUEEN) {
				if (blocksDiagonal(piece.pos, king, move.start)) {
					if (onSameDiagonal(move.finish, king, piece.pos)) return false;

					int direction = Math.abs(piece.pos - move.start) % 7 == 0 ? 7 : 9;
					if (piece.pos - move.start > 0) direction *= -1;
					
					int newPos = move.start;
					for (int j = 0; j < getDistanceVert(move.start, king) - 1; j++) {
						newPos += direction;
						if (!board[newPos].isEmpty()) return false;
					}
					return true;
				}
			}

			if (piece.type == Constants.ROOK || piece.type == Constants.QUEEN) {
				if (blocksLine(piece.pos, king, move.start)) {
					if (onSameLine(move.finish, king, piece.pos)) return false;

					int direction = onColumn(move.start, piece.pos) ? 8 : 1;
					if (piece.pos - move.start > 0) direction *= -1;
					
					int newPos = move.start;
					for (int j = 0; j < getDistance(move.start, king) - 1; j++) {
						newPos += direction;
						if (!board[newPos].isEmpty() && !(passant && newPos == enPassant)) return false;
					}
					return true;
					}
				}
			}
		
		return false;
	}
	
	private void hardAttackUpdate() {
		for (int color = 0; color < 2; color ++) {
			for (int j = 0;  j < attacks[color].length; j++) {
				attacks[color][j] = new HashSet<ChessPiece>();
			}
			for (final ChessPiece piece : pieces[color]) {
				pieceAttacks(piece.pos, false);
			}
		}
	}
	
	private HashSet<ChessPiece> softAttackUpdate(Move move) {
		final boolean passant = move.isSpecial() && (board[move.start].type == Constants.PAWN || board[move.finish].type == Constants.PAWN);
		final HashSet<ChessPiece> pieces = new HashSet<ChessPiece>();
		for (int color = 0; color < 2; color ++) {
			for (final ChessPiece attacker : attacks[color][move.start]) {
				if (attacker.type == Constants.BISHOP || attacker.type == Constants.ROOK || attacker.type == Constants.QUEEN) {
					if (attacker.pos != move.finish) {
						pieces.add(attacker);
					}
				}
			}

			for (final ChessPiece attacker : attacks[color][move.finish]) {
				if (attacker.type == Constants.BISHOP || attacker.type == Constants.ROOK || attacker.type == Constants.QUEEN) {
					if (attacker.pos != move.start) {
						pieces.add(attacker);
					}
				}
			}

			if (passant) {
				for (final ChessPiece attacker : attacks[color][enPassant]) {
					if (attacker.type == Constants.BISHOP || attacker.type == Constants.ROOK || attacker.type == Constants.QUEEN) {
						if (attacker.pos != move.finish && attacker.pos != move.start) {
							pieces.add(attacker);
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
	
	private boolean blocksLine(int attacker, int target, int defender) {
		return onSameLine(attacker, target, defender) && getDistance(defender, target) < getDistance(attacker, target) &&
				getDistance(defender, attacker) < getDistance (target, attacker);
	}
	
	private boolean onSameLine(int pos1, int pos2, int pos3) {
		return (onColumn(pos1, pos2) && onColumn(pos1, pos3)) || (onRow(pos1, pos2) && onRow(pos1, pos3));
	}
	
	private boolean onLine(int pos1, int pos2) {
		return onRow(pos1, pos2) || onColumn(pos1, pos2);
	}
	
	private boolean onL(int pos1, int pos2) {
		return getDistance(pos1, pos2) == 3;
	}
	
	private boolean blocksDiagonal(int attacker, int target, int defender) {
		return onSameDiagonal(attacker, target, defender) && getDistance(defender, target) < getDistance(attacker, target) &&
				getDistance(defender, attacker) < getDistance (target, attacker);
	}
	
	private boolean onSameDiagonal(int pos1, int pos2, int pos3) {
		return onDiagonal(pos1, pos2) && onDiagonal(pos2, pos3) && onDiagonal(pos1, pos3);
	}
	
	private boolean onDiagonal(int pos1, int pos2) {
		return getDistanceVert(pos1, pos2) == getDistanceHor(pos1, pos2);
	}
	
	private boolean onColumn(int pos1, int pos2) {
		return getColumn(pos1) == getColumn(pos2);
	}
	
	private boolean onRow(int pos1, int pos2) {
		return getRow(pos1) == getRow(pos2);
	}
	
	private boolean onBoard(int pos) {
		return (pos >= 0 && pos <= 63);
	}
	
	private int getDistance(int pos1, int pos2) {
		return getDistanceHor(pos1, pos2) + getDistanceVert(pos1, pos2);
	}
	
	private int getDistanceHor(int pos1, int pos2) {
		return Math.abs(getColumn(pos1) - getColumn(pos2));
	}
	
	private int getDistanceVert(int pos1, int pos2) {
		return Math.abs(getRow(pos1) - getRow(pos2));
	}
	
	public int getRow(int pos) {
		return pos / 8;
	}
	
	public int getColumn(int pos) {
		return pos % 8;
	}
	
	private boolean doubleCheck(int color) {
		return numAttacks(kingPos[color], color) >= 2;
	}
	
	private boolean isChecked(int color) {
		return isAttacked(kingPos[color], color);
	}
	
	private boolean isAttacked(int pos, int color) {
		return numAttacks(pos,color) >= 1;
	}
	
	private int numAttacks(int pos, int color) {
		return attacks[next(color)][pos].size();
	}
	
	public HashSet<ChessPiece> getPieces(int color) {
		return pieces[color];
	}
	
	public ChessPiece getPiece(int pos) {
		return board[pos];
	}
	
	public void promote(byte type) {
		board[promotingPawn].type = type;
		updatePosition(board[promotingPawn], promotingPawn, false);
		pieceAttacks(promotingPawn, false);
		next_turn();
		check = isChecked(turn);
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
			final HashSet<Move> moves = new HashSet<Move>();
			piece_moves(piece.pos, Constants.ALL_MOVES, moves);
			//System.out.println(moves.size());
			if(moves.size() > 0) {
				return Constants.PROGRESS;
			}
		}
		GAME_OVER = true;
		return check ? Constants.WIN : Constants.DRAW;
	}
	
	private boolean hasInsufficientMaterial() {
		for (int color = 0; color < 2; color++) {
			final HashSet<ChessPiece> colorPieces = pieces[color];
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
		GAME_OVER = true;
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
		System.out.println(check);
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