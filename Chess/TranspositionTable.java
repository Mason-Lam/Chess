package Chess;

public class TranspositionTable {
    public class TTEntry {
        public long key;
        public int depth;
        public int score;
        public int flag;
        public int bestMove;

        public static final int EXACT = 0;
        public static final int LOWER_BOUND = 1;
        public static final int UPPER_BOUND = 2;

        public void set(long key, int depth, int score, int flag, int bestMove) {
            this.key = key;
            this.depth = depth;
            this.score = score;
            this.flag = flag;
            this.bestMove = bestMove;
        }
    }

    private final TTEntry[] table;
    private final int sizeMask;

    public TranspositionTable(int sizePowerOf2) {
        int size = 1 << sizePowerOf2; // e.g. 2^20 = 1M entries
        table = new TTEntry[size];
        sizeMask = size - 1;
        for (int i = 0; i < size; i++) {
            table[i] = new TTEntry();
        }
    }

    private int index(long key) {
        return (int)(key) & sizeMask;
    }

    public TTEntry lookup(long key) {
        TTEntry entry = table[index(key)];
        if (entry.key == key) {
            return entry;
        }
        return null;
    }

    public void store(long key, int depth, int score, int flag, int bestMove) {
        int idx = index(key);
        TTEntry entry = table[idx];

        // Replace if slot is empty or the new entry is deeper
        if (entry.key != key || depth >= entry.depth) {
            entry.set(key, depth, score, flag, bestMove);
        }
    }

    public void clear() {
        for (TTEntry entry : table) {
            entry.key = 0;
            entry.depth = 0;
        }
    }
}
