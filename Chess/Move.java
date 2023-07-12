package Chess;

public class Move {
	
	public enum Type {
		ATTACK,
		MOVE,
		SPECIAL
	}
	

	private final int moveID;
	public final int start;
	public final int finish;
	public final Type type;

	public Move(int start, int finish, Type type) {
		this.start = start;
		this.finish = finish;
		this.type = type;
		int typeAdd = 0;
		if (type == Type.ATTACK) {
			typeAdd = 1;
		}
		if (type == Type.SPECIAL) {
			typeAdd = 2;
		}
		moveID = typeAdd + (start << 2) + (finish << 8);
	}

	public Move invert() {
		return new Move(finish, start, type);
	}

	// public int getFinish() {
	// 	return finish;
	// 	//return moveID >> 8;
	// }

	// public int getStart () {
	// 	return start;
	// 	//return 63 & (moveID >> 2);
	// }

	// public Type getType() {
	// 	return type;
	// 	// final int typeID = 3 & moveID;
	// 	// if (typeID == 2) {
	// 	// 	return Type.SPECIAL;
	// 	// }
	// 	// if (typeID == 1) {
	// 	// 	return Type.ATTACK;
	// 	// }
	// 	// return Type.MOVE;
	// }

	public boolean isSpecial() {
		return type == Type.SPECIAL;
	}

	@Override
	public boolean equals(Object anObject) {
		if (this == anObject) return true;
		if (anObject instanceof Move) {
			final Move aMove = (Move) anObject;
			//return (aMove.start == start && aMove.finish == finish && aMove.type == type);
			return aMove.moveID == moveID;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return moveID;
	}

}
