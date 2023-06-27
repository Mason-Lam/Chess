package Chess;

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
			final HashSet<Move> moves = new HashSet<Move>();
			final ChessPiece piece = board.getPiece(i);
			board.piece_moves(i, Constants.ALL_MOVES, moves);
			if(depth == 1) {
				if (moves.size() > 0) {
					for (final Move move : moves) {
						if(piece.type == Constants.PAWN && board.getRow(move.finish) == Constants.PROMOTION_LINE[board.getTurn()]) {
							count += moves.size() * 3;
						}
						break;
					}
				}
				count += moves.size();
				continue;
			}
			
			for (final Move move : moves) {
				final ChessPiece capturedPiece = getCapturedPiece(move);
				board.make_move(move, false);
				if (board.is_promote()) {
					for (final byte type : Constants.PROMOTION_PIECES) {
						board.promote(type);
						count += totalMoves(depth - 1);
						board.unPromote(move.finish, store);
					}
				}
				else {
					count += totalMoves(depth - 1);
				}

				board.undoMove(move, capturedPiece, store);
			}
		}
		return count;
	}

	private ChessPiece getCapturedPiece(Move move) {
		return (move.isSpecial() && board.getPiece(move.start).type == Constants.PAWN) ? 
			board.getPiece(board.getEnPassant()) : board.getPiece(move.finish);
	}
}
