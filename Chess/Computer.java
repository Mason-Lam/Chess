package Chess;

import java.util.ArrayList;

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
		final BoardStorage store = (depth != 1) ? new BoardStorage(board.getEnPassant(), board.halfMove, board.getCastling(board.getTurn())) : null;
		final PieceSet pieces = board.getPieces(board.getTurn());
		ChessGame.timeMisc += System.currentTimeMillis() - prevTime;
		for(final ChessPiece piece : pieces) {
			final ArrayList<Move> moves = new ArrayList<Move>(Constants.MAX_MOVES[piece.type]);
			piece.pieceMoves(moves);
			if(depth == 1) {
				if (moves.size() > 0) {
					final Move move = moves.get(0);
					if(piece.type == Constants.PAWN && ChessBoard.getRow(move.finish) == Constants.PROMOTION_LINE[board.getTurn()]) {
						count += moves.size() * 4;
						continue;
					}
				}
				count += moves.size();
				continue;
			}
			
			for (int moveIndex = 0; moveIndex < moves.size(); moveIndex ++) {
				final Move move = moves.get(moveIndex);
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
