package Chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class ChessBoard {
	
	public int turn;
	public String fenString;
	
	private ArrayList<ChessPiece> attackers;
	private int[][] attacks;
	
	private int[] kingPos;
	private HashMap<ChessPiece, Integer>[] diagonalAttackers;
	private HashMap<ChessPiece, Integer>[] straightAttackers;
	private HashMap<ChessPiece, Integer>[] piecePositions;
	
	private ChessPiece[] board;
	private boolean[][] castling;
	
	private int enPassant;
	private int promotingPawn;
	private boolean check;
	private boolean GAME_OVER;
	
	public ChessBoard () {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}
	
	@SuppressWarnings("unchecked")
	public ChessBoard (String fen) {
		turn = Constants.WHITE;
		fenString = fen;
		
		attacks = new int[2][1];
		attacks[Constants.BLACK] = new int[64];
		attacks[Constants.WHITE] = new int[64];
		attackers = new ArrayList<ChessPiece>();

		diagonalAttackers = new HashMap[2];
		straightAttackers = new HashMap[2];
		piecePositions = new HashMap[2];
		diagonalAttackers[Constants.BLACK] = new HashMap<ChessPiece,Integer>();
		diagonalAttackers[Constants.WHITE] = new HashMap<ChessPiece,Integer>();
		straightAttackers[Constants.BLACK] = new HashMap<ChessPiece,Integer>();
		straightAttackers[Constants.WHITE] = new HashMap<ChessPiece,Integer>();
		piecePositions[Constants.BLACK] = new HashMap<ChessPiece,Integer>();
		piecePositions[Constants.WHITE] = new HashMap<ChessPiece,Integer>();
		
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
	
	public ChessBoard copyBoard() {
		ChessBoard newBoard = new ChessBoard(new String(fenString));
		newBoard.enPassant = enPassant;
		newBoard.promotingPawn = promotingPawn;
		newBoard.GAME_OVER = GAME_OVER;
		return newBoard;
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
		
		fen = turn == Constants.WHITE ? fen + " w" : fen + " b";
		
		if (!castling[Constants.WHITE][0] && !castling[Constants.WHITE][1]) fen += " -";
		if (castling[Constants.WHITE][1]) fen += " K";
		if (castling[Constants.WHITE][0]) fen += "Q";
		
		if (!castling[Constants.BLACK][0] && !castling[Constants.BLACK][1]) fen += " -";
		if (castling[Constants.BLACK][1]) fen += "k";
		if (castling[Constants.BLACK][0]) fen += "q";
		
		if (enPassant == Constants.EMPTY) fen += " -";
		else {
			int passant = turn == Constants.BLACK ? enPassant + 8 : enPassant - 8;
			fen += " " + Constants.COLUMNS[getColumn(passant)] + getRow(passant);
		}
		
		return fen;
	}
	
	//Ex: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 -> starting position
	private void fen_to_board(String fen) {
		int index = 0;
		for (int i = 0; i < fen.length(); i++) {
			char letter = fen.charAt(i);
			if (letter == ' ') continue;
			
			//Turn, Castling, en Passant
			if (index > 63) {
				
				//Turn
				if (letter == 'b') {
					turn = Constants.BLACK;
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
					enPassant = ((int) letter - 97) + (8 - Character.getNumericValue(fen.charAt(i + 1))) * 8;
					enPassant = turn == Constants.BLACK ? enPassant - 8: enPassant + 8;
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
			int pieceValue = Character.getNumericValue(letter);
			if (pieceValue < 10 && pieceValue > 0) {
				for (int j = 0; j < pieceValue; j++) {
					board[index] = ChessPiece.empty();
					index ++;
				}
				continue;
			}
			ChessPiece piece = Constants.charToPiece(letter);
			board[index] = piece;
			piecePositions[piece.color].put(piece, index);
			
			if (piece.type == Constants.KING) 
				kingPos[piece.color] = index;
			if (piece.type == Constants.BISHOP || piece.type == Constants.QUEEN) 
				diagonalAttackers[piece.color].put(piece, index);
			if (piece.type == Constants.ROOK || piece.type == Constants.QUEEN) 
				straightAttackers[piece.color].put(piece, index);
			
			index++;
		}
	}
	
	public void make_move(Move move, boolean permanent) {
		ChessPiece piece = board[move.start];
		int passant = -1;
		attackers = new ArrayList<ChessPiece>();
		
		softAttackUpdate(move, -1);
		pieceAttacks(move.start, piece.color, -1);

		if (move.type == Move.Type.ATTACK) {
			pieceAttacks(move.finish, next(piece.color), -1);
			updatePosition(board[move.finish], move.finish, true);
		}
		
		board[move.start] = ChessPiece.empty();

		if (piece.type == Constants.PAWN) {
			if (move.isSpecial()) {
				pieceAttacks(enPassant, next(piece.color), -1);
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
		pieceAttacks(move.finish, piece.color, 1);
		
		softAttackUpdate(move, 1);
		check = isChecked(next(piece.color));
		enPassant = passant;
		if (!is_promote()) next_turn();
		if (permanent) fenString = board_to_fen();
	}
	
	private void updatePosition(ChessPiece piece, int newPos, boolean remove) {
		if (remove) {
			//pieceAttacks(newPos, piece.color, -1);
			piecePositions[piece.color].remove(piece);
			if (piece.type == Constants.BISHOP || piece.type == Constants.QUEEN)
				diagonalAttackers[piece.color].remove(piece);
			
			if (piece.type == Constants.ROOK || piece.type == Constants.QUEEN)
				straightAttackers[piece.color].remove(piece);
			board[newPos] = ChessPiece.empty();
			return;
		}
		board[newPos] = piece;
		//pieceAttacks(newPos, piece.color, 1);
		piecePositions[piece.color].put(piece, newPos);
		if (piece.type == Constants.BISHOP || piece.type == Constants.QUEEN)
			diagonalAttackers[piece.color].put(piece, newPos);
		
		if (piece.type == Constants.ROOK || piece.type == Constants.QUEEN)
			straightAttackers[piece.color].put(piece, newPos);
		
		if (piece.type == Constants.KING)
			kingPos[piece.color] = newPos;
	}
	
	public void piece_moves(int pos, boolean[] types, ArrayList<Move> moves) {
		if (is_promote() || GAME_OVER) return;
		boolean doubleCheck = doubleCheck(board[pos].color);
		if (board[pos].type == Constants.PAWN && !doubleCheck) pawn(pos, board[pos].color, types, moves);
		else if (board[pos].type == Constants.KNIGHT && !doubleCheck) knight(pos, board[pos].color, types, moves);
		else if (board[pos].type == Constants.BISHOP && !doubleCheck) bishop(pos,board[pos].color, types, moves);
		else if (board[pos].type == Constants.ROOK && !doubleCheck) rook(pos, board[pos].color, types, moves);
		else if (board[pos].type == Constants.QUEEN && !doubleCheck) queen(pos,board[pos].color, types, moves);
		else if (board[pos].type == Constants.KING) king(pos, board[pos].color, types, moves);
	}
	
	private void pawn(int pos, int color, boolean[] types, ArrayList<Move> moves) {
		//Moves
		if (types[0]) {
			int newPos = pos;
			for (int i = 0; i < 2; i++) {
				newPos = color == Constants.WHITE ? newPos - 8 : newPos + 8;
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
			if (Math.abs(enPassant - pos) != 1) return;
			int newPos = color == Constants.WHITE ? enPassant - 8 : enPassant + 8;
			if (board[newPos].isEmpty()) addMove(moves, new Move(pos, newPos, Move.Type.SPECIAL));
		}
	}
	
	private void knight(int pos, int color, boolean[] types, ArrayList<Move> moves) {
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
	
	private void bishop(int pos, int color, boolean[] types, ArrayList<Move> moves) {
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
	
	private void rook(int pos, int color, boolean[] types, ArrayList<Move> moves) {
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
	
	private void queen(int pos, int color, boolean[] types, ArrayList<Move> moves) {
		rook(pos, color, types, moves);
		bishop(pos, color, types, moves);
	}
	
	private void king(int pos, int color, boolean[] types, ArrayList<Move> moves) {
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
					if (!(board[pos - i].isEmpty() && !isAttacked(pos - i, color))) {
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
	
	private void addMove(ArrayList<Move> moves, Move move) {
		if (validMove(move) || !Constants.CHECKS) {
			moves.add(move);
		}
	}
	
	private boolean validMove(Move move) {
		ChessPiece piece = board[move.start];
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
		for (int i = 0; i < attackers.size(); i++) {
			ChessPiece attacker = attackers.get(i);
			int attackerPos = piecePositions[next(turn)].get(attacker);
			if (attacker.type == Constants.QUEEN || attacker.type == Constants.BISHOP) {
				if (onDiagonal(move.finish, attackerPos) && move.finish != attackerPos) return false;
			}
			if (attacker.type == Constants.QUEEN || attacker.type == Constants.ROOK) {
				if (onLine(move.finish, attackerPos) && move.finish != attackerPos) return false;
			}
		}
		return true;
	}
	
	private boolean stopsCheck(Move move) {
		ChessPiece attacker = attackers.get(0);
		int attackerPos = piecePositions[next(turn)].get(attacker);
		if (attacker.type == Constants.PAWN && move.isSpecial() && enPassant == attackerPos) return true;
		if (attacker.type == Constants.PAWN || attacker.type == Constants.KNIGHT) return move.finish == attackerPos;
		if (move.finish == attackerPos) return true;
		
		int king = kingPos[turn];
		if (attacker.type == Constants.QUEEN || attacker.type == Constants.BISHOP) {
			if (blocksDiagonal(attackerPos, king, move.finish))
				return true;
		}
		
		if (attacker.type == Constants.QUEEN || attacker.type == Constants.ROOK) {
			if (blocksLine(attackerPos, king, move.finish)) 
				return true;
		}
		
		return false;
	}
	
	private boolean isPinned(Move move) {
		if (!isAttacked(move.start, board[move.start].color) && !(move.isSpecial() && board[move.start].type == Constants.PAWN)) return false;
		int king = kingPos[board[move.start].color];
		//Checking if pinned by bishop or queen
		if (onDiagonal(move.start, king)) {
			for (Integer attacker : diagonalAttackers[next(board[move.start].color)].values()) {
				if (onSameDiagonal (move.start, king, attacker) && blocksDiagonal(attacker, king, move.start)) {
					if (move.finish == attacker) continue;
					
					if (onSameDiagonal(move.finish, king, attacker)) continue;

					int direction = Math.abs(attacker - move.start) % 7 == 0 ? 7 : 9;
					if (attacker - move.start > 0) direction *= -1;
					
					int newPos = attacker;
					boolean blocked = false;
					for (int j = 0; j < getDistanceVert(move.start, attacker) - 1; j++) {
						newPos += direction;
						if (!board[newPos].isEmpty()) {
							blocked = true;
							break;
						}
					}
					
					if (blocked) continue;
					
					newPos = move.start;
					for (int j = 0; j < getDistanceVert(move.start, king) - 1; j++) {
						newPos += direction;
						if (!board[newPos].isEmpty()) {
							blocked = true;
							break;
						}
					}
					
					if (!blocked) return true;
				}
			}
		}
		
		if (onLine(move.start, king)) {
			for (Integer attacker : straightAttackers[next(board[move.start].color)].values()) {
				if (onSameLine(move.start, king, attacker) && blocksLine(attacker, king, move.start)) {
					if (move.finish == attacker) continue;
					
					if (onSameLine(move.finish, king, attacker)) continue;

					int direction = onColumn(move.start, attacker) ? 8 : 1;
					if (attacker - move.start > 0) direction *= -1;
					
					int newPos = attacker;
					boolean blocked = false;
					for (int j = 0; j < getDistance(move.start, attacker) - 1; j++) {
						newPos += direction;
						if (!board[newPos].isEmpty()) {
							if (move.isSpecial() && newPos == enPassant) continue;
							blocked = true;
							break;
						}
					}
					
					if (blocked) continue;
					
					newPos = move.start;
					for (int j = 0; j < getDistance(move.start, king) - 1; j++) {
						newPos += direction;
						if (!board[newPos].isEmpty()) {
							if (move.isSpecial() && newPos == enPassant) continue;
							blocked = true;
							break;
						}
					}
					
					if (!blocked) return true;
				}
			}
		}
		
		return false;
	}
	
	private void hardAttackUpdate() {
		for (int color = 0; color < 2; color ++) {
			Arrays.fill(attacks[color], 0);
			var pieces =  piecePositions[color].values();
			for (Integer i : pieces) {
				pieceAttacks(i, color, 1);
			}
		}
	}
	
	private void softAttackUpdate(Move move, int amount) {
		boolean passant = move.isSpecial() && (board[move.start].type == Constants.PAWN || board[move.finish].type == Constants.PAWN);
		for (int color = 0; color < 2; color ++) {
			var straight = straightAttackers[color].values();
			var diagonal = diagonalAttackers[color].values();
			for (Integer i : straight) {
				if ((onLine(move.finish, i) || onLine(move.start, i) || (onLine(enPassant, i) && passant)) 
						&& !(i == move.start || i == move.finish)) {
					pieceAttacks(i, color, amount);
				}
			}
			
			for (Integer j : diagonal) {
				if ((onDiagonal(move.finish, j) || onDiagonal(move.start, j) || (onDiagonal(enPassant, j) && passant)) 
						&& !(j == move.start || j == move.finish)) {
					pieceAttacks(j, color, amount);
				}
			}
		}
	}
	
	private void pieceAttacks(int pos, int color, int amount) {
		ChessPiece piece = board[pos];
		int attackCount = numAttacks(kingPos[next(color)], next(color));
		if (piece.type == Constants.PAWN) pawnAttacks(pos, color, amount); 
		else if (piece.type == Constants.KNIGHT) knightAttacks(pos, color, amount); 
		else if (piece.type == Constants.BISHOP) bishopAttacks(pos, color, amount); 
		else if (piece.type == Constants.ROOK) rookAttacks(pos, color, amount); 
		else if (piece.type == Constants.QUEEN) queenAttacks(pos, color, amount); 
		else if (piece.type == Constants.KING) kingAttacks(pos, color, amount);
		if (numAttacks(kingPos[next(color)], next(color)) > attackCount && amount != -1) attackers.add(piece);
	}
	
	private void pawnAttacks(int pos, int color, int amount) {
		for (int i = 0; i < 2; i++) {
			int newPos = pos + Constants.PAWN_DIAGONALS[color][i];
			if (!onBoard(newPos) || !onDiagonal(pos,newPos)) continue;
			attacks[color][newPos] += amount;
		}
	}
	
	private void knightAttacks(int pos, int color, int amount) {
		for (int i = 0; i < Constants.KNIGHT_MOVES.length; i++) {
			int newPos = pos + Constants.KNIGHT_MOVES[i];
			if (!onBoard(newPos) || !onL(pos, newPos)) continue;
			attacks[color][newPos] += amount;
		}
	}
	
	private void bishopAttacks(int pos, int color, int amount) {
		for (int i = 0; i < Constants.DIAGONALS.length; i++) {
			int newPos = pos;
			while (true) {
				newPos += Constants.DIAGONALS[i];
				if (!onBoard(newPos) || !onDiagonal(pos, newPos)) break;
				
				attacks[color][newPos] += amount;
				if (!board[newPos].isEmpty()) break;
			}
		}
	}
	
	private void rookAttacks(int pos, int color, int amount) {
		for (int i = 0; i < Constants.STRAIGHT.length; i++) {
			int newPos = pos;
			while (true) {
				newPos += Constants.STRAIGHT[i];
				if (!onBoard(newPos) || (!onColumn(pos, newPos) && i < 2) || (!onRow(pos, newPos) && i >= 2)) break;
				
				attacks[color][newPos] += amount;
				if (!board[newPos].isEmpty()) break;
			}
		}
	}
	
	private void queenAttacks(int pos, int color, int amount) {
		rookAttacks(pos, color, amount);
		bishopAttacks(pos, color, amount);
	}
	
	private void kingAttacks(int pos, int color, int amount) {
		for (int i = 0; i < Constants.KING_MOVES.length; i++) {
			int newPos = pos + Constants.KING_MOVES[i];
			if (!onBoard(newPos) || getDistance(pos, newPos) > 2) continue;
			attacks[color][newPos] += amount;
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
		return attacks[next(color)][pos];
	}
	
	public Collection<Integer> getPiecePositions(int color) {
		return piecePositions[color].values();
	}
	
	public ChessPiece getPiece(int pos) {
		return board[pos];
	}
	
	public void promote(byte type) {
		board[promotingPawn].type = type;
		updatePosition(board[promotingPawn], promotingPawn, false);
		pieceAttacks(promotingPawn, turn, 1);
		check = isChecked(next(turn));
		next_turn();
		promotingPawn = -1;
		fenString = board_to_fen();
	}
	
	public int isWinner() {
		if (hasInsufficientMaterial()) return Constants.DRAW;
		var positions = piecePositions[turn].values();
		for(Integer i : positions) {
			ArrayList<Move> moves = new ArrayList<Move>();
			piece_moves(i, Constants.ALL_MOVES, moves);
			//System.out.println(moves.size());
			if(moves.size()>0) {
				return Constants.PROGRESS;
			}
		}
		GAME_OVER = true;
		return check ? Constants.WIN : Constants.DRAW;
	}
	
	private boolean hasInsufficientMaterial() {
		for (int color = 0; color < 2; color++) {
			HashMap<ChessPiece, Integer> pieces = piecePositions[color];
			if (pieces.size() > 3) return false;
			
			if (pieces.size() == 3) {
				int knightCount = 0;
				for (ChessPiece piece : pieces.keySet()) {
					if (piece.type == Constants.KNIGHT) knightCount ++;
				}
				if (knightCount != 2) return false;
			}
			
			if (pieces.size() == 2) {
				for (ChessPiece piece : pieces.keySet()) {
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
	
	public void displayAttacks() {
		boolean valid = true;
		for (int color = 0; color < 2; color++) {
			var colorAttacks = attacks[color];
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					int attacks = colorAttacks[i * 8 + j];
					if (attacks < 0) valid = false;
					System.out.print(attacks + " ");
				}
				System.out.println();
			}
			System.out.println();
		}
		System.out.println(check);
		if (!valid) System.out.println("ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR");
	}
	
	public Computer getComputer() {
		return new Computer(this);
	}
}