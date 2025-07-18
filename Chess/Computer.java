package Chess;

import java.util.ArrayList;

import Chess.ChessBoard.BoardStorage;
import Chess.Constants.PieceConstants.PieceType;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.Constants.PositionConstants.*;
import static Chess.BoardUtil.*;

/**
 * A class that represents the computer player in the game of chess.
 */
public class Computer {

	public final ChessBoard board;
	
	/**
	 * Creates a new computer player with the specified board.
	 * @param board The board to play on.
	 */
	public Computer(ChessBoard board) {
		this.board = board;
	}

	/**
	 * Returns the total number of possible moves at the specified depth.
	 * @param depth The depth to search to.
	 * @return The total number of possible moves.
	 */
	public int totalMoves(int depth) {
		int count = 0;
		final PieceSet pieces = board.getPieces(board.getTurn());

		//Base case.
		if (depth == 1) {
			for (final ChessPiece piece : pieces) {
				final ArrayList<Move> moves = new ArrayList<Move>(MAX_MOVES[piece.getType().arrayIndex]);
				piece.pieceMoves(moves);
				if (moves.size() > 0 && piece.isPawn() && getRow(moves.get(0).finish) == PROMOTION_ROW[board.getTurn().arrayIndex]){
					count += moves.size() * 4;
					continue;
				}
				count += moves.size();
				continue;
			}
			return count;
		}

		//Recursive case.
		final BoardStorage store = board.copyData();
		final ArrayList<Move> moves = new ArrayList<Move>(MAX_MOVES[6]);
		for (final ChessPiece piece : pieces) {
			piece.pieceMoves(moves);
		}

		for (final Move move : moves) {
			final ChessPiece capturedPiece = board.isEnPassant(move) ? board.getPiece(board.getEnPassant()) : board.getPiece(move.finish);
			final int prevCount = count;
			board.makeMove(move);

			if (board.is_promote()) {
				for (final PieceType type : PROMOTION_PIECES) {
					board.promote(type);
					count += totalMoves(depth - 1);
					board.unPromote(move.finish);
				}
			}
			else {
				count += totalMoves(depth - 1);
			}
			if (depth == 1) logMove(move, count - prevCount);
			board.undoMove(move, capturedPiece, store);
		}
		return count;
	}

	private void logMove(Move move, int count) {
		System.out.print(indexToSquare(getColumn(move.start), 8 - getRow(move.start)));
		System.out.print(indexToSquare(getColumn(move.finish), 8 - getRow(move.finish)));
		System.out.println(" : " + count);
	}
}
