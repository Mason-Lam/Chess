package Chess;

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

	public final ChessBoard board;
	
	public Computer(ChessBoard board) {
		this.board = board;
	}

	public int[][] copyAttacks() {
		final int[][] x;
		x = new int[2][64];
		x[0] = new int[64];
		x[1] = new int[64];
		for (int color = 0; color < 2; color++) {
			for (int i = 0; i < 64; i++) {
				x[color][i] = board.getAttacks(i, color).size();
			}
		}
		return x;
	}

	public boolean compareAttacks(int[][] x, int[][] y) {
		for (int color = 0; color < 2; color++) {
			for (int i = 0; i < 64; i++) {
				if (x[color][i] != y[color][i]) return false;
			}
		}
		return true;
	}

	public int totalMoves(int depth) {
		// final int[][] x;
		// if (depth == 2) {
		// 	x = copyAttacks();
		// }
		// else {
		// 	x = null;
		// }
		int count = 0;
		long prevTime = System.currentTimeMillis();
		final BoardStorage store = (depth != 1) ? new BoardStorage(board.getFenString(), board.getEnPassant(), board.getCastling(board.getTurn())) : null;
		final PieceSet pieces = board.getPieces(board.getTurn());
		ChessGame.timeMisc += System.currentTimeMillis() - prevTime;
		for(final ChessPiece piece : pieces) {
			final MoveList moves = piece.piece_moves(Constants.ALL_MOVES);
			if(depth == 1) {
				// if (board.getFenString().equals("rn1q1k1r/pp1bbppp/2p5/8/2B5/P7/1PP1NnPP/RNBQK2R/ w KQ - -")) {
				// 	board.displayAttacks();
				// 	for (Move move : moves) {
				// 		final String start = Constants.indexToSquare(board.getColumn(move.start), 8 - board.getRow(move.start));
				// 		final String finish = Constants.indexToSquare(board.getColumn(move.finish), 8 - board.getRow(move.finish));
				// 		System.out.println(start + finish);
				// 	}
				// }
				if (moves.size() > 0) {
					final Move move = moves.get(0);
					if(piece.type == Constants.PAWN && board.getRow(move.getFinish()) == Constants.PROMOTION_LINE[board.getTurn()]) {
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
				// final int prevCount = count;
				board.make_move(move, false);
				if (board.is_promote()) {
					for (final byte type : Constants.PROMOTION_PIECES) {
						board.promote(type);
						count += totalMoves(depth - 1);
						board.unPromote(move.getFinish(), store);
					}
				}
				else {
					count += totalMoves(depth - 1);
				}
				// if (depth == 3) {
				// 	final String start = Constants.indexToSquare(board.getColumn(move.start), 8 - board.getRow(move.start));
				// 	final String finish = Constants.indexToSquare(board.getColumn(move.finish), 8 - board.getRow(move.finish));
				// 	System.out.println(start + finish + ": " + (count - prevCount));
				// }
				board.undoMove(move, capturedPiece, store);
				// if (depth == 2) {
				// 	final int[][] y = copyAttacks();
				// 	if (!compareAttacks(x, y)) {
				// 		System.out.println(move.start + " : " + move.finish);
				// 	}
				// }
			}
		}
		return count;
	}

	private ChessPiece getCapturedPiece(Move move) {
		return (move.isSpecial() && board.getPiece(move.getStart()).type == Constants.PAWN) ? 
			board.getPiece(board.getEnPassant()) : board.getPiece(move.getFinish());
	}
}
