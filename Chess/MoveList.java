package Chess;

public class MoveList {

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
}
