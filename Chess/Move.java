package Chess;

public class Move {
	
	public enum Type {
		ATTACK,
		MOVE,
		SPECIAL
	}
	
	public final int start;
	public final int finish;
	public final Type type;
	public final ChessPiece capturedPiece;

	public Move(int start, int finish, Type type, ChessPiece capturedPiece) {
		this.start = start;
		this.finish = finish;
		this.type = type;
		this.capturedPiece = capturedPiece;
	}
	
	public Move(int start, int finish, Type type){
		this(start, finish, type, ChessPiece.empty());
	}

	public boolean isSpecial() {
		return type == Type.SPECIAL;
	}

}
