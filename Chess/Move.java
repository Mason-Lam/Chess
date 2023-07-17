package Chess;

public class Move {

	private final int moveID;
	public final int start;
	public final int finish;
	public final boolean SPECIAL;

	public Move(int start, int finish, boolean special) {
		this.start = start;
		this.finish = finish;
		this.SPECIAL = special;
		final int typeAdd = SPECIAL ? 1 : 0;
		moveID = typeAdd + (start << 1) + (finish << 7);
	}

	public Move invert() {
		return new Move(finish, start, SPECIAL);
	}

	// public int getFinish() {
	// 	return finish;
	// 	//return moveID >> 7;
	// }

	// public int getStart () {
	// 	return start;
	// 	//return 63 & (moveID >> 1);
	// }

	// public Type getType() {
	// 	return type;
	// 	// final int typeID = 1 & moveID;
	// 	// if (typeID == 2) {
	// 	// 	return Type.SPECIAL;
	// 	// }
	// 	// if (typeID == 1) {
	// 	// 	return Type.ATTACK;
	// 	// }
	// 	// return Type.MOVE;
	// }

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
