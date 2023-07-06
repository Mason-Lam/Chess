package Chess;

import java.util.HashSet;

public class ChessPiece {
	public byte type;
	public byte color;
	public int pos;
	public final int pieceID;
	private final ChessBoard board;
	private static int pieceIDCount = 0;
	
	public ChessPiece(byte type, byte color, int pos, ChessBoard board) {
		this.type = type;
		this.color = color;
		this.pos = pos;
		this.board = board;
		pieceID = pieceIDCount;
		pieceIDCount ++;
	}

	public MoveList piece_moves(boolean[] types) {
		long prevTime = System.currentTimeMillis();
		final MoveList moves = new MoveList(type);
		if (board.is_promote() || board.getTurn() != color) return moves;

		if (type == Constants.KING) {
			king(pos, color, types, moves);
			ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
			return moves;
		}

		if (board.doubleCheck(color)) return moves;

		if (type == Constants.PAWN) pawn(types, moves);
		else if (type == Constants.KNIGHT) knight(pos, color, types, moves);
		else if (type == Constants.BISHOP) bishop(pos, color, types, moves);
		else if (type == Constants.ROOK) rook(pos, color, types, moves);
		else if (type == Constants.QUEEN) queen(pos, color, types, moves);
		ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
		return moves;
	}
	
	private void pawn(boolean[] types, MoveList moves) {
		//Moves
		long prevTime = System.currentTimeMillis();
		if (types[0]) {
			int newPos = pos;
			for (int i = 0; i < 2; i++) {
				newPos = (color == Constants.WHITE) ? newPos - 8 : newPos + 8;
				if (!board.onBoard(newPos)) break;
				
				if (board.getPiece(newPos).isEmpty()) addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
				else break;
				
				if (board.getRow(pos) != Constants.PAWN_STARTS[color]) break;
			}
		}
		//Attacks
		if (types[1]) {
			for (int i = 0; i < 2; i++) {
				int newPos = pos + Constants.PAWN_DIAGONALS[color][i];
				if (!board.onBoard(newPos) || !board.onDiagonal(pos,newPos)) continue;
				if (!board.getPiece(newPos).isEmpty() && board.getPiece(newPos).color != color) 
					addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
			}
		}
		//EnPassant
		if (types[2]) {
			if (board.getEnPassant() == -1) return;
			if (Math.abs(board.getEnPassant() - pos) != 1 || board.getDistance(board.getEnPassant(), pos) != 1) return;
			int newPos = color == Constants.WHITE ? board.getEnPassant() - 8 : board.getEnPassant() + 8;
			if (board.getPiece(newPos).isEmpty()) addMove(moves, new Move(pos, newPos, Move.Type.SPECIAL));
		}
		ChessGame.timePawnGen += System.currentTimeMillis() - prevTime;
	}
	
