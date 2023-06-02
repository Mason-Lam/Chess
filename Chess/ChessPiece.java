package Chess;

public class ChessPiece {
	byte type;
	byte color;
	
	public ChessPiece(byte type, byte color) {
		this.type = type;
		this.color = color;
	}
	
	public boolean isEmpty() {
		return type == Constants.EMPTY;
	}
	
	public static ChessPiece empty() {
		return new ChessPiece(Constants.EMPTY, Constants.EMPTY);
	}
}
