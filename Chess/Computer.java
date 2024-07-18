package Chess;

import java.util.ArrayList;

import Chess.ChessBoard.BoardStorage;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;

public class Computer {

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
				final ArrayList<Move> moves = new ArrayList<Move>(MAX_MOVES[piece.getType()]);
				prevTime = System.currentTimeMillis();
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
		final BoardStorage store = board.copyData();
		for (final ChessPiece piece : pieces) {
			piece.pieceMoves(moves);
		}
		ChessGame.timeMisc += System.currentTimeMillis() - prevTime;

		for (int i = 0; i < moves.size(); i++) {
			final Move move = moves.get(i);
				final ChessPiece capturedPiece = getCapturedPiece(move);
				// final int prevCount = count;
				// try {
					board.makeMove(move);
				// }
				// catch (Exception e) {
				// 	board.displayAttacks();
				// 	// System.out.println(board.getFenString());
				// 	return 0;
				// }
				if (board.is_promote()) {
					for (final byte type : PROMOTION_PIECES) {
						board.promote(type);
						count += totalMoves(depth - 1);
						board.unPromote(move.finish);
					}
				}
				else {
					count += totalMoves(depth - 1);
				}
				// if (depth == 2) logMove(move, count - prevCount);
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
