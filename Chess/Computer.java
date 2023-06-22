package Chess;

import java.util.ArrayList;
import java.util.HashSet;

public class Computer {

	public static class BoardStorage {
		public final String fenString;
		public final int enPassant;
		private final boolean[] castling;

		public BoardStorage(String fenString, int enPassant, boolean[] castling) {
			this.fenString = fenString;
			this.enPassant = enPassant;
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

	private final ChessBoard board;
	
	public Computer(ChessBoard board) {
		this.board = board;
	}

	public int totalMoves(int depth) {
		int count = 0;
		final BoardStorage store = new BoardStorage(board.getFenString(), board.getEnPassant(), board.getCastling(board.getTurn()));
		final HashSet<Integer> positions = new HashSet<Integer>();
		for (final ChessPiece piece : board.getPieces(board.getTurn())) {
			positions.add(piece.pos);
		}

		for(final Integer i : positions) {
			final ArrayList<Move> moves = new ArrayList<Move>();
			final ChessPiece piece = board.getPiece(i);
			board.piece_moves(i, Constants.ALL_MOVES, moves);
			if(depth == 1) {
				if (moves.size() > 0) {
					if(piece.type == Constants.PAWN && board.getRow(moves.get(0).finish) == Constants.PROMOTION_LINE[board.getTurn()]) {
						count += moves.size() * 4;
						continue;
					}
				}
				count += moves.size();
				continue;
			}
			
			for (int j = 0; j < moves.size(); j++) {
				final Move move = moves.get(j);
				board.make_move(move, false);
				if (board.is_promote()) {
					for (byte type : Constants.PROMOTION_PIECES) {
						board.promote(type);
						count += totalMoves(depth - 1);
						board.unPromote(move.finish, store);
					}
				}
				else {
					count += totalMoves(depth - 1);
				}

				board.undoMove(move, store);
			}
		}
		return count;
	}
}
