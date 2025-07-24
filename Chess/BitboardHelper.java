package Chess;

import static Chess.BoardUtil.*;
import static Chess.Constants.DirectionConstants.*;
import static Chess.Constants.DirectionConstants.Direction.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.Constants.PositionConstants.*;
import static Chess.Constants.PieceConstants.PieceColor.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;

public class BitboardHelper {
    public static final long RANK1 = 0x00000000000000FFL;
    public static final long RANK2 = 0x000000000000FF00L;
    public static final long RANK3 = 0x0000000000FF0000L;
    public static final long RANK4 = 0x00000000FF000000L;
    public static final long RANK5 = 0x000000FF00000000L;
    public static final long RANK6 = 0x0000FF0000000000L;
    public static final long RANK7 = 0x00FF000000000000L;
    public static final long RANK8 = 0xFF00000000000000L;

    public static final long RANKS[] = {
        RANK1, RANK2, RANK3, RANK4, RANK5, RANK6, RANK7, RANK8
    };

    public static final long FILE_A = 0x0101010101010101L;
    public static final long FILE_B = 0x0202020202020202L;
    public static final long FILE_C = 0x0404040404040404L;
    public static final long FILE_D = 0x0808080808080808L;
    public static final long FILE_E = 0x1010101010101010L;
    public static final long FILE_F = 0x2020202020202020L;
    public static final long FILE_G = 0x4040404040404040L;
    public static final long FILE_H = 0x8080808080808080L;

    public static final long[] FILES = {
        FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F, FILE_G, FILE_H
    };

    public static final long[][] PAWN_MOVES = new long[2][64];

    public static final long[][] PAWN_ATTACKS = new long[2][64];

    public static final long[] KNIGHT_MOVES = new long[64];

    public static final long[] KING_MOVES = new long[64];
    public static final long[][] KING_CASTLE_EMPTY_SQUARES = new long[2][2];
    public static final long[][] KING_CASTLE_ATTACK_SQUARES = new long[2][2];

    public static final long[] BISHOP_MASKS = new long[64];
    public static final long[] ROOK_MASKS = new long[64];

    public static final long[] BISHOP_MAGIC_NUMBERS = new long[64];
    public static final long[] ROOK_MAGIC_NUMBERS = new long[64];

    public static final long[][] BISHOP_MOVES = new long[64][];
    public static final long[][] ROOK_MOVES = new long[64][];

    public static void initializeBitBoard() {
        
        readMagicNumberFile();

        // findMagicNumbers(PieceType.BISHOP);
        // findMagicNumbers(PieceType.ROOK);

        for (int square = 0; square < 64; square ++) {
            initPawnMoves(square);
            initKnightMoves(square);
            initKingMoves(square);
            initBishopMask(square);
            initRookMask(square);

            initBishopMoves(square);
            initRookMoves(square);
        }
        initKingCastleMoves();
        // final int square = 0;
        // final int blockerPermutation = 24;
        // final long mask = BISHOP_MASKS[square];
        // final long blockers = generateBlockerPermutations(mask)[blockerPermutation];
        // final long moves = BISHOP_MOVES[square][(int) ((blockers * BISHOP_MAGIC_NUMBERS[square]) >>> (64 - Long.bitCount(mask)))];
        // displayBitboard(mask);
        // System.out.println();
        // displayBitboard(moves);
        // System.out.println();
        // displayBitboard(generateBlockerPermutations(mask)[blockerPermutation]);

    }

    private static void initPawnMoves(int square) {
        for (final PieceColor color : PIECE_COLORS) {
            final long b = 1L << square;
            long moves = 0;
            long attacks = 0;
            if (color == WHITE) {
                moves |= b >>> UP.absoluteArrayValue;
                moves |= (b & RANKS[PAWN_STARTING_ROW[color.arrayIndex]]) >>> UP.absoluteArrayValue * 2;
                attacks |= (b & ~FILE_A) >>> UPLEFT.absoluteArrayValue;
                attacks |= (b & ~FILE_H) >>> UPRIGHT.absoluteArrayValue;
            }
            else {
                moves |= b << DOWN.absoluteArrayValue;
                moves |= (b & RANKS[PAWN_STARTING_ROW[color.arrayIndex]]) << DOWN.absoluteArrayValue * 2;
                attacks |= (b & ~FILE_A) << DOWNLEFT.absoluteArrayValue;
                attacks |= (b & ~FILE_H) << DOWNRIGHT.absoluteArrayValue;
            }
            PAWN_MOVES[color.arrayIndex][square] = moves;
            PAWN_ATTACKS[color.arrayIndex][square] = attacks;
        }
    }

