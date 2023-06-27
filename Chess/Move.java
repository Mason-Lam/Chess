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

	public Move(int start, int finish, Type type) {
		this.start = start;
		this.finish = finish;
		this.type = type;
	}

	public boolean isSpecial() {
		return type == Type.SPECIAL;
	}

}
