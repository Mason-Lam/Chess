package Chess;

import java.util.ArrayList;

import Chess.ChessBoard.BoardStorage;
import Chess.Constants.PieceConstants.PieceType;
import Chess.TranspositionTable.TTEntry;

import static Chess.Constants.MoveConstants.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.Constants.PositionConstants.*;
import static Chess.BoardUtil.*;

/**
 * A class that represents the computer player in the game of chess.
 */
public class Computer {

	public final ChessBoard board;
	public final TranspositionTable table;
	
	/**
	 * Creates a new computer player with the specified board.
	 * @param board The board to play on.
	 */
	public Computer(ChessBoard board) {
		this.board = board;
		table = new TranspositionTable(20);
	}

	/**
	 * Returns the total number of possible moves at the specified depth.
	 * @param depth The depth to search to.
	 * @return The total number of possible moves.
	 */
	public int totalMoves(final int depth) {
		return totalMoves(depth, false);
	}

	/**
	 * Returns the total number of possible moves at the specified depth.
	 * @param depth The depth to search to.
	 * @param useZobristHashing Whether or not to use hashing optimization: Warning can result in wrong answers.
	 * @return The total number of possible moves.
	 */
	public int totalMoves(final int depth, final boolean useZobristHashing) {
		if (useZobristHashing) {
			TTEntry entry = table.lookup(board.hash());
			if (entry != null && entry.depth == depth) {
				return entry.score;
			}
		}

		//Base case.
		if (depth == 1) {
			final int[] moveCounter = new int[1];
			board.getBitboard().generateAllMoves((Move move) ->  {
				if (board.getPiece(move.getStart()).isPawn()) {
					if (getRow(move.getFinish()) == PROMOTION_ROW[board.getTurn().arrayIndex]) {
						moveCounter[0] += 4;
						return;
					}
				}
				moveCounter[0] ++;
			}, board.getTurn(), false);

			if (useZobristHashing) table.store(board.hash(), 1, moveCounter[0], 0, 0);
			return moveCounter[0];
		}

		//Recursive case.
		int count = 0;

		final BoardStorage store = board.copyData();
		final ArrayList<Move> moves = new ArrayList<Move>(MAX_MOVES[6]);
		board.getBitboard().generateAllMoves(moves, board.getTurn(), false);

		for (final Move move : moves) {
			final ChessPiece capturedPiece = board.isEnPassant(move) ? board.getPiece(board.getEnPassant()) : board.getPiece(move.getFinish());
			final int prevCount = count;
			board.makeMove(move);

			if (board.is_promote()) {
				for (final PieceType type : PROMOTION_PIECES) {
					board.promote(type);
					count += totalMoves(depth - 1, useZobristHashing);
					board.unPromote(move.getFinish());
				}
			}
			else {
				count += totalMoves(depth - 1, useZobristHashing);
			}
			if (depth == 1) logMove(move, count - prevCount);
			board.undoMove(move, capturedPiece, store);
		}
		if (useZobristHashing) table.store(board.hash(), depth, count, 0, 0);
		return count;
	}

	private void logMove(Move move, int count) {
		System.out.print(indexToSquare(getColumn(move.getStart()), 8 - getRow(move.getStart())));
		System.out.print(indexToSquare(getColumn(move.getFinish()), 8 - getRow(move.getFinish())));
		System.out.println(" : " + count);
	}
}
