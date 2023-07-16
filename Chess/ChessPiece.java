package Chess;

import java.util.ArrayList;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;

public class ChessPiece {
	public byte type;
	public int pos;
	private ChessPiece pinPiece;
	
	public final byte color;
	public final int pieceID;
	private final ChessBoard board;
	
	public ChessPiece(byte type, byte color, int pos, ChessBoard board, int pieceID) {
		this.type = type;
		this.pos = pos;
		this.color = color;
		this.board = board;
		this.pieceID = pieceID;

		pinPiece = null;
	}

	public void pieceAttacks(boolean remove) {
		switch(type) {
			case (PAWN): pawnAttacks(remove);
				break;
			case (KNIGHT): knightAttacks(remove);
				break;
			case (KING): kingAttacks(remove);
				break;
			default: slidingAttacks(remove);
				break;
		}
		//ChessGame.timeDebug += System.currentTimeMillis() - prevTime;
	}
	
	private void pawnAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < 2; i++) {
			if (ChessBoard.getEdge(PAWN_DIAGONALS[color][i], pos) < 1) continue;
			final int newPos = pos + PAWN_DIAGONALS[color][i];

			board.modifyAttacks(this, newPos, remove);
		}
		ChessGame.timePawnAttack += System.currentTimeMillis() - prevTime;
	}
	
	private void knightAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < KNIGHT_MOVES.length; i++) {
			final int newPos = pos + KNIGHT_MOVES[i];
			if (!ChessBoard.onBoard(newPos) || !ChessBoard.onL(pos, newPos)) continue;

			board.modifyAttacks(this, newPos, remove);
		}
		ChessGame.timeKnightAttack += System.currentTimeMillis() - prevTime;
	}

	private void slidingAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		final int startingIndex = isBishop() ? 4 : 0;
		final int endIndex = isRook() ? 4 : 8;
		for (int i = startingIndex; i < endIndex; i++) {
			if (remove) {
				removeAttacks(DIRECTIONS[i], pos);
				continue;
			}
			addAttacks(DIRECTIONS[i], pos);
		}
		ChessGame.timeSlidingAttack += System.currentTimeMillis() - prevTime;
	}
	
	private void kingAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < DIRECTIONS.length; i++) {
			if (ChessBoard.getEdge(DIRECTIONS[i], pos) < 1) continue;
			final int newPos = pos + DIRECTIONS[i];

			board.modifyAttacks(this, newPos, remove);
		}
		ChessGame.timeKingAttack += System.currentTimeMillis() - prevTime;
	}

	public void softAttack(Move move, boolean undoMove) {
		long prevTime = System.currentTimeMillis();
		if (pos == move.start || pos == move.finish) return;
		if (isPawn() || isKnight() || isKing()) return;

		if (board.isCastle(move) && (Math.abs(pos - move.finish) == 1 || Math.abs(pos - move.start) == 1)) return;
		
		final boolean attackingStart = board.getAttacks(move.start, color).contains(this);
		final boolean attackingEnd = board.getAttacks(move.finish, color).contains(this);

		if (attackingStart) {
			final int direction = ChessBoard.onDiagonal(pos, move.start) ? ChessBoard.getDiagonalOffset(pos, move.start) : ChessBoard.getHorizontalOffset(pos, move.start);
			if (!(move.type == Move.Type.ATTACK && undoMove)) addAttacks(direction, move.start);
		}
		if (attackingEnd) {
			final int direction = ChessBoard.onDiagonal(pos, move.finish) ? ChessBoard.getDiagonalOffset(pos, move.finish) : ChessBoard.getHorizontalOffset(pos, move.finish);
			if (!(move.type == Move.Type.ATTACK && !undoMove)) removeAttacks(direction, move.finish);
		}

		if (board.isPassant(move)) {
			final int enPassant = board.getEnPassant();
			if (!board.getAttacks(enPassant, color).contains(this)) return;

			final int direction = ChessBoard.onDiagonal(pos, enPassant) ? ChessBoard.getDiagonalOffset(pos, enPassant) : ChessBoard.getHorizontalOffset(pos, enPassant);
			if (undoMove) {
				removeAttacks(direction, enPassant);
			}
			else {
				addAttacks(direction, enPassant);
			}
		}
		ChessGame.timeSoftAttack += System.currentTimeMillis() - prevTime;
	}
	
	private void addAttacks(int direction, int startingPos) {
		addAttacks(direction, startingPos, ChessBoard.getEdge(direction, startingPos));
	}

	private void removeAttacks(int direction, int startingPos) {
		removeAttacks(direction, startingPos, ChessBoard.getEdge(direction, startingPos));
	}

	private void addAttacks(int direction, int startingPos, int distance) {
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = startingPos + direction * i;
			board.addAttacker(this, newPos);
			if (!board.getPiece(newPos).isEmpty()) break;
		}
	}

	private void removeAttacks(int direction, int startingPos, int distance) {
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = startingPos + direction * i;
			board.removeAttacker(this, newPos);
			if (!board.getPiece(newPos).isEmpty()) break;
		}
	}

	public void pieceMoves(ArrayList<Move> moves) {
		pieceMoves(moves, false);
	}

	public void pieceMoves(ArrayList<Move> moves, boolean attacksOnly) {
		long prevTime = System.currentTimeMillis();
		if (board.is_promote() || board.getTurn() != color) return;

		if (isKing()) {
			king(moves, attacksOnly);
			ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
			return;
		}

		if (board.doubleCheck(color)) return;

		switch (type) {
			case PAWN: pawn(moves, attacksOnly);
				break;
			case KNIGHT: knight(moves, attacksOnly);
				break;
			default: slidingMoves(moves, attacksOnly);
				break;
		}
		pinPiece = null;
		ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
	}
	
	private void pawn(ArrayList<Move> moves, boolean attacksOnly) {
		//Moves
		long prevTime = System.currentTimeMillis();
		final int direction = (color == WHITE) ? -8 : 8;
		if (board.getPiece(pos + direction).isEmpty()) {
			addMove(moves, new Move(pos, pos + direction, Move.Type.MOVE), attacksOnly);
			if (ChessBoard.getRow(pos) == PAWN_STARTS[color]) {
				if (board.getPiece(pos + direction * 2).isEmpty()) addMove(moves, new Move(pos, pos + direction * 2, Move.Type.MOVE), attacksOnly);
			}
		}
		//Attacks
		for (int i = 0; i < 2; i++) {
			if (ChessBoard.getEdge(PAWN_DIAGONALS[color][i], pos) < 1) continue;
			final int newPos = pos + PAWN_DIAGONALS[color][i];
			if (board.getPiece(newPos).color == board.next(color)) addMove(moves, new Move(pos, newPos, Move.Type.ATTACK), attacksOnly);
		}

		//EnPassant
		if (board.getEnPassant() == EMPTY) return;
		if (Math.abs(board.getEnPassant() - pos) != 1 || ChessBoard.getDistance(board.getEnPassant(), pos) != 1) return;
		final int newPos = (color == WHITE) ? board.getEnPassant() - 8 : board.getEnPassant() + 8;
		if (board.getPiece(newPos).isEmpty()) addMove(moves, new Move(pos, newPos, Move.Type.SPECIAL), attacksOnly);
		ChessGame.timePawnGen += System.currentTimeMillis() - prevTime;
	}
	
	private void knight(ArrayList<Move> moves, boolean attacksOnly) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < KNIGHT_MOVES.length; i++) {
			final int newPos = pos + KNIGHT_MOVES[i];
			if (!ChessBoard.onBoard(newPos) || !ChessBoard.onL(pos, newPos)) continue;
			
			final ChessPiece piece = board.getPiece(newPos);
			if (piece.color == color) continue;
			addMove(moves, new Move(pos, newPos, piece.isEmpty() ? Move.Type.MOVE : Move.Type.ATTACK), attacksOnly);
		}
		ChessGame.timeKnightGen += System.currentTimeMillis() - prevTime;
	}
	
	private void king(ArrayList<Move> moves, boolean attacksOnly) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < DIRECTIONS.length; i++) {
			if (ChessBoard.getEdge(DIRECTIONS[i], pos) < 1) continue;
			final int newPos = pos + DIRECTIONS[i];
			final ChessPiece piece = board.getPiece(newPos);
			if(piece.color == color) continue;

			addMove(moves, new Move(pos, newPos, piece.isEmpty() ? Move.Type.MOVE : Move.Type.ATTACK), attacksOnly);
		}
		//Castling (complete mess)
		if (!board.isChecked(color)) {
			boolean canCastle = true;
			//Queenside
			if (board.getCastling(color)[0]) {
				for (int i = 1; i < 4; i++) {
					if (!(board.getPiece(pos - i).isEmpty() && (!board.isAttacked(pos - i, color) || i == 3))) {
						canCastle = false;
						break;
					}	
				}
				if (canCastle && board.getPiece(ROOK_POSITIONS[color][0]).isRook())
					addMove(moves, new Move(pos, ROOK_POSITIONS[color][0] + 2, Move.Type.SPECIAL), attacksOnly);
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
				if (canCastle && board.getPiece(ROOK_POSITIONS[color][1]).isRook())
					addMove(moves, new Move(pos, ROOK_POSITIONS[color][1] - 1, Move.Type.SPECIAL), attacksOnly);
			}
		}
		ChessGame.timeKingGen += System.currentTimeMillis() - prevTime;
	}

	private void slidingMoves(ArrayList<Move> moves, boolean attacksOnly) {
		long prevTime = System.currentTimeMillis();
		final int startingIndex = isBishop() ? 4 : 0;
		final int endIndex = isRook() ? 4 : 8;
		for (int i = startingIndex; i < endIndex; i++) {
			for (int j = 1; j < ChessBoard.getEdge(DIRECTIONS[i], pos) + 1; j++) {
				final int newPos = pos + DIRECTIONS[i] * j;
				final ChessPiece piece = board.getPiece(newPos);

				if (piece.color == color) break;
				
				addMove(moves, new Move(pos, newPos, piece.isEmpty() ? Move.Type.MOVE : Move.Type.ATTACK), attacksOnly);
				if (!piece.isEmpty()) break;
			}
		}
		ChessGame.timeSlidingGen += System.currentTimeMillis() - prevTime;
	}

	private void addMove(ArrayList<Move> moves, Move move, boolean attacksOnly) {
		long prevTime = System.currentTimeMillis();
		if (attacksOnly && move.type != Move.Type.ATTACK) return;
		
		if (!CHECKS) {
			moves.add(move);
			return;
		}
		if (validMove(move)) {
			ChessGame.timeValidMove += System.currentTimeMillis() - prevTime;
			prevTime = System.currentTimeMillis();
			moves.add(move);
			ChessGame.timeValidPart += System.currentTimeMillis() - prevTime;
			return;
		}
		ChessGame.timeValidMove += System.currentTimeMillis() - prevTime;
	}

	private boolean validMove(Move move) {
		final ChessPiece piece = board.getPiece(move.start);
		if (piece.isKing()) {
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
		if (board.isAttacked(move.finish, color)) return false;
		if (!board.isChecked(color)) return true;
		for (final ChessPiece attacker : board.getAttackers(this)) {
			if (move.finish == attacker.pos) return true;
			if (attacker.isQueen() || attacker.isBishop()) {
				if (ChessBoard.onSameDiagonal(move.start, move.finish, attacker.pos)) return false;
			}
			if (attacker.isQueen() || attacker.isRook()) {
				if (ChessBoard.onSameLine(move.start, move.finish, attacker.pos)) return false;
			}
		}
		return true;
	}
	
	private boolean stopsCheck(Move move) {
		final int king = board.getKingPos(color);
		final ChessPiece attacker = board.getKingAttacker(color);
		if (board.isPassant(move) && board.getEnPassant() == attacker.pos) return true;
		if (attacker.isPawn() || attacker.isKnight()) return move.finish == attacker.pos;
		if (move.finish == attacker.pos) return true;
		
		if (ChessBoard.onDiagonal(king, attacker.pos)) {
			return ChessBoard.blocksDiagonal(attacker.pos, king, move.finish);
		}
		return ChessBoard.blocksLine(attacker.pos, king, move.finish);
	}
	
	private boolean isPinned(Move move) {
		if (pinPiece == null) pinPiece = getPin();
		final int king = board.getKingPos(color);

		if (board.isPassant(move)) {
			final PieceSet attackers = new PieceSet();
			final int enPassant = board.getEnPassant();
			if ((ChessBoard.onDiagonal(king, enPassant) || ChessBoard.onLine(king, enPassant)) && board.isAttacked(enPassant, color)) {
				attackers.addAll(board.getAttacks(enPassant, board.next(color)));
			}
			if (!pinPiece.isEmpty()) {
				attackers.add(pinPiece);
			}

			for (final ChessPiece piece : attackers) {
				if (piece.isPawn() || piece.isKnight() || piece.isKing()) continue;

				if (piece.isBishop() || piece.isQueen()) {
					final int pinnedPos = ChessBoard.onDiagonal(piece.pos, board.getEnPassant()) ? board.getEnPassant() : move.start;
					if (ChessBoard.blocksDiagonal(piece.pos, king, pinnedPos)) {
						if (ChessBoard.onSameDiagonal(move.finish, king, piece.pos) && pinnedPos == move.start) return false;

						final int direction = ChessBoard.getDiagonalOffset(piece.pos, pinnedPos);

						for (int j = 1; j < ChessBoard.getDistanceVert(pinnedPos, king); j++) {
							if (!board.getPiece(pinnedPos + direction * j).isEmpty()) return false;
						}
						return true;
					}
				}

				if (piece.isRook() || piece.isQueen()) {
					if (ChessBoard.blocksLine(piece.pos, king, move.start)) {
						if (ChessBoard.onSameLine(move.finish, king, piece.pos)) return false;

						final int direction = ChessBoard.getHorizontalOffset(piece.pos, move.start);

						for (int j = 1; j < ChessBoard.getDistance(move.start, king); j++) {
							final int newPos = move.start + direction * j;
							if (!board.getPiece(newPos).isEmpty() && newPos != board.getEnPassant()) return false;
						}
						return true;
					}
				}
			}
			return false;
		}
		else {
			if (pinPiece.isEmpty()) return false;
			if (ChessBoard.onDiagonal(pos, king)) return !ChessBoard.onSameDiagonal(move.finish, king, pinPiece.pos);
			if (ChessBoard.onLine(pos, king)) return !ChessBoard.onSameLine(move.finish, king, pinPiece.pos);
			new Exception("Invalid Attacker");
			return true;
		}
	}

	private ChessPiece getPin() {
		final int king = board.getKingPos(color);

		final boolean pinnedPiece = (ChessBoard.onDiagonal(king, pos) || ChessBoard.onLine(king, pos)) && board.isAttacked(this);
		if (!pinnedPiece) return empty();
		final PieceSet attackers = board.getAttackers(this);
		for (final ChessPiece attacker : attackers) {
			if (attacker.isPawn() || attacker.isKnight() || attacker.isKing()) continue;

			if (ChessBoard.blocksDiagonal(attacker.pos, king, pos)) {

				final int direction = ChessBoard.getDiagonalOffset(pos, king);

				for (int j = 1; j < ChessBoard.getDistanceVert(pos, king); j++) {
					if (!board.getPiece(pos + direction * j).isEmpty()) return empty();
				}
				return attacker;
			}

			if (ChessBoard.blocksLine(attacker.pos, king, pos)) {

				final int direction = ChessBoard.getHorizontalOffset(pos, king);

				for (int j = 1; j < ChessBoard.getDistance(pos, king); j++) {
					if (!board.getPiece(pos + direction * j).isEmpty()) return empty();
				}
				return attacker;
			}
		}
		return empty();
	}

	public void setPos(int newPos) {
		pos = newPos;
	}
	
	public boolean isEmpty() {
		return type == EMPTY;
	}

	public boolean isPawn() {
		return type == PAWN;
	}

	public boolean isKnight() {
		return type == KNIGHT;
	}

	public boolean isBishop() {
		return type == BISHOP;
	}

	public boolean isRook() {
		return type == ROOK;
	}

	public boolean isQueen() {
		return type == QUEEN;
	}

	public boolean isKing() {
		return type == KING;
	}

	public boolean isDiagonal() {
		return (isBishop() || isQueen());
	}

	public boolean isLine() {
		return (isRook() || isQueen());
	}

	@Override
	public boolean equals(Object anObject) {
		if (this == anObject) return true;
		if (anObject instanceof ChessPiece) {
			final ChessPiece piece = (ChessPiece) anObject;
			return ((piece.type == type) && (piece.color == color) && (piece.pos == pos) && (piece.pieceID == pieceID));
		}
		return false;
	}
	
	public static ChessPiece empty() {
		return new ChessPiece(EMPTY, EMPTY, EMPTY, null, EMPTY);
	}
}
