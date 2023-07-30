package Chess;

import java.util.ArrayList;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.ChessBoard.*;

public class ChessPiece {
	public byte type;
	public int pos;
	private boolean updatingCopy;
	private ChessPiece pinPiece;
	private ArrayList<Move> movesCopy;
	
	public final byte color;
	public final int pieceID;
	private final ChessBoard board;
	
	public ChessPiece(byte type, byte color, int pos, ChessBoard board, int pieceID) {
		this.type = type;
		this.pos = pos;
		this.color = color;
		this.board = board;
		this.pieceID = pieceID;

		updatingCopy = false;
		movesCopy = !isEmpty() ? new ArrayList<Move>(MAX_MOVES[type]) : null;
		pinPiece = null;
	}

	public void pieceAttacks(boolean remove) {
		reset();
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
			if (getEdge(PAWN_DIAGONALS[color][i], pos) < 1) continue;
			final int newPos = pos + PAWN_DIAGONALS[color][i];

			board.modifyAttacks(this, newPos, remove);
		}
		ChessGame.timePawnAttack += System.currentTimeMillis() - prevTime;
	}
	
	private void knightAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < KNIGHT_MOVES.length; i++) {
			final int newPos = pos + KNIGHT_MOVES[i];
			if (!onBoard(newPos) || !onL(pos, newPos)) continue;

			board.modifyAttacks(this, newPos, remove);
			if (board.getPiece(newPos).color != color && !remove) movesCopy.add(new Move(pos, newPos, false));
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
			addAttacksSliding(DIRECTIONS[i]);
		}
		ChessGame.timeSlidingAttack += System.currentTimeMillis() - prevTime;
	}
	
	private void kingAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < DIRECTIONS.length; i++) {
			if (getEdge(DIRECTIONS[i], pos) < 1) continue;
			final int newPos = pos + DIRECTIONS[i];

			board.modifyAttacks(this, newPos, remove);
			if (board.getPiece(newPos).color != color && !remove) movesCopy.add(new Move(pos, newPos, false));
		}
		ChessGame.timeKingAttack += System.currentTimeMillis() - prevTime;
	}

	public void softAttack(Move move, int index, boolean isAttack, boolean undoMove) {
		// if (pos == move.start || pos == move.finish) return;
		if (isPawn() || isKnight() || isKing()) return;

		if (board.isCastle(move) && (Math.abs(pos - move.finish) == 1 || Math.abs(pos - move.start) == 1)) return;

		reset();
		if (index == 0) {
			final int direction = onDiagonal(pos, move.start) ? getDiagonalOffset(pos, move.start) : getHorizontalOffset(pos, move.start);
			if (!(isAttack && undoMove)) addAttacks(direction, move.start);
			return;
		}

		if (index == 1) {
			final int direction = onDiagonal(pos, move.finish) ? getDiagonalOffset(pos, move.finish) : getHorizontalOffset(pos, move.finish);
			if (!(isAttack && !undoMove)) removeAttacks(direction, move.finish);
			return;
		}

		final int enPassant = board.getEnPassant();
		final int direction = onDiagonal(pos, enPassant) ? getDiagonalOffset(pos, enPassant) : getHorizontalOffset(pos, enPassant);
		if (undoMove) {
			removeAttacks(direction, enPassant);
		}
		else {
			addAttacks(direction, enPassant);
		}
	}

	private void addAttacksSliding(int direction) {
		final int distance = getEdge(direction, pos);
		for (int i = 1; i < distance + 1; i++) {
			final int newPos = pos + direction * i;
			board.addAttacker(this, newPos);
			if (board.getPiece(newPos).color != color) movesCopy.add(new Move(pos, newPos, false));

			if (!board.getPiece(newPos).isEmpty()) break;
		}
	}
	
	private void addAttacks(int direction, int startingPos) {
		addAttacks(direction, startingPos, getEdge(direction, startingPos));
	}

	private void removeAttacks(int direction, int startingPos) {
		removeAttacks(direction, startingPos, getEdge(direction, startingPos));
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

		if (isKing()) {
			updatingCopy = movesCopy.isEmpty();
			king(moves, attacksOnly);
			ChessGame.timeKingGen += System.currentTimeMillis() - prevTime;
			ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
			return;
		}

		if (board.doubleCheck(color)) return;

		updatingCopy = movesCopy.isEmpty();
		long prevTime2 = System.currentTimeMillis();
		switch (type) {
			case PAWN: 
				pawn(moves, attacksOnly);
				ChessGame.timePawnGen += System.currentTimeMillis() - prevTime2;
				break;
			case KNIGHT: 
				knight(moves, attacksOnly);
				ChessGame.timeKnightGen += System.currentTimeMillis() - prevTime2;
				break;
			default: 
				slidingMoves(moves, attacksOnly);
				ChessGame.timeSlidingGen += System.currentTimeMillis() - prevTime2;
				break;
		}
		pinPiece = null;
		ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
	}
	
	private void pawn(ArrayList<Move> moves, boolean attacksOnly) {
		if (board.getEnPassant() != EMPTY) {
			if (Math.abs(board.getEnPassant() - pos) == 1 && getDistance(board.getEnPassant(), pos) == 1) {
				final int newPos = (color == WHITE) ? board.getEnPassant() - 8 : board.getEnPassant() + 8;
				if (board.getPiece(newPos).isEmpty()) addMove(moves, new Move(pos, newPos, true), attacksOnly);
			}
		}

		if (!updatingCopy) {
			pinPiece = getPin();
			if (board.isChecked(color)) {
				checkMoves(moves, attacksOnly);
				return;
			}

			if (pinPiece.isEmpty()) {
				copy(moves, attacksOnly);
				return;
			}

			if (ChessBoard.onColumn(pinPiece.pos, pos)) {
				copyPawn(moves, attacksOnly);
				return;
			}

			if (ChessBoard.onRow(pinPiece.pos, pos)) {
				return;
			}

			checkPawn(moves);
			return;
		}
		
		final int direction = (color == WHITE) ? -8 : 8;
		if (board.getPiece(pos + direction).isEmpty()) {
			addMove(moves, new Move(pos, pos + direction, false), attacksOnly);
			if (getRow(pos) == PAWN_STARTS[color]) {
				if (board.getPiece(pos + direction * 2).isEmpty()) addMove(moves, new Move(pos, pos + direction * 2, false), attacksOnly);
			}
		}
		//Attacks
		for (int i = 0; i < 2; i++) {
			if (getEdge(PAWN_DIAGONALS[color][i], pos) < 1) continue;
			final int newPos = pos + PAWN_DIAGONALS[color][i];
			if (board.getPiece(newPos).color == board.next(color)) addMove(moves, new Move(pos, newPos, false), attacksOnly);
		}
	}
	
	private void knight(ArrayList<Move> moves, boolean attacksOnly) {
		pinPiece = getPin();
		if (!pinPiece.isEmpty()) return;

		if (!updatingCopy) {
			if (!board.isChecked(color)) {
				copy(moves, attacksOnly);
				return;
			}
			checkMoves(moves, attacksOnly);
			return;
		}

		for (int i = 0; i < KNIGHT_MOVES.length; i++) {
			final int newPos = pos + KNIGHT_MOVES[i];
			if (!onBoard(newPos) || !onL(pos, newPos)) continue;
			
			if (board.getPiece(newPos).color == color) continue;
			addMove(moves, new Move(pos, newPos, false), attacksOnly);
		}
	}
	
	private void king(ArrayList<Move> moves, boolean attacksOnly) {
		if (castleCheck(true)) {
			addMove(moves, new Move(pos, ROOK_POSITIONS[color][1] - 1, true), attacksOnly);
		}

		if (castleCheck(false)) {
			addMove(moves, new Move(pos, ROOK_POSITIONS[color][0] + 2, true), attacksOnly);
		}

		if (!updatingCopy) {
			if (!board.isChecked(color)) {
				copyKing(moves, attacksOnly);
				return;
			}
			checkMoves(moves, attacksOnly);
			return;
		}

		for (int i = 0; i < DIRECTIONS.length; i++) {
			if (getEdge(DIRECTIONS[i], pos) < 1) continue;
			final int newPos = pos + DIRECTIONS[i];
			if(board.getPiece(newPos).color == color) continue;

			addMove(moves, new Move(pos, newPos, false), attacksOnly);
		}
	}

	private boolean castleCheck(boolean kingSide) {
		if (board.isChecked(color)) return false;
		if (!board.getCastling(color)[kingSide ? 1 : 0]) return false;

		final int rookPos = ROOK_POSITIONS[color][kingSide ? 1 : 0];
		if (!board.getPiece(rookPos).isRook()) return false;

		final int distance = Math.abs(rookPos - pos);
		for (int i = 1; i < distance; i++) {
			if (!board.getPiece(kingSide ? pos + i : pos - i).isEmpty()) return false;
		}

		for (int i = 1; i < 3; i++) {
			if (board.isAttacked(kingSide ? pos + i : pos - i, color)) return false;
		}

		return true;
	}

	private void slidingMoves(ArrayList<Move> moves, boolean attacksOnly) {
		if (!updatingCopy) {
			pinPiece = getPin();
			if (!board.isChecked(color) && pinPiece.isEmpty()) {
				copy(moves, attacksOnly);
				return;
			}
			checkMoves(moves, attacksOnly);
			return;
		}

		final int startingIndex = isBishop() ? 4 : 0;
		final int endIndex = isRook() ? 4 : 8;
		for (int i = startingIndex; i < endIndex; i++) {
			for (int j = 1; j < getEdge(DIRECTIONS[i], pos) + 1; j++) {
				final int newPos = pos + DIRECTIONS[i] * j;
				final ChessPiece piece = board.getPiece(newPos);

				if (piece.color == color) break;
				
				addMove(moves, new Move(pos, newPos, false), attacksOnly);

				if (!piece.isEmpty()) break;
			}
		}
	}

	private void addMove(ArrayList<Move> moves, Move move, boolean attacksOnly) {
		long prevTime = System.currentTimeMillis();
		if (updatingCopy && !move.SPECIAL) {
			movesCopy.add(move);
		}
		if (attacksOnly) {
			if (board.getPiece(move.finish).isEmpty() && !board.isPassant(move)) return;
		}
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
				if (onSameDiagonal(move.start, move.finish, attacker.pos)) return false;
			}
			if (attacker.isQueen() || attacker.isRook()) {
				if (onSameLine(move.start, move.finish, attacker.pos)) return false;
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
		
		if (onDiagonal(king, attacker.pos)) {
			return blocksDiagonal(attacker.pos, king, move.finish);
		}
		return blocksLine(attacker.pos, king, move.finish);
	}
	
	private boolean isPinned(Move move) {
		pinPiece = getPin();
		final int king = board.getKingPos(color);

		if (board.isPassant(move)) {
			final PieceSet attackers = new PieceSet();
			final int enPassant = board.getEnPassant();
			if ((onDiagonal(king, enPassant) || onLine(king, enPassant)) && board.isAttacked(enPassant, color)) {
				attackers.addAll(board.getAttacks(enPassant, board.next(color)));
			}
			if (!pinPiece.isEmpty()) {
				attackers.add(pinPiece);
			}

			for (final ChessPiece piece : attackers) {
				if (piece.isPawn() || piece.isKnight() || piece.isKing()) continue;

				if (piece.isBishop() || piece.isQueen()) {
					final int pinnedPos = onDiagonal(piece.pos, board.getEnPassant()) ? board.getEnPassant() : move.start;
					if (blocksDiagonal(piece.pos, king, pinnedPos)) {
						if (onSameDiagonal(move.finish, king, piece.pos) && pinnedPos == move.start) return false;

						final int direction = getDiagonalOffset(piece.pos, pinnedPos);

						for (int j = 1; j < getDistanceVert(pinnedPos, king); j++) {
							if (!board.getPiece(pinnedPos + direction * j).isEmpty()) return false;
						}
						return true;
					}
				}

				if (piece.isRook() || piece.isQueen()) {
					if (blocksLine(piece.pos, king, move.start)) {
						if (onSameLine(move.finish, king, piece.pos)) return false;

						final int direction = getHorizontalOffset(piece.pos, move.start);

						for (int j = 1; j < getDistance(move.start, king); j++) {
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
			if (onDiagonal(pos, king)) return !onSameDiagonal(move.finish, king, pinPiece.pos);
			if (onLine(pos, king)) return !onSameLine(move.finish, king, pinPiece.pos);
			new Exception("Invalid Attacker");
			return true;
		}
	}

	private ChessPiece getPin() {
		if (pinPiece != null) return pinPiece;
		final int king = board.getKingPos(color);

		final boolean pinnedPiece = (onDiagonal(king, pos) || onLine(king, pos)) && board.isAttacked(this);
		if (!pinnedPiece) return empty();
		final PieceSet attackers = board.getAttackers(this);
		for (final ChessPiece attacker : attackers) {
			if (attacker.isPawn() || attacker.isKnight() || attacker.isKing()) continue;

			if (blocksDiagonal(attacker.pos, king, pos)) {

				final int direction = getDiagonalOffset(pos, king);

				for (int j = 1; j < getDistanceVert(pos, king); j++) {
					if (!board.getPiece(pos + direction * j).isEmpty()) return empty();
				}
				return attacker;
			}

			if (blocksLine(attacker.pos, king, pos)) {

				final int direction = getHorizontalOffset(pos, king);

				for (int j = 1; j < getDistance(pos, king); j++) {
					if (!board.getPiece(pos + direction * j).isEmpty()) return empty();
				}
				return attacker;
			}
		}
		return empty();
	}

	private void checkMoves(ArrayList<Move> moves, boolean attacksOnly) {
		for (int i = 0; i < movesCopy.size(); i++) {
			ChessGame.copyCount ++;
			addMove(moves, movesCopy.get(i), attacksOnly);
		}
	}

	private void checkPawn(ArrayList<Move> moves) {
		for (int i = 0; i < movesCopy.size(); i++) {
			ChessGame.copyCount ++;
			final Move move = movesCopy.get(i);
			if (move.finish == pinPiece.pos) {
				moves.add(move);
				return;
			}
		}
	}

	private void copy(ArrayList<Move> moves, boolean attacksOnly) {
		for (int i = 0; i < movesCopy.size(); i++) {
			ChessGame.copyCount ++;
			final Move move = movesCopy.get(i);
			if (attacksOnly) {
				if (board.getPiece(move.finish).isEmpty() && !board.isPassant(move)) continue;
			}
			moves.add(move);
		}
	}

	private void copyPawn(ArrayList<Move> moves, boolean attacksOnly) {
		if (attacksOnly) return;
		for (int i = 0; i < movesCopy.size(); i++) {
			ChessGame.copyCount ++;
			final Move move = movesCopy.get(i);
			if (ChessBoard.onColumn(move.start, move.finish)) moves.add(move);
		}
	}

	private void copyKing(ArrayList<Move> moves, boolean attacksOnly) {
		for (int i = 0; i < movesCopy.size(); i++) {
			ChessGame.copyCount ++;
			final Move move = movesCopy.get(i);
			if (attacksOnly) {
				if (board.getPiece(move.finish).isEmpty() && !board.isPassant(move)) continue;
			}
			if (CHECKS && board.isAttacked(move.finish, color)) continue;
			moves.add(move);
		}
	}

	public void updateCopy(boolean remove, int square) {
		final Move move = new Move(pos, square, false);
		if (remove) {
			movesCopy.remove(move);
			return;
		}
		movesCopy.add(move);
	}

	public void reset() {
		movesCopy = new ArrayList<Move>(MAX_MOVES[type]);
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

	public boolean hasCopy() {
		return movesCopy.size() > 0;
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
