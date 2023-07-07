package Chess;

import java.util.Iterator;

public class PieceSet implements Iterable<ChessPiece> {
    private ChessPiece[] map;
    private int size;

    public PieceSet() {
        map = new ChessPiece[16];
        size = 0;
    }

    public void addAll(PieceSet pieceSet) {
        for (final ChessPiece piece : pieceSet) {
            add(piece);
        }
    }

    public void add(ChessPiece piece) {
        if (map[piece.pieceID] == null) size ++;
        map[piece.pieceID] = piece;
    }

    public void remove(ChessPiece piece) {
        if (map[piece.pieceID] == null) return;
        map[piece.pieceID] = null;
        size --;
    }

    public int size() {
        return size;
    }

    public PieceSet clone() {
        final PieceSet pieceSet = new PieceSet();
        for (final ChessPiece piece : this) {
            pieceSet.add(piece);
        }
        return pieceSet;
    }

    public class PieceEntry {
        public PieceEntry nextNode;
        public PieceEntry prevNode;
        public ChessPiece piece;

        public PieceEntry(ChessPiece piece, PieceEntry prevNode) {
            this.piece = piece;
            this.prevNode = prevNode;
            nextNode = null;
        }
    }

    public class PieceMapIterator implements Iterator<ChessPiece> {
        private PieceSet map;
        private int currentIndex;
        private int cursor;

        public PieceMapIterator(PieceSet map) {
            this.map = map;
            currentIndex = 0;
            cursor = 0;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < map.size();
        }

        @Override
        public ChessPiece next() {
            while (true) {
                final ChessPiece piece = map.map[cursor];
                cursor ++;
                if (piece != null) {
                    currentIndex ++;
                    return piece;
                }
            }
        }
        
    }

    @Override
    public PieceMapIterator iterator() {
        return new PieceMapIterator(this);
    }
}