	private void knight(int pos, int color, boolean[] types, MoveList moves) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.KNIGHT_MOVES.length; i++) {
			int newPos = pos + Constants.KNIGHT_MOVES[i];
			if (!board.onBoard(newPos) || !board.onL(pos, newPos)) continue;
			
			if (board.getPiece(newPos).isEmpty()) { 
				if (types[0])
					addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
			}
			else if (board.getPiece(newPos).color != color && types[1]) 
				addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
		}
		ChessGame.timeKnightGen += System.currentTimeMillis() - prevTime;
	}
	
	private void bishop(int pos, int color, boolean[] types, MoveList moves) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.DIAGONALS.length; i++) {
			int newPos = pos;
			while (true) {
				newPos += Constants.DIAGONALS[i];
				if (!board.onBoard(newPos) || !board.onDiagonal(pos, newPos)) break;
				

				if (board.getPiece(newPos).color == color) break;
				if (!board.getPiece(newPos).isEmpty()) {
					if (types[1])
						addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
					break;
				}
				if (types[0])
					addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
			}
		}
		ChessGame.timeBishopGen += System.currentTimeMillis() - prevTime;
	}
	
	private void rook(int pos, int color, boolean[] types, MoveList moves) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.STRAIGHT.length; i++) {
			int newPos = pos;
			while (true) {
				newPos += Constants.STRAIGHT[i];
				if (!board.onBoard(newPos) || (!board.onColumn(pos, newPos) && i < 2) || (!board.onRow(pos, newPos) && i >= 2)) break;
				
				if (board.getPiece(newPos).color == color) break;
				if (!board.getPiece(newPos).isEmpty()) {
					if (types[1])
						addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
					break;
				}
				if (types[0])
					addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
			}
		}
		ChessGame.timeRookGen += System.currentTimeMillis() - prevTime;
	}
	
	private void queen(int pos, int color, boolean[] types, MoveList moves) {
		rook(pos, color, types, moves);
		bishop(pos, color, types, moves);
	}
	
	private void king(int pos, int color, boolean[] types, MoveList moves) {
		long prevTime = System.currentTimeMillis();
		if (types[0] || types[1]) {
			for (int i = 0; i < Constants.KING_MOVES.length; i++) {
				int newPos = pos + Constants.KING_MOVES[i];
				if (!board.onBoard(newPos) || board.getDistance(pos, newPos) > 2) continue;
				if (board.getPiece(newPos).isEmpty()) {
					if (types[0])
						addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
				}
				else if (board.getPiece(newPos).color != color && types[1]) 
					addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
			}
		}
		//Castling
		if (types[2] && !board.isChecked(color)) {
			boolean canCastle = true;
			//Queenside
			if (board.getCastling(color)[0]) {
				for (int i = 1; i < 4; i++) {
					if (!(board.getPiece(pos - i).isEmpty() && (!board.isAttacked(pos - i, color) || i == 3))) {
						canCastle = false;
						break;
					}	
				}
				if (canCastle && board.getPiece(Constants.ROOK_POSITIONS[color][0]).type == Constants.ROOK)
					addMove(moves, new Move(pos, Constants.ROOK_POSITIONS[color][0] + 2, Move.Type.SPECIAL));
			}
			
			//Kingside
			canCastle = true;
			if (board.getCastling(color)[1]) {
				for (int i = 1; i < 3; i++) {
					if (!(board.getPiece(pos + i).isEmpty() && !board.isAttacked(pos + i, color))) {
						canCastle = false;
						break;
					}	
				}
				if (canCastle && board.getPiece(Constants.ROOK_POSITIONS[color][1]).type == Constants.ROOK)
					addMove(moves, new Move(pos, Constants.ROOK_POSITIONS[color][1] - 1, Move.Type.SPECIAL));
			}
		}
		ChessGame.timeKingGen += System.currentTimeMillis() - prevTime;
	}

	private void addMove(MoveList moves, Move move) {
		long prevTime = System.currentTimeMillis();
		if (validMove(move) || !Constants.CHECKS) {
			ChessGame.timeValidMove += System.currentTimeMillis() - prevTime;
			prevTime = System.currentTimeMillis();
			moves.add(move);
			ChessGame.timeValidPart += System.currentTimeMillis() - prevTime;
			return;
		}
		ChessGame.timeValidMove += System.currentTimeMillis() - prevTime;
	}

	private boolean validMove(Move move) {
		final ChessPiece piece = board.getPiece(move.getStart());
		if (piece.type == Constants.KING) {
			return kingCheck(move);
		}
		if (board.doubleCheck(piece.color)) return false;
		if (board.isChecked(piece.color)) {
			if (!stopsCheck(move)) {
				return false;
			}
		}
		return !isPinned(move);
	}
	
	private boolean kingCheck(Move move) {
		if (board.isAttacked(move.getFinish(), color)) return false;
		if (!board.isChecked(color)) return true;
		for (final ChessPiece attacker : board.getAttackers(move.start, color)) {
			if (attacker.type == Constants.QUEEN || attacker.type == Constants.BISHOP) {
				if (board.onSameDiagonal(move.getStart(), move.getFinish(), attacker.pos) && move.getFinish() != attacker.pos) return false;
			}
			if (attacker.type == Constants.QUEEN || attacker.type == Constants.ROOK) {
				if (board.onSameLine(move.getStart(), move.getFinish(), attacker.pos) && move.getFinish() != attacker.pos) return false;
			}
		}
		return true;
	}
	
	private boolean stopsCheck(Move move) {
		final int king = board.getKingPos(color);
		ChessPiece attacker = ChessPiece.empty();
		for (final ChessPiece piece : board.getAttackers(king, color)) attacker = piece;
		if (attacker.type == Constants.PAWN && move.isSpecial() && board.getEnPassant() == attacker.pos) return true;
		if (attacker.type == Constants.PAWN || attacker.type == Constants.KNIGHT) return move.getFinish() == attacker.pos;
		if (move.getFinish() == attacker.pos) return true;
		
		if (board.onDiagonal(king, attacker.pos)) {
			return board.blocksDiagonal(attacker.pos, king, move.getFinish());
		}
		return board.blocksLine(attacker.pos, king, move.getFinish());
	}
	
	private boolean isPinned(Move move) {
		final int king = board.getKingPos(color);
		if (!board.onDiagonal(king, move.getStart()) && !board.onLine(king, move.getStart())) return false;

		final boolean passant = (move.isSpecial() && board.getPiece(move.start).type == Constants.PAWN);
		final HashSet<ChessPiece> attackers;
		if (passant) {
			if (!board.isAttacked(move.getStart(), color) && !board.isAttacked(board.getEnPassant(), color)) return false;
			attackers = new HashSet<ChessPiece>() {
				{
					addAll(board.getAttackers(move.getStart(), color));
					addAll(board.getAttackers(board.getEnPassant(), color));
				}
			};
		}
		else {
			if (!board.isAttacked(move.getStart(), color)) return false;
			attackers = board.getAttackers(move.getStart(), color);
		}

		for (final ChessPiece piece : attackers) {
			if (piece.type == Constants.PAWN || piece.type == Constants.KNIGHT || piece.type == Constants.KING) continue;

			if (piece.type == Constants.BISHOP || piece.type == Constants.QUEEN) {
				int pinnedPos = move.start;
				if (passant && board.onDiagonal(piece.pos, board.getEnPassant())) pinnedPos = board.getEnPassant();
				if (board.blocksDiagonal(piece.pos, king, pinnedPos)) {
					if (board.onSameDiagonal(move.getFinish(), king, piece.pos) && pinnedPos == move.start) return false;

					int direction = Math.abs(piece.pos - move.getStart()) % 7 == 0 ? 7 : 9;
					if (piece.pos - pinnedPos > 0) direction *= -1;
					
					int newPos = pinnedPos;
					for (int j = 0; j < board.getDistanceVert(move.getStart(), king) - 1; j++) {
						newPos += direction;
						if (!board.getPiece(newPos).isEmpty()) return false;
					}
					return true;
				}
			}

			if (piece.type == Constants.ROOK || piece.type == Constants.QUEEN) {
				if (board.blocksLine(piece.pos, king, move.getStart())) {
					if (board.onSameLine(move.getFinish(), king, piece.pos)) return false;

					int direction = board.onColumn(move.getStart(), piece.pos) ? 8 : 1;
					if (piece.pos - move.getStart() > 0) direction *= -1;
					
					int newPos = move.getStart();
					for (int j = 0; j < board.getDistance(move.getStart(), king) - 1; j++) {
						newPos += direction;
						if (!board.getPiece(newPos).isEmpty() && !(passant && newPos == board.getEnPassant())) return false;
					}
					return true;
					}
				}
			}
		
		return false;
	}

	public void setPos(int newPos) {
		pos = newPos;
	}
	
	public boolean isEmpty() {
		return type == Constants.EMPTY;
	}
	
	public static ChessPiece empty() {
		return new ChessPiece(Constants.EMPTY, Constants.EMPTY, Constants.EMPTY, null);
	}
}
