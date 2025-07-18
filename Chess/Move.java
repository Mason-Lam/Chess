package Chess;

import java.util.HashMap;

public class Move {

	private final int moveID;
	public final int start;
	public final int finish;
	public final boolean SPECIAL;

	public Move(int start, int finish) {
		this(start, finish, false);
	}

	public Move(int start, int finish, boolean special) {
		this.start = start;
		this.finish = finish;
		this.SPECIAL = special;
		final int typeAdd = SPECIAL ? 1 : 0;
		moveID = typeAdd + (start << 1) + (finish << 7);
	}

	public boolean contains(int square) {
		return start == square || finish == square;
	}

	public Move invert() {
		return new Move(finish, start, SPECIAL);
	}

	@Override
	public String toString() {
		return start + ":" + finish + ":" + SPECIAL;
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
