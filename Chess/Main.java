package Chess;

import Chess.Constants.PieceConstants.PieceColor;

public class Main {
	
	public static void main(String[] args) {
		BitboardHelper.initializeBitBoard();
		Tests.runTestsDeep(false, true);
		@SuppressWarnings("unused")
		final ChessGame game = new ChessGame(PieceColor.COLORLESS,0);
	}
}
