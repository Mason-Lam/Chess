package Chess;

import java.util.ArrayList;

public class Computer {
	ChessBoard board;
	
	public Computer(ChessBoard board) {
		this.board = board;
	}
	
	public int totalMoves(int depth) {
		return totalMoves(depth, board);
	}
	
	private int totalMoves(int depth, ChessBoard board) {
		int count = 0;
		var positions = board.getPiecePositions(board.turn);
		for(Integer i : positions) {
			ArrayList<Move> moves = new ArrayList<Move>();
			ChessPiece piece = board.getPiece(i);
			board.piece_moves(i, Constants.ALL_MOVES, moves);
			if(depth == 1) {
				if (moves.size() > 0) {
					if(piece.type == Constants.PAWN && board.getRow(moves.get(0).finish) == Constants.PROMOTION_LINE[board.turn]) {
						count += moves.size() * 4;
						continue;
					}
				}
				count += moves.size();
				continue;
			}
			
			for (int j = 0; j < moves.size(); j++) {
				Move move = moves.get(j);
				ChessBoard copyBoard = board.copyBoard();
				copyBoard.make_move(move, true);
				if (copyBoard.is_promote()) {
					for (byte type : Constants.PROMOTION_PIECES) {
						ChessBoard promoteBoard = copyBoard.copyBoard();
						promoteBoard.promote(type);
						count += totalMoves(depth - 1, promoteBoard);
					}
					continue;
				}
				count += totalMoves(depth - 1, copyBoard);
			}
		}
		return count;
	}
}
