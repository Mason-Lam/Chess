package Chess;

public class Move {

	private final int moveID;

	public Move(int start, int finish) {
		this(start, finish, false);
	}

	public Move(int start, int finish, boolean special) {
		final int typeAdd = special ? 1 : 0;
		moveID = typeAdd + (start << 1) + (finish << 7);
	}

	public int getStart() {
		return (moveID >>> 1) % 64;
	}

	public int getFinish() {
		return (moveID >>> 7);
	}

	public boolean isSpecial() {
		return moveID % 2 == 1;
	}

	public boolean contains(int square) {
		return getStart() == square || getFinish() == square;
	}

	public Move invert() {
		return new Move(getFinish(), getStart(), isSpecial());
	}

	@Override
	public String toString() {
		return getStart() + ":" + getFinish() + ":" + isSpecial();
	}

	@Override
	public boolean equals(Object anObject) {
		if (this == anObject) return true;
		if (anObject instanceof Move) {
			final Move aMove = (Move) anObject;
			return aMove.moveID == moveID;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return moveID;
	}

}
