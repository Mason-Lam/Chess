package Chess;

import Chess.Constants.PieceConstants.PieceColor;

public class Main {
	
	public static void main(String[] args) {
		Tests.runTestsDeep(false, true);
		// Tests.runTests();
		@SuppressWarnings("unused")
		final ChessGame game = new ChessGame(PieceColor.COLORLESS,0);
	}
}
