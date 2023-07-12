package Chess;

import java.util.Iterator;

public class MoveList implements Iterable<Move> {

    private final Move[] moves;
    private int index;

    public MoveList() {
        this(Constants.EMPTY);
    }

    public MoveList(byte type) {
        int size = 0;
        switch(type) {
            case Constants.PAWN: size = 4;
                break;
            case Constants.KNIGHT: size = 8;
                break;
            case Constants.BISHOP: size = 13;
                break;
            case Constants.ROOK: size = 14;
                break;
            case Constants.QUEEN: size = 27;
                break;
            case Constants.KING: size = 9;
                break;
            default: size = 0;
                break;
        }
        moves = new Move[size];
        index = 0;
    }

    public Move get(int index) {
        return moves[index];
    }

    public void add(Move move) {
        moves[index] = move;
        index ++;
    }

    public int size() {
        return index;
    }

    @Override
    public MoveIterator iterator() {
        return new MoveIterator(this);
    }

    public class MoveIterator implements Iterator<Move> {
        private MoveList list;
        private int currentIndex;

        public MoveIterator(MoveList list) {
            this.list = list;
            currentIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < list.index;
        }

        @Override
        public Move next() {
            currentIndex ++;
            return list.moves[currentIndex - 1];
        }

    }
}
