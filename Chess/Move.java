package Chess;

import java.util.Objects;

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

	@Override
	public boolean equals(Object anObject) {
		if (this == anObject) return true;
		if (anObject instanceof Move) {
			final Move aMove = (Move) anObject;
			return (start == aMove.start && finish == aMove.finish && type == aMove.type);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, finish, type);
	}

}
