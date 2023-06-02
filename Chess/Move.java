package Chess;

public class Move {
	
	public enum Type {
		ATTACK,
		MOVE,
		SPECIAL
	}
	
	public int start;
	public int finish;
	public Type type;
	
	public Move(int Start, int Finish, Type type){
		this.start = Start;
		this.finish = Finish;
		this.type = type;
	}

	public boolean isSpecial() {
		return type == Type.SPECIAL;
	}

}
