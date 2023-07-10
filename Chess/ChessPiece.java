package Chess;

public class ChessPiece {
	public byte type;
	public byte color;
	public int pos;
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
	}
	
	private void pawnAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < 2; i++) {
			final int newPos = pos + Constants.PAWN_DIAGONALS[color][i];
			if (!board.onBoard(newPos) || !board.onDiagonal(pos,newPos)) continue;

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
			if (!board.onBoard(newPos) || !board.onL(pos, newPos)) continue;

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
			int newPos = pos;
			while (true) {
				newPos += Constants.DIAGONALS[i];
				if (!board.onBoard(newPos) || !board.onDiagonal(pos, newPos)) break;
				
				if (remove) {
					board.removeAttacker(this, newPos);
				}
				else {
					board.addAttacker(this, newPos);
				}

				if (!board.getPiece(newPos).isEmpty()) break;
			}
		}
		ChessGame.timeBishopAttack += System.currentTimeMillis() - prevTime;
	}

	public void softPieceUpdate(Move move, boolean undoMove) {
		if (pos == move.start || pos == move.finish) return;
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
	}

	private void softQueenUpdate(Move move, boolean undoMove) {
		softBishopUpdate(move, undoMove);
		softRookUpdate(move, undoMove);
	}

	private void softBishopUpdate(Move move, boolean undoMove) {
		final int start = undoMove ? move.finish : move.start;
		final int finish = undoMove ? move.start : move.finish;
		final boolean attackingStart = board.getAttacks(start, color).contains(this);
		final boolean attackingEnd = board.getAttacks(finish, color).contains(this);
		final boolean attackingPassant = move.isSpecial() && (board.getPiece(finish).type == Constants.PAWN)
			 && board.getAttacks(board.getEnPassant(), color).contains(this);
		if (!attackingStart && !attackingEnd && !attackingPassant) return;

		final boolean undoAttack = move.type == Move.Type.ATTACK && undoMove;
		final boolean moveAttack = move.type == Move.Type.ATTACK && !undoMove;
		if (board.onSameDiagonal(start, finish, pos)) {
			if (attackingStart && attackingEnd) {
				if (!moveAttack) removeAttacks(getDiagonalOffset(finish, start), finish);
			}
			else {
				if (!undoAttack) addAttacks(getDiagonalOffset(start, finish), start);
			}
		}

		else {
			if (board.onDiagonal(start, pos) && attackingStart) {
				if (!undoAttack) addAttacks(getDiagonalOffset(pos, start), start);
			}
			if (board.onDiagonal(finish, pos) && attackingEnd) {
				if (!moveAttack) removeAttacks(getDiagonalOffset(pos, finish), finish);
			}
		}

		if (attackingPassant && board.onDiagonal(board.getEnPassant(), pos)) {
			final int enPassant = board.getEnPassant();
			if (undoMove) {
				removeAttacks(getDiagonalOffset(pos, enPassant), enPassant);
			}
			else {
				addAttacks(getDiagonalOffset(pos, enPassant), enPassant);
			}
		}
	}

	private void softRookUpdate(Move move, boolean undoMove) {
		final int start = undoMove ? move.finish : move.start;
		final int finish = undoMove ? move.start : move.finish;
		final ChessPiece piece = board.getPiece(finish);
		final boolean attackingStart = board.getAttacks(start, color).contains(this);
		final boolean attackingEnd = board.getAttacks(finish, color).contains(this);
		final boolean attackingPassant = move.isSpecial() && piece.type == Constants.PAWN
			 && board.getAttacks(board.getEnPassant(), color).contains(this);
		if (!attackingStart && !attackingEnd && !attackingPassant) return;

		if (move.isSpecial() && piece.type == Constants.KING && (Math.abs(pos - finish) == 1 || Math.abs(pos - start) == 1)) return;

		final boolean undoAttack = move.type == Move.Type.ATTACK && undoMove;
		final boolean moveAttack = move.type == Move.Type.ATTACK && !undoMove;
		if (board.onSameLine(start, finish, pos)) {
			if (attackingStart && attackingEnd) {
				if (!moveAttack) removeAttacks(getHorizontalOffset(finish, start), finish);
			}
			else {
				if (!undoAttack) addAttacks(getHorizontalOffset(start, finish), start);
			}
		}

		else {
			if (board.onLine(start, pos) && attackingStart) {
				if (!undoAttack) addAttacks(getHorizontalOffset(pos, start), start);
			}

			if (board.onLine(finish, pos) && attackingEnd) {
				if (!moveAttack) removeAttacks(getHorizontalOffset(pos, finish), finish);
			}
		}

		if (attackingPassant && board.onLine(board.getEnPassant(), pos)) {
			final int enPassant = board.getEnPassant();
			if (undoMove) {
				removeAttacks(getHorizontalOffset(pos, enPassant), enPassant);
			}
			else {
				addAttacks(getHorizontalOffset(pos, enPassant), enPassant);
			}
		}
	}

	private int getDiagonalOffset(int startingPos, int endPos) {
		int direction = Math.abs(startingPos - endPos) % 7 == 0 ? 7 : 9;
		if (startingPos - endPos > 0) direction *= -1;
		return direction;
	}

	private int getHorizontalOffset(int startingPos, int endPos) {
		int direction = board.onColumn(startingPos, endPos) ? 8 : 1;
		if (startingPos - endPos > 0) direction *= -1;
		return direction;
	}

	private int getEdge(int direction, int pos) {
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
	
	private void rookAttacks(boolean remove) {
		long prevTime = System.currentTimeMillis();
		for (int i = 0; i < Constants.STRAIGHT.length; i++) {
			int newPos = pos;
			while (true) {
				newPos += Constants.STRAIGHT[i];
				if (!board.onBoard(newPos) || (!board.onColumn(pos, newPos) && i < 2) || (!board.onRow(pos, newPos) && i >= 2)) break;
				
				if (remove) {
					board.removeAttacker(this, newPos);
				}
				else {
					board.addAttacker(this, newPos);
				}

				if (!board.getPiece(newPos).isEmpty()) break;
			}
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
			final int newPos = pos + Constants.KING_MOVES[i];
			if (!board.onBoard(newPos) || board.getDistance(pos, newPos) > 2) continue;

			if (remove) {
				board.removeAttacker(this, newPos);
				continue;
			}
			board.addAttacker(this, newPos);
		}
		ChessGame.timeKingAttack += System.currentTimeMillis() - prevTime;
	}

	public MoveList piece_moves(boolean[] types) {
		long prevTime = System.currentTimeMillis();
		final MoveList moves = new MoveList(type);
		if (board.is_promote() || board.getTurn() != color) return moves;

		if (type == Constants.KING) {
			king(types, moves);
			ChessGame.timeMoveGen += System.currentTimeMillis() - prevTime;
			return moves;
		}

		if (board.doubleCheck(color)) return moves;

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
	
	private void knight(boolean[] types, MoveList moves) {
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
	
	private void bishop(boolean[] types, MoveList moves) {
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
	
	private void rook(boolean[] types, MoveList moves) {
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
	
	private void queen(boolean[] types, MoveList moves) {
		rook(types, moves);
		bishop(types, moves);
	}
	
	private void king(boolean[] types, MoveList moves) {
		long prevTime = System.currentTimeMillis();
		if (types[0] || types[1]) {
			for (int i = 0; i < Constants.KING_MOVES.length; i++) {
				final int newPos = pos + Constants.KING_MOVES[i];
				if (!board.onBoard(newPos) || board.getDistance(pos, newPos) > 2) continue;
				if (board.getPiece(newPos).isEmpty() && types[0]) {
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
				if (board.onSameDiagonal(move.start, move.finish, attacker.pos)) return false;
			}
			if (attacker.type == Constants.QUEEN || attacker.type == Constants.ROOK) {
				if (board.onSameLine(move.start, move.finish, attacker.pos)) return false;
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
		
		if (board.onDiagonal(king, attacker.pos)) {
			return board.blocksDiagonal(attacker.pos, king, move.finish);
		}
		return board.blocksLine(attacker.pos, king, move.finish);
	}
	
	private boolean isPinned(Move move) {
		final int king = board.getKingPos(color);
		if (!board.onDiagonal(king, move.start) && !board.onLine(king, move.start)) return false;

		final boolean passant = (move.isSpecial() && board.getPiece(move.start).type == Constants.PAWN);
		final PieceSet attackers;
		if (passant) {
			if (!board.isAttacked(move.start, color) && !board.isAttacked(board.getEnPassant(), color)) return false;
			attackers = board.getAttackers(this).clone();
			attackers.addAll(board.getAttacks(board.getEnPassant(), board.next(color)));
		}
		else {
			if (!board.isAttacked(move.start, color)) return false;
			attackers = board.getAttackers(this);
		}

		for (final ChessPiece piece : attackers) {
			if (piece.type == Constants.PAWN || piece.type == Constants.KNIGHT || piece.type == Constants.KING) continue;

			if (piece.type == Constants.BISHOP || piece.type == Constants.QUEEN) {
				int pinnedPos = move.start;
				if (passant && board.onDiagonal(piece.pos, board.getEnPassant())) pinnedPos = board.getEnPassant();
				if (board.blocksDiagonal(piece.pos, king, pinnedPos)) {
					if (board.onSameDiagonal(move.finish, king, piece.pos) && pinnedPos == move.start) return false;

					final int direction = getDiagonalOffset(piece.pos, pinnedPos);

					for (int j = 0; j < board.getDistanceVert(move.start, king) - 1; j++) {
						pinnedPos += direction;
						if (!board.getPiece(pinnedPos).isEmpty()) return false;
					}
					return true;
				}
			}

			if (piece.type == Constants.ROOK || piece.type == Constants.QUEEN) {
				if (board.blocksLine(piece.pos, king, move.start)) {
					if (board.onSameLine(move.finish, king, piece.pos)) return false;

					final int direction = getHorizontalOffset(piece.pos, move.start);
					
					int newPos = move.start;
					for (int j = 0; j < board.getDistance(move.start, king) - 1; j++) {
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
			//return (aMove.start == start && aMove.finish == finish && aMove.type == type);
			return ((piece.type == type) && (piece.color == color) && (piece.pos == pos) && (piece.pieceID == pieceID));
		}
		return false;
	}
	
	public static ChessPiece empty() {
		return new ChessPiece(Constants.EMPTY, Constants.EMPTY, Constants.EMPTY, null, Constants.EMPTY);
	}
}
