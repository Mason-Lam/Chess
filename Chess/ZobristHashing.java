package Chess;

import static Chess.Constants.PositionConstants.KINGSIDE;
import static Chess.Constants.PositionConstants.QUEENSIDE;
import static Chess.BoardUtil.getColumn;
import static Chess.Constants.PositionConstants.EMPTY;

import java.util.*;

import Chess.Constants.PieceConstants.PieceColor;

public class ZobristHashing {

    private static final int NUM_PIECES = 12;
    private static final int NUM_SQUARES = 64;

    private long[][] zobristTable = new long[NUM_PIECES][NUM_SQUARES];
    private long[] zobristCastling = new long[16];   // 4 bits of castling rights
    private long[] zobristEnPassant = new long[9];    // file 0–7 or no en-passant (-1 = index 8)
    private long zobristBlackToMove;

    private long currentHash = 0;

    private final ChessBoard board;
    private int castlingRights = 0b1111;     // 4 bits: KQkq
    private int enPassantFile = -1;          // File where en passant is possible (-1 = none)
    private boolean whiteToMove = true;

    public ZobristHashing(ChessBoard board) {
        this.board = board;
        initZobristTable(board);
        computeFullHash();
    }


    private void initZobristTable(ChessBoard board) {
        Random rand = new Random(42);
        for (int p = 0; p < NUM_PIECES; p++) {
            for (int s = 0; s < NUM_SQUARES; s++) {
                zobristTable[p][s] = rand.nextLong();
            }
        }
        for (int i = 0; i < 16; i++) {
            zobristCastling[i] = rand.nextLong();
        }
        for (int i = 0; i < 9; i++) {
            zobristEnPassant[i] = rand.nextLong();
        }
        zobristBlackToMove = rand.nextLong();

        whiteToMove = board.getTurn() == PieceColor.WHITE;

        final boolean[] whiteCastlingRights = board.getCastlingPotential(PieceColor.WHITE);
        final boolean[] blackCastlingRights = board.getCastlingPotential(PieceColor.BLACK);

        castlingRights = 0;
        if (whiteCastlingRights[KINGSIDE]) castlingRights |= 0b1000; // White kingside
        if (whiteCastlingRights[QUEENSIDE]) castlingRights |= 0b0100; // White queenside
        if (blackCastlingRights[KINGSIDE]) castlingRights |= 0b0010; // Black kingside
        if (blackCastlingRights[QUEENSIDE]) castlingRights |= 0b0001; // Black queenside

        if (board.getEnPassant() == EMPTY) enPassantFile = -1;
		else {
            enPassantFile = getColumn(board.getEnPassant());
		} 
    }


    public void computeFullHash() {
        currentHash = 0;

        // Piece positions
        for (int square = 0; square < NUM_SQUARES; square++) {
            final ChessPiece piece = board.getPiece(square);
            if (piece.isEmpty()) continue;
            int tableIndex = piece.getType().arrayIndex + (piece.color == PieceColor.WHITE ? 0 : 6);
            currentHash ^= zobristTable[tableIndex][square];
        }

        // Castling rights
        currentHash ^= zobristCastling[castlingRights];

        // En passant
        int epIndex = (enPassantFile == -1) ? 8 : enPassantFile;
        currentHash ^= zobristEnPassant[epIndex];

        // Side to move
        if (!whiteToMove) {
            currentHash ^= zobristBlackToMove;
        }
    }

    public void toggleSideToMove() {
        whiteToMove = !whiteToMove;
        currentHash ^= zobristBlackToMove;
    }

    public void setCastlingRights(int newRights) {
        currentHash ^= zobristCastling[castlingRights]; // XOR out old
        castlingRights = newRights;
        currentHash ^= zobristCastling[castlingRights]; // XOR in new
    }

    public void setCastlingRights(PieceColor color, boolean[] values) {
        setCastlingRights(color, QUEENSIDE, values[QUEENSIDE]);
        setCastlingRights(color, KINGSIDE, values[KINGSIDE]);
    }

    public void setCastlingRights(PieceColor color, int side, boolean value) {
        // XOR out the current castling state
        currentHash ^= zobristCastling[castlingRights];

        // Bitmask positions
        // White K = 0b1000, White Q = 0b0100, Black K = 0b0010, Black Q = 0b0001
        int mask = 0;
        if (color == PieceColor.WHITE && side == KINGSIDE) mask = 0b1000;
        else if (color == PieceColor.WHITE && side == QUEENSIDE) mask = 0b0100;
        else if (color == PieceColor.BLACK && side == KINGSIDE) mask = 0b0010;
        else if (color == PieceColor.BLACK && side == QUEENSIDE) mask = 0b0001;

        // Set or clear the bit
        if (value) {
            castlingRights |= mask;
        } else {
            castlingRights &= ~mask;
        }

        // XOR in the new castling state
        currentHash ^= zobristCastling[castlingRights];
    }


    public void setEnPassantFile(int newFile) {
        int oldIndex = (enPassantFile == -1) ? 8 : enPassantFile;
        int newIndex = (newFile == -1) ? 8 : newFile;
        currentHash ^= zobristEnPassant[oldIndex]; // XOR out old
        enPassantFile = newFile;
        currentHash ^= zobristEnPassant[newIndex]; // XOR in new
    }

    public void flipPiece(int square, ChessPiece piece) {
        int tableIndex = piece.getType().arrayIndex + (piece.color == PieceColor.WHITE ? 0 : 6);
        currentHash ^= zobristTable[tableIndex][square];
    }

    public long getHash() {
        return currentHash;
    }
}