    private static void initKnightMoves(int square) {
        final long b = 1L << square;
        long attacks = 0;

        attacks |= (b & ~FILE_H) >>> sumDirectionsAbsolute(UP, UP, RIGHT);
        attacks |= (b & ~FILE_A) >>> sumDirectionsAbsolute(UP, UP, LEFT);
        attacks |= (b & ~FILE_H & ~ FILE_G) >>> sumDirectionsAbsolute(UP, RIGHT, RIGHT);
        attacks |= (b & ~FILE_A & ~FILE_B) >>> sumDirectionsAbsolute(UP, LEFT, LEFT);

        attacks |= (b & ~FILE_H) << sumDirectionsAbsolute(DOWN, DOWN, RIGHT);
        attacks |= (b & ~FILE_A) << sumDirectionsAbsolute(DOWN, DOWN, LEFT);
        attacks |= (b & ~FILE_H & ~ FILE_G) << sumDirectionsAbsolute(DOWN, RIGHT, RIGHT);
        attacks |= (b & ~FILE_A & ~FILE_B) << sumDirectionsAbsolute(DOWN, LEFT, LEFT);

        KNIGHT_MOVES[square] = attacks;
    }

    private static void initKingMoves(int square) {
        final long b = 1L << square;
        long attacks = 0;

        attacks |= (b & ~FILE_A) >>> UPLEFT.absoluteArrayValue;
        attacks |= (b & ~FILE_A) >>> LEFT.absoluteArrayValue;
        attacks |= (b & ~FILE_A) << DOWNLEFT.absoluteArrayValue;

        attacks |= (b & ~FILE_H) >>> UPRIGHT.absoluteArrayValue;
        attacks |= (b & ~FILE_H) << RIGHT.absoluteArrayValue;
        attacks |= (b & ~FILE_H) << DOWNRIGHT.absoluteArrayValue;

        attacks |= b >>> UP.absoluteArrayValue;
        attacks |= b << DOWN.absoluteArrayValue;

        KING_MOVES[square] = attacks;
    }

