package Chess;

import java.util.ArrayList;

public class ChessPiece {
	public byte type;
	public int pos;
	
	public final byte color;
	public final int pieceID;
	private final ChessBoard board;
	
	public ChessPiece(byte type, byte color, int pos, ChessBoard board, int pieceID) {
		this.type = type;
		this.color = color;
		this.pos = pos;
		this.board = board;
		this.pieceID = pieceID;
	}

	public void pieceAttacks(boolean remove) {
		switch(type) {
			case (Constants.PAWN): pawnAttacks(remove);
				break;
			case (Constants.KNIGHT): knightAttacks(remove);
				break;
			case (Constants.BISHOP): bishopAttacks(remove);
				break;
			case (Constants.ROOK): rookAttacks(remove);
				break;
			case (Constants.QUEEN): queenAttacks(remove);
				break;
			case (Constants.KING): kingAttacks(remove);
				break;
		}
		//ChessGame.timeDebug += System.currentTimeMillis() - prevTime;
	}
	
	private void pawnAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < 2; i++) {
			if (ChessBoard.getEdge(Constants.PAWN_DIAGONALS[color][i], pos) < 1) continue;
			final int newPos = pos + Constants.PAWN_DIAGONALS[color][i];

			if (remove) {
				board.removeAttacker(this, newPos);
				continue;
			}
			board.addAttacker(this, newPos);
		}
		ChessGame.timePawnAttack += System.currentTimeMillis() - prevTime;
	}
	
	private void knightAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.KNIGHT_MOVES.length; i++) {
			final int newPos = pos + Constants.KNIGHT_MOVES[i];
			if (!ChessBoard.onBoard(newPos) || !ChessBoard.onL(pos, newPos)) continue;

			if (remove) {
				board.removeAttacker(this, newPos);
				continue;
			}
			board.addAttacker(this, newPos);
		}
		ChessGame.timeKnightAttack += System.currentTimeMillis() - prevTime;
	}
	
	private void bishopAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.DIAGONALS.length; i++) {
			if (remove) {
				removeAttacks(Constants.DIAGONALS[i], pos);
				continue;
			}
			addAttacks(Constants.DIAGONALS[i], pos);
		}
		ChessGame.timeBishopAttack += System.currentTimeMillis() - prevTime;
	}
	
	private void rookAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.STRAIGHT.length; i++) {
			if (remove) {
				removeAttacks(Constants.STRAIGHT[i], pos);
				continue;
			}
			addAttacks(Constants.STRAIGHT[i], pos);
		}
		ChessGame.timeRookAttack += System.currentTimeMillis() - prevTime;
	}
	
	private void queenAttacks(boolean remove) {
		rookAttacks(remove);
		bishopAttacks(remove);
	}
	
	private void kingAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.KING_MOVES.length; i++) {
			if (ChessBoard.getEdge(Constants.KING_MOVES[i], pos) < 1) continue;
			final int newPos = pos + Constants.KING_MOVES[i];

			if (remove) {
				board.removeAttacker(this, newPos);
				continue;
			}
			board.addAttacker(this, newPos);
		}
		ChessGame.timeKingAttack += System.currentTimeMillis() - prevTime;
	}

	public void softPieceUpdate(Move move, boolean undoMove) {
		if (pos == move.start || pos == move.finish) return;
		long prevTime = System.currentTimeMillis();
		switch (type) {
			case Constants.QUEEN: 
				softQueenUpdate(move, undoMove);
				break;
			case Constants.ROOK:
				softRookUpdate(move, undoMove);
				break;
			case Constants.BISHOP:
				softBishopUpdate(move, undoMove);
				break;
			default:
				break;
		};
		ChessGame.timeSoftAttack += System.currentTimeMillis() - prevTime;
	}

	private void softQueenUpdate(Move move, boolean undoMove) {
		softBishopUpdate(move, undoMove);
		softRookUpdate(move, undoMove);
	}

	private void softBishopUpdate(Move move, boolean undoMove) {
		long prevTime = System.currentTimeMillis();
		move = undoMove ? move.invert() : move;
		final boolean[] attacks = getSoftAttacks(move, undoMove);
		if (!attacks[0] && !attacks[1] && !attacks[2]) {
			ChessGame.timeSoftBishop += System.currentTimeMillis() - prevTime;
			return;
		}

		if (ChessBoard.onSameDiagonal(move.start, move.finish, pos)) {
			if (attacks[0] && attacks[1]) {
				if (!attacks[4]) removeAttacks(ChessBoard.getDiagonalOffset(move.finish, move.start), move.finish);
			}
			else {
				if (!attacks[3] && !attacks[4]) addAttacks(ChessBoard.getDiagonalOffset(move.start, move.finish), move.start);
			}
		}

		else {
			if (ChessBoard.onDiagonal(move.start, pos) && attacks[0]) {
				if (!attacks[3]) addAttacks(ChessBoard.getDiagonalOffset(pos, move.start), move.start);
			}
			if (ChessBoard.onDiagonal(move.finish, pos) && attacks[1]) {
				if (!attacks[4]) removeAttacks(ChessBoard.getDiagonalOffset(pos, move.finish), move.finish);
			}
		}

		if (attacks[2] && ChessBoard.onDiagonal(board.getEnPassant(), pos)) {
			final int enPassant = board.getEnPassant();
			if (undoMove) {
				removeAttacks(ChessBoard.getDiagonalOffset(pos, enPassant), enPassant);
			}
			else {
				addAttacks(ChessBoard.getDiagonalOffset(pos, enPassant), enPassant);
			}
		}
		ChessGame.timeSoftBishop += System.currentTimeMillis() - prevTime;
	}

	private void softRookUpdate(Move move, boolean undoMove) {
		long prevTime = System.currentTimeMillis();
		move = undoMove ? move.invert() : move;

		if (move.isSpecial() && board.getPiece(move.finish).type == Constants.KING && (Math.abs(pos - move.finish) == 1 || Math.abs(pos - move.start) == 1)) return;

		final boolean[] attacks = getSoftAttacks(move, undoMove);
		if (!attacks[0] && !attacks[1] && !attacks[2]) {
			ChessGame.timeSoftRook += System.currentTimeMillis() - prevTime;
			return;
		}

		if (ChessBoard.onSameLine(move.start, move.finish, pos)) {
			if (attacks[0] && attacks[1]) {
				if (!attacks[4]) removeAttacks(ChessBoard.getHorizontalOffset(move.finish, move.start), move.finish);
			}
			else {
				if (!attacks[3] && !attacks[4]) addAttacks(ChessBoard.getHorizontalOffset(move.start, move.finish), move.start);
			}
		}

		else {
			if (ChessBoard.onLine(move.start, pos) && attacks[0]) {
				if (!attacks[3]) addAttacks(ChessBoard.getHorizontalOffset(pos, move.start), move.start);
			}

			if (ChessBoard.onLine(move.finish, pos) && attacks[1]) {
				if (!attacks[4]) removeAttacks(ChessBoard.getHorizontalOffset(pos, move.finish), move.finish);
			}
		}

		if (attacks[2] && ChessBoard.onLine(board.getEnPassant(), pos)) {
			final int enPassant = board.getEnPassant();
			if (undoMove) {
				removeAttacks(ChessBoard.getHorizontalOffset(pos, enPassant), enPassant);
			}
			else {
				addAttacks(ChessBoard.getHorizontalOffset(pos, enPassant), enPassant);
			}
		}
		ChessGame.timeSoftRook += System.currentTimeMillis() - prevTime;
	}

	private boolean[] getSoftAttacks(Move move, boolean undoMove) {
		boolean attackingPassant = false;
		if (move.isSpecial() && (board.getPiece(move.finish).type == Constants.PAWN)) {
			attackingPassant = board.getAttacks(board.getEnPassant(), color).contains(this);
		}
		final boolean[] attacks = new boolean[5];
		attacks[0] = board.getAttacks(move.start, color).contains(this);
		attacks[1] = board.getAttacks(move.finish, color).contains(this);
		attacks[2] = attackingPassant;
		attacks[3] = move.type == Move.Type.ATTACK && undoMove && attacks[0]; //undoAttack
		attacks[4] = move.type == Move.Type.ATTACK && !undoMove && attacks[1]; //moveAttack
		return attacks;
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
		pieceMoves(moves, Constants.ALL_MOVES);
	}

	public void pieceMoves(ArrayList<Move> moves, boolean[] types) {
		long prevTime = System.currentTimeMillis();
		if (board.is_promote() || board.getTurn() != color) return;

		if (type == Constants.KING) {
			king(types, moves);
			ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
			return;
		}

		if (board.doubleCheck(color)) return;

		switch (type) {
			case Constants.PAWN: pawn(types, moves);
				break;
			case Constants.KNIGHT: knight(types, moves);
				break;
			case Constants.BISHOP: bishop(types, moves);
				break;
			case Constants.ROOK: rook(types, moves);
				break;
			case Constants.QUEEN: queen(types, moves);
				break;
		}
		ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
	}
	
	private void pawn(boolean[] types, ArrayList<Move> moves) {
		//Moves
		long prevTime = System.currentTimeMillis();
		if (types[0]) {
			final int direction = (color == Constants.WHITE) ? -8 : 8;
			if (board.getPiece(pos + direction).isEmpty()) {
				addMove(moves, new Move(pos, pos + direction, Move.Type.MOVE));
				if (ChessBoard.getRow(pos) == Constants.PAWN_STARTS[color]) {
					if (board.getPiece(pos + direction * 2).isEmpty()) addMove(moves, new Move(pos, pos + direction * 2, Move.Type.MOVE));
				}
			}
		}
		//Attacks
		if (types[1]) {
			for (int i = 0; i < 2; i++) {
				if (ChessBoard.getEdge(Constants.PAWN_DIAGONALS[color][i], pos) < 1) continue;
				final int newPos = pos + Constants.PAWN_DIAGONALS[color][i];
				if (board.getPiece(newPos).color == board.next(color)) addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
			}
		}
		//EnPassant
		if (types[2]) {
			if (board.getEnPassant() == Constants.EMPTY) return;
			if (Math.abs(board.getEnPassant() - pos) != 1 || ChessBoard.getDistance(board.getEnPassant(), pos) != 1) return;
			final int newPos = (color == Constants.WHITE) ? board.getEnPassant() - 8 : board.getEnPassant() + 8;
			if (board.getPiece(newPos).isEmpty()) addMove(moves, new Move(pos, newPos, Move.Type.SPECIAL));
		}
		ChessGame.timePawnGen += System.currentTimeMillis() - prevTime;
	}
	
	private void knight(boolean[] types, ArrayList<Move> moves) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.KNIGHT_MOVES.length; i++) {
			final int newPos = pos + Constants.KNIGHT_MOVES[i];
			if (!ChessBoard.onBoard(newPos) || !ChessBoard.onL(pos, newPos)) continue;
			
			if (board.getPiece(newPos).isEmpty()) { 
				if (types[0])
					addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
			}
			else if (board.getPiece(newPos).color != color && types[1]) 
				addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
		}
		ChessGame.timeKnightGen += System.currentTimeMillis() - prevTime;
	}
	
	private void bishop(boolean[] types, ArrayList<Move> moves) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.DIAGONALS.length; i++) {
			final int distance = ChessBoard.getEdge(Constants.DIAGONALS[i], pos);
			for (int j = 1; j < distance + 1; j++) {
				final int newPos = pos + Constants.DIAGONALS[i] * j;
				final ChessPiece piece = board.getPiece(newPos);
				if (piece.color == color) break;
				if (!piece.isEmpty()) {
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
	
	private void rook(boolean[] types, ArrayList<Move> moves) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.STRAIGHT.length; i++) {
			final int distance = ChessBoard.getEdge(Constants.STRAIGHT[i], pos);
			for (int j = 1; j < distance + 1; j++) {
				final int newPos = pos + Constants.STRAIGHT[i] * j;
				final ChessPiece piece = board.getPiece(newPos);
				if (piece.color == color) break;
				if (!piece.isEmpty()) {
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
	
	private void queen(boolean[] types, ArrayList<Move> moves) {
		rook(types, moves);
		bishop(types, moves);
	}
	
	private void king(boolean[] types, ArrayList<Move> moves) {
		long prevTime = System.currentTimeMillis();
		if (types[0] || types[1]) {
			for (int i = 0; i < Constants.KING_MOVES.length; i++) {
				if (ChessBoard.getEdge(Constants.KING_MOVES[i], pos) < 1) continue;
				final int newPos = pos + Constants.KING_MOVES[i];

				if (board.getPiece(newPos).isEmpty() && types[0]) {
					addMove(moves, new Move(pos, newPos, Move.Type.MOVE));
				}
				else if (board.getPiece(newPos).color != color && types[1]) 
					addMove(moves, new Move(pos, newPos, Move.Type.ATTACK));
			}
		}
		//Castling (complete mess)
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

	private void addMove(ArrayList<Move> moves, Move move) {
		long prevTime = System.currentTimeMillis();
		if (!Constants.CHECKS) {
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
		if (board.isAttacked(move.finish, color)) return false;
		if (!board.isChecked(color)) return true;
		for (final ChessPiece attacker : board.getAttackers(this)) {
			if (move.finish == attacker.pos) return true;
			if (attacker.type == Constants.QUEEN || attacker.type == Constants.BISHOP) {
				if (ChessBoard.onSameDiagonal(move.start, move.finish, attacker.pos)) return false;
			}
			if (attacker.type == Constants.QUEEN || attacker.type == Constants.ROOK) {
				if (ChessBoard.onSameLine(move.start, move.finish, attacker.pos)) return false;
			}
		}
		return true;
	}
	
	private boolean stopsCheck(Move move) {
		final int king = board.getKingPos(color);
		ChessPiece attacker = ChessPiece.empty();
		for (final ChessPiece piece : board.getAttackers(board.getPiece(king))) attacker = piece;
		if (attacker.type == Constants.PAWN && move.isSpecial() && board.getEnPassant() == attacker.pos) return true;
		if (attacker.type == Constants.PAWN || attacker.type == Constants.KNIGHT) return move.finish == attacker.pos;
		if (move.finish == attacker.pos) return true;
		
		if (ChessBoard.onDiagonal(king, attacker.pos)) {
			return ChessBoard.blocksDiagonal(attacker.pos, king, move.finish);
		}
		return ChessBoard.blocksLine(attacker.pos, king, move.finish);
	}
	
	private boolean isPinned(Move move) {
		final int king = board.getKingPos(color);

		final boolean pinnedPiece = (ChessBoard.onDiagonal(king, move.start) || ChessBoard.onLine(king, move.start)) && board.isAttacked(this);
		final boolean passant = (move.isSpecial() && board.getPiece(move.start).type == Constants.PAWN);
		if (!pinnedPiece && !passant) return false;

		final PieceSet attackers;
		if (passant) {
			final int enPassant = board.getEnPassant();
			final boolean pinnedPassant = (ChessBoard.onDiagonal(king, enPassant) || ChessBoard.onLine(king, enPassant)) && board.isAttacked(enPassant, color);
			if (pinnedPiece && pinnedPassant) {
				attackers = board.getAttackers(this).clone();
				attackers.addAll(board.getAttacks(enPassant, board.next(color)));
			}
			else if (pinnedPiece) {
				attackers = board.getAttackers(this);
			}
			else if (pinnedPassant) {
				attackers = board.getAttacks(enPassant, board.next(color));
			}
			else {
				return false;
			}
		}
		else {
			if (!pinnedPiece) return false;
			attackers = board.getAttackers(this);
		}

		for (final ChessPiece piece : attackers) {
			if (piece.type == Constants.PAWN || piece.type == Constants.KNIGHT || piece.type == Constants.KING) continue;

			if (piece.type == Constants.BISHOP || piece.type == Constants.QUEEN) {
				final int pinnedPos = (passant && ChessBoard.onDiagonal(piece.pos, board.getEnPassant())) ? board.getEnPassant() : move.start;
				if (ChessBoard.blocksDiagonal(piece.pos, king, pinnedPos)) {
					if (ChessBoard.onSameDiagonal(move.finish, king, piece.pos) && pinnedPos == move.start) return false;

					final int direction = ChessBoard.getDiagonalOffset(piece.pos, pinnedPos);

					for (int j = 1; j < ChessBoard.getDistanceVert(pinnedPos, king); j++) {
						if (!board.getPiece(pinnedPos + direction * j).isEmpty()) return false;
					}
					return true;
				}
			}

			if (piece.type == Constants.ROOK || piece.type == Constants.QUEEN) {
				if (ChessBoard.blocksLine(piece.pos, king, move.start)) {
					if (ChessBoard.onSameLine(move.finish, king, piece.pos)) return false;

					final int direction = ChessBoard.getHorizontalOffset(piece.pos, move.start);
					
					int newPos = move.start;
					for (int j = 0; j < ChessBoard.getDistance(move.start, king) - 1; j++) {
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
		return new ChessPiece(Constants.EMPTY, Constants.EMPTY, Constants.EMPTY, null, Constants.EMPTY);
	}
}
