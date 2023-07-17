package Chess;

import java.util.ArrayList;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;

public class Computer {

	public static class BoardStorage {
		public final int enPassant;
		public final int halfMove;
		private final boolean[] castling;

		public BoardStorage(int enPassant, int halfMove, boolean[] castling) {
			this.enPassant = enPassant;
			this.halfMove = halfMove;
			this.castling = new boolean[2];
			this.castling[0] = castling[0];
			this.castling[1] = castling[1];
		}

		public boolean[] getCastling() {
			final boolean[] castle = new boolean[2];
			castle[0] = castling[0];
			castle[1] = castling[1];
			return castle;
		}
	}

	public final ChessBoard board;
	
	public Computer(ChessBoard board) {
		this.board = board;
	}

	public int totalMoves(int depth) {
		int count = 0;
		long prevTime = System.currentTimeMillis();
		final PieceSet pieces = board.getPieces(board.getTurn());
		if (depth == 1) {
			for (final ChessPiece piece : pieces) {
				final ArrayList<Move> moves = new ArrayList<Move>(MAX_MOVES[piece.type]);
				piece.pieceMoves(moves);
				if (moves.size() > 0) {
					if(piece.isPawn() && ChessBoard.getRow(moves.get(0).finish) == PROMOTION_LINE[board.getTurn()]) {
						count += moves.size() * 4;
						continue;
					}
				}
				count += moves.size();
				continue;
			}
			return count;
		}

		final ArrayList<Move> moves = new ArrayList<Move>(MAX_MOVES[6]);
		final BoardStorage store = new BoardStorage(board.getEnPassant(), board.halfMove, board.getCastling(board.getTurn()));
		for (final ChessPiece piece : pieces) {
			piece.pieceMoves(moves);
		}
		ChessGame.timeMisc += System.currentTimeMillis() - prevTime;

		for (int i = 0; i < moves.size(); i++) {
			final Move move = moves.get(i);
				final ChessPiece capturedPiece = getCapturedPiece(move);
				// final int prevCount = count;
				board.make_move(move, false);
				if (board.is_promote()) {
					for (final byte type : PROMOTION_PIECES) {
						board.promote(type);
						count += totalMoves(depth - 1);
						board.unPromote(move.finish, store);
					}
				}
				else {
					count += totalMoves(depth - 1);
				}
				// if (depth == 1) logMove(move, count - prevCount);
				board.undoMove(move, capturedPiece, store);
		}
		return count;
	}

	private void logMove(Move move, int count) {
		System.out.print(indexToSquare(ChessBoard.getColumn(move.start), 8 - ChessBoard.getRow(move.start)));
		System.out.print(indexToSquare(ChessBoard.getColumn(move.finish), 8 - ChessBoard.getRow(move.finish)));
		System.out.println(" : " + count);
	}

	private ChessPiece getCapturedPiece(Move move) {
		return (board.isPassant(move)) ? board.getPiece(board.getEnPassant()) : board.getPiece(move.finish);
	}
}
