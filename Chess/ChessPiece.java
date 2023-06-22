package Chess;

public class ChessPiece {
	public byte type;
	public byte color;
	public int pos;
	
	public ChessPiece(byte type, byte color, int pos) {
		this.type = type;
		this.color = color;
		this.pos = pos;
	}

	public void setPos(int newPos) {
		pos = newPos;
	}
	
	public boolean isEmpty() {
		return type == Constants.EMPTY;
	}
	
	public static ChessPiece empty() {
		return new ChessPiece(Constants.EMPTY, Constants.EMPTY, Constants.EMPTY);
	}
}