    private static void initKingCastleMoves() {

        KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][QUEENSIDE] = (1L << BLACK_KING_POS + LEFT.rawArrayValue) | (1L << BLACK_QUEENSIDE_CASTLE_SQUARE);
        KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][KINGSIDE] = (1L << BLACK_KING_POS + RIGHT.rawArrayValue) | (1L << BLACK_KINGSIDE_CASTLE_SQUARE);
        KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][QUEENSIDE] = (1L << WHITE_KING_POS + LEFT.rawArrayValue) | (1L << WHITE_QUEENSIDE_CASTLE_SQUARE);
        KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][KINGSIDE] = (1L << WHITE_KING_POS + RIGHT.rawArrayValue) | (1L << WHITE_KINGSIDE_CASTLE_SQUARE);

        KING_CASTLE_ATTACK_SQUARES[BLACK.arrayIndex][QUEENSIDE] = KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][QUEENSIDE] | (1L << BLACK_KING_POS);
        KING_CASTLE_ATTACK_SQUARES[BLACK.arrayIndex][KINGSIDE] = KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][KINGSIDE] | (1L << BLACK_KING_POS);
        KING_CASTLE_ATTACK_SQUARES[WHITE.arrayIndex][QUEENSIDE] = KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][QUEENSIDE] | (1L << WHITE_KING_POS);
        KING_CASTLE_ATTACK_SQUARES[WHITE.arrayIndex][KINGSIDE] = KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][KINGSIDE] | (1L << WHITE_KING_POS);

        KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][QUEENSIDE] |= squareToBitboard(BLACK_KING_POS + LEFT.rawArrayValue * 3);
        KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][QUEENSIDE] |= squareToBitboard(WHITE_KING_POS + LEFT.rawArrayValue * 3);
    }

    private static void initBishopMask(int square) {
        final long squareMask = 1L << square;
        long mask = 0L;

        for (int i = 1; i < getNumSquaresFromEdge(UPLEFT, square); i++) mask |= squareMask >>> UPLEFT.absoluteArrayValue * i;
        for (int i = 1; i < getNumSquaresFromEdge(UPRIGHT, square); i++) mask |= squareMask >>> UPRIGHT.absoluteArrayValue * i;
        for (int i = 1; i < getNumSquaresFromEdge(DOWNLEFT, square); i++) mask |= squareMask << DOWNLEFT.absoluteArrayValue * i;
        for (int i = 1; i < getNumSquaresFromEdge(DOWNRIGHT, square); i++) mask |= squareMask << DOWNRIGHT.absoluteArrayValue * i;

        BISHOP_MASKS[square] = mask;
    }

    private static void initRookMask(int square) {
        final int rank = getRow(square);
        final int file = getColumn(square);
        long mask = RANKS[rank] ^ FILES[file];

        if (rank != 0)
            mask = mask & ~RANKS[0]; // Exclude rank 1
        if (rank != 7)  
            mask = mask & ~RANKS[7]; // Exclude rank 8
        if (file != 0)
            mask = mask & ~FILES[0]; // Exclude file A
        if (file != 7)
            mask = mask & ~FILES[7]; // Exclude file H

        ROOK_MASKS[square] = mask;
    }

    private static void initBishopMoves(int square) {
        final long mask = BISHOP_MASKS[square];
        final long[] blockerPermutations = generateBlockerPermutations(mask);

        final int numRelevantSquares = Long.bitCount(mask);
        final long[] attackTable = new long[1 << numRelevantSquares];
        final long magicNumber = BISHOP_MAGIC_NUMBERS[square];

        final HashSet<Integer> used = new HashSet<Integer>();

        for (final long blockerPermutation : blockerPermutations) {
            final int index = (int)(((blockerPermutation) * magicNumber) >>> (64 - numRelevantSquares));
            if (!used.add(index)) {
                System.out.println("Bishop error on square " + square);
            }
            attackTable[index] = generateAttacksFrom(DIAGONAL_DIRECTIONS, blockerPermutation, square);
        }
        BISHOP_MOVES[square] = attackTable;
    }

    private static void initRookMoves(int square) {
        final long mask = ROOK_MASKS[square];
        final long[] blockerPermutations = generateBlockerPermutations(mask);

        final int numRelevantSquares = Long.bitCount(mask);
        final long[] attackTable = new long[1 << numRelevantSquares];
        final long magicNumber = ROOK_MAGIC_NUMBERS[square];

        final HashSet<Integer> used = new HashSet<Integer>();

        for (final long blockerPermutation : blockerPermutations) {
            final int index = (int)(((blockerPermutation) * magicNumber) >>> (64 - numRelevantSquares));
            if (!used.add(index)) {
                System.out.println("Rook error on square " + square);
            }
            attackTable[index] = generateAttacksFrom(STRAIGHT_DIRECTIONS, blockerPermutation, square);
        }
        ROOK_MOVES[square] = attackTable;
    }

    private static void readMagicNumberFile() {
        try (DataInputStream in = new DataInputStream(new FileInputStream("Chess//BISHOP_MAGIC_NUMBERS.bin"))) {
            for (int i = 0; i < 64; i++) {
                BISHOP_MAGIC_NUMBERS[i] = in.readLong();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // System.out.println(Arrays.toString(BISHOP_MAGIC_NUMBERS));

        try (DataInputStream in = new DataInputStream(new FileInputStream("Chess//ROOK_MAGIC_NUMBERS.bin"))) {
            for (int i = 0; i < 64; i++) {
                ROOK_MAGIC_NUMBERS[i] = in.readLong();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // System.out.println(Arrays.toString(ROOK_MAGIC_NUMBERS));
    }

    private static void findMagicNumbers(PieceType type) {
        final long[] MAGIC_NUMBERS = type == PieceType.BISHOP ? BISHOP_MAGIC_NUMBERS : ROOK_MAGIC_NUMBERS;
        for (int square = 0; square < 64; square++) {
            if (MAGIC_NUMBERS[square] != 0) {
                continue;
            }
            MAGIC_NUMBERS[square] = findMagicNumber(square, type);
        }

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream("Chess//" + type.name() + "_MAGIC_NUMBERS.bin"))) {
            for (long magic : MAGIC_NUMBERS) {
                out.writeLong(magic);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static long findMagicNumber(int square, PieceType type) {
        final long masks[] = type == PieceType.BISHOP ? BISHOP_MASKS : ROOK_MASKS;
        final long[] blockerPermutations = generateBlockerPermutations(masks[square]);
        final Random random = new Random();
        int count = 0;
        while (true) {
            long magic = random.nextLong() & random.nextLong() & random.nextLong();
            if (isMagic(blockerPermutations, masks[square], magic)) {
                System.out.println("Magic number found for " + type + " on square " + square + ": " + magic);
                return magic;
            }
            count ++;

            if (count % 10000000 == 0) System.out.println(count);
        }
        // for (long count = 5710000000L; count < Long.MAX_VALUE; count++) {
        //     if (isMagic(blockerPermutations, masks[square], count)) {
        //         System.out.println("Magic number found for " + type + " on square " + square + ": " + count);
        //         return count;
        //     }
        //     if (count % 10000000 == 0) System.out.println(count);
        // }
        // System.out.println("No magic number found for " + type + " on square " + square);
        // return 0;
    }

    private static boolean isMagic(long[] blockerPermutations, long mask, long magic) {
        Set<Integer> used = new HashSet<>();
        for (final long blockers : blockerPermutations) {
            int index = (int) ((blockers * magic) >>> (64 - Long.bitCount(mask)));
            if (!used.add(index)) {
                return false; // collision
            }
        }
        return true; // perfect hash
    }


    private static long[] generateBlockerPermutations(long mask) {
        final int numRelevantSquares = Long.bitCount(mask);
        final int numPermutations = 1 << numRelevantSquares;
        final long[] permutations = new long[numPermutations];

        final ArrayList<Integer> bitIndices = getBitIndices(mask);

        for (int i = 0; i < numPermutations; i++) {
            long blockers = 0L;
            for (int j = 0; j < numRelevantSquares; j++) {
                if (((i >>> j) & 1) != 0) {
                    blockers |= 1L << bitIndices.get(j);
                }
            }
            permutations[i] = blockers;
        }
        return permutations;
    }

    private static long generateAttacksFrom(Direction[] directions, long blockers, int square) {
        long attacks = 0L;

        for (final Direction direction : directions) {
            for (int delta = 1; delta < getNumSquaresFromEdge(direction, square) + 1; delta ++) {
                final int newSquare = square + direction.rawArrayValue * delta;
                final long mask = (1L) << newSquare;
                attacks |= mask;
                if ((blockers & mask) != 0) break;
            }
        }

        return attacks;
    }



    public static long setBit(long bitboard, int index) {
        if (index < 0 || index >= 64) {
            throw new IllegalArgumentException("Index must be between 0 and 63.");
        }
        return bitboard |= (1L << index);
    }

    public static long clearBit(long bitboard, int index) {
        if (index < 0 || index >= 64) {
            throw new IllegalArgumentException("Index must be between 0 and 63.");
        }
        bitboard &= ~(1L << index);
        return bitboard;
    }

    public static long squareToBitboard(int index) {
        return 1L << index;
    }

    public static int firstBitToIndex(long bitboard) {
        return Long.numberOfTrailingZeros(bitboard);
    }

    public static boolean isBitSet(long bitboard, int index) {
        if (index < 0 || index >= 64) {
            throw new IllegalArgumentException("Index must be between 0 and 63.");
        }
        return (bitboard & (1L << index)) != 0;
    }

    public static PieceType charToPieceType(char letter) {
		for (final PieceType pieceType : PIECE_TYPES) {
			if (Character.toLowerCase(letter) == pieceType.characterRepresentation) {
				return pieceType;
			}
		}
		throw new IllegalArgumentException("Invalid piece character");
	}

    public static void applyFunctionByBitIndices(long bitboard, IntConsumer consumer) {
        while (bitboard != 0) {
            final int index = Long.numberOfTrailingZeros(bitboard);
            consumer.accept(index);
            bitboard &= bitboard - 1; // Clear the LSB
        }
    }

    public static ArrayList<Integer> getBitIndices(long bitboard) {
        final ArrayList<Integer> indices = new ArrayList<>();
        applyFunctionByBitIndices(bitboard, indices::add);
        return indices;
    }

    public static void displayBitboard(long bitboard) {
        for(int row = 0; row < 8; row ++) {
            for (int col = 0; col < 8; col++) {
                final long mask = (1L) << (row * 8 + col);
                System.out.print(((bitboard & mask) != 0 ? "1" : "0") + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
